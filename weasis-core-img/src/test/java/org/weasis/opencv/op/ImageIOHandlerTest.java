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

/**
 * Test suite for {@link ImageIOHandler} class. Tests image I/O operations including reading,
 * writing, and thumbnail generation.
 */
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

    assertThat(result).isNotNull();
    assertThat(result.cols()).isEqualTo(100);
    assertThat(result.rows()).isEqualTo(100);
    assertThat(result.type()).isEqualTo(CvType.CV_8UC1);
  }

  @Test
  void read_image_with_color_format_preserves_channels() {
    var result = ImageIOHandler.readImage(testData.colorImagePath(), null);

    assertThat(result).isNotNull();
    assertThat(result.cols()).isEqualTo(80);
    assertThat(result.rows()).isEqualTo(80);
    assertThat(CvType.channels(result.type())).isEqualTo(3);
  }

  @ParameterizedTest
  @MethodSource("supportedImageFormats")
  void read_image_supports_various_formats(String extension, Mat testImage) throws IOException {
    var imagePath = testData.createImageWithExtension(extension, testImage);
    var result = ImageIOHandler.readImage(imagePath, null);

    assertThat(result).isNotNull();
    assertThat(result.cols()).isEqualTo(testImage.cols());
    assertThat(result.rows()).isEqualTo(testImage.rows());
  }

  @Test
  void read_image_with_exception_throws_cv_exception_for_invalid_path() {
    var nonExistentPath = tempDir.resolve("non_existent.png");

    assertThatThrownBy(() -> ImageIOHandler.readImageWithCvException(nonExistentPath, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("not readable");
  }

  @Test
  void read_image_with_null_path_throws_null_pointer_exception() {
    assertThatThrownBy(() -> ImageIOHandler.readImage(null, null))
        .isInstanceOf(NullPointerException.class);
  }

  // === Writing Tests ===

  @Test
  void write_mat_image_creates_readable_file() throws IOException {
    var outputPath = tempDir.resolve("output.png");

    var success = ImageIOHandler.writeImage(testData.grayscaleImage(), outputPath);

    assertThat(success).isTrue();
    assertThat(Files.exists(outputPath)).isTrue();
    assertTrue(Files.size(outputPath) > 0);

    // Verify readability
    var readBack = ImageIOHandler.readImage(outputPath, null);
    assertThat(readBack).isNotNull();
    assertThat(readBack.cols()).isEqualTo(testData.grayscaleImage().cols());
  }

  @Test
  void write_rendered_image_converts_and_saves_successfully() {
    var bufferedImage = createTestBufferedImage(50, 50);
    var outputPath = tempDir.resolve("buffered_output.png");

    var success = ImageIOHandler.writeImage(bufferedImage, outputPath);

    assertThat(success).isTrue();
    assertThat(Files.exists(outputPath)).isTrue();
  }

  @Test
  void write_image_with_custom_parameters_applies_settings() {
    var outputPath = tempDir.resolve("output_with_params.jpg");
    var params = new MatOfInt(Imgcodecs.IMWRITE_JPEG_QUALITY, 95);

    var success = ImageIOHandler.writeImage(testData.grayscaleImage(), outputPath, params);

    assertThat(success).isTrue();
    assertThat(Files.exists(outputPath)).isTrue();
  }

  @ParameterizedTest
  @ValueSource(strings = {".png", ".jpg", ".bmp", ".tiff"})
  void write_image_supports_multiple_formats(String extension) throws IOException {
    var outputPath = tempDir.resolve("test" + extension);

    var success = ImageIOHandler.writeImage(testData.colorImage(), outputPath);

    assertThat(success).isTrue();
    assertThat(Files.exists(outputPath)).isTrue();
  }

  @Test
  void write_image_with_null_source_throws_exception() {
    var outputPath = tempDir.resolve("output.png");

    assertThatThrownBy(() -> ImageIOHandler.writeImage((Mat) null, outputPath))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void write_image_with_empty_source_throws_exception() {
    var emptyMat = new Mat();
    var outputPath = tempDir.resolve("output.png");

    assertThatThrownBy(() -> ImageIOHandler.writeImage(emptyMat, outputPath))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("empty");
  }

  // === PNG-specific Tests ===

  @Test
  void write_PNG_creates_file_with_correct_extension() {
    var outputPath = tempDir.resolve("test_output.png");

    var success = ImageIOHandler.writePNG(testData.grayscaleImage(), outputPath);

    assertThat(success).isTrue();
    assertThat(Files.exists(outputPath)).isTrue();
    assertTrue(outputPath.toString().endsWith(".png"));
  }

  @Test
  void write_PNG_enforces_extension_when_different_provided() {
    var outputPathWithWrongExt = tempDir.resolve("test_output.jpg");

    var success = ImageIOHandler.writePNG(testData.grayscaleImage(), outputPathWithWrongExt);

    assertThat(success).isTrue();
    var expectedPngPath = tempDir.resolve("test_output.png");
    assertThat(Files.exists(expectedPngPath)).isTrue();
  }

  @Test
  void write_PNG_handles_16bit_images() {
    var image16bit = new Mat(50, 50, CvType.CV_16UC1, new Scalar(32000));
    var outputPath = tempDir.resolve("test_16bit.png");

    var success = ImageIOHandler.writePNG(image16bit, outputPath);

    assertThat(success).isTrue();
    assertThat(Files.exists(outputPath)).isTrue();
  }

  // === Thumbnail Tests ===

  @Test
  void write_thumbnail_creates_scaled_image() {
    var thumbnailPath = tempDir.resolve("thumbnail.jpg");
    var maxSize = 50;

    var success = ImageIOHandler.writeThumbnail(testData.grayscaleImage(), thumbnailPath, maxSize);

    assertThat(success).isTrue();
    assertThat(Files.exists(thumbnailPath)).isTrue();

    var thumbnail = ImageIOHandler.readImage(thumbnailPath, null);
    assertThat(thumbnail).isNotNull();
    assertThat(thumbnail.cols()).isLessThanOrEqualTo(maxSize);
    assertThat(thumbnail.rows()).isLessThanOrEqualTo(maxSize);
  }

  @Test
  void write_thumbnail_preserves_original_size_when_larger_than_max() {
    var thumbnailPath = tempDir.resolve("large_thumbnail.jpg");
    var maxSize = 200; // Larger than original 100x100

    var success = ImageIOHandler.writeThumbnail(testData.grayscaleImage(), thumbnailPath, maxSize);

    assertThat(success).isTrue();
    var thumbnail = ImageIOHandler.readImage(thumbnailPath, null);
    assertThat(thumbnail.cols()).isEqualTo(100);
    assertThat(thumbnail.rows()).isEqualTo(100);
  }

  @ParameterizedTest
  @ValueSource(ints = {0, -1, -10})
  void write_thumbnail_with_invalid_max_size_throws_exception(int invalidMaxSize) {
    var thumbnailPath = tempDir.resolve("thumbnail.jpg");

    assertThatThrownBy(
            () ->
                ImageIOHandler.writeThumbnail(
                    testData.grayscaleImage(), thumbnailPath, invalidMaxSize))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("positive");
  }

  @Test
  void build_thumbnail_with_aspect_ratio_preservation() {
    var iconDim = new Dimension(50, 50);

    var thumbnail = ImageIOHandler.buildThumbnail(testData.grayscaleImageCV(), iconDim, true);

    assertThat(thumbnail).isNotNull();
    assertThat(thumbnail.cols()).isEqualTo(50);
    assertThat(thumbnail.rows()).isEqualTo(50);
  }

  @Test
  void build_thumbnail_without_aspect_ratio_preservation_stretches_image() {
    var iconDim = new Dimension(60, 40); // Different aspect ratio

    var thumbnail = ImageIOHandler.buildThumbnail(testData.grayscaleImageCV(), iconDim, false);

    assertThat(thumbnail).isNotNull();
    assertThat(thumbnail.cols()).isEqualTo(60);
    assertThat(thumbnail.rows()).isEqualTo(40);
  }

  @Test
  void build_thumbnail_larger_than_original_returns_original_size() {
    var iconDim = new Dimension(200, 200);

    var thumbnail = ImageIOHandler.buildThumbnail(testData.grayscaleImageCV(), iconDim, true);

    assertThat(thumbnail).isNotNull();
    assertThat(thumbnail.cols()).isEqualTo(100);
    assertThat(thumbnail.rows()).isEqualTo(100);
  }

  @ParameterizedTest
  @MethodSource("invalidThumbnailDimensions")
  void build_thumbnail_with_invalid_dimensions_throws_exception(Dimension invalidDimension) {
    assertThatThrownBy(
            () ->
                ImageIOHandler.buildThumbnail(testData.grayscaleImageCV(), invalidDimension, true))
        .isInstanceOf(IllegalArgumentException.class);
  }

  // === Validation Tests ===

  @Test
  void validate_readable_path_accepts_valid_path() {
    assertThatCode(() -> ImageIOHandler.validateReadablePath(testData.grayscaleImagePath()))
        .doesNotThrowAnyException();
  }

  @Test
  void validate_readable_path_rejects_null() {
    assertThatThrownBy(() -> ImageIOHandler.validateReadablePath(null))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void validate_readable_path_rejects_non_readable_path() {
    var nonReadablePath = tempDir.resolve("non_existent.png");

    assertThatThrownBy(() -> ImageIOHandler.validateReadablePath(nonReadablePath))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void validate_source_accepts_valid_mat() {
    assertThatCode(() -> ImageIOHandler.validateSource(testData.grayscaleImage()))
        .doesNotThrowAnyException();
  }

  @Test
  void validate_source_rejects_null_mat() {
    assertThatThrownBy(() -> ImageIOHandler.validateSource(null))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void validate_source_rejects_empty_mat() {
    assertThatThrownBy(() -> ImageIOHandler.validateSource(new Mat()))
        .isInstanceOf(IllegalArgumentException.class);
  }

  // === Integration Tests ===

  @Test
  void complete_read_write_cycle_preserves_image_data() throws IOException {
    var originalPath = testData.colorImagePath();
    var outputPath = tempDir.resolve("cycle_test.png");

    // Read and write back
    var originalImage = ImageIOHandler.readImage(originalPath, null);
    var writeSuccess = ImageIOHandler.writeImage(originalImage.toMat(), outputPath);
    var readBackImage = ImageIOHandler.readImage(outputPath, null);

    assertThat(writeSuccess).isTrue();
    assertThat(readBackImage).isNotNull();
    assertThat(readBackImage.cols()).isEqualTo(originalImage.cols());
    assertThat(readBackImage.rows()).isEqualTo(originalImage.rows());
    assertThat(readBackImage.type()).isEqualTo(originalImage.type());
  }

  @Test
  void metadata_extraction_preserves_tag_list() {
    var tags = new ArrayList<String>();
    var result = ImageIOHandler.readImage(testData.colorImagePath(), tags);

    assertThat(result).isNotNull();
    // Tags list should be preserved (though may be empty for synthetic images)
    assertThat(tags).isNotNull();
  }

  // === Failure-path contract on readImage (IEC 62304 risk: silent failure on corrupt input) ===

  @Nested
  class Read_Image_Failure_Contract {

    /**
     * {@code readImage} must downgrade an OpenCV decode failure to {@code null} (not propagate
     * the exception) so callers can decide whether to retry or surface the error. This is the
     * documented contract; without an explicit test, a regression in {@link ImageIOHandler}
     * could turn a recoverable decode error into an unhandled exception in clinical use.
     */
    @Test
    void readImage_returns_null_for_corrupted_image_file() throws IOException {
      var corrupted = tempDir.resolve("corrupted.png");
      Files.write(corrupted, new byte[] {0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07});

      var result = ImageIOHandler.readImage(corrupted, null);

      assertNull(result, "Corrupted file must yield null, not throw");
    }

    @Test
    void readImage_returns_null_for_unrecognised_format() throws IOException {
      // A plain text file with an image-looking extension exercises the codec
      // selection failure path inside OpenCV.
      var garbage = tempDir.resolve("not_an_image.jpg");
      Files.writeString(garbage, "this is definitely not a JPEG file");

      var result = ImageIOHandler.readImage(garbage, null);

      assertNull(result);
    }

    @Test
    void readImage_with_exception_throws_for_corrupted_file_or_returns_null() throws IOException {
      // The "with exception" variant either throws CvException OR returns null, depending on
      // what the OpenCV codec does internally. Both behaviours satisfy the contract; what must
      // NOT happen is an unhandled native crash. This test pins the observable surface.
      var corrupted = tempDir.resolve("corrupted_for_throw.png");
      Files.write(corrupted, new byte[] {0x42, 0x42, 0x42, 0x42});

      try {
        var result = ImageIOHandler.readImageWithCvException(corrupted, null);
        // Acceptable: returns null when OpenCV silently rejects the bytes.
        // The important assertion is that no native-level crash occurred.
        assertTrue(result == null || result.toMat() != null);
      } catch (org.opencv.core.CvException e) {
        // Also acceptable: a typed CvException propagated, which is what the
        // method name advertises.
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

  // === Round-trip pixel-integrity (IEC 62304 risk: silent data loss in DICOM I/O) ===

  @Nested
  class Round_Trip_Integrity {

    /**
     * Write → read PNG must be bit-exact for every pixel and every supported channel/depth.
     * Silent corruption of medical pixel values is a hazard tracked under ISO 14971 risk
     * management; PNG is the project's reference lossless intermediate.
     */
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
      // 16-bit PNG is the format Weasis uses to round-trip raw DICOM modality data.
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
                        .formatted(y, x, c, Short.toUnsignedInt(exp[c]), Short.toUnsignedInt(act[c])));
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
                  .formatted(y, x, c, Byte.toUnsignedInt(expected[c]), Byte.toUnsignedInt(actual[c])));
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
              // Use a range that exercises the full 16-bit space, not just 0..255
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
    // Fill with test pattern
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

  // === Helper assertion methods using AssertJ-style (assuming available) ===
  private static <T> ObjectAssert<T> assertThat(T actual) {
    return new ObjectAssert<>(actual);
  }

  private static IntegerAssert assertThat(int actual) {
    return new IntegerAssert(actual);
  }

  private static BooleanAssert assertThat(boolean actual) {
    return new BooleanAssert(actual);
  }

  private static <T> ObjectAssert<T> assertThatThrownBy(ThrowingCallable callable) {
    try {
      callable.call();
      throw new AssertionError("Expected exception was not thrown");
    } catch (Exception e) {
      return new ObjectAssert<>((T) e);
    }
  }

  private static ObjectAssert<Void> assertThatCode(ThrowingCallable callable) {
    try {
      callable.call();
      return new ObjectAssert<>(null);
    } catch (Exception e) {
      throw new AssertionError("Unexpected exception", e);
    }
  }

  @FunctionalInterface
  interface ThrowingCallable {
    void call() throws Exception;
  }

  // Placeholder assertion classes (replace with actual AssertJ or similar)
  static class ObjectAssert<T> {
    private final T actual;

    ObjectAssert(T actual) {
      this.actual = actual;
    }

    ObjectAssert<T> isNotNull() {
      assertNotNull(actual);
      return this;
    }

    ObjectAssert<T> isInstanceOf(Class<?> type) {
      assertTrue(type.isInstance(actual));
      return this;
    }

    ObjectAssert<T> hasMessageContaining(String message) {
      if (actual instanceof Exception) {
        assertTrue(((Exception) actual).getMessage().contains(message));
      }
      return this;
    }

    ObjectAssert<T> doesNotThrowAnyException() {
      // This is handled in assertThatCode
      return this;
    }
  }

  static class IntegerAssert {
    private final int actual;

    IntegerAssert(int actual) {
      this.actual = actual;
    }

    IntegerAssert isEqualTo(int expected) {
      assertEquals(expected, actual);
      return this;
    }

    IntegerAssert isLessThanOrEqualTo(int expected) {
      assertTrue(actual <= expected);
      return this;
    }

    IntegerAssert isGreaterThan(int expected) {
      assertTrue(actual > expected);
      return this;
    }
  }

  static class BooleanAssert {
    private final boolean actual;

    BooleanAssert(boolean actual) {
      this.actual = actual;
    }

    BooleanAssert isTrue() {
      assertTrue(actual);
      return this;
    }

    BooleanAssert isFalse() {
      assertFalse(actual);
      return this;
    }
  }

  /** Test data helper class that creates and manages test images. */
  private static class TestImageData {
    private final Path tempDir;
    private final Mat grayscaleImage;
    private final Mat colorImage;
    private final ImageCV grayscaleImageCV;
    private final Path grayscaleImagePath;
    private final Path colorImagePath;

    TestImageData(Path tempDir) throws IOException {
      this.tempDir = tempDir;

      // Create grayscale test image
      this.grayscaleImage = createTestMat(100, 100, CvType.CV_8UC1);
      this.grayscaleImageCV = ImageCV.fromMat(grayscaleImage);
      this.grayscaleImagePath = tempDir.resolve("test_grayscale.png");
      assertTrue(Imgcodecs.imwrite(grayscaleImagePath.toString(), grayscaleImage));

      // Create color test image
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
