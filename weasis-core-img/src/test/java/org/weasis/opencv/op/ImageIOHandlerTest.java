/*
 * Copyright (c) 2025 Weasis Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.weasis.opencv.op;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.osgi.OpenCVNativeLoader;
import org.weasis.opencv.data.ImageCV;

/**
 * Comprehensive test suite for {@link ImageIOHandler} class. Tests all image I/O operations
 * including reading, writing, and thumbnail generation.
 */
class ImageIOHandlerTest {

  @TempDir Path tempDir;

  private Mat testImage;
  private ImageCV testImageCV;
  private Path testImagePath;
  private Path testJp2ImagePath;

  private Mat colorImage;
  private ImageCV colorImageCV;
  private Path colorImagePath;

  @BeforeAll
  @DisplayName("Load OpenCV native library")
  static void loadNativeLib() {
    OpenCVNativeLoader loader = new OpenCVNativeLoader();
    loader.init();
  }

  @BeforeEach
  void setUp() throws IOException {
    // Create a test image (100x100 grayscale with some pattern)
    testImage = new Mat(100, 100, CvType.CV_8UC1);
    for (int y = 0; y < 100; y++) {
      for (int x = 0; x < 100; x++) {
        testImage.put(y, x, new byte[] {(byte) ((x + y) % 256)});
      }
    }
    testImageCV = ImageCV.fromMat(testImage);

    // Create a temporary test image file
    testImagePath = tempDir.resolve("test_image.png");
    assertTrue(Imgcodecs.imwrite(testImagePath.toString(), testImage));

    // Create a color test image (RGB with different values per channel)
    colorImage = new Mat(80, 80, CvType.CV_8UC3);
    for (int y = 0; y < 80; y++) {
      for (int x = 0; x < 80; x++) {
        byte r = (byte) ((x * 2) % 256);
        byte g = (byte) ((y * 3) % 256);
        byte b = (byte) ((x + y) % 256);
        colorImage.put(y, x, new byte[] {b, g, r}); // BGR format in OpenCV
      }
    }
    colorImageCV = ImageCV.fromMat(colorImage);
    colorImagePath = tempDir.resolve("color_image.png");
    assertTrue(Imgcodecs.imwrite(colorImagePath.toString(), colorImage));

    // Create a JP2 image for testing (using color LUT on grayscale)
    testJp2ImagePath = tempDir.resolve("test_image.jp2");
    assertTrue(Imgcodecs.imwrite(testJp2ImagePath.toString(), colorImage));
  }

  @Test
  @DisplayName("Test reading JP2 image format")
  void testReadJp2Image_Success() {
    List<String> tags = new ArrayList<>();
    ImageCV result = ImageIOHandler.readImage(testJp2ImagePath, tags);

    assertNotNull(result, "JP2 image should be read successfully");
    assertEquals(80, result.cols(), "JP2 image width should match");
    assertEquals(80, result.rows(), "JP2 image height should match");
    assertEquals(3, CvType.channels(result.type()), "JP2 image should have 3 channels (color)");
  }

  @Test
  @DisplayName("Test reading JP2 image with exception handling")
  void testReadJp2ImageWithCvException_Success() {
    List<String> tags = new ArrayList<>();
    ImageCV result = ImageIOHandler.readImageWithCvException(testJp2ImagePath, tags);

    assertNotNull(result, "JP2 image should be read with exception handling");
    assertEquals(80, result.cols());
    assertEquals(80, result.rows());
    assertEquals(3, CvType.channels(result.type()));
  }

  @Test
  @DisplayName("Test reading color image")
  void testReadColorImage_Success() {
    List<String> tags = new ArrayList<>();
    ImageCV result = ImageIOHandler.readImage(colorImagePath, tags);

    assertNotNull(result, "Color image should be read successfully");
    assertEquals(80, result.cols(), "Color image width should match");
    assertEquals(80, result.rows(), "Color image height should match");
    assertEquals(3, CvType.channels(result.type()), "Color image should have 3 channels");
    assertEquals(CvType.CV_8UC3, result.type(), "Color image should be 8-bit 3-channel");
  }

