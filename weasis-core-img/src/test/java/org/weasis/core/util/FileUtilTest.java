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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.DefaultLocale;
import org.mockito.Mockito;

class FileUtilTest {

  /** Method under test: {@link FileUtil#getValidFileName(String)} */
  @Test
  void testGetValidFileName() {
    assertEquals("foo.txt", FileUtil.getValidFileName("foo.txt"));
    assertEquals(StringUtil.EMPTY_STRING, FileUtil.getValidFileName(null));
    char[] c1 = {0, 27, 124};
    assertEquals("foo.txt", FileUtil.getValidFileName(new String(c1) + "/foo.txt"));
  }

  /** Method under test: {@link FileUtil#getValidFileNameWithoutHTML(String)} */
  @Test
  void testGetValidFileNameWithoutHTML() {
    assertEquals(
        "This is bold and italic text.",
        FileUtil.getValidFileNameWithoutHTML("<p>This is <b>bold</b> and <i>italic</i> text.</p>"));
    assertEquals(StringUtil.EMPTY_STRING, FileUtil.getValidFileNameWithoutHTML(null));
    assertEquals(
        "This is a line break",
        FileUtil.getValidFileNameWithoutHTML("<html>This is</p> a line <br>break<empty/>"));
  }

  /** Method under test: {@link FileUtil#safeClose(XMLStreamWriter)} */
  @Test
  void testSafeClose() throws XMLStreamException {
    XMLStreamWriter writer = Mockito.mock(XMLStreamWriter.class);
    FileUtil.safeClose((XMLStreamReader) null);
    Mockito.verify(writer, Mockito.times(0)).close();

    FileUtil.safeClose(writer);
    Mockito.verify(writer).close();

    Mockito.doThrow(new XMLStreamException()).when(writer).close();
    FileUtil.safeClose(writer);
    Mockito.verify(writer, Mockito.times(2)).close();
  }

  /** Method under test: {@link FileUtil#safeClose(XMLStreamReader)} */
  @Test
  void testSafeClose2() throws XMLStreamException {
    XMLStreamReader reader = Mockito.mock(XMLStreamReader.class);
    FileUtil.safeClose((XMLStreamReader) null);
    Mockito.verify(reader, Mockito.times(0)).close();

    FileUtil.safeClose(reader);
    Mockito.verify(reader).close();

    Mockito.doThrow(new XMLStreamException()).when(reader).close();
    FileUtil.safeClose(reader);
    Mockito.verify(reader, Mockito.times(2)).close();
  }

  /**
   * Method under test:
   *
   * <ul>
   *   <li>{@link FileUtil#createTempDir(File)}
   *   <li>{@link FileUtil#delete(File)}
   *   <li>{@link FileUtil#getAllFilesInDirectory(File, List)}
   * </ul>
   */
  @Test
  void testCreateTempDir() throws IOException {
    assertThrows(IllegalStateException.class, () -> FileUtil.createTempDir(null));

    File file = Paths.get(System.getProperty("java.io.tmpdir"), "tempTest").toFile();
    try {
      assertThrows(IllegalStateException.class, () -> FileUtil.createTempDir(file));
      file.mkdirs();
      File folder = FileUtil.createTempDir(file);
      assertTrue(folder.isDirectory());
      File myFirstFile = new File(folder, "first");
      File mySecondFile = new File(folder, "second"); // NON-NLS
      assertTrue(myFirstFile.createNewFile());
      assertTrue(mySecondFile.createNewFile());
      ArrayList<File> files = new ArrayList<>();
      FileUtil.getAllFilesInDirectory(file, files);
      assertEquals(2, files.size());
    } finally {
      FileUtil.delete(file);
      assertFalse(file.exists());
    }
  }

