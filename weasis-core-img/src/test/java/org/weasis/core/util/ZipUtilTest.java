/*
 * Copyright (c) 2025 Weasis Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.weasis.core.util;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@DisplayNameGeneration(ReplaceUnderscores.class)
class ZipUtilTest {

  @TempDir Path tempDir;

  private Path sourceFolder;
  private Path zipFile;
  private TestDataBuilder testDataBuilder;

  @BeforeEach
  void setUp() {
    sourceFolder = tempDir.resolve("testSource");
    zipFile = tempDir.resolve("test.zip");
    testDataBuilder = new TestDataBuilder();
  }

  @Nested
  class Zip_Creation_Tests {

    @Test
    void should_create_zip_from_directory_with_files() throws IOException {
      // Given
      var structure =
          testDataBuilder
              .directory(sourceFolder)
              .file("readme.txt", "Welcome to the project")
              .file(
                  "config.json",
                  """
              {
                "name": "test-project",
                "version": "1.0.0"
              }
              """)
              .subdirectory("src")
              .file("Main.java", "public class Main { }")
              .build();

      // When
      ZipUtil.zip(sourceFolder, zipFile);

      // Then
      assertThat(zipFile).exists().hasPositiveSize();
      verifyZipContains(zipFile, "readme.txt", "config.json", "src/Main.java");
    }

    @Test
    void should_create_zip_from_empty_directory() throws IOException {
      // Given
      Files.createDirectories(sourceFolder);

      // When
      ZipUtil.zip(sourceFolder, zipFile);

      // Then
      assertThat(zipFile).exists().hasPositiveSize();
    }

    @Test
    void should_preserve_empty_subdirectories() throws IOException {
      // Given
      testDataBuilder
          .directory(sourceFolder)
          .file("app.txt", "Application file")
          .emptyDirectory("logs")
          .emptyDirectory("temp")
          .build();

      // When
      ZipUtil.zip(sourceFolder, zipFile);

      // Then
      assertThat(zipFile).exists();
      verifyZipContains(zipFile, "app.txt", "logs/", "temp/");
    }

    @Test
    void should_create_parent_directories_for_zip_file() throws IOException {
      // Given
      testDataBuilder.directory(sourceFolder).file("test.txt", "content").build();
      var nestedZipFile = tempDir.resolve("nested/deep/archive.zip");

      // When
      ZipUtil.zip(sourceFolder, nestedZipFile);

      // Then
      assertThat(nestedZipFile).exists();
      assertThat(nestedZipFile.getParent()).exists().isDirectory();
    }

    @Test
    void should_handle_special_characters_in_file_names() throws IOException {
      // Given
      var specialFiles =
          List.of(
              "file with spaces.txt",
              "file-with-dashes.txt",
              "file_with_underscores.txt",
              "файл-кириллица.txt",
              "файл中文.txt");

      var builder = testDataBuilder.directory(sourceFolder);
      for (var filename : specialFiles) {
        builder.file(filename, "Content of " + filename);
      }
      builder.build();

      // When
      ZipUtil.zip(sourceFolder, zipFile);

      // Then
      assertThat(zipFile).exists();
      verifyZipContains(zipFile, specialFiles.toArray(String[]::new));
    }

    @Test
    void should_handle_large_file_structure() throws IOException {
      // Given
      var builder = testDataBuilder.directory(sourceFolder);

      // Create multiple levels and files
      for (int i = 1; i <= 5; i++) {
        var levelDir = "level" + i;
        builder.subdirectory(levelDir);
        for (int j = 1; j <= 3; j++) {
          builder.file("file" + j + ".txt", "Content for level " + i + " file " + j);
        }
        builder.up(); // Go back to parent directory
      }
      builder.build();

      // When
      ZipUtil.zip(sourceFolder, zipFile);

      // Then
      assertThat(zipFile).exists().hasPositiveSize();
      var expectedEntries = new String[15]; // 5 levels * 3 files each
      int index = 0;
      for (int i = 1; i <= 5; i++) {
        for (int j = 1; j <= 3; j++) {
          expectedEntries[index++] = "level" + i + "/file" + j + ".txt";
        }
      }
      verifyZipContains(zipFile, expectedEntries);
    }
  }

  @Nested
  class Zip_Extraction_Tests {

    @Test
    void should_extract_zip_to_directory_from_path() throws IOException {
      // Given
      var originalStructure =
          testDataBuilder
              .directory(sourceFolder)
              .file("app.properties", "app.name=TestApp\napp.version=1.0")
              .subdirectory("data")
              .file(
                  "sample.json",
                  """
              {"users": [{"name": "John", "age": 30}]}
              """)
              .build();

      ZipUtil.zip(sourceFolder, zipFile);
      var extractFolder = tempDir.resolve("extracted");

      // When
      ZipUtil.unzip(zipFile, extractFolder);

      // Then
      verifyExtractedStructure(extractFolder, originalStructure);
    }

    @Test
    void should_extract_zip_from_input_stream() throws IOException {
      // Given
      var originalStructure =
          testDataBuilder
              .directory(sourceFolder)
              .file("stream-test.txt", "Testing stream extraction")
              .build();

      ZipUtil.zip(sourceFolder, zipFile);
      var extractFolder = tempDir.resolve("extracted");

      // When
      try (var inputStream = Files.newInputStream(zipFile)) {
        ZipUtil.unzip(inputStream, extractFolder);
      }

      // Then
      verifyExtractedStructure(extractFolder, originalStructure);
    }

    @Test
    void should_create_target_directory_if_not_exists() throws IOException {
      // Given
      testDataBuilder.directory(sourceFolder).file("test.txt", "content").build();
      ZipUtil.zip(sourceFolder, zipFile);
      var deepExtractFolder = tempDir.resolve("very/deep/nested/extracted");

      // When
      ZipUtil.unzip(zipFile, deepExtractFolder);

      // Then
      assertThat(deepExtractFolder).exists().isDirectory();
      assertThat(deepExtractFolder.resolve("test.txt")).exists();
    }

    @Test
    void should_preserve_complex_directory_structure() throws IOException {
      // Given
      var structure =
          testDataBuilder
              .directory(sourceFolder)
              .file("root.txt", "Root level file")
              .subdirectory("level1")
              .file("file1.txt", "Level 1 content")
              .subdirectory("level2")
              .file("deep.txt", "Deep content")
              .file("another.txt", "Another deep file")
              .up()
              .up() // Back to root
              .subdirectory("parallel")
              .file("parallel.txt", "Parallel branch")
              .build();

      ZipUtil.zip(sourceFolder, zipFile);
      var extractFolder = tempDir.resolve("extracted");

      // When
      ZipUtil.unzip(zipFile, extractFolder);

      // Then
      assertThat(extractFolder.resolve("root.txt")).exists();
      assertThat(extractFolder.resolve("level1/file1.txt")).exists();
      assertThat(extractFolder.resolve("level1/level2/deep.txt")).exists();
      assertThat(extractFolder.resolve("level1/level2/another.txt")).exists();
      assertThat(extractFolder.resolve("parallel/parallel.txt")).exists();

      verifyExtractedStructure(extractFolder, structure);
    }

    @Test
    void should_handle_empty_directories() throws IOException {
      // Given
      testDataBuilder
          .directory(sourceFolder)
          .file("file.txt", "content")
          .emptyDirectory("empty1")
          .emptyDirectory("empty2")
          .build();

      ZipUtil.zip(sourceFolder, zipFile);
      var extractFolder = tempDir.resolve("extracted");

      // When
      ZipUtil.unzip(zipFile, extractFolder);

      // Then
      assertThat(extractFolder.resolve("file.txt")).exists();
      assertThat(extractFolder.resolve("empty1")).exists().isDirectory();
      assertThat(extractFolder.resolve("empty2")).exists().isDirectory();
    }
  }

  @Nested
  class Security_Tests {

    @Test
    void should_prevent_zip_slip_with_relative_paths() throws IOException {
      // Given
      var maliciousZip = createMaliciousZip("../../../malicious.txt");
      var extractFolder = tempDir.resolve("safe-zone");

      // When & Then
      var exception =
          assertThrows(IOException.class, () -> ZipUtil.unzip(maliciousZip, extractFolder));

      assertTrue(exception.getMessage().contains("Entry is outside the target directory"));
      assertFalse(Files.exists(tempDir.resolve("malicious.txt")));
    }

    @Test
    void should_prevent_zip_slip_with_absolute_paths() throws IOException {
      // Given
      var maliciousZip = createMaliciousZip("/tmp/malicious.txt");
      var extractFolder = tempDir.resolve("safe-zone");

      // When & Then
      var exception =
          assertThrows(IOException.class, () -> ZipUtil.unzip(maliciousZip, extractFolder));

      assertTrue(exception.getMessage().contains("Entry is outside the target directory"));
    }

    @Test
    void should_handle_multiple_directory_traversal_attempts() throws IOException {
      // Given
      var maliciousZip = tempDir.resolve("multi-attack.zip");
      var maliciousPaths =
          List.of("../../attack1.txt", "../../../attack2.txt", "../../../../attack3.txt");

      try (var zos = new ZipOutputStream(Files.newOutputStream(maliciousZip))) {
        for (var path : maliciousPaths) {
          zos.putNextEntry(new ZipEntry(path));
          zos.write(("Attack content: " + path).getBytes(StandardCharsets.UTF_8));
          zos.closeEntry();
        }
      }

      var extractFolder = tempDir.resolve("safe-zone");

      // When & Then
      assertThrows(IOException.class, () -> ZipUtil.unzip(maliciousZip, extractFolder));
    }
  }

  @Nested
  class Error_Handling_Tests {

    @Test
    void should_throw_null_pointer_exception_for_null_zip_parameters() {
      assertThrows(NullPointerException.class, () -> ZipUtil.zip(null, null));
      assertThrows(NullPointerException.class, () -> ZipUtil.zip(null, zipFile));
      assertThrows(NullPointerException.class, () -> ZipUtil.zip(sourceFolder, null));
    }

    @Test
    void should_throw_null_pointer_exception_for_null_unzip_parameters() {
      var extractFolder = tempDir.resolve("extracted");

      assertThrows(NullPointerException.class, () -> ZipUtil.unzip((Path) null, null));
      assertThrows(NullPointerException.class, () -> ZipUtil.unzip(zipFile, null));
      assertThrows(NullPointerException.class, () -> ZipUtil.unzip((Path) null, extractFolder));
      assertThrows(
          NullPointerException.class, () -> ZipUtil.unzip((InputStream) null, extractFolder));
    }

    @Test
    void should_throw_io_exception_for_non_existent_source_directory() {
      var nonExistentDir = tempDir.resolve("does-not-exist");

      var exception = assertThrows(IOException.class, () -> ZipUtil.zip(nonExistentDir, zipFile));

      assertTrue(exception.getMessage().contains("does not exist"));
    }

    @Test
    void should_throw_io_exception_for_file_instead_of_directory() throws IOException {
      var file = tempDir.resolve("not-a-directory.txt");
      Files.createFile(file);

      var exception = assertThrows(IOException.class, () -> ZipUtil.zip(file, zipFile));
      assertTrue(exception.getMessage().contains("not a directory"));
    }

    @Test
    void should_throw_io_exception_for_non_existent_zip_file() {
      var nonExistentZip = tempDir.resolve("missing.zip");
      var extractFolder = tempDir.resolve("extracted");

      var exception =
          assertThrows(IOException.class, () -> ZipUtil.unzip(nonExistentZip, extractFolder));

      assertTrue(exception.getMessage().contains("does not exist"));
    }

    @Test
    void should_handle_input_stream_that_throws_null_pointer() {
      var extractFolder = tempDir.resolve("extracted");

      assertThrows(
          NoSuchFileException.class,
          () -> ZipUtil.unzip(Files.newInputStream(zipFile), extractFolder));
    }
  }

  // ================= Helper Classes and Methods =================

  /** Fluent builder for creating test directory structures with real data */
  /** Fluent builder for creating test directory structures with real data */
  private static class TestDataBuilder {
    private Path rootDir;
    private Path currentDir;
    private final Set<FileStructure> createdFiles = new HashSet<>();

    TestDataBuilder directory(Path path) throws IOException {
      Files.createDirectories(path);
      this.rootDir = path;
      this.currentDir = path;
      return this;
    }

    TestDataBuilder file(String name, String content) throws IOException {
      var filePath = currentDir.resolve(name);
      Files.writeString(filePath, content, StandardCharsets.UTF_8);
      createdFiles.add(
          new FileStructure(
              rootDir.relativize(filePath), content.getBytes(StandardCharsets.UTF_8)));
      return this;
    }

    TestDataBuilder subdirectory(String name) throws IOException {
      currentDir = currentDir.resolve(name);
      Files.createDirectories(currentDir);
      return this;
    }

    TestDataBuilder emptyDirectory(String name) throws IOException {
      var dirPath = currentDir.resolve(name);
      Files.createDirectories(dirPath);
      createdFiles.add(
          new FileStructure(rootDir.relativize(dirPath), null)); // null indicates directory
      return this;
    }

    TestDataBuilder up() {
      currentDir = currentDir.getParent();
      return this;
    }

    Set<FileStructure> build() {
      return Set.copyOf(createdFiles);
    }
  }

  private record FileStructure(Path relativePath, byte[] content) {
    boolean isDirectory() {
      return content == null;
    }
  }

  /** Custom assertion helper for Path objects */
  private static PathAssert assertThat(Path actual) {
    return new PathAssert(actual);
  }

  private static class PathAssert {
    private final Path actual;

    PathAssert(Path actual) {
      this.actual = actual;
    }

    PathAssert exists() {
      assertTrue(Files.exists(actual), "Expected path to exist: " + actual);
      return this;
    }

    PathAssert isDirectory() {
      assertTrue(Files.isDirectory(actual), "Expected path to be directory: " + actual);
      return this;
    }

    PathAssert hasPositiveSize() throws IOException {
      assertTrue(Files.size(actual) > 0, "Expected file to have positive size: " + actual);
      return this;
    }
  }

  private void verifyExtractedStructure(Path extractFolder, Set<FileStructure> expectedStructure)
      throws IOException {
    for (var structure : expectedStructure) {
      var extractedPath = extractFolder.resolve(structure.relativePath());

      assertTrue(
          Files.exists(extractedPath),
          "Expected extracted file/directory to exist: " + structure.relativePath());

      if (structure.isDirectory()) {
        assertTrue(
            Files.isDirectory(extractedPath), "Expected directory: " + structure.relativePath());
      } else {
        var actualContent = Files.readAllBytes(extractedPath);
        assertArrayEquals(
            structure.content(),
            actualContent,
            "Content mismatch for file: " + structure.relativePath());
      }
    }
  }

  private void verifyZipContains(Path zipPath, String... expectedEntries) throws IOException {
    try (var zis = new ZipInputStream(Files.newInputStream(zipPath))) {
      var foundEntries = new HashSet<String>();

      ZipEntry entry;
      while ((entry = zis.getNextEntry()) != null) {
        foundEntries.add(entry.getName());
      }

      for (var expectedEntry : expectedEntries) {
        assertTrue(
            foundEntries.contains(expectedEntry),
            "Expected entry not found in zip: "
                + expectedEntry
                + ". Found entries: "
                + foundEntries);
      }
    }
  }

  private Path createMaliciousZip(String maliciousPath) throws IOException {
    var maliciousZip = tempDir.resolve("malicious.zip");

    try (var zos = new ZipOutputStream(Files.newOutputStream(maliciousZip))) {
      // Malicious entry
      zos.putNextEntry(new ZipEntry(maliciousPath));
      var content = "Malicious content attempting directory traversal";
      zos.write(content.getBytes(StandardCharsets.UTF_8));
      zos.closeEntry();

      // Normal entry for contrast
      zos.putNextEntry(new ZipEntry("legitimate-file.txt"));
      zos.write("Normal content".getBytes(StandardCharsets.UTF_8));
      zos.closeEntry();
    }

    return maliciousZip;
  }
}