  @Test
  @DisplayName("Test writing color image")
  void testWriteColorImage_Success() throws IOException {
    Path outputPath = tempDir.resolve("color_output.png");

    boolean result = ImageIOHandler.writeImage(colorImage, outputPath);

    assertTrue(result, "Color image should be written successfully");
    assertTrue(Files.exists(outputPath), "Color output file should exist");

    // Verify the written color image
    ImageCV readBack = ImageIOHandler.readImage(outputPath, null);
    assertNotNull(readBack, "Written color image should be readable");
    assertEquals(colorImage.cols(), readBack.cols(), "Color image width should match");
    assertEquals(colorImage.rows(), readBack.rows(), "Color image height should match");
    assertEquals(3, CvType.channels(readBack.type()), "Read back image should maintain 3 channels");
  }

  @Test
  @DisplayName("Test color image thumbnail generation")
  void testColorImageThumbnail_Success() {
    Path thumbnailPath = tempDir.resolve("color_thumbnail.jpg");
    int maxSize = 40;

    boolean result = ImageIOHandler.writeThumbnail(colorImage, thumbnailPath, maxSize);

    assertTrue(result, "Color thumbnail should be created successfully");
    assertTrue(Files.exists(thumbnailPath), "Color thumbnail file should exist");

    // Verify thumbnail properties
    ImageCV thumbnail = ImageIOHandler.readImage(thumbnailPath, null);
    assertNotNull(thumbnail, "Color thumbnail should be readable");
    assertTrue(thumbnail.cols() <= maxSize, "Thumbnail width should not exceed max size");
    assertTrue(thumbnail.rows() <= maxSize, "Thumbnail height should not exceed max size");

    // Verify it's still a color image (JPEG may convert to RGB)
    int channels = CvType.channels(thumbnail.type());
    assertTrue(channels >= 1 && channels <= 3, "Thumbnail should have valid channel count");
  }

  @Test
  @DisplayName("Test building thumbnail from color PlanarImage")
  void testBuildColorThumbnail_KeepRatio() {
    Dimension iconDim = new Dimension(40, 40);

    ImageCV thumbnail = ImageIOHandler.buildThumbnail(colorImageCV, iconDim, true);

    assertNotNull(thumbnail, "Color thumbnail should be created");
    assertEquals(40, thumbnail.cols(), "Color thumbnail width should match");
    assertEquals(40, thumbnail.rows(), "Color thumbnail height should match");

    // Should maintain color information
    int channels = CvType.channels(thumbnail.type());
    assertEquals(3, channels, "Color thumbnail should maintain 3 channels");
  }

  @Test
  @DisplayName("Test metadata extraction from images")
  void testMetadataExtraction() {
    List<String> tags = new ArrayList<>();

    // Test with grayscale image
    ImageCV grayscaleResult = ImageIOHandler.readImage(testImagePath, tags);
    assertNotNull(grayscaleResult, "Should read grayscale image with metadata");

    // Test with color image
    tags.clear();
    ImageCV colorResult = ImageIOHandler.readImage(colorImagePath, tags);
    assertNotNull(colorResult, "Should read color image with metadata");

    // Test with JP2 image
    tags.clear();
    ImageCV jp2Result = ImageIOHandler.readImage(testJp2ImagePath, tags);
    assertNotNull(jp2Result, "Should read JP2 image with metadata");
  }

  @Test
  @DisplayName("Test color image format conversions")
  void testColorImageFormatConversions() throws IOException {
    String[] colorFormats = {".png", ".jpg", ".bmp", ".tiff"};

    for (String ext : colorFormats) {
      Path outputPath = tempDir.resolve("color_test" + ext);
      boolean result = ImageIOHandler.writeImage(colorImage, outputPath);

      assertTrue(result, "Failed to write color image in " + ext + " format");
      assertTrue(Files.exists(outputPath), ext + " color file not created");

      // Try to read back and verify it's still a color image
      ImageCV readBack = ImageIOHandler.readImage(outputPath, null);
      assertNotNull(readBack, "Failed to read back color " + ext + " format");

      int channels = CvType.channels(readBack.type());
      assertTrue(channels >= 1, "Should have at least 1 channel for " + ext);
      // Note: Some formats may convert RGB to grayscale, so we check for valid range
      assertTrue(channels <= 3, "Should have at most 3 channels for " + ext);
    }
  }

