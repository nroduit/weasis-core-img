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

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import javax.imageio.stream.MemoryCacheImageInputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junitpioneer.jupiter.DefaultLocale;

@DisplayNameGeneration(ReplaceUnderscores.class)
class FileUtilTest {

  @TempDir Path tempDir;

  private static final String TEST_DATA = "TestFileContent";
  private static final byte[] TEST_BYTES = TEST_DATA.getBytes(StandardCharsets.UTF_8);

  @Nested
  class File_Name_Validation {

    @Test
    void should_return_valid_filename_when_given_valid_input() {
      assertEquals("foo.txt", FileUtil.getValidFileName("foo.txt"));
      assertEquals("valid_file-name.ext", FileUtil.getValidFileName("valid_file-name.ext"));
    }

    @Test
    void should_return_empty_string_when_filename_is_null() {
      assertEquals("", FileUtil.getValidFileName(null));
    }

    @ParameterizedTest
    @ValueSource(
        strings = {
          "\0\u001B|/foo.txt", // Null + ESC + pipe + slash
          "file:<>.txt", // Colon, less than, greater than
          "file\"*.txt" // Quote + asterisk
        })
    void should_remove_illegal_characters_from_filename(String input) {
      var result = FileUtil.getValidFileName(input);
      assertTrue(result.matches("[^\\u0000-\\u001F\"*/:<>?\\\\|]*"));
    }

    @Test
    void should_trim_whitespace_from_filename() {
      assertEquals("filename", FileUtil.getValidFileName("  filename  "));
    }
  }

  @Nested
  class HTML_File_Name_Validation {

    @ParameterizedTest
    @CsvSource({
      "'<p>This is <b>bold</b> and <i>italic</i> text.</p>', 'This is bold and italic text.'",
      "'<html>This is</p> a line <br>break<empty/>', 'This is a line break'",
      "'<div><span>Nested</span> tags</div>', 'Nested tags'"
    })
    void should_remove_html_tags_and_validate_filename(String input, String expected) {
      assertEquals(expected, FileUtil.getValidFileNameWithoutHTML(input));
    }

    @Test
    void should_return_empty_string_when_html_filename_is_null() {
      assertEquals("", FileUtil.getValidFileNameWithoutHTML(null));
    }
  }

  @Nested
  class Temporary_Directory_Operations {

    @Test
    void should_create_unique_temporary_directory() throws IOException {
      var tempDir1 = FileUtil.createTempDir(tempDir);
      var tempDir2 = FileUtil.createTempDir(tempDir);

      assertAll(
          () -> assertTrue(Files.isDirectory(tempDir1)),
          () -> assertTrue(Files.isDirectory(tempDir2)),
          () -> assertNotEquals(tempDir1, tempDir2));
    }

    @Test
    void should_throw_when_base_directory_is_null() {
      assertThrows(IllegalArgumentException.class, () -> FileUtil.createTempDir((Path) null));
    }

    @Test
    void should_throw_when_base_directory_does_not_exist() {
      var nonExistentDir = tempDir.resolve("nonexistent");
      assertThrows(IllegalArgumentException.class, () -> FileUtil.createTempDir(nonExistentDir));
    }
  }

  @Nested
  class Directory_File_Operations {

    @BeforeEach
    void setup() throws IOException {
      // Create test directory structure
      var subDir = tempDir.resolve("subdir");
      Files.createDirectories(subDir);
      Files.createFile(tempDir.resolve("file1.txt"));
      Files.createFile(tempDir.resolve("file2.jpg"));
      Files.createFile(subDir.resolve("subfile.txt"));
    }

    @Test
    void should_get_all_files_recursively() {
      var files = new ArrayList<Path>();
      FileUtil.getAllFilesInDirectory(tempDir, files);

      assertEquals(3, files.size());
      assertTrue(files.stream().anyMatch(p -> p.getFileName().toString().equals("file1.txt")));
      assertTrue(files.stream().anyMatch(p -> p.getFileName().toString().equals("subfile.txt")));
    }

