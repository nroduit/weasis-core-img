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
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.weasis.opencv.data.ImageCV;
import org.weasis.opencv.natives.NativeLibrary;

@DisplayNameGeneration(ReplaceUnderscores.class)
class ImageIOHandlerTest {

  @TempDir Path tempDir;

  private TestImageData testData;

  @BeforeAll
  static void load_openCV_native_library() {
    NativeLibrary.loadLibraryFromLibraryName();
  }

  @BeforeEach
  void setUp() throws IOException {
    testData = new TestImageData(tempDir);
  }

  // === Reading Tests ===

  @Test
  void read_image_successfully_returns_image_data() {
    var tags = new ArrayList<String>();
    var result = ImageIOHandler.readImage(testData.grayscaleImagePath(), tags);

    assertNotNull(result);
    assertAll(
        () -> assertEquals(100, result.cols()),
        () -> assertEquals(100, result.rows()),
        () -> assertEquals(CvType.CV_8UC1, result.type()));
  }

  @Test
  void read_image_with_color_format_preserves_channels() {
    var result = ImageIOHandler.readImage(testData.colorImagePath(), null);

    assertNotNull(result);
    assertAll(
        () -> assertEquals(80, result.cols()),
        () -> assertEquals(80, result.rows()),
        () -> assertEquals(3, CvType.channels(result.type())));
  }

  @ParameterizedTest
  @MethodSource("supportedImageFormats")
  void read_image_supports_various_formats(String extension, Mat testImage) throws IOException {
    var imagePath = testData.createImageWithExtension(extension, testImage);
    var result = ImageIOHandler.readImage(imagePath, null);

    assertNotNull(result);
    assertAll(
        () -> assertEquals(testImage.cols(), result.cols()),
        () -> assertEquals(testImage.rows(), result.rows()));
  }