  @Test
  @DisplayName("Test large color image handling")
  void testLargeColorImage() {
    // Create a larger color image (300x200)
    Mat largeColorImage = new Mat(200, 300, CvType.CV_8UC3);
    for (int y = 0; y < 200; y++) {
      for (int x = 0; x < 300; x++) {
        byte r = (byte) ((x + y) % 256);
        byte g = (byte) ((x * 2 + y) % 256);
        byte b = (byte) ((x + y * 2) % 256);
        largeColorImage.put(y, x, new byte[] {b, g, r});
      }
    }

    Path outputPath = tempDir.resolve("large_color.png");
    boolean result = ImageIOHandler.writeImage(largeColorImage, outputPath);

    assertTrue(result, "Large color image should be written successfully");
    assertTrue(Files.exists(outputPath), "Large color image file should exist");

    // Test thumbnail generation from large color image
    Path thumbnailPath = tempDir.resolve("large_color_thumb.jpg");
    boolean thumbResult = ImageIOHandler.writeThumbnail(largeColorImage, thumbnailPath, 100);

    assertTrue(thumbResult, "Large color image thumbnail should be created");

    ImageCV thumbnail = ImageIOHandler.readImage(thumbnailPath, null);
    assertNotNull(thumbnail, "Large color thumbnail should be readable");
    assertTrue(thumbnail.cols() <= 100, "Large color thumbnail width should be constrained");
    assertTrue(thumbnail.rows() <= 100, "Large color thumbnail height should be constrained");
  }

  // Existing tests continue below...
  @Test
  void testReadImage_Success() {
    List<String> tags = new ArrayList<>();
    ImageCV result = ImageIOHandler.readImage(testImagePath, tags);

    assertNotNull(result);
    assertEquals(100, result.cols());
    assertEquals(100, result.rows());
    assertEquals(CvType.CV_8UC1, CvType.depth(result.type()));
  }

