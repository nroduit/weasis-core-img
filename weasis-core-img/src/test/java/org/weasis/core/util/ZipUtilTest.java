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

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@DisplayName("ZipUtil Tests")
class ZipUtilTest {

  @TempDir Path tempDir;

  private Path sourceFolder;
  private Path zipFile;
  private byte[] testData;

  @BeforeEach
  void setUp() {
    sourceFolder = tempDir.resolve("testSource");
    zipFile = tempDir.resolve("test.zip");
    testData = "Test file content for ZipUtil".getBytes(StandardCharsets.UTF_8);
  }

  @Nested
  @DisplayName("Zip Creation Tests")
  class ZipCreationTests {

    @Test
    @DisplayName("Should create zip from directory with files")
    void shouldCreateZipFromDirectoryWithFiles() throws IOException {
      // Given
      createTestDirectoryStructure();

      // When
      ZipUtil.zip(sourceFolder, zipFile);

      // Then
      assertTrue(Files.exists(zipFile));
      assertTrue(Files.size(zipFile) > 0);
      verifyZipContents(zipFile, "test1.txt", "test2.txt", "subfolder/test3.txt");
    }

    @Test
    @DisplayName("Should create zip from empty directory")
    void shouldCreateZipFromEmptyDirectory() throws IOException {
      // Given
      Files.createDirectories(sourceFolder);

      // When
      ZipUtil.zip(sourceFolder, zipFile);

      // Then
      assertTrue(Files.exists(zipFile));
      assertTrue(Files.size(zipFile) > 0);
    }

    @Test
    @DisplayName("Should create zip with empty subdirectories")
    void shouldCreateZipWithEmptySubdirectories() throws IOException {
      // Given
      Files.createDirectories(sourceFolder.resolve("emptyFolder"));
      createTestFile(sourceFolder.resolve("test.txt"));

      // When
      ZipUtil.zip(sourceFolder, zipFile);

      // Then
      assertTrue(Files.exists(zipFile));
      verifyZipContains(zipFile, "emptyFolder/");
      verifyZipContains(zipFile, "test.txt");
    }

    @Test
    @DisplayName("Should create parent directories for zip file")
    void shouldCreateParentDirectoriesForZipFile() throws IOException {
      // Given
      createTestDirectoryStructure();
      Path nestedZipFile = tempDir.resolve("nested/deep/test.zip");

      // When
      ZipUtil.zip(sourceFolder, nestedZipFile);

      // Then
      assertTrue(Files.exists(nestedZipFile));
      assertTrue(Files.exists(nestedZipFile.getParent()));
    }

    @Test
    @DisplayName("Should handle special characters in file names")
    void shouldHandleSpecialCharactersInFileNames() throws IOException {
      // Given
      Files.createDirectories(sourceFolder);
      createTestFile(sourceFolder.resolve("file with spaces.txt"));
      createTestFile(sourceFolder.resolve("file-with-dashes.txt"));
      createTestFile(sourceFolder.resolve("file_with_underscores.txt"));

      // When
      ZipUtil.zip(sourceFolder, zipFile);

      // Then
      assertTrue(Files.exists(zipFile));
      verifyZipContents(
          zipFile, "file with spaces.txt", "file-with-dashes.txt", "file_with_underscores.txt");
    }
  }

  @Nested
  @DisplayName("Zip Extraction Tests")
  class ZipExtractionTests {

    @Test
    @DisplayName("Should extract zip to directory from Path")
    void shouldExtractZipToDirectoryFromPath() throws IOException {
      // Given
      createTestDirectoryStructure();
      ZipUtil.zip(sourceFolder, zipFile);
      Path extractFolder = tempDir.resolve("extracted");

      // When
      ZipUtil.unzip(zipFile, extractFolder);

      // Then
      verifyExtractedFiles(extractFolder);
    }

    @Test
    @DisplayName("Should extract zip to directory from InputStream")
    void shouldExtractZipToDirectoryFromInputStream() throws IOException {
      // Given
      createTestDirectoryStructure();
      ZipUtil.zip(sourceFolder, zipFile);
      Path extractFolder = tempDir.resolve("extracted");

      // When
      try (InputStream inputStream = Files.newInputStream(zipFile)) {
        ZipUtil.unzip(inputStream, extractFolder);
      }

      // Then
      verifyExtractedFiles(extractFolder);
    }

    @Test
    @DisplayName("Should create target directory if it doesn't exist")
    void shouldCreateTargetDirectoryIfNotExists() throws IOException {
      // Given
      createTestDirectoryStructure();
      ZipUtil.zip(sourceFolder, zipFile);
      Path extractFolder = tempDir.resolve("deep/nested/extracted");

      // When
      ZipUtil.unzip(zipFile, extractFolder);

      // Then
      assertTrue(Files.exists(extractFolder));
      verifyExtractedFiles(extractFolder);
    }