    @Test
    void should_get_files_non_recursively() {
      var files = new ArrayList<Path>();
      FileUtil.getAllFilesInDirectory(tempDir, files, false);

      assertEquals(2, files.size());
      assertFalse(files.stream().anyMatch(p -> p.getFileName().toString().equals("subfile.txt")));
    }

    @Test
    void should_handle_null_directory_gracefully() {
      var files = new ArrayList<Path>();
      FileUtil.getAllFilesInDirectory(null, files);

      assertTrue(files.isEmpty());
    }
  }

  @Nested
  class File_And_Directory_Deletion {

    @Test
    void should_return_false_when_deleting_null_path() {
      assertFalse(FileUtil.delete((Path) null));
    }

    @Test
    void should_return_false_when_deleting_nonexistent_path() {
      assertFalse(FileUtil.delete(tempDir.resolve("nonexistent")));
    }

    @Test
    void should_delete_file_successfully() throws IOException {
      var testFile = tempDir.resolve("test.txt");
      Files.createFile(testFile);

      assertTrue(FileUtil.delete(testFile));
      assertFalse(Files.exists(testFile));
    }

    @Test
    void should_delete_directory_with_contents_recursively() throws IOException {
      var subDir = tempDir.resolve("subdir");
      Files.createDirectories(subDir);
      Files.createFile(subDir.resolve("file1.txt"));
      Files.createFile(subDir.resolve("file2.txt"));

      assertTrue(FileUtil.delete(subDir));
      assertFalse(Files.exists(subDir));
    }
  }

  // Add to existing FileUtilTest class

  @Nested
  class Directory_Content_Deletion {

    @Test
    void should_delete_directory_contents_at_specific_level() throws IOException {
      // Create nested directory structure
      var level0Dir = tempDir.resolve("level0");
      var level1Dir = level0Dir.resolve("level1");
      var level2Dir = level1Dir.resolve("level2");
      Files.createDirectories(level2Dir);

      // Create files at different levels
      Files.createFile(level0Dir.resolve("file0.txt"));
      Files.createFile(level1Dir.resolve("file1.txt"));
      Files.createFile(level2Dir.resolve("file2.txt"));

      // Delete contents at level 1 and below
      FileUtil.deleteDirectoryContents(level0Dir, 1, 0);

      assertAll(
          () -> assertTrue(Files.exists(level0Dir), "Level 0 directory should still exist"),
          () -> assertFalse(Files.exists(level1Dir), "Level 1 directory should be deleted"),
          () -> assertFalse(Files.exists(level2Dir), "Level 2 directory should be deleted"),
          () ->
              assertFalse(
                  Files.exists(level0Dir.resolve("file0.txt")),
                  "File at level 0 should be deleted"));
    }

    @Test
    void should_preserve_directory_when_level_not_reached() throws IOException {
      var level0Dir = tempDir.resolve("preserve_test");
      var level1Dir = level0Dir.resolve("level1");
      Files.createDirectories(level1Dir);
      Files.createFile(level1Dir.resolve("test.txt"));

      // Set deleteDirLevel to 2, but only go to level 1
      FileUtil.deleteDirectoryContents(level0Dir, 2, 0);

      assertAll(
          () -> assertTrue(Files.exists(level0Dir), "Level 0 should be preserved"),
          () -> assertTrue(Files.exists(level1Dir), "Level 1 should be preserved"),
          () ->
              assertFalse(Files.exists(level1Dir.resolve("test.txt")), "Files should be deleted"));
    }