  /** Method under test: {@link FileUtil#delete(Path)} */
  @Test
  void testDelete() throws IOException {
    assertFalse(FileUtil.delete((File) null));
    assertFalse(FileUtil.delete(new File("pathNotExists")));
    assertFalse(FileUtil.deleteFile(new File("pathNotExists")));
    Path directory = Paths.get(System.getProperty("java.io.tmpdir"), "tempTest");
    try {
      Files.createDirectories(directory);
      ArrayList<File> files = new ArrayList<>();
      File myFirstFile = new File(directory.toString(), "first");
      File mySecondFile = new File(directory.toString(), "second"); // NON-NLS
      assertTrue(myFirstFile.createNewFile());
      assertTrue(mySecondFile.createNewFile());
      FileUtil.getAllFilesInDirectory(directory.toFile(), files);
      assertEquals(2, files.size());
    } finally {
      assertFalse(FileUtil.deleteFile(directory));
      assertTrue(FileUtil.delete(directory));
      assertFalse(Files.isReadable(directory));
      assertFalse(FileUtil.delete(directory));
    }
  }

  /**
   * Method under test:
   *
   * <ul>
   *   <li>{@link FileUtil#getOutputPath(Path, Path)}
   *   <li>{@link FileUtil#prepareToWriteFile(File)}
   * </ul>
   */
  @Test
  void testGetOutputPath() throws IOException {
    Path directory = Paths.get(System.getProperty("java.io.tmpdir"), "tempTest");
    try {
      Path input = Paths.get(directory.toString(), "input", "test.jpg");
      Path output = Paths.get(directory.toString(), "output", "test-out.dcm");
      FileUtil.prepareToWriteFile(input.toFile());
      FileUtil.prepareToWriteFile(output.toFile());
      Files.createFile(input);
      Files.createFile(output);
      Path out = FileUtil.getOutputPath(input, output);
      assertEquals(output, out);

      input = Paths.get(directory.toString(), "input", "dir1", "test.jpg");
      output = Paths.get(directory.toString(), "output");
      Files.createDirectories(input.getParent());
      Files.createFile(input);
      out = FileUtil.getOutputPath(input, output);
      assertEquals(Paths.get(output.toString(), "test.jpg"), out);

      out = FileUtil.getOutputPath(input.getParent(), output);
      assertEquals(Paths.get(output.toString(), "dir1"), out);
    } finally {
      FileUtil.recursiveDelete(directory.toFile());
      assertFalse(Files.isReadable(directory));
    }
  }

  /** Method under test: {@link FileUtil#addFileIndex(Path, int, int)} */
  @Test
  void testAddFileIndex() {
    Path path = Paths.get("temp", "folder");
    assertEquals(
        Paths.get(path.toString(), "test.jpg"),
        FileUtil.addFileIndex(Paths.get(path.toString(), "test.jpg"), 5, 0));
    assertEquals(
        Paths.get(path.toString(), "test-5.jpg"),
        FileUtil.addFileIndex(Paths.get(path.toString(), "test.jpg"), 5, 1));
    assertEquals(
        Paths.get(path.toString(), "test-505.jpg"),
        FileUtil.addFileIndex(Paths.get(path.toString(), "test.jpg"), 505, 1));
    assertEquals(
        Paths.get(path.toString(), "test-005.jpg"),
        FileUtil.addFileIndex(Paths.get(path.toString(), "test.jpg"), 5, 3));
    assertEquals(
        Paths.get(path.toString(), "test-985.jpg"),
        FileUtil.addFileIndex(Paths.get(path.toString(), "test.jpg"), 985, 3));
  }

  /** Method under test: {@link FileUtil#nameWithoutExtension(String)} */
  @Test
  void testNameWithoutExtension() {
    assertEquals("folder/fileNoExtension", FileUtil.nameWithoutExtension("folder/fileNoExtension"));
    assertNull(FileUtil.nameWithoutExtension(null));
    assertEquals("file.archive", FileUtil.nameWithoutExtension("file.archive.jpg"));
  }

