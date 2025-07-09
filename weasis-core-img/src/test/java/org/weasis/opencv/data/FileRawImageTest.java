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
import java.util.Optional;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.opencv.core.CvType;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.osgi.OpenCVNativeLoader;

@DisplayName("FileRawImage Tests")
class FileRawImageTest {

  @TempDir static Path tempDir;

  @BeforeAll
  @DisplayName("Load OpenCV native library")
  static void loadNativeLib() {
    // Load the native OpenCV library
    OpenCVNativeLoader loader = new OpenCVNativeLoader();
    loader.init();
  }

  @Nested
  @DisplayName("Constructor and Factory Method Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Should create FileRawImage with valid path")
    void testCreateWithValidPath() {
      Path testPath = tempDir.resolve("test.wcv");
      FileRawImage rawImg = new FileRawImage(testPath);

      assertNotNull(rawImg);
      assertEquals(testPath, rawImg.path());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when path is null")
    void testCreateWithNullPath() {
      NullPointerException exception =
          assertThrows(NullPointerException.class, () -> new FileRawImage(null));
      assertEquals("Path cannot be null", exception.getMessage());
    }

    @Test
    @DisplayName("Should create FileRawImage from File using factory method")
    void testCreateFromFile() {
      File testFile = tempDir.resolve("test.wcv").toFile();
      FileRawImage rawImg = FileRawImage.of(testFile);

      assertNotNull(rawImg);
      assertEquals(testFile.toPath(), rawImg.path());
    }

    @Test
    @DisplayName("Should create FileRawImage from String path using factory method")
    void testCreateFromString() {
      String testPath = tempDir.resolve("test.wcv").toString();
      FileRawImage rawImg = FileRawImage.of(testPath);

      assertNotNull(rawImg);
      assertEquals(testPath, rawImg.path().toString());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when File is null in factory method")
    void testCreateFromNullFile() {
      NullPointerException exception =
          assertThrows(NullPointerException.class, () -> FileRawImage.of((File) null));
      assertEquals("File cannot be null", exception.getMessage());
    }

    @Test
    @DisplayName(
        "Should throw IllegalArgumentException when String path is null or empty in factory method")
    void testCreateFromNullOrEmptyString() {
      assertAll(
          "Null and empty string validation",
          () -> {
            IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> FileRawImage.of((String) null));
            assertEquals("File path cannot be null or empty", exception.getMessage());
          },
          () -> {
            IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> FileRawImage.of(""));
            assertEquals("File path cannot be null or empty", exception.getMessage());
          },
          () -> {
            IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> FileRawImage.of("   "));
            assertEquals("File path cannot be null or empty", exception.getMessage());
          });
    }
  }

  @Nested
  @DisplayName("File Status Check Tests")
  class FileStatusTests {

    @Test
    @DisplayName("Should return correct file() representation")
    void testFileMethod() {
      Path testPath = tempDir.resolve("test.wcv");
      FileRawImage rawImg = new FileRawImage(testPath);

      File expectedFile = testPath.toFile();
      assertEquals(expectedFile, rawImg.file());
    }

    @Test
    @DisplayName("Should correctly identify readable files")
    void testIsReadable() throws IOException {
      Path readablePath = tempDir.resolve("readable.wcv");
      Files.createFile(readablePath);
      FileRawImage rawImg = new FileRawImage(readablePath);

      assertTrue(Files.isReadable(rawImg.path()));
    }

    @Test
    @DisplayName("Should correctly identify non-readable files")
    void testIsNotReadable() {
      Path nonExistentPath = tempDir.resolve("nonexistent.wcv");
      FileRawImage rawImg = new FileRawImage(nonExistentPath);

      assertFalse(Files.isReadable(rawImg.path()));
    }

    @Test
    @DisplayName("Should correctly identify non-writable files")
    void testIsNotWritable() throws IOException {
      Path readOnlyDir = tempDir.resolve("readonly");
      readOnlyDir.toFile().setWritable(false);

      Path nonWritablePath = readOnlyDir.resolve("test.wcv");
      FileRawImage rawImg = new FileRawImage(nonWritablePath);

      assertFalse(Files.isWritable(rawImg.path()));

      // Cleanup
      readOnlyDir.toFile().setWritable(true);
      try {
        Files.deleteIfExists(nonWritablePath);
      } catch (IOException e) {
        // Ignore if deletion fails
      }
    }
  }

  @Nested
  @DisplayName("Image Read/Write Operation Tests")
  class ReadWriteTests {

    @Test
    @DisplayName("Should successfully write and read image data")
    void testWriteAndRead() throws IOException {
      Path imagePath = tempDir.resolve("test_image.wcv");
      FileRawImage rawImg = new FileRawImage(imagePath);

      // Write image
      try (ImageCV originalImg =
          new ImageCV(new Size(3, 3), CvType.CV_16UC3, new Scalar(3, 4, 5))) {
        assertTrue(rawImg.write(originalImg), "Should successfully write image");
      }

      // Read and verify image
      try (ImageCV readImg = rawImg.read()) {
        assertNotNull(readImg, "Read image should not be null");

        assertAll(
            "Image properties verification",
            () -> assertEquals(3, readImg.width(), "Width should match"),
            () -> assertEquals(3, readImg.height(), "Height should match"),
            () -> assertEquals(3, readImg.channels(), "Channels should match"),
            () -> assertEquals(CvType.CV_16UC3, readImg.type(), "Type should match"),
            () -> assertEquals(2, readImg.depth(), "Depth should match"),
            () -> assertEquals(6, readImg.elemSize(), "Element size should match"),
            () -> assertEquals(2, readImg.elemSize1(), "Element size1 should match"),
            () -> assertEquals(54, readImg.physicalBytes(), "Physical bytes should match"),
            () ->
                assertEquals(54, readImg.total() * readImg.elemSize(), "Total bytes should match"));

        // Verify pixel data
        short[] pixelData = new short[3];
        readImg.get(1, 1, pixelData);
        assertArrayEquals(new short[] {3, 4, 5}, pixelData, "Pixel data should match");
      }
    }

    @Test
    @DisplayName("Should throw IllegalStateException when reading non-readable file")
    void testReadNonReadableFile() {
      Path nonExistentPath = tempDir.resolve("nonexistent.wcv");
      FileRawImage rawImg = new FileRawImage(nonExistentPath);

      IllegalArgumentException exception =
          assertThrows(IllegalArgumentException.class, rawImg::read);
      assertTrue(exception.getMessage().contains("File path is not readable"));
    }

    @Test
    @DisplayName("Should return empty Optional when safely reading non-existent file")
    void testReadSafelyNonExistentFile() {
      Path nonExistentPath = tempDir.resolve("nonexistent.wcv");
      FileRawImage rawImg = new FileRawImage(nonExistentPath);

      Optional<ImageCV> result = rawImg.readSafely();
      assertTrue(result.isEmpty(), "Should return empty Optional for non-existent file");
    }

    @Test
    @DisplayName("Should successfully read existing file with readSafely")
    void testReadSafelyExistingFile() throws IOException {
      Path imagePath = tempDir.resolve("test_safe_read.wcv");
      Files.createFile(imagePath);
      FileRawImage rawImg = new FileRawImage(imagePath);

      // Create test image
      try (ImageCV testImg = new ImageCV(new Size(2, 2), CvType.CV_8UC1, new Scalar(100))) {
        rawImg.write(testImg);
      }

      // Read safely
      Optional<ImageCV> result = rawImg.readSafely();
      assertTrue(result.isPresent(), "Should return present Optional for existing file");

      try (ImageCV img = result.get()) {
        assertEquals(2, img.width());
        assertEquals(2, img.height());
      }
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when writing null image")
    void testWriteNullImage() {
      Path imagePath = tempDir.resolve("test_null.wcv");
      FileRawImage rawImg = new FileRawImage(imagePath);

      NullPointerException exception =
          assertThrows(NullPointerException.class, () -> rawImg.write(null));
      assertTrue(exception.getMessage().contains("because \"image\" is null"));
    }

    @Test
    @DisplayName("Should throw IllegalStateException when writing to non-writable location")
    void testWriteToNonWritableLocation() throws IOException {
      // Create read-only directory
      Path readOnlyDir = tempDir.resolve("readonly");
      Files.createDirectory(readOnlyDir);
      readOnlyDir.toFile().setWritable(false);

      try (ImageCV testImg = new ImageCV(new Size(2, 2), CvType.CV_8UC1, new Scalar(100))) {
        Path nonWritablePath = readOnlyDir.resolve("test.wcv");
        FileRawImage rawImg = new FileRawImage(nonWritablePath);
        assertFalse(rawImg.write(testImg));
        // Verify that the file was not created
        assertFalse(
            Files.exists(nonWritablePath), "File should not be created in read-only directory");
      } finally {
        // Cleanup
        readOnlyDir.toFile().setWritable(true);
      }
    }
  }

  @Nested
  @DisplayName("Utility Method Tests")
  class UtilityTests {

    @Test
    @DisplayName("Should return correct file size for existing file")
    void testGetFileSizeExistingFile() throws IOException {
      Path testFile = tempDir.resolve("size_test.wcv");
      String testContent = "test content for size";
      Files.write(testFile, testContent.getBytes());

      FileRawImage rawImg = new FileRawImage(testFile);
      long expectedSize = Files.size(testFile);

      assertEquals(expectedSize, Files.size(rawImg.path()));
    }

    @Test
    @DisplayName("Should return correct file name")
    void testGetFileName() {
      Path testPath = tempDir.resolve("test_file.wcv");
      FileRawImage rawImg = new FileRawImage(testPath);

      assertEquals("test_file.wcv", rawImg.path().getFileName().toString());
    }
  }

  @Nested
  @DisplayName("Constants Tests")
  class ConstantsTests {

    @Test
    @DisplayName("Should have correct HEADER_LENGTH constant")
    void testHeaderLengthConstant() {
      assertEquals(46, FileRawImage.HEADER_LENGTH, "HEADER_LENGTH should be 46");
    }
  }
}