    @Test
    void should_handle_mixed_files_and_directories() throws IOException {
      var baseDir = tempDir.resolve("mixed_content");
      Files.createDirectories(baseDir);

      // Create mixed content
      Files.createFile(baseDir.resolve("file1.txt"));
      Files.createFile(baseDir.resolve("file2.txt"));
      var subDir1 = baseDir.resolve("subdir1");
      var subDir2 = baseDir.resolve("subdir2");
      Files.createDirectories(subDir1);
      Files.createDirectories(subDir2);
      Files.createFile(subDir1.resolve("nested.txt"));

      FileUtil.deleteDirectoryContents(baseDir, 0, 0);

      assertAll(
          () -> assertFalse(Files.exists(baseDir), "Base directory should be deleted"),
          () -> assertFalse(Files.exists(baseDir.resolve("file1.txt"))),
          () -> assertFalse(Files.exists(baseDir.resolve("file2.txt"))),
          () -> assertFalse(Files.exists(subDir1)),
          () -> assertFalse(Files.exists(subDir2)));
    }

    @Test
    void should_handle_empty_directory_gracefully() throws IOException {
      var emptyDir = tempDir.resolve("empty");
      Files.createDirectories(emptyDir);

      assertDoesNotThrow(() -> FileUtil.deleteDirectoryContents(emptyDir, 1, 0));
      assertTrue(Files.exists(emptyDir), "Empty directory should still exist");
    }

    @Test
    void should_handle_null_directory_gracefully() {
      assertDoesNotThrow(() -> FileUtil.deleteDirectoryContents((Path) null, 1, 0));
    }

    @Test
    void should_handle_non_directory_path() throws IOException {
      var file = tempDir.resolve("not_a_directory.txt");
      Files.createFile(file);

      assertDoesNotThrow(() -> FileUtil.deleteDirectoryContents(file, 1, 0));
      assertTrue(Files.exists(file), "File should remain unchanged");
    }
  }

  @Nested
  class Recursive_Directory_Deletion {

    @Test
    void should_delete_all_contents_and_preserve_root_directory() throws IOException {
      // Create complex directory structure
      var rootDir = tempDir.resolve("recursive_test");
      var subDir1 = rootDir.resolve("sub1");
      var subDir2 = rootDir.resolve("sub2");
      var nestedDir = subDir1.resolve("nested");

      Files.createDirectories(nestedDir);
      Files.createDirectories(subDir2);

      // Create files at various levels
      Files.write(rootDir.resolve("root.txt"), "root content".getBytes());
      Files.write(subDir1.resolve("sub1.txt"), "sub1 content".getBytes());
      Files.write(subDir2.resolve("sub2.txt"), "sub2 content".getBytes());
      Files.write(nestedDir.resolve("nested.txt"), "nested content".getBytes());

      FileUtil.recursiveDelete(rootDir, false); // Keep root

      assertAll(
          () -> assertTrue(Files.exists(rootDir), "Root directory should be preserved"),
          () -> assertFalse(Files.exists(subDir1), "Subdirectory 1 should be deleted"),
          () -> assertFalse(Files.exists(subDir2), "Subdirectory 2 should be deleted"),
          () -> assertFalse(Files.exists(nestedDir), "Nested directory should be deleted"),
          () -> assertEquals(0, Files.list(rootDir).count(), "Root directory should be empty"));
    }

    @Test
    void should_delete_all_contents_and_root_directory() throws IOException {
      var rootDir = tempDir.resolve("delete_root_test");
      var subDir = rootDir.resolve("subdir");
      Files.createDirectories(subDir);
      Files.createFile(subDir.resolve("file.txt"));

      FileUtil.recursiveDelete(rootDir, true); // Delete root too

      assertFalse(Files.exists(rootDir), "Root directory should be deleted");
    }

    @Test
    void should_use_delete_root_true_by_default() throws IOException {
      var rootDir = tempDir.resolve("default_behavior_test");
      Files.createDirectories(rootDir);
      Files.createFile(rootDir.resolve("test.txt"));

      FileUtil.recursiveDelete(rootDir); // Default behavior

      assertFalse(Files.exists(rootDir), "Directory should be deleted by default");
    }