    @Test
    @DisplayName("Should preserve directory structure during extraction")
    void shouldPreserveDirectoryStructureDuringExtraction() throws IOException {
      // Given
      createComplexDirectoryStructure();
      ZipUtil.zip(sourceFolder, zipFile);
      Path extractFolder = tempDir.resolve("extracted");

      // When
      ZipUtil.unzip(zipFile, extractFolder);

      // Then
      assertTrue(Files.exists(extractFolder.resolve("level1/level2/deep.txt")));
      assertTrue(Files.exists(extractFolder.resolve("level1/file1.txt")));
      assertTrue(Files.exists(extractFolder.resolve("root.txt")));
      assertTrue(Files.isDirectory(extractFolder.resolve("level1/level2")));
    }

    @Test
    @DisplayName("Should handle empty directories in zip")
    void shouldHandleEmptyDirectoriesInZip() throws IOException {
      // Given
      Files.createDirectories(sourceFolder.resolve("emptyDir"));
      createTestFile(sourceFolder.resolve("test.txt"));
      ZipUtil.zip(sourceFolder, zipFile);
      Path extractFolder = tempDir.resolve("extracted");

      // When
      ZipUtil.unzip(zipFile, extractFolder);

      // Then
      assertTrue(Files.exists(extractFolder.resolve("test.txt")));
      assertTrue(Files.exists(extractFolder.resolve("emptyDir")));
      assertTrue(Files.isDirectory(extractFolder.resolve("emptyDir")));
    }
  }

  @Nested
  @DisplayName("Security Tests")
  class SecurityTests {

    @Test
    @DisplayName("Should prevent zip slip attack with relative paths")
    void shouldPreventZipSlipAttackWithRelativePaths() throws IOException {
      // Given
      Path maliciousZip = createMaliciousZip("../../../malicious.txt");
      Path extractFolder = tempDir.resolve("extracted");

      // When & Then
      IOException exception =
          assertThrows(IOException.class, () -> ZipUtil.unzip(maliciousZip, extractFolder));
      assertThat(exception.getMessage()).contains("Entry is outside the target directory");
    }

    @Test
    @DisplayName("Should prevent zip slip attack with absolute paths")
    void shouldPreventZipSlipAttackWithAbsolutePaths() throws IOException {
      // Given
      Path maliciousZip = createMaliciousZip("/tmp/malicious.txt");
      Path extractFolder = tempDir.resolve("extracted");

      // When & Then
      IOException exception =
          assertThrows(IOException.class, () -> ZipUtil.unzip(maliciousZip, extractFolder));
      assertThat(exception.getMessage()).contains("Entry is outside the target directory");
    }
  }

  @Nested
  @DisplayName("Error Handling Tests")
  class ErrorHandlingTests {

    @Test
    @DisplayName("Should throw IllegalArgumentException for null zip parameters")
    void shouldThrowIllegalArgumentExceptionForNullZipParameters() {
      assertThrows(IllegalArgumentException.class, () -> ZipUtil.zip(null, null));
      assertThrows(IllegalArgumentException.class, () -> ZipUtil.zip(null, zipFile));
      assertThrows(IllegalArgumentException.class, () -> ZipUtil.zip(sourceFolder, null));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException for null unzip parameters")
    void shouldThrowIllegalArgumentExceptionForNullUnzipParameters() {
      Path extractFolder = tempDir.resolve("extracted");

      assertThrows(IllegalArgumentException.class, () -> ZipUtil.unzip((Path) null, null));
      assertThrows(IllegalArgumentException.class, () -> ZipUtil.unzip(zipFile, null));
      assertThrows(IllegalArgumentException.class, () -> ZipUtil.unzip((Path) null, extractFolder));
      assertThrows(
          IllegalArgumentException.class, () -> ZipUtil.unzip((InputStream) null, extractFolder));
      assertThrows(
          NoSuchFileException.class, () -> ZipUtil.unzip(Files.newInputStream(zipFile), null));
    }

    @Test
    @DisplayName("Should throw IOException for non-existent source directory")
    void shouldThrowIOExceptionForNonExistentSourceDirectory() {
      Path nonExistentDir = tempDir.resolve("nonExistent");

      IOException exception =
          assertThrows(IOException.class, () -> ZipUtil.zip(nonExistentDir, zipFile));
      assertThat(exception.getMessage()).contains("does not exist");
    }

    @Test
    @DisplayName("Should throw IOException for file instead of directory")
    void shouldThrowIOExceptionForFileInsteadOfDirectory() throws IOException {
      Path file = tempDir.resolve("notADirectory.txt");
      Files.createFile(file);

      IOException exception = assertThrows(IOException.class, () -> ZipUtil.zip(file, zipFile));
      assertThat(exception.getMessage()).contains("not a directory");
    }

    @Test
    @DisplayName("Should throw IOException for non-existent zip file")
    void shouldThrowIOExceptionForNonExistentZipFile() {
      Path nonExistentZip = tempDir.resolve("nonExistent.zip");
      Path extractFolder = tempDir.resolve("extracted");

      IOException exception =
          assertThrows(IOException.class, () -> ZipUtil.unzip(nonExistentZip, extractFolder));
      assertThat(exception.getMessage()).contains("does not exist");
    }
  }