  /** Method under test: {@link FileUtil#getExtension(String)} */
  @Test
  void testGetExtension() {
    assertEquals(StringUtil.EMPTY_STRING, FileUtil.getExtension("filename"));
    assertEquals(StringUtil.EMPTY_STRING, FileUtil.getExtension(null));
    assertEquals(".zip", FileUtil.getExtension("filename-archive.zip"));
  }

  /** Method under test: {@link FileUtil#isFileExtensionMatching(File, String[])} */
  @Test
  void testIsFileExtensionMatching() {
    String[] FILE_EXTENSIONS = {"dcm", "dic", "dicm", "dicom"};
    assertFalse(FileUtil.isFileExtensionMatching(null, null));
    assertFalse(
        FileUtil.isFileExtensionMatching(
            new File("test.dcm"), new String[] {StringUtil.EMPTY_STRING}));
    assertFalse(FileUtil.isFileExtensionMatching(new File("test.dcm"), null));
    assertFalse(FileUtil.isFileExtensionMatching(new File("test"), FILE_EXTENSIONS));
    assertFalse(FileUtil.isFileExtensionMatching(new File("test.d"), FILE_EXTENSIONS));
    assertTrue(FileUtil.isFileExtensionMatching(new File("test.dcm"), FILE_EXTENSIONS));
    assertTrue(FileUtil.isFileExtensionMatching(new File("test.DCM"), FILE_EXTENSIONS));
    assertTrue(FileUtil.isFileExtensionMatching(new File("test.dcm"), new String[] {".dcm"}));
    assertTrue(FileUtil.isFileExtensionMatching(new File("test.dcm"), new String[] {"DCM"}));
  }