    @Test
    void should_handle_deeply_nested_structure() throws IOException {
      // Create 10 levels deep
      var currentDir = tempDir.resolve("deep_test");
      Files.createDirectories(currentDir);

      var paths = new ArrayList<Path>();
      paths.add(currentDir);

      for (int i = 0; i < 10; i++) {
        currentDir = currentDir.resolve("level" + i);
        Files.createDirectories(currentDir);
        Files.createFile(currentDir.resolve("file" + i + ".txt"));
        paths.add(currentDir);
      }

      FileUtil.recursiveDelete(paths.get(0), false);

      assertAll(
          () -> assertTrue(Files.exists(paths.get(0)), "Root should be preserved"),
          () ->
              paths
                  .subList(1, paths.size())
                  .forEach(
                      path ->
                          assertFalse(Files.exists(path), "Deep path should be deleted: " + path)));
    }

    @Test
    void should_handle_directory_with_many_files() throws IOException {
      var testDir = tempDir.resolve("many_files_test");
      Files.createDirectories(testDir);

      // Create 100 files
      var createdFiles = new ArrayList<Path>();
      for (int i = 0; i < 100; i++) {
        var file = testDir.resolve("file" + i + ".txt");
        Files.write(file, ("Content " + i).getBytes());
        createdFiles.add(file);
      }

      FileUtil.recursiveDelete(testDir, false);

      assertAll(
          () -> assertTrue(Files.exists(testDir), "Directory should be preserved"),
          () ->
              createdFiles.forEach(
                  file -> assertFalse(Files.exists(file), "File should be deleted: " + file)));
    }

    @Test
    void should_handle_null_directory_safely() {
      assertDoesNotThrow(() -> FileUtil.recursiveDelete((Path) null));
      assertDoesNotThrow(() -> FileUtil.recursiveDelete((Path) null, true));
      assertDoesNotThrow(() -> FileUtil.recursiveDelete((Path) null, false));
    }

    @Test
    void should_handle_non_existent_directory() {
      var nonExistent = tempDir.resolve("does_not_exist");
      assertDoesNotThrow(() -> FileUtil.recursiveDelete(nonExistent));
      assertDoesNotThrow(() -> FileUtil.recursiveDelete(nonExistent, false));
    }

    @Test
    void should_handle_file_instead_of_directory() throws IOException {
      var file = tempDir.resolve("not_a_directory.txt");
      Files.write(file, "content".getBytes());

      assertDoesNotThrow(() -> FileUtil.recursiveDelete(file));
      assertTrue(Files.exists(file), "File should remain unchanged when not a directory");
    }

    @Test
    void should_handle_symbolic_links() throws IOException {
      var targetDir = tempDir.resolve("target");
      var linkDir = tempDir.resolve("link_test");
      Files.createDirectories(targetDir);
      Files.createDirectories(linkDir);
      Files.createFile(targetDir.resolve("target_file.txt"));

      try {
        var symLink = linkDir.resolve("sym_link");
        Files.createSymbolicLink(symLink, targetDir);

        FileUtil.recursiveDelete(linkDir, false);

        assertAll(
            () -> assertTrue(Files.exists(linkDir), "Link directory should be preserved"),
            () -> assertFalse(Files.exists(symLink), "Symbolic link should be deleted"),
            () -> assertTrue(Files.exists(targetDir), "Target directory should remain"),
            () ->
                assertFalse(
                    Files.exists(targetDir.resolve("target_file.txt")),
                    "Target file should be deleted"));
      } catch (UnsupportedOperationException e) {
        // Skip test on systems that don't support symbolic links
        assumeTrue(false, "Symbolic links not supported on this system");
      }
    }
  }

  @Nested
  class Directory_Deletion_Error_Handling {

