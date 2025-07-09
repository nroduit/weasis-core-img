/*
 * Copyright (c) 2023 Weasis Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.weasis.core.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.DefaultLocale;
import org.mockito.Mockito;

class FileUtilTest {

  @Test
  @DisplayName("Should write ImageInputStream data to file successfully")
  void testWriteFileSuccess() throws IOException {
    Path path = Paths.get(System.getProperty("java.io.tmpdir"), "testWriteFile.test");
    byte[] data = "TestImageData".getBytes(StandardCharsets.UTF_8);

    ImageInputStream imageInputStream =
        new MemoryCacheImageInputStream(new ByteArrayInputStream(data));
    try {
      assertEquals(-1, FileUtil.writeFile(imageInputStream, path));
      assertTrue(Files.exists(path));
      assertArrayEquals(data, Files.readAllBytes(path));
    } finally {
      FileUtil.delete(path);
      assertFalse(Files.exists(path));
    }
  }

  @Test
  @DisplayName("Should handle IOException during writing ImageInputStream data")
  void testWriteFileIOException() throws IOException {
    Path path = Paths.get(System.getProperty("java.io.tmpdir"), "testWriteFileIOException.test");
    ImageInputStream imageInputStream = Mockito.mock(ImageInputStream.class);

    try {
      Mockito.when(imageInputStream.read(Mockito.any()))
          .thenThrow(new IOException("Test IOException"));
      assertThrows(StreamIOException.class, () -> FileUtil.writeFile(imageInputStream, path));
      assertFalse(Files.exists(path));
    } finally {
      StreamUtil.safeClose(imageInputStream);
    }
  }

  @Test
  @DisplayName("Should handle InterruptedIOException during writing ImageInputStream data")
  void testWriteFileInterruptedException() throws IOException {
    Path path = Paths.get(System.getProperty("java.io.tmpdir"), "testWriteFileInterrupted.test");
    ImageInputStream imageInputStream = Mockito.mock(ImageInputStream.class);
    InterruptedIOException exception = new InterruptedIOException("Test InterruptedIOException");
    exception.bytesTransferred = 5;

    try {
      Mockito.when(imageInputStream.read(Mockito.any())).thenThrow(exception);
      int bytesWritten = FileUtil.writeFile(imageInputStream, path);
      assertEquals(5, bytesWritten);
      assertFalse(Files.exists(path));
    } finally {
      StreamUtil.safeClose(imageInputStream);
    }
  }

  @Test
  @DisplayName("Should validate file names by removing illegal characters")
  void testGetValidFileName() {
    assertEquals("foo.txt", FileUtil.getValidFileName("foo.txt"));
    assertEquals(StringUtil.EMPTY_STRING, FileUtil.getValidFileName(null));
    char[] c1 = {0, 27, 124};
    assertEquals("foo.txt", FileUtil.getValidFileName(new String(c1) + "/foo.txt"));
  }

  @Test
  @DisplayName("Should validate file names by removing HTML tags and illegal characters")
  void testGetValidFileNameWithoutHTML() {
    assertEquals(
        "This is bold and italic text.",
        FileUtil.getValidFileNameWithoutHTML("<p>This is <b>bold</b> and <i>italic</i> text.</p>"));
    assertEquals(StringUtil.EMPTY_STRING, FileUtil.getValidFileNameWithoutHTML(null));
    assertEquals(
        "This is a line break",
        FileUtil.getValidFileNameWithoutHTML("<html>This is</p> a line <br>break<empty/>"));
  }

  @Test
  @DisplayName("Should safely close XMLStreamWriter without throwing exceptions")
  void testSafeClose() throws XMLStreamException {
    XMLStreamWriter writer = Mockito.mock(XMLStreamWriter.class);
    StreamUtil.safeClose((XMLStreamReader) null);
    Mockito.verify(writer, Mockito.times(0)).close();

    StreamUtil.safeClose(writer);
    Mockito.verify(writer).close();

    Mockito.doThrow(new XMLStreamException()).when(writer).close();
    StreamUtil.safeClose(writer);
    Mockito.verify(writer, Mockito.times(2)).close();
  }

  @Test
  @DisplayName("Should safely close XMLStreamReader without throwing exceptions")
  void testSafeClose2() throws XMLStreamException {
    XMLStreamReader reader = Mockito.mock(XMLStreamReader.class);
    StreamUtil.safeClose((XMLStreamReader) null);
    Mockito.verify(reader, Mockito.times(0)).close();

    StreamUtil.safeClose(reader);
    Mockito.verify(reader).close();

    Mockito.doThrow(new XMLStreamException()).when(reader).close();
    StreamUtil.safeClose(reader);
    Mockito.verify(reader, Mockito.times(2)).close();
  }

  @Test
  @DisplayName("Should create temporary directories and manage file operations")
  void testCreateTempDir() throws IOException {
    assertThrows(IllegalArgumentException.class, () -> FileUtil.createTempDir((Path) null));

    Path path = Paths.get(System.getProperty("java.io.tmpdir"), "tempTest");
    try {
      assertThrows(IllegalArgumentException.class, () -> FileUtil.createTempDir(path));
      Files.createDirectories(path);
      Path folder = FileUtil.createTempDir(path);
      assertTrue(Files.isDirectory(folder));
      Path myFirstFile = folder.resolve("first");
      Path mySecondFile = folder.resolve("second");
      Files.createFile(myFirstFile);
      Files.createFile(mySecondFile);
      ArrayList<Path> files = new ArrayList<>();
      FileUtil.getAllFilesInDirectory(path, files);
      assertEquals(2, files.size());
    } finally {
      FileUtil.delete(path);
      assertFalse(Files.exists(path));
    }
  }

  @Test
  @DisplayName("Should delete files and directories properly")
  void testDelete() throws IOException {
    assertFalse(FileUtil.delete((Path) null));
    assertFalse(FileUtil.delete(Paths.get("pathNotExists")));
    Path directory = Paths.get(System.getProperty("java.io.tmpdir"), "tempTest");
    try {
      Files.createDirectories(directory);
      ArrayList<Path> files = new ArrayList<>();
      Path myFirstFile = directory.resolve("first");
      Path mySecondFile = directory.resolve("second");
      Files.createFile(myFirstFile);
      Files.createFile(mySecondFile);
      FileUtil.getAllFilesInDirectory(directory, files);
      assertEquals(2, files.size());
    } finally {
      assertTrue(FileUtil.delete(directory));
      assertFalse(Files.isReadable(directory));
      assertFalse(FileUtil.delete(directory));
    }
  }

  @Test
  @DisplayName("Should compute output paths and prepare file directories")
  void testGetOutputPath() throws IOException {
    Path directory = Paths.get(System.getProperty("java.io.tmpdir"), "tempTest");
    try {
      Path input = directory.resolve("input").resolve("test.jpg");
      Path output = directory.resolve("output").resolve("test-out.dcm");
      FileUtil.prepareToWriteFile(input);
      FileUtil.prepareToWriteFile(output);
      Files.createFile(input);
      Files.createFile(output);
      Path out = FileUtil.getOutputPath(input, output);
      assertEquals(output, out);

      input = directory.resolve("input").resolve("dir1").resolve("test.jpg");
      output = directory.resolve("output");
      Files.createDirectories(input.getParent());
      Files.createFile(input);
      out = FileUtil.getOutputPath(input, output);
      assertEquals(output.resolve("test.jpg"), out);

      out = FileUtil.getOutputPath(input.getParent(), output);
      assertEquals(output.resolve("dir1"), out);
    } finally {
      FileUtil.recursiveDelete(directory);
      assertFalse(Files.isReadable(directory));
    }
  }

  @Test
  @DisplayName("Should add file index to file names with proper formatting")
  void testAddFileIndex() {
    Path path = Paths.get("temp", "folder");
    assertEquals(path.resolve("test.jpg"), FileUtil.addFileIndex(path.resolve("test.jpg"), 5, 0));
    assertEquals(path.resolve("test-5.jpg"), FileUtil.addFileIndex(path.resolve("test.jpg"), 5, 1));
    assertEquals(
        path.resolve("test-505.jpg"), FileUtil.addFileIndex(path.resolve("test.jpg"), 505, 1));
    assertEquals(
        path.resolve("test-005.jpg"), FileUtil.addFileIndex(path.resolve("test.jpg"), 5, 3));
    assertEquals(
        path.resolve("test-985.jpg"), FileUtil.addFileIndex(path.resolve("test.jpg"), 985, 3));
  }

  @Test
  @DisplayName("Should extract file names without extensions")
  void testNameWithoutExtension() {
    assertEquals("folder/fileNoExtension", FileUtil.nameWithoutExtension("folder/fileNoExtension"));
    assertNull(FileUtil.nameWithoutExtension(null));
    assertEquals("file.archive", FileUtil.nameWithoutExtension("file.archive.jpg"));
  }

  @Test
  @DisplayName("Should extract file extensions correctly")
  void testGetExtension() {
    assertEquals(StringUtil.EMPTY_STRING, FileUtil.getExtension("filename"));
    assertEquals(StringUtil.EMPTY_STRING, FileUtil.getExtension(null));
    assertEquals(".zip", FileUtil.getExtension("filename-archive.zip"));
  }

  @Test
  @DisplayName("Should check file extension matching correctly")
  void testIsFileExtensionMatching() {
    String[] fileExtensions = {"dcm", "dic", "dicm", "dicom"};
    assertFalse(FileUtil.isFileExtensionMatching((Path) null, null));
    assertFalse(
        FileUtil.isFileExtensionMatching(
            Paths.get("test.dcm"), new String[] {StringUtil.EMPTY_STRING}));
    assertFalse(FileUtil.isFileExtensionMatching(Paths.get("test.dcm"), null));
    assertFalse(FileUtil.isFileExtensionMatching(Paths.get("test"), fileExtensions));
    assertFalse(FileUtil.isFileExtensionMatching(Paths.get("test.d"), fileExtensions));
    assertTrue(FileUtil.isFileExtensionMatching(Paths.get("test.dcm"), fileExtensions));
    assertTrue(FileUtil.isFileExtensionMatching(Paths.get("test.DCM"), fileExtensions));
    assertTrue(FileUtil.isFileExtensionMatching(Paths.get("test.dcm"), new String[] {".dcm"}));
    assertTrue(FileUtil.isFileExtensionMatching(Paths.get("test.dcm"), new String[] {"DCM"}));
  }

  @Test
  @DisplayName("Should write input streams to files with exception handling")
  void testWriteStreamWithIOException() throws IOException {
    Path path = Paths.get(System.getProperty("java.io.tmpdir"), "testWriteStream.test");
    try (FileInputStream inputStream = new FileInputStream(new FileDescriptor())) {
      assertThrows(
          StreamIOException.class, () -> FileUtil.writeStreamWithIOException(inputStream, path));
      // File is automatically deleted when an error occurs
      assertFalse(Files.exists(path));
    }

    byte[] data = "AXAXAXAX".getBytes(StandardCharsets.UTF_8);
    FileUtil.writeStreamWithIOException(new ByteArrayInputStream(data), path);
    assertTrue(Files.exists(path));
    assertEquals(Files.size(path), data.length);
    assertTrue(FileUtil.delete(path));
  }

  @Test
  @DisplayName("Should write input streams to files")
  void testWriteStream() throws IOException {
    Path path = Paths.get(System.getProperty("java.io.tmpdir"), "testWriteStream.test");
    try (FileInputStream inputStream = new FileInputStream(new FileDescriptor())) {
      assertThrows(StreamIOException.class, () -> FileUtil.writeStream(inputStream, path));
      // File is automatically deleted when an error occurs
      assertFalse(Files.exists(path));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    byte[] data = "AXAXAXAX".getBytes(StandardCharsets.UTF_8);
    assertEquals(-1, FileUtil.writeStream(new ByteArrayInputStream(data), path, false));
    assertTrue(Files.exists(path));
    assertEquals(Files.size(path), data.length);
    assertTrue(FileUtil.delete(path));
    assertFalse(Files.exists(path));
  }

  @Test
  @DisplayName("Should write input streams to path with proper error handling")
  void testWriteStream2() throws IOException {
    Path path = Paths.get(System.getProperty("java.io.tmpdir"), "testWriteStream.test");
    try (FileInputStream inputStream = new FileInputStream(new FileDescriptor())) {
      assertThrows(StreamIOException.class, () -> FileUtil.writeStream(inputStream, path, true));
      // File is automatically deleted when an error occurs
      assertFalse(Files.isReadable(path));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    byte[] data = "AXAXAXAX".getBytes(StandardCharsets.UTF_8);
    assertEquals(-1, FileUtil.writeStream(new ByteArrayInputStream(data), path, false));
    assertTrue(Files.isReadable(path));
    assertEquals(Files.size(path), data.length);
    assertTrue(FileUtil.delete(path));
    assertFalse(Files.isReadable(path));
  }

  @Test
  @DefaultLocale(language = "en", country = "US")
  @DisplayName("Should format byte counts in human readable format")
  void testHumanReadableByte() {
    assertEquals("1 B", FileUtil.humanReadableByte(1L, true));
    assertEquals("1 B", FileUtil.humanReadableByte(1L, false));
    assertEquals("1.0 kB", FileUtil.humanReadableByte(1000L, true));
    assertEquals("1.0 KiB", FileUtil.humanReadableByte(1024L, false));
    assertEquals("1.3 GB", FileUtil.humanReadableByte(1256799945L, true));
    assertEquals("1.2 GiB", FileUtil.humanReadableByte(1256799945L, false));
    assertEquals("-9.2 EB", FileUtil.humanReadableByte(Long.MIN_VALUE, true));
    assertEquals("-8.0 EiB", FileUtil.humanReadableByte(Long.MIN_VALUE, false));
  }

  @Test
  @DisplayName("Should copy folders recursively with proper file counting")
  void testCopyFolder() throws IOException {
    Path directory = Paths.get(System.getProperty("java.io.tmpdir"), "tempTest");
    try {
      Path input = directory.resolve("src-copy").resolve("dir1");
      Path output = directory.resolve("dst-copy");
      Files.createDirectories(input);
      Path file1 = input.resolve("test1.jpg");
      Path file2 = input.resolve("test2.jpg");
      Files.createFile(file1);
      Files.createFile(file2);

      ArrayList<Path> files = new ArrayList<>();
      FileUtil.copyFolder(input.getParent(), output);
      assertTrue(Files.isReadable(output.resolve("dir1")));
      FileUtil.getAllFilesInDirectory(output, files);
      assertEquals(2, files.size());
    } finally {
      FileUtil.deleteDirectoryContents(directory, 0, 5);
      assertFalse(Files.isReadable(directory));
    }
  }
}