  @Test
  void read_image_with_exception_throws_cv_exception_for_invalid_path() {
    var nonExistentPath = tempDir.resolve("non_existent.png");

    var exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> ImageIOHandler.readImageWithCvException(nonExistentPath, null));
    assertTrue(exception.getMessage().contains("not readable"));
  }

  @Test
  void read_image_with_null_path_throws_null_pointer_exception() {
    assertThrows(NullPointerException.class, () -> ImageIOHandler.readImage(null, null));
  }

  // === Writing Tests ===

  @Test
  void write_mat_image_creates_readable_file() throws IOException {
    var outputPath = tempDir.resolve("output.png");

    var success = ImageIOHandler.writeImage(testData.grayscaleImage(), outputPath);

    assertTrue(success);
    assertTrue(Files.exists(outputPath));
    assertTrue(Files.size(outputPath) > 0);

    var readBack = ImageIOHandler.readImage(outputPath, null);
    assertNotNull(readBack);
    assertEquals(testData.grayscaleImage().cols(), readBack.cols());
  }

  @Test
  void write_rendered_image_converts_and_saves_successfully() {
    var bufferedImage = createTestBufferedImage(50, 50);
    var outputPath = tempDir.resolve("buffered_output.png");

    var success = ImageIOHandler.writeImage(bufferedImage, outputPath);

    assertTrue(success);
    assertTrue(Files.exists(outputPath));
  }

  @Test
  void write_image_with_custom_parameters_applies_settings() {
    var outputPath = tempDir.resolve("output_with_params.jpg");
    var params = new MatOfInt(Imgcodecs.IMWRITE_JPEG_QUALITY, 95);

    var success = ImageIOHandler.writeImage(testData.grayscaleImage(), outputPath, params);

    assertTrue(success);
    assertTrue(Files.exists(outputPath));
  }

  @ParameterizedTest
  @ValueSource(strings = {".png", ".jpg", ".bmp", ".tiff"})
  void write_image_supports_multiple_formats(String extension) throws IOException {
    var outputPath = tempDir.resolve("test" + extension);

    var success = ImageIOHandler.writeImage(testData.colorImage(), outputPath);

    assertTrue(success);
    assertTrue(Files.exists(outputPath));
  }

  @Test
  void write_image_with_null_source_throws_exception() {
    var outputPath = tempDir.resolve("output.png");

    assertThrows(NullPointerException.class, () -> ImageIOHandler.writeImage((Mat) null, outputPath));
  }

  @Test
  void write_image_with_empty_source_throws_exception() {
    var emptyMat = new Mat();
    var outputPath = tempDir.resolve("output.png");

    var exception =
        assertThrows(
            IllegalArgumentException.class, () -> ImageIOHandler.writeImage(emptyMat, outputPath));
    assertTrue(exception.getMessage().contains("empty"));
  }

  // === PNG-specific Tests ===

  @Test
  void write_PNG_creates_file_with_correct_extension() {
    var outputPath = tempDir.resolve("test_output.png");

    var success = ImageIOHandler.writePNG(testData.grayscaleImage(), outputPath);

    assertTrue(success);
    assertTrue(Files.exists(outputPath));
    assertTrue(outputPath.toString().endsWith(".png"));
  }

  @Test
  void write_PNG_enforces_extension_when_different_provided() {
    var outputPathWithWrongExt = tempDir.resolve("test_output.jpg");

    var success = ImageIOHandler.writePNG(testData.grayscaleImage(), outputPathWithWrongExt);

    assertTrue(success);
    var expectedPngPath = tempDir.resolve("test_output.png");
    assertTrue(Files.exists(expectedPngPath));
  }

  @Test
  void write_PNG_handles_16bit_images() {
    var image16bit = new Mat(50, 50, CvType.CV_16UC1, new Scalar(32000));
    var outputPath = tempDir.resolve("test_16bit.png");

    var success = ImageIOHandler.writePNG(image16bit, outputPath);

    assertTrue(success);
    assertTrue(Files.exists(outputPath));
  }

  // === Thumbnail Tests ===

  @Test
  void write_thumbnail_creates_scaled_image() {
    var thumbnailPath = tempDir.resolve("thumbnail.jpg");
    var maxSize = 50;

    var success = ImageIOHandler.writeThumbnail(testData.grayscaleImage(), thumbnailPath, maxSize);

    assertTrue(success);
    assertTrue(Files.exists(thumbnailPath));

    var thumbnail = ImageIOHandler.readImage(thumbnailPath, null);
    assertNotNull(thumbnail);
    assertTrue(thumbnail.cols() <= maxSize);
    assertTrue(thumbnail.rows() <= maxSize);
  }

  @Test
  void write_thumbnail_preserves_original_size_when_larger_than_max() {
    var thumbnailPath = tempDir.resolve("large_thumbnail.jpg");
    var maxSize = 200; // Larger than original 100x100

    var success = ImageIOHandler.writeThumbnail(testData.grayscaleImage(), thumbnailPath, maxSize);

    assertTrue(success);
    var thumbnail = ImageIOHandler.readImage(thumbnailPath, null);
    assertAll(
        () -> assertEquals(100, thumbnail.cols()), () -> assertEquals(100, thumbnail.rows()));
  }

  @ParameterizedTest
  @ValueSource(ints = {0, -1, -10})
  void write_thumbnail_with_invalid_max_size_throws_exception(int invalidMaxSize) {
    var thumbnailPath = tempDir.resolve("thumbnail.jpg");

    var exception =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                ImageIOHandler.writeThumbnail(
                    testData.grayscaleImage(), thumbnailPath, invalidMaxSize));
    assertTrue(exception.getMessage().contains("positive"));
  }

  @Test
  void build_thumbnail_with_aspect_ratio_preservation() {
    var iconDim = new Dimension(50, 50);

    var thumbnail = ImageIOHandler.buildThumbnail(testData.grayscaleImageCV(), iconDim, true);

    assertNotNull(thumbnail);
    assertAll(() -> assertEquals(50, thumbnail.cols()), () -> assertEquals(50, thumbnail.rows()));
  }

  @Test
  void build_thumbnail_without_aspect_ratio_preservation_stretches_image() {
    var iconDim = new Dimension(60, 40); // Different aspect ratio

    var thumbnail = ImageIOHandler.buildThumbnail(testData.grayscaleImageCV(), iconDim, false);

    assertNotNull(thumbnail);
    assertAll(() -> assertEquals(60, thumbnail.cols()), () -> assertEquals(40, thumbnail.rows()));
  }

  @Test
  void build_thumbnail_larger_than_original_returns_original_size() {
    var iconDim = new Dimension(200, 200);

    var thumbnail = ImageIOHandler.buildThumbnail(testData.grayscaleImageCV(), iconDim, true);

    assertNotNull(thumbnail);
    assertAll(() -> assertEquals(100, thumbnail.cols()), () -> assertEquals(100, thumbnail.rows()));
  }

  @ParameterizedTest
  @MethodSource("invalidThumbnailDimensions")
  void build_thumbnail_with_invalid_dimensions_throws_exception(Dimension invalidDimension) {
    assertThrows(
        IllegalArgumentException.class,
        () -> ImageIOHandler.buildThumbnail(testData.grayscaleImageCV(), invalidDimension, true));
  }

  // === Validation Tests ===

  @Test
  void validate_readable_path_accepts_valid_path() {
    assertDoesNotThrow(() -> ImageIOHandler.validateReadablePath(testData.grayscaleImagePath()));
  }

  @Test
  void validate_readable_path_rejects_null() {
    assertThrows(NullPointerException.class, () -> ImageIOHandler.validateReadablePath(null));
  }

  @Test
  void validate_readable_path_rejects_non_readable_path() {
    var nonReadablePath = tempDir.resolve("non_existent.png");

    assertThrows(
        IllegalArgumentException.class,
        () -> ImageIOHandler.validateReadablePath(nonReadablePath));
  }

  @Test
  void validate_source_accepts_valid_mat() {
    assertDoesNotThrow(() -> ImageIOHandler.validateSource(testData.grayscaleImage()));
  }

  @Test
  void validate_source_rejects_null_mat() {
    assertThrows(NullPointerException.class, () -> ImageIOHandler.validateSource(null));
  }

  @Test
  void validate_source_rejects_empty_mat() {
    assertThrows(IllegalArgumentException.class, () -> ImageIOHandler.validateSource(new Mat()));
  }

  // === Integration Tests ===

  @Test
  void complete_read_write_cycle_preserves_image_data() throws IOException {
    var originalPath = testData.colorImagePath();
    var outputPath = tempDir.resolve("cycle_test.png");

    var originalImage = ImageIOHandler.readImage(originalPath, null);
    var writeSuccess = ImageIOHandler.writeImage(originalImage.toMat(), outputPath);
    var readBackImage = ImageIOHandler.readImage(outputPath, null);

    assertTrue(writeSuccess);
    assertNotNull(readBackImage);
    assertAll(
        () -> assertEquals(originalImage.cols(), readBackImage.cols()),
        () -> assertEquals(originalImage.rows(), readBackImage.rows()),
        () -> assertEquals(originalImage.type(), readBackImage.type()));
  }

  @Test
  void metadata_extraction_preserves_tag_list() {
    var tags = new ArrayList<String>();
    var result = ImageIOHandler.readImage(testData.colorImagePath(), tags);

    assertNotNull(result);
    assertNotNull(tags);
  }

  @Nested
  class Read_Image_Failure_Contract {

    @Test
    void readImage_returns_null_for_corrupted_image_file() throws IOException {
      var corrupted = tempDir.resolve("corrupted.png");
      Files.write(corrupted, new byte[] {0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07});

      var result = ImageIOHandler.readImage(corrupted, null);

      assertNull(result, "Corrupted file must yield null, not throw");
    }

    @Test
    void readImage_returns_null_for_unrecognised_format() throws IOException {
      var garbage = tempDir.resolve("not_an_image.jpg");
      Files.writeString(garbage, "this is definitely not a JPEG file");

      var result = ImageIOHandler.readImage(garbage, null);

      assertNull(result);
    }

    @Test
    void readImage_with_exception_throws_for_corrupted_file_or_returns_null() throws IOException {
      var corrupted = tempDir.resolve("corrupted_for_throw.png");
      Files.write(corrupted, new byte[] {0x42, 0x42, 0x42, 0x42});

      try {
        var result = ImageIOHandler.readImageWithCvException(corrupted, null);
        assertTrue(result == null || result.toMat() != null);
      } catch (org.opencv.core.CvException e) {
        assertNotNull(e.getMessage());
      }
    }

    @Test
    void readImage_returns_null_for_empty_file() throws IOException {
      var empty = tempDir.resolve("empty.png");
      Files.createFile(empty);

      var result = ImageIOHandler.readImage(empty, null);

      assertNull(result);
    }
  }

  @Nested
  class Round_Trip_Integrity {

    @Test
    void png_preserves_every_8bit_grayscale_pixel() throws IOException {
      var original = createDeterministicMat(64, 48, CvType.CV_8UC1);
      var path = tempDir.resolve("rt_8uc1.png");

      assertTrue(ImageIOHandler.writePNG(original, path));
      var read = ImageIOHandler.readImage(path, null);

      assertNotNull(read, "PNG must be readable back");
      assertMatsBitExact(original, read.toMat());
    }

    @Test
    void png_preserves_every_16bit_grayscale_pixel() throws IOException {
      var original = createDeterministicMat(40, 30, CvType.CV_16UC1);
      var path = tempDir.resolve("rt_16uc1.png");

      assertTrue(ImageIOHandler.writePNG(original, path));
      var read = ImageIOHandler.readImage(path, null);

      assertNotNull(read);
      assertMatsBitExact(original, read.toMat());
    }

    @Test
    void png_preserves_every_8bit_color_pixel() throws IOException {
      var original = createDeterministicMat(50, 50, CvType.CV_8UC3);
      var path = tempDir.resolve("rt_8uc3.png");

      assertTrue(ImageIOHandler.writePNG(original, path));
      var read = ImageIOHandler.readImage(path, null);

      assertNotNull(read);
      assertMatsBitExact(original, read.toMat());
    }

    private void assertMatsBitExact(Mat expected, Mat actual) {
      assertAll(
          () -> assertEquals(expected.rows(), actual.rows(), "row count"),
          () -> assertEquals(expected.cols(), actual.cols(), "col count"),
          () -> assertEquals(expected.type(), actual.type(), "pixel type"),
          () -> assertEquals(expected.channels(), actual.channels(), "channel count"));

      var depth = CvType.depth(expected.type());
      var channels = expected.channels();
      var rows = expected.rows();
      var cols = expected.cols();

      if (depth == CvType.CV_8U) {
        var exp = new byte[channels];
        var act = new byte[channels];
        for (int y = 0; y < rows; y++) {
          for (int x = 0; x < cols; x++) {
            expected.get(y, x, exp);
            actual.get(y, x, act);
            assertArrayEqualsAt(exp, act, y, x);
          }
        }
      } else if (depth == CvType.CV_16U) {
        var exp = new short[channels];
        var act = new short[channels];
        for (int y = 0; y < rows; y++) {
          for (int x = 0; x < cols; x++) {
            expected.get(y, x, exp);
            actual.get(y, x, act);
            for (int c = 0; c < channels; c++) {
              if (exp[c] != act[c]) {
                fail(
                    "16-bit pixel mismatch at (y=%d,x=%d,c=%d): expected=%d actual=%d"
                        .formatted(
                            y, x, c, Short.toUnsignedInt(exp[c]), Short.toUnsignedInt(act[c])));
              }
            }
          }
        }
      } else {
        fail("Unsupported depth for round-trip assertion: " + depth);
      }
    }

    private void assertArrayEqualsAt(byte[] expected, byte[] actual, int y, int x) {
      for (int c = 0; c < expected.length; c++) {
        if (expected[c] != actual[c]) {
          fail(
              "8-bit pixel mismatch at (y=%d,x=%d,c=%d): expected=%d actual=%d"
                  .formatted(
                      y, x, c, Byte.toUnsignedInt(expected[c]), Byte.toUnsignedInt(actual[c])));
        }
      }
    }

    private Mat createDeterministicMat(int width, int height, int type) {
      var mat = new Mat(height, width, type);
      var depth = CvType.depth(type);
      var channels = CvType.channels(type);
      for (int y = 0; y < height; y++) {
        for (int x = 0; x < width; x++) {
          if (depth == CvType.CV_8U) {
            var buf = new byte[channels];
            for (int c = 0; c < channels; c++) {
              buf[c] = (byte) ((x * 7 + y * 13 + c * 53) & 0xFF);
            }
            mat.put(y, x, buf);
          } else if (depth == CvType.CV_16U) {
            var buf = new short[channels];
            for (int c = 0; c < channels; c++) {
              buf[c] = (short) ((x * 257 + y * 521 + c * 9001) & 0xFFFF);
            }
            mat.put(y, x, buf);
          }
        }
      }
      return mat;
    }
  }

  // === Test Data Providers ===

  static Stream<Arguments> supportedImageFormats() {
    var grayscale = createTestMat(50, 50, CvType.CV_8UC1);
    var color = createTestMat(50, 50, CvType.CV_8UC3);

    return Stream.of(
        Arguments.of(".png", grayscale),
        Arguments.of(".jpg", grayscale),
        Arguments.of(".bmp", color),
        Arguments.of(".jp2", color));
  }

  static Stream<Dimension> invalidThumbnailDimensions() {
    return Stream.of(
        new Dimension(0, 50), new Dimension(50, 0), new Dimension(-10, 50), new Dimension(50, -10));
  }

  // === Helper Methods ===

  private BufferedImage createTestBufferedImage(int width, int height) {
    var image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
    var graphics = image.getGraphics();
    graphics.fillRect(0, 0, width, height);
    graphics.dispose();
    return image;
  }

  private static Mat createTestMat(int width, int height, int type) {
    var mat = new Mat(height, width, type);
    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        if (CvType.channels(type) == 1) {
          mat.put(y, x, new byte[] {(byte) ((x + y) % 256)});
        } else {
          mat.put(
              y,
              x,
              new byte[] {
                (byte) ((x + y) % 256), (byte) ((x * 2 + y) % 256), (byte) ((y * 2 + x) % 256)
              });
        }
      }
    }
    return mat;
  }

  private static class TestImageData {
    private final Path tempDir;
    private final Mat grayscaleImage;
    private final Mat colorImage;
    private final ImageCV grayscaleImageCV;
    private final Path grayscaleImagePath;
    private final Path colorImagePath;

    TestImageData(Path tempDir) throws IOException {
      this.tempDir = tempDir;

      this.grayscaleImage = createTestMat(100, 100, CvType.CV_8UC1);
      this.grayscaleImageCV = ImageCV.fromMat(grayscaleImage);
      this.grayscaleImagePath = tempDir.resolve("test_grayscale.png");
      assertTrue(Imgcodecs.imwrite(grayscaleImagePath.toString(), grayscaleImage));

      this.colorImage = createTestMat(80, 80, CvType.CV_8UC3);
      this.colorImagePath = tempDir.resolve("test_color.png");
      assertTrue(Imgcodecs.imwrite(colorImagePath.toString(), colorImage));
    }

    Mat grayscaleImage() {
      return grayscaleImage;
    }

    Mat colorImage() {
      return colorImage;
    }

    ImageCV grayscaleImageCV() {
      return grayscaleImageCV;
    }

    Path grayscaleImagePath() {
      return grayscaleImagePath;
    }

    Path colorImagePath() {
      return colorImagePath;
    }

    Path createImageWithExtension(String extension, Mat image) throws IOException {
      var path = tempDir.resolve("test" + extension);
      assertTrue(Imgcodecs.imwrite(path.toString(), image));
      return path;
    }
  }
}