    @Test
    void should_continue_deletion_despite_io_errors() throws IOException {
      var testDir = tempDir.resolve("error_handling_test");
      Files.createDirectories(testDir);

      // Create some files that can be deleted
      var file1 = testDir.resolve("file1.txt");
      var file2 = testDir.resolve("file2.txt");
      Files.createFile(file1);
      Files.createFile(file2);

      // This should not throw exceptions even if some files can't be deleted
      assertDoesNotThrow(() -> FileUtil.recursiveDelete(testDir, false));

      // Verify directory structure is handled appropriately
      assertTrue(Files.exists(testDir), "Directory should exist");
    }

    @Test
    void should_handle_permission_issues_gracefully() throws IOException {
      var testDir = tempDir.resolve("permission_test");
      Files.createDirectories(testDir);
      var file = testDir.resolve("test.txt");
      Files.createFile(file);

      // Test should not throw exceptions even with potential permission issues
      assertDoesNotThrow(() -> FileUtil.deleteDirectoryContents(testDir, 0, 0));
      assertDoesNotThrow(() -> FileUtil.recursiveDelete(testDir));
    }

    @Test
    void should_log_errors_without_propagating_exceptions() throws IOException {
      // Create test scenario that might cause logging
      var testDir = tempDir.resolve("logging_test");
      Files.createDirectories(testDir);
      var subDir = testDir.resolve("subdir");
      Files.createDirectories(subDir);
      Files.createFile(subDir.resolve("file.txt"));

      // These operations should complete without throwing exceptions
      assertDoesNotThrow(
          () -> {
            FileUtil.deleteDirectoryContents(testDir, 1, 0);
            FileUtil.recursiveDelete(testDir, true);
          });
    }
  }

  @Nested
  class File_Path_Operations {

    @ParameterizedTest
    @CsvSource({
      "folder/fileNoExtension, folder/fileNoExtension",
      "file.archive.jpg, file.archive",
      "simple.txt, simple"
    })
    void should_extract_filename_without_extension(String input, String expected) {
      assertEquals(expected, FileUtil.nameWithoutExtension(input));
    }

    @Test
    void should_return_null_when_filename_is_null() {
      assertNull(FileUtil.nameWithoutExtension(null));
    }

    @ParameterizedTest
    @CsvSource({"filename, ''", "filename-archive.zip, .zip", "file.tar.gz, .gz"})
    void should_extract_file_extension_correctly(String input, String expected) {
      assertEquals(expected, FileUtil.getExtension(input));
    }

    @Test
    void should_return_empty_string_when_getting_extension_of_null() {
      assertEquals("", FileUtil.getExtension(null));
    }
  }

  @Nested
  class File_Extension_Matching {

    private final String[] MEDICAL_EXTENSIONS = {"dcm", "dic", "dicm", "dicom"};

    @Test
    void should_return_false_for_null_inputs() {
      assertAll(
          () -> assertFalse(FileUtil.isFileExtensionMatching((Path) null, null)),
          () -> assertFalse(FileUtil.isFileExtensionMatching(Paths.get("test.dcm"), null)),
          () -> assertFalse(FileUtil.isFileExtensionMatching((Path) null, MEDICAL_EXTENSIONS)));
    }

    @Test
    void should_return_false_for_files_without_extensions() {
      assertFalse(FileUtil.isFileExtensionMatching(Paths.get("test"), MEDICAL_EXTENSIONS));
    }

    @ParameterizedTest
    @ValueSource(strings = {"test.dcm", "TEST.DCM", "file.DICOM", "sample.dic"})
    void should_match_extensions_case_insensitively(String filename) {
      assertTrue(FileUtil.isFileExtensionMatching(Paths.get(filename), MEDICAL_EXTENSIONS));
    }

    @Test
    void should_handle_extensions_with_and_without_dots() {
      var path = Paths.get("test.dcm");
      assertAll(
          () -> assertTrue(FileUtil.isFileExtensionMatching(path, new String[] {".dcm"})),
          () -> assertTrue(FileUtil.isFileExtensionMatching(path, new String[] {"dcm"})),
          () -> assertTrue(FileUtil.isFileExtensionMatching(path, new String[] {"DCM"})));
    }
  }