  /** Method under test: {@link FileUtil#writeStreamWithIOException(InputStream, File)} */
  @Test
  void testWriteStreamWithIOException() throws StreamIOException {
    File file = Paths.get(System.getProperty("java.io.tmpdir"), "testWriteStream.test").toFile();
    try (FileInputStream inputStream = new FileInputStream(new FileDescriptor())) {
      assertThrows(
          StreamIOException.class, () -> FileUtil.writeStreamWithIOException(inputStream, file));
      // File is automatically deleted when an error occurs
      assertFalse(file.exists());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    byte[] data = "AXAXAXAX".getBytes(StandardCharsets.UTF_8);
    FileUtil.writeStreamWithIOException(new ByteArrayInputStream(data), file);
    assertTrue(file.exists());
    assertEquals(file.length(), data.length);
    assertTrue(FileUtil.delete(file));
  }

  /** Method under test: {@link FileUtil#writeStream(InputStream, File)} */
  @Test
  void testWriteStream() throws StreamIOException {
    File file = Paths.get(System.getProperty("java.io.tmpdir"), "testWriteStream.test").toFile();
    try (FileInputStream inputStream = new FileInputStream(new FileDescriptor())) {
      assertThrows(StreamIOException.class, () -> FileUtil.writeStream(inputStream, file));
      // File is automatically deleted when an error occurs
      assertFalse(file.exists());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    byte[] data = "AXAXAXAX".getBytes(StandardCharsets.UTF_8);
    assertEquals(-1, FileUtil.writeStream(new ByteArrayInputStream(data), file, false));
    assertTrue(file.exists());
    assertEquals(file.length(), data.length);
    assertTrue(FileUtil.delete(file));
    assertFalse(file.exists());
  }

  /** Method under test: {@link FileUtil#writeStream(InputStream, Path, boolean)} */
  @Test
  void testWriteStream2() throws IOException {
    Path path = Paths.get(System.getProperty("java.io.tmpdir"), "testWriteStream.test");
    try (FileInputStream inputStream = new FileInputStream(new FileDescriptor())) {
      assertFalse(FileUtil.writeStream(inputStream, path, true));
      // File is automatically deleted when an error occurs
      assertFalse(Files.isReadable(path));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    byte[] data = "AXAXAXAX".getBytes(StandardCharsets.UTF_8);
    assertTrue(FileUtil.writeStream(new ByteArrayInputStream(data), path, false));
    assertTrue(Files.isReadable(path));
    assertEquals(Files.size(path), data.length);
    assertTrue(FileUtil.delete(path));
    assertFalse(Files.isReadable(path));
  }

  /**
   * Method under test:
   *
   * <ul>
   *   <li>{@link FileUtil#writeFile(ImageInputStream, File)}
   *   <li>{@link FileUtil#nioWriteFile(FileInputStream, FileOutputStream)}
   *   <li>{@link FileUtil#nioWriteFile(InputStream, OutputStream, int)}
   *   <li>{@link FileUtil#nioCopyFile(File, File)}
   * </ul>
   */
  @Test
  void testWriteFileImageInputStream() throws IOException {
    assertFalse(FileUtil.nioWriteFile(null, null));
    assertFalse(FileUtil.nioWriteFile(null, null, 4));
    assertFalse(FileUtil.nioCopyFile(null, null));

    File file = Paths.get(System.getProperty("java.io.tmpdir"), "testImageStream.test").toFile();
    byte[] data = "AXAXAXAX".getBytes("UTF-8");
    assertEquals(
        -1,
        FileUtil.writeFile(new MemoryCacheImageInputStream(new ByteArrayInputStream(data)), file));
    assertTrue(file.exists());
    assertEquals(file.length(), data.length);
    assertTrue(file.exists());

    File file2 = Paths.get(System.getProperty("java.io.tmpdir"), "testNioWriteFile.test").toFile();
    try (FileInputStream inputStream = new FileInputStream(file);
        FileOutputStream outputStream = new FileOutputStream(file2)) {
      assertTrue(FileUtil.nioWriteFile(inputStream, outputStream));
      assertTrue(file2.exists());
      assertEquals(file2.length(), data.length);
      assertTrue(file2.exists());
      assertTrue(FileUtil.delete(file2));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    try (FileInputStream inputStream = new FileInputStream(file);
        FileOutputStream outputStream = new FileOutputStream(file2)) {
      assertTrue(FileUtil.nioWriteFile(inputStream, outputStream, 4));
      assertTrue(file2.exists());
      assertEquals(file2.length(), data.length);
      assertTrue(file2.exists());
      assertTrue(FileUtil.delete(file2));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    assertTrue(FileUtil.nioCopyFile(file, file2));
    assertTrue(file2.exists());
    assertEquals(file2.length(), data.length);
    assertTrue(file2.exists());
    assertTrue(FileUtil.delete(file2));

    try (FileInputStream inputStream = new FileInputStream(new FileDescriptor());
        FileOutputStream outputStream = new FileOutputStream(file)) {
      assertFalse(FileUtil.nioWriteFile(inputStream, outputStream));
      assertTrue(FileUtil.delete(file));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Method under test:
   *
   * <ul>
   *   <li>{@link FileUtil#zip(File, File)}
   *   <li>{@link FileUtil#unzip(File, File)}
   *   <li>{@link FileUtil#unzip(InputStream, File)}
   * </ul>
   */
  @Test
  void testZip() throws IOException {
    FileUtil.zip(null, null);

    Path folder = Paths.get(System.getProperty("java.io.tmpdir"), "tempZipFolder");
    File file = Paths.get(System.getProperty("java.io.tmpdir"), "testWriteStream.zip").toFile();
    FileUtil.zip(null, file);
    FileUtil.unzip(file, null);

    Files.createDirectories(folder);
    Path file1 = Paths.get(folder.toString(), "test1.jpg");
    byte[] data = "AXAXAXAX".getBytes(StandardCharsets.UTF_8);
    assertEquals(
        -1,
        FileUtil.writeFile(
            new MemoryCacheImageInputStream(new ByteArrayInputStream(data)), file1.toFile()));
    assertEquals(Files.size(file1), data.length);
    assertTrue(
        FileUtil.nioCopyFile(file1.toFile(), Paths.get(folder.toString(), "test2.jpg").toFile()));
    Files.createDirectories(Paths.get(folder.toString(), "folder"));
    assertTrue(
        FileUtil.nioCopyFile(
            file1.toFile(), Paths.get(folder.toString(), "folder", "test3.jpg").toFile()));
    assertTrue(
        FileUtil.nioCopyFile(
            file1.toFile(), Paths.get(folder.toString(), "folder", "test4.jpg").toFile()));

    FileUtil.zip(folder.toFile(), file);

    Path folder2 = Paths.get(System.getProperty("java.io.tmpdir"), "tempZipFolderCopy");
    FileUtil.unzip(file, folder2.toFile());
    assertEquals(data.length, Files.size(Paths.get(folder2.toString(), "test1.jpg")));
    assertEquals(data.length, Files.size(Paths.get(folder2.toString(), "folder", "test3.jpg")));
    assertTrue(FileUtil.delete(folder2));
    assertFalse(Files.isReadable(folder2));

    try (FileInputStream inputStream = new FileInputStream(file)) {
      FileUtil.unzip(inputStream, folder2.toFile());
      assertEquals(data.length, Files.size(Paths.get(folder2.toString(), "test2.jpg")));
      assertEquals(data.length, Files.size(Paths.get(folder2.toString(), "folder", "test4.jpg")));
      assertTrue(FileUtil.delete(folder2));
      assertFalse(Files.isReadable(folder2));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    assertTrue(FileUtil.delete(folder));
    assertFalse(Files.isReadable(folder));
  }

  /** Method under test: {@link FileUtil#humanReadableByte(long, boolean)} */
  @Test
  @DefaultLocale(language = "en", country = "US")
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

  /**
   * Method under test:
   *
   * <ul>
   *   <li>{@link FileUtil#storeProperties(File, Properties, String)}
   *   <li>{@link FileUtil#readProperties(File, Properties)}
   * </ul>
   */
  @Test
  void testReadProperties() {
    Properties props = new Properties();
    props.setProperty("key", "value");
    File propsFile = Paths.get(System.getProperty("java.io.tmpdir"), "test.properties").toFile();
    try {
      FileUtil.storeProperties(propsFile, props, "comment");
      Properties actualReadPropertiesResult = FileUtil.readProperties(propsFile, null);
      assertEquals("value", actualReadPropertiesResult.getProperty("key"));

      actualReadPropertiesResult.clear();
      FileUtil.readProperties(propsFile, actualReadPropertiesResult);
      assertEquals("value", actualReadPropertiesResult.getProperty("key"));
    } finally {
      assertTrue(FileUtil.delete(propsFile.toPath()));
    }
  }

  /** Method under test: {@link FileUtil#copyFolder(Path, Path, CopyOption[])} */
  @Test
  void testCopyFolder() throws IOException {
    Path directory = Paths.get(System.getProperty("java.io.tmpdir"), "tempTest");
    try {
      Path input = Paths.get(directory.toString(), "src-copy", "dir1");
      Path output = Paths.get(directory.toString(), "dst-copy");
      Files.createDirectories(input);
      Path file1 = Paths.get(input.toString(), "test1.jpg");
      Path file2 = Paths.get(input.toString(), "test2.jpg");
      Files.createFile(file1);
      Files.createFile(file2);

      ArrayList<File> files = new ArrayList<>();
      FileUtil.copyFolder(input.getParent(), output);
      assertTrue(Files.isReadable(Paths.get(output.toString(), "dir1")));
      FileUtil.getAllFilesInDirectory(output.toFile(), files);
      assertEquals(2, files.size());
    } finally {
      FileUtil.deleteDirectoryContents(directory.toFile(), 0, 5);
      assertFalse(Files.isReadable(directory));
    }
  }
}