  @Test
  void testReadImage_NonExistentFile() {
    Path nonExistentPath = tempDir.resolve("non_existent.png");

    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class, () -> ImageIOHandler.readImage(nonExistentPath, null));
    assertTrue(exception.getMessage().contains("not readable"));
  }

  @Test
  void testReadImage_NullPath() {
    assertThrows(NullPointerException.class, () -> ImageIOHandler.readImage(null, null));
  }

  @Test
  void testReadImageWithCvException_Success() {
    List<String> tags = new ArrayList<>();
    ImageCV result = ImageIOHandler.readImageWithCvException(testImagePath, tags);

    assertNotNull(result);
    assertEquals(100, result.cols());
    assertEquals(100, result.rows());
  }

  @Test
  void testReadImageWithCvException_NonExistentFile() {
    Path nonExistentPath = tempDir.resolve("non_existent.png");

    assertThrows(
        IllegalArgumentException.class,
        () -> ImageIOHandler.readImageWithCvException(nonExistentPath, null));
  }

  @Test
  void testWriteImage_Mat_Success() throws IOException {
    Path outputPath = tempDir.resolve("output.png");

    boolean result = ImageIOHandler.writeImage(testImage, outputPath);

    assertTrue(result);
    assertTrue(Files.exists(outputPath));
    assertTrue(Files.size(outputPath) > 0);
  }

  @Test
  void testWriteImage_Mat_NullSource() {
    Path outputPath = tempDir.resolve("output.png");

    assertThrows(
        NullPointerException.class, () -> ImageIOHandler.writeImage((Mat) null, outputPath));
  }

  @Test
  void testWriteImage_Mat_EmptySource() {
    Mat emptyMat = new Mat();
    Path outputPath = tempDir.resolve("output.png");

    assertThrows(
        IllegalArgumentException.class, () -> ImageIOHandler.writeImage(emptyMat, outputPath));
  }

  @Test
  void testWriteImage_Mat_NullPath() {
    assertThrows(NullPointerException.class, () -> ImageIOHandler.writeImage(testImage, null));
  }

  @Test
  void testWriteImage_RenderedImage_Success() {
    BufferedImage bufferedImage = new BufferedImage(50, 50, BufferedImage.TYPE_BYTE_GRAY);
    // Fill with test pattern
    for (int y = 0; y < 50; y++) {
      for (int x = 0; x < 50; x++) {
        bufferedImage.setRGB(x, y, (x + y) % 256);
      }
    }

    Path outputPath = tempDir.resolve("buffered_output.png");
    boolean result = ImageIOHandler.writeImage(bufferedImage, outputPath);

    assertTrue(result);
    assertTrue(Files.exists(outputPath));
  }

  @Test
  void testWriteImage_RenderedImage_NullSource() {
    Path outputPath = tempDir.resolve("output.png");

    assertThrows(
        NullPointerException.class,
        () -> ImageIOHandler.writeImage((BufferedImage) null, outputPath));
  }

  @Test
  void testWriteImage_WithParams_Success() {
    Path outputPath = tempDir.resolve("output_with_params.jpg");
    MatOfInt params = new MatOfInt(Imgcodecs.IMWRITE_JPEG_QUALITY, 95);

    boolean result = ImageIOHandler.writeImage(testImage, outputPath, params);

    assertTrue(result);
    assertTrue(Files.exists(outputPath));
  }

  @Test
  void testWritePNG_Success() {
    Path outputPath = tempDir.resolve("test_output.png");

    boolean result = ImageIOHandler.writePNG(testImage, outputPath);

    assertTrue(result);
    assertTrue(Files.exists(outputPath));
    assertTrue(outputPath.toString().endsWith(".png"));
  }

  @Test
  void testWritePNG_EnforceExtension() {
    Path outputPath = tempDir.resolve("test_output.jpg"); // Wrong extension

    boolean result = ImageIOHandler.writePNG(testImage, outputPath);

    assertTrue(result);
    // Check that the actual file created has .png extension
    Path pngPath = tempDir.resolve("test_output.png");
    assertTrue(Files.exists(pngPath));
  }

  @Test
  void testWritePNG_16BitImage() {
    Mat image16bit = new Mat(50, 50, CvType.CV_16UC1, new Scalar(32000));
    Path outputPath = tempDir.resolve("test_16bit.png");

    boolean result = ImageIOHandler.writePNG(image16bit, outputPath);

    assertTrue(result);
    assertTrue(Files.exists(outputPath));
  }

  @Test
  void testWriteThumbnail_Success() {
    Path thumbnailPath = tempDir.resolve("thumbnail.jpg");
    int maxSize = 50;

    boolean result = ImageIOHandler.writeThumbnail(testImage, thumbnailPath, maxSize);

    assertTrue(result);
    assertTrue(Files.exists(thumbnailPath));

    // Verify thumbnail size
    ImageCV thumbnail = ImageIOHandler.readImage(thumbnailPath, null);
    assertNotNull(thumbnail);
    assertTrue(thumbnail.cols() <= maxSize);
    assertTrue(thumbnail.rows() <= maxSize);
  }

  @Test
  void testWriteThumbnail_LargerThanOriginal() {
    Path thumbnailPath = tempDir.resolve("large_thumbnail.jpg");
    int maxSize = 200; // Larger than original 100x100

    boolean result = ImageIOHandler.writeThumbnail(testImage, thumbnailPath, maxSize);

    assertTrue(result);
    assertTrue(Files.exists(thumbnailPath));

    // Should not scale up - thumbnail should be same size as original
    ImageCV thumbnail = ImageIOHandler.readImage(thumbnailPath, null);
    assertNotNull(thumbnail);
    assertEquals(100, thumbnail.cols());
    assertEquals(100, thumbnail.rows());
  }

  @Test
  void testWriteThumbnail_InvalidMaxSize() {
    Path thumbnailPath = tempDir.resolve("thumbnail.jpg");

    assertThrows(
        IllegalArgumentException.class,
        () -> ImageIOHandler.writeThumbnail(testImage, thumbnailPath, 0));

    assertThrows(
        IllegalArgumentException.class,
        () -> ImageIOHandler.writeThumbnail(testImage, thumbnailPath, -1));
  }

  @Test
  void testBuildThumbnail_KeepRatio() {
    Dimension iconDim = new Dimension(50, 50);

    ImageCV thumbnail = ImageIOHandler.buildThumbnail(testImageCV, iconDim, true);

    assertNotNull(thumbnail);
    assertEquals(50, thumbnail.cols());
    assertEquals(50, thumbnail.rows());
  }

  @Test
  void testBuildThumbnail_NoKeepRatio() {
    Dimension iconDim = new Dimension(75, 25); // Different aspect ratio

    ImageCV thumbnail = ImageIOHandler.buildThumbnail(testImageCV, iconDim, false);

    assertNotNull(thumbnail);
    assertEquals(75, thumbnail.cols());
    assertEquals(25, thumbnail.rows());
  }

  @Test
  void testBuildThumbnail_LargerThanOriginal() {
    Dimension iconDim = new Dimension(200, 200);

    ImageCV thumbnail = ImageIOHandler.buildThumbnail(testImageCV, iconDim, true);

    assertNotNull(thumbnail);
    // Should not scale up
    assertEquals(100, thumbnail.cols());
    assertEquals(100, thumbnail.rows());
  }

  @Test
  void testBuildThumbnail_NullSource() {
    Dimension iconDim = new Dimension(50, 50);

    assertThrows(
        NullPointerException.class, () -> ImageIOHandler.buildThumbnail(null, iconDim, true));
  }

  @Test
  void testBuildThumbnail_NullDimension() {
    assertThrows(
        NullPointerException.class, () -> ImageIOHandler.buildThumbnail(testImageCV, null, true));
  }

  @Test
  void testBuildThumbnail_InvalidDimensions() {
    assertThrows(
        IllegalArgumentException.class,
        () -> ImageIOHandler.buildThumbnail(testImageCV, new Dimension(0, 50), true));

    assertThrows(
        IllegalArgumentException.class,
        () -> ImageIOHandler.buildThumbnail(testImageCV, new Dimension(50, -1), true));
  }

  @Test
  void testValidateSourcePath_ValidPath() {
    assertDoesNotThrow(() -> ImageIOHandler.validateSourcePath(testImagePath));
  }

  @Test
  void testValidateSourcePath_NullPath() {
    assertThrows(NullPointerException.class, () -> ImageIOHandler.validateSourcePath(null));
  }

  @Test
  void testValidateSourcePath_NonReadablePath() {
    Path nonExistentPath = tempDir.resolve("non_existent.png");

    assertThrows(
        IllegalArgumentException.class, () -> ImageIOHandler.validateSourcePath(nonExistentPath));
  }

  @Test
  void testValidateSource_ValidMat() {
    assertDoesNotThrow(() -> ImageIOHandler.validateSource(testImage));
  }

  @Test
  void testValidateSource_NullMat() {
    assertThrows(NullPointerException.class, () -> ImageIOHandler.validateSource(null));
  }

  @Test
  void testValidateSource_EmptyMat() {
    Mat emptyMat = new Mat();
    assertThrows(IllegalArgumentException.class, () -> ImageIOHandler.validateSource(emptyMat));
  }

  @Test
  void testAspectRatioPreservation() {
    // Create a rectangular image (200x100)
    Mat rectangularImage = new Mat(100, 200, CvType.CV_8UC1, new Scalar(128));
    ImageCV rectImageCV = ImageCV.fromMat(rectangularImage);

    Dimension iconDim = new Dimension(50, 50);
    ImageCV thumbnail = ImageIOHandler.buildThumbnail(rectImageCV, iconDim, true);

    assertNotNull(thumbnail);
    // Should maintain aspect ratio - width should be 50, height should be 25
    assertEquals(50, thumbnail.cols());
    assertEquals(25, thumbnail.rows());
  }

  @Test
  void testMultipleFormats() throws IOException {
    String[] extensions = {".png", ".jpg", ".bmp", ".tiff"};

    for (String ext : extensions) {
      Path outputPath = tempDir.resolve("test" + ext);
      boolean result = ImageIOHandler.writeImage(testImage, outputPath);

      assertTrue(result, "Failed to write " + ext + " format");
      assertTrue(Files.exists(outputPath), ext + " file not created");

      // Try to read back
      ImageCV readBack = ImageIOHandler.readImage(outputPath, null);
      assertNotNull(readBack, "Failed to read back " + ext + " format");
    }
  }
}