  @Nested
  class Stream_Writing_Operations {

    @Test
    void should_write_inputstream_to_file_successfully() throws IOException {
      var testFile = tempDir.resolve("stream_test.txt");
      var inputStream = new ByteArrayInputStream(TEST_BYTES);

      var result = FileUtil.writeStream(inputStream, testFile);

      assertAll(
          () -> assertEquals(-1, result), // Success indicator
          () -> assertTrue(Files.exists(testFile)),
          () -> assertArrayEquals(TEST_BYTES, Files.readAllBytes(testFile)));
    }

    @Test
    void should_write_stream_without_closing_when_specified() throws IOException {
      var testFile = tempDir.resolve("stream_no_close.txt");
      var inputStream = new ByteArrayInputStream(TEST_BYTES);

      var result = FileUtil.writeStream(inputStream, testFile, false);

      assertEquals(-1, result);
      assertTrue(Files.exists(testFile));
      // Stream should still be open (though ByteArrayInputStream doesn't really close)
    }

    @Test
    void should_throw_exception_when_write_fails() throws IOException {
      var testFile = tempDir.resolve("fail_test.txt");
      // Create a stream that will fail when read
      var failingStream = new FailingInputStream();

      assertThrows(StreamIOException.class, () -> FileUtil.writeStream(failingStream, testFile));
      assertFalse(Files.exists(testFile)); // File should be cleaned up
    }

    @Test
    void should_write_stream_with_io_exception_wrapper() throws IOException {
      var testFile = tempDir.resolve("wrapper_test.txt");
      var inputStream = new ByteArrayInputStream(TEST_BYTES);

      assertDoesNotThrow(() -> FileUtil.writeStreamWithIOException(inputStream, testFile));
      assertTrue(Files.exists(testFile));
    }
  }

  @Nested
  class Image_Stream_Operations {

    @Test
    void should_write_image_input_stream_successfully() throws IOException {
      var testFile = tempDir.resolve("image_test.dat");
      var imageInputStream = new MemoryCacheImageInputStream(new ByteArrayInputStream(TEST_BYTES));

      var result = FileUtil.writeFile(imageInputStream, testFile);

      assertAll(
          () -> assertEquals(-1, result),
          () -> assertTrue(Files.exists(testFile)),
          () -> assertArrayEquals(TEST_BYTES, Files.readAllBytes(testFile)));
    }

    @Test
    void should_handle_interrupted_io_exception() throws IOException {
      var testFile = tempDir.resolve("interrupted_test.dat");
      var interruptingStream = new InterruptingImageInputStream();

      var bytesWritten = FileUtil.writeFile(interruptingStream, testFile);

      assertEquals(5, bytesWritten); // Should return bytesTransferred
      assertFalse(Files.exists(testFile)); // File should be deleted on error
    }
  }

  @Nested
  class File_Path_Utilities {

    @Test
    void should_prepare_file_directories() throws IOException {
      var nestedFile = tempDir.resolve("deep").resolve("nested").resolve("file.txt");

      FileUtil.prepareToWriteFile(nestedFile);

      assertTrue(Files.exists(nestedFile.getParent()));
    }

    @Test
    void should_return_file_path_when_output_is_file() throws IOException {
      var inputFile = tempDir.resolve("input.jpg");
      var outputFile = tempDir.resolve("output.dcm");
      Files.createFile(inputFile);
      Files.createFile(outputFile);

      var result = FileUtil.getOutputPath(inputFile, outputFile);

      assertEquals(outputFile, result);
    }

    @Test
    void should_resolve_filename_when_output_is_directory() throws IOException {
      var inputFile = tempDir.resolve("input.jpg");
      var outputDir = tempDir.resolve("output");
      Files.createFile(inputFile);
      Files.createDirectories(outputDir);

      var result = FileUtil.getOutputPath(inputFile, outputDir);

      assertEquals(outputDir.resolve("input.jpg"), result);
    }

