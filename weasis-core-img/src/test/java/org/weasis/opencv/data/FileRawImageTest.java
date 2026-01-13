/*
 * Copyright (c) 2025 Weasis Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.weasis.opencv.data;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.opencv.core.CvType;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.weasis.opencv.natives.NativeLibrary;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class FileRawImageTest {

  @TempDir static Path tempDir;

  @BeforeAll
  static void load_native_library() {
    NativeLibrary.loadLibraryFromLibraryName();
  }

  @Nested
  class Constructor_and_Factory_Methods {

    @Test
    void should_create_with_valid_path() {
      var testPath = tempDir.resolve("test.wcv");
      var rawImg = new FileRawImage(testPath);

      assertAll(() -> assertNotNull(rawImg), () -> assertEquals(testPath, rawImg.path()));
    }

    @Test
    void should_reject_null_path() {
      var exception = assertThrows(NullPointerException.class, () -> new FileRawImage(null));
      assertEquals("Path cannot be null", exception.getMessage());
    }

    @Test
    void should_create_from_file() {
      var testFile = tempDir.resolve("test.wcv").toFile();
      var rawImg = FileRawImage.of(testFile);

      assertAll(() -> assertNotNull(rawImg), () -> assertEquals(testFile.toPath(), rawImg.path()));
    }

    @Test
    void should_create_from_string_path() {
      var testPath = tempDir.resolve("test.wcv").toString();
      var rawImg = FileRawImage.of(testPath);

      assertAll(
          () -> assertNotNull(rawImg), () -> assertEquals(testPath, rawImg.path().toString()));
    }

    @Test
    void should_reject_null_file() {
      var exception = assertThrows(NullPointerException.class, () -> FileRawImage.of((File) null));
      assertEquals("File cannot be null", exception.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "\t", "\n"})
    @NullSource
    void should_reject_invalid_string_paths(String invalidPath) {
      var exception =
          assertThrows(IllegalArgumentException.class, () -> FileRawImage.of(invalidPath));
      assertEquals("File path cannot be null or empty", exception.getMessage());
    }
  }

  @Nested
  class File_Operations {

    @Test
    void should_provide_legacy_file_access() {
      var testPath = tempDir.resolve("test.wcv");
      var rawImg = new FileRawImage(testPath);

      assertEquals(testPath.toFile(), rawImg.file());
    }

    @Test
    void should_have_correct_string_representation() {
      var testPath = tempDir.resolve("test.wcv");
      var rawImg = new FileRawImage(testPath);

      assertEquals("FileRawImage[path=" + testPath + "]", rawImg.toString());
    }

    @Test
    void should_handle_header_length_constant() {
      assertEquals(46, FileRawImage.HEADER_LENGTH);
    }
  }

  @Nested
  class Image_Read_Write_Operations {

    private ImageCV createTestImage(Size size, int type, Scalar value) {
      return new ImageCV(size, type, value);
    }

    private void verifyImageProperties(
        ImageCV image,
        int expectedWidth,
        int expectedHeight,
        int expectedChannels,
        int expectedType) {
      assertAll(
          "Image properties",
          () -> assertEquals(expectedWidth, image.width()),
          () -> assertEquals(expectedHeight, image.height()),
          () -> assertEquals(expectedChannels, image.channels()),
          () -> assertEquals(expectedType, image.type()));
    }

    @Test
    void should_write_and_read_image_successfully() throws IOException {
      var imagePath = tempDir.resolve("test_image.wcv");
      var rawImg = new FileRawImage(imagePath);

      // Create and write test image
      try (var originalImg =
          createTestImage(new Size(3, 3), CvType.CV_16UC3, new Scalar(3, 4, 5))) {
        assertTrue(rawImg.write(originalImg), "Should successfully write image");
      }

      // Read and verify
      try (var readImg = rawImg.read()) {
        assertNotNull(readImg, "Read image should not be null");

        verifyImageProperties(readImg, 3, 3, 3, CvType.CV_16UC3);

        // Verify additional properties
        assertAll(
            "Additional image properties",
            () -> assertEquals(2, readImg.depth()),
            () -> assertEquals(6, readImg.elemSize()),
            () -> assertEquals(2, readImg.elemSize1()),
            () -> assertEquals(54, readImg.physicalBytes()));

        // Verify pixel data
        var pixelData = new short[3];
        readImg.get(1, 1, pixelData);
        assertArrayEquals(new short[] {3, 4, 5}, pixelData);
      }
    }

    @Test
    void should_handle_different_image_types() throws IOException {
      var testData =
          new Object[][] {
            {new Size(2, 2), CvType.CV_8UC1, new Scalar(100), 4L},
            {new Size(3, 3), CvType.CV_8UC3, new Scalar(50, 100, 150), 27L},
            {new Size(4, 4), CvType.CV_32FC1, new Scalar(3.14), 64L}
          };

      for (var data : testData) {
        var size = (Size) data[0];
        var type = (int) data[1];
        var scalar = (Scalar) data[2];
        var expectedBytes = (long) data[3];

        var imagePath = tempDir.resolve("test_type_" + type + ".wcv");
        var rawImg = new FileRawImage(imagePath);

        try (var testImg = createTestImage(size, type, scalar)) {
          assertTrue(rawImg.write(testImg));

          try (var readImg = rawImg.read()) {
            assertEquals(expectedBytes, readImg.physicalBytes());
          }
        }
      }
    }

    @Test
    void should_throw_exception_for_non_readable_file() {
      var nonExistentPath = tempDir.resolve("nonexistent.wcv");
      var rawImg = new FileRawImage(nonExistentPath);

      var exception = assertThrows(IllegalArgumentException.class, rawImg::read);
      assertTrue(exception.getMessage().contains("File path is not readable"));
    }

    @Test
    void should_return_empty_optional_for_safe_read_failure() {
      var nonExistentPath = tempDir.resolve("nonexistent.wcv");
      var rawImg = new FileRawImage(nonExistentPath);

      var result = rawImg.readSafely();
      assertTrue(result.isEmpty());
    }

    @Test
    void should_successfully_safe_read_existing_file() throws IOException {
      var imagePath = tempDir.resolve("safe_read_test.wcv");
      var rawImg = new FileRawImage(imagePath);

      // Create and write test image
      try (var testImg = createTestImage(new Size(2, 2), CvType.CV_8UC1, new Scalar(100))) {
        rawImg.write(testImg);
      }

      // Safe read
      var result = rawImg.readSafely();
      assertTrue(result.isPresent());

      try (var img = result.orElseThrow()) {
        verifyImageProperties(img, 2, 2, 1, CvType.CV_8UC1);
      }
    }

    @Test
    void should_reject_null_image_for_write() {
      var imagePath = tempDir.resolve("null_test.wcv");
      var rawImg = new FileRawImage(imagePath);

      var exception = assertThrows(NullPointerException.class, () -> rawImg.write(null));
      assertEquals("Image cannot be null", exception.getMessage());
    }

    @Test
    void should_handle_write_failure_gracefully() throws IOException {
      // Create read-only directory on Unix-like systems
      var readOnlyDir = tempDir.resolve("readonly");
      Files.createDirectory(readOnlyDir);
      var readOnlyFile = readOnlyDir.toFile();

      try {
        readOnlyFile.setWritable(false);

        var nonWritablePath = readOnlyDir.resolve("test.wcv");
        var rawImg = new FileRawImage(nonWritablePath);

        try (var testImg = createTestImage(new Size(2, 2), CvType.CV_8UC1, new Scalar(100))) {
          assertFalse(rawImg.write(testImg));
          assertFalse(Files.exists(nonWritablePath));
        }
      } finally {
        readOnlyFile.setWritable(true);
      }
    }
  }

  @Nested
  class File_System_Integration {

    @Test
    void should_handle_file_size_operations() throws IOException {
      var testFile = tempDir.resolve("size_test.wcv");
      var testContent = "test content for size verification";
      Files.write(testFile, testContent.getBytes());

      var rawImg = new FileRawImage(testFile);
      var expectedSize = Files.size(testFile);

      assertEquals(expectedSize, Files.size(rawImg.path()));
    }

    @Test
    void should_extract_file_name_correctly() {
      var testCases =
          new String[][] {
            {"simple.wcv", "simple.wcv"},
            {"path/to/file.wcv", "file.wcv"},
            {"complex-name_123.wcv", "complex-name_123.wcv"}
          };

      for (var testCase : testCases) {
        var path = tempDir.resolve(testCase[0]);
        var rawImg = new FileRawImage(path);
        var fileName = path.getFileName();

        assertEquals(testCase[1], fileName != null ? fileName.toString() : "");
      }
    }

    @Test
    void should_work_with_different_path_types() {
      var absolutePath = tempDir.resolve("absolute.wcv").toAbsolutePath();
      var relativePath = Path.of("relative.wcv");

      assertAll(
          () -> assertDoesNotThrow(() -> new FileRawImage(absolutePath)),
          () -> assertDoesNotThrow(() -> new FileRawImage(relativePath)));
    }
  }
}