  // ******* Helper Methods *******

  private void createTestDirectoryStructure() throws IOException {
    Files.createDirectories(sourceFolder);
    createTestFile(sourceFolder.resolve("test1.txt"));
    createTestFile(sourceFolder.resolve("test2.txt"));

    Path subfolder = sourceFolder.resolve("subfolder");
    Files.createDirectories(subfolder);
    createTestFile(subfolder.resolve("test3.txt"));
  }

  private void createComplexDirectoryStructure() throws IOException {
    Files.createDirectories(sourceFolder);
    createTestFile(sourceFolder.resolve("root.txt"));

    Path level1 = sourceFolder.resolve("level1");
    Files.createDirectories(level1);
    createTestFile(level1.resolve("file1.txt"));

    Path level2 = level1.resolve("level2");
    Files.createDirectories(level2);
    createTestFile(level2.resolve("deep.txt"));
  }

  private void createTestFile(Path filePath) throws IOException {
    Files.write(filePath, testData);
  }

  private void verifyExtractedFiles(Path extractFolder) throws IOException {
    assertTrue(Files.exists(extractFolder.resolve("test1.txt")));
    assertTrue(Files.exists(extractFolder.resolve("test2.txt")));
    assertTrue(Files.exists(extractFolder.resolve("subfolder/test3.txt")));

    assertEquals(testData.length, Files.size(extractFolder.resolve("test1.txt")));
    assertArrayEquals(testData, Files.readAllBytes(extractFolder.resolve("test1.txt")));
  }

  private void verifyZipContents(Path zipPath, String... expectedEntries) throws IOException {
    try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipPath))) {
      java.util.Set<String> foundEntries = new java.util.HashSet<>();
      ZipEntry entry;

      while ((entry = zis.getNextEntry()) != null) {
        foundEntries.add(entry.getName());
      }

      for (String expectedEntry : expectedEntries) {
        assertTrue(
            foundEntries.contains(expectedEntry),
            "Expected entry not found in zip: " + expectedEntry);
      }
    }
  }

  private void verifyZipContains(Path zipPath, String expectedEntry) throws IOException {
    try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipPath))) {
      ZipEntry entry;
      while ((entry = zis.getNextEntry()) != null) {
        if (entry.getName().equals(expectedEntry)) {
          return; // Found the entry
        }
      }
    }
    fail("Expected entry not found in zip: " + expectedEntry);
  }

  private Path createMaliciousZip(String maliciousPath) throws IOException {
    Path maliciousZip = tempDir.resolve("malicious.zip");

    try (FileOutputStream fos = new FileOutputStream(maliciousZip.toFile());
        ZipOutputStream zos = new ZipOutputStream(fos)) {

      // Create a zip entry with a malicious path that attempts directory traversal
      ZipEntry maliciousEntry = new ZipEntry(maliciousPath);
      zos.putNextEntry(maliciousEntry);

      // Write some dummy content
      String content =
          "This is malicious content that should not be extracted outside the target directory";
      zos.write(content.getBytes(StandardCharsets.UTF_8));
      zos.closeEntry();

      // Optionally add a normal entry for comparison
      ZipEntry normalEntry = new ZipEntry("normal-file.txt");
      zos.putNextEntry(normalEntry);
      zos.write("Normal content".getBytes(StandardCharsets.UTF_8));
      zos.closeEntry();
    }

    return maliciousZip;
  }

  // Custom assertion helper
  private StringAssert assertThat(String actual) {
    return new StringAssert(actual);
  }

  private record StringAssert(String actual) {

    void contains(String expected) {
      assertTrue(
          actual != null && actual.contains(expected),
          "Expected string to contain: " + expected + ", but was: " + actual);
    }
  }
}