    @ParameterizedTest
    @CsvSource({
      "test.jpg, 5, 0, test.jpg", // indexSize 0 returns original
      "test.jpg, 5, 1, test-5.jpg", // Single digit
      "test.jpg, 505, 1, test-505.jpg", // Large number
      "test.jpg, 5, 3, test-005.jpg", // Zero padding
      "test.jpg, 985, 3, test-985.jpg" // Large number with padding
    })
    void should_add_file_index_correctly(
        String filename, int index, int indexSize, String expected) {
      var path = tempDir.resolve(filename);
      var result = FileUtil.addFileIndex(path, index, indexSize);
      assertEquals(tempDir.resolve(expected), result);
    }
  }

  @Nested
  class Folder_Operations {

    @Test
    void should_copy_folder_recursively() throws IOException {
      // Setup source structure
      var sourceDir = tempDir.resolve("source");
      var subDir = sourceDir.resolve("subdir");
      Files.createDirectories(subDir);
      Files.write(sourceDir.resolve("file1.txt"), "content1".getBytes());
      Files.write(subDir.resolve("file2.txt"), "content2".getBytes());

      var targetDir = tempDir.resolve("target");

      FileUtil.copyFolder(sourceDir, targetDir);

      assertAll(
          () -> assertTrue(Files.exists(targetDir.resolve("file1.txt"))),
          () -> assertTrue(Files.exists(targetDir.resolve("subdir").resolve("file2.txt"))),
          () -> assertEquals("content1", Files.readString(targetDir.resolve("file1.txt"))),
          () ->
              assertEquals(
                  "content2", Files.readString(targetDir.resolve("subdir").resolve("file2.txt"))));
    }

    @Test
    void should_copy_folder_with_overwrite_option() throws IOException {
      var sourceDir = tempDir.resolve("source");
      var targetDir = tempDir.resolve("target");
      Files.createDirectories(sourceDir);
      Files.createDirectories(targetDir);

      Files.write(sourceDir.resolve("file.txt"), "new content".getBytes());
      Files.write(targetDir.resolve("file.txt"), "old content".getBytes());

      FileUtil.copyFolder(sourceDir, targetDir, StandardCopyOption.REPLACE_EXISTING);

      assertEquals("new content", Files.readString(targetDir.resolve("file.txt")));
    }
  }

  @Test
  @DefaultLocale(language = "en", country = "US")
  void should_format_bytes_in_human_readable_format() {
    assertAll(
        () -> assertEquals("1 B", FileUtil.humanReadableByte(1L, true)),
        () -> assertEquals("1 B", FileUtil.humanReadableByte(1L, false)),
        () -> assertEquals("1.0 kB", FileUtil.humanReadableByte(1000L, true)),
        () -> assertEquals("1.0 KiB", FileUtil.humanReadableByte(1024L, false)),
        () -> assertEquals("1.3 GB", FileUtil.humanReadableByte(1256799945L, true)),
        () -> assertEquals("1.2 GiB", FileUtil.humanReadableByte(1256799945L, false)),
        () -> assertEquals("-9.2 EB", FileUtil.humanReadableByte(Long.MIN_VALUE, true)),
        () -> assertEquals("-8.0 EiB", FileUtil.humanReadableByte(Long.MIN_VALUE, false)));
  }

  // Helper classes for testing edge cases
  private static class FailingInputStream extends InputStream {
    @Override
    public int read() throws IOException {
      throw new IOException("Simulated read failure");
    }
  }

  private static class InterruptingImageInputStream extends MemoryCacheImageInputStream {
    public InterruptingImageInputStream() {
      super(new ByteArrayInputStream("dummy".getBytes()));
    }

    @Override
    public int read(byte[] b) throws IOException {
      var exception = new InterruptedIOException("Simulated interruption");
      exception.bytesTransferred = 5;
      throw exception;
    }
  }
}
