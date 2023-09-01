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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

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

  /**
   * Method under test:
   *
   * <ul>
   *   <li>{@link FileUtil#createTempDir(File)}
   *   <li>{@link FileUtil#delete(File)}
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
    } finally {
      FileUtil.delete(file);
      assertFalse(file.exists());
    }
  }

  /** Method under test: {@link FileUtil#getAllFilesInDirectory(File, List)} */
  @Test
  void testGetAllFilesInDirectory3() throws IOException {
    File directory = Paths.get(System.getProperty("java.io.tmpdir"), "tempTest").toFile();
    try {
      directory.mkdirs();
      ArrayList<File> files = new ArrayList<>();
      File myFirstFile = new File(directory, "first");
      File mySecondFile = new File(directory, "second"); // NON-NLS
      assertTrue(myFirstFile.createNewFile());
      assertTrue(mySecondFile.createNewFile());
      FileUtil.getAllFilesInDirectory(directory, files);
      assertEquals(2, files.size());
    } finally {
      FileUtil.recursiveDelete(directory);
      assertFalse(directory.exists());
    }
  }

  /** Method under test: {@link FileUtil#delete(Path)} */
  @Test
  void testDelete() throws IOException {
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
      FileUtil.delete(directory);
      assertFalse(Files.isReadable(directory));
    }
  }

  /** Method under test: {@link FileUtil#getOutputPath(Path, Path)} */
  @Test
  void testGetOutputPath() throws IOException {
    Path directory = Paths.get(System.getProperty("java.io.tmpdir"), "tempTest");
    try {
      Path input = Paths.get(directory.toString(), "input", "test.jpg");
      Path output = Paths.get(directory.toString(), "output", "test-out.dcm");
      Files.createDirectories(input.getParent());
      Files.createDirectories(output.getParent());
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
      FileUtil.delete(directory);
      assertFalse(Files.isReadable(directory));
    }
  }

  /** Method under test: {@link FileUtil#getExtension(String)} */
  @Test
  void testGetExtension() {
    assertEquals(StringUtil.EMPTY_STRING, FileUtil.getExtension("filename"));
    assertEquals(StringUtil.EMPTY_STRING, FileUtil.getExtension(null));
    assertEquals(".zip", FileUtil.getExtension("filename-archive.zip"));
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
