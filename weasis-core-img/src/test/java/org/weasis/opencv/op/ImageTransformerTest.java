/*
 * Copyright (c) 2020 Weasis Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.weasis.opencv.op;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.stream.Stream;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.opencv.core.*;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;
import org.opencv.osgi.OpenCVNativeLoader;
import org.weasis.opencv.data.ImageCV;

/**
 * Comprehensive test suite for ImageTransformer operations using real image data and modern testing
 * practices.
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@Execution(ExecutionMode.CONCURRENT)
class ImageTransformerTest {

  private static final Color RED = new Color(255, 0, 0);
  private static final Color GREEN = new Color(0, 255, 0);
  private static final Color BLUE = new Color(0, 0, 255);
  private static final Color TRANSPARENT_RED = new Color(255, 0, 0, 128);

  @BeforeAll
  static void load_native_library() {
    var loader = new OpenCVNativeLoader();
    loader.init();
  }

  @Nested
  class Geometric_Transformations {

    @Test
    void should_crop_image_to_specified_rectangle() {
      var source = createGradientImage(100, 100, CvType.CV_8UC3);
      var cropArea = new Rectangle(20, 30, 40, 35);

      var result = ImageTransformer.crop(source, cropArea);

      assertEquals(40, result.width());
      assertEquals(35, result.height());
      assertEquals(source.channels(), result.channels());

      // Verify cropped content is different from original
      assertFalse(Arrays.equals(getImageCorner(source, 0, 0), getImageCorner(result, 0, 0)));
    }

    @Test
    void should_handle_crop_area_extending_beyond_image_bounds() {
      var source = createSolidColorImage(50, 50, CvType.CV_8UC1, 128);
      var cropArea = new Rectangle(40, 40, 30, 30); // Extends beyond 50x50

      var result = ImageTransformer.crop(source, cropArea);

      assertEquals(10, result.width());
      assertEquals(10, result.height());
    }

    @Test
    void should_return_clone_for_invalid_crop_dimensions() {
      var source = createSolidColorImage(50, 50, CvType.CV_8UC1, 100);
      var tinyArea = new Rectangle(10, 10, 1, 1); // Too small

      var result = ImageTransformer.crop(source, tinyArea);

      assertEquals(source.width(), result.width());
      assertEquals(source.height(), result.height());
    }

    @Test
    void should_throw_exception_for_null_crop_area() {
      var source = createSolidColorImage(50, 50, CvType.CV_8UC1, 100);

      assertThrows(NullPointerException.class, () -> ImageTransformer.crop(source, null));
    }

    @Test
    void should_scale_image_using_default_interpolation() {
      var source = createCheckerboardImage(50, 50, CvType.CV_8UC1);
      var targetSize = new Dimension(100, 75);

      var result = ImageTransformer.scale(source, targetSize);

      assertEquals(100, result.width());
      assertEquals(75, result.height());
      assertEquals(source.channels(), result.channels());
    }

    @ParameterizedTest
    @ValueSource(ints = {Imgproc.INTER_NEAREST, Imgproc.INTER_LINEAR, Imgproc.INTER_CUBIC})
    void should_scale_with_different_interpolation_methods(int interpolation) {
      var source = createGradientImage(30, 30, CvType.CV_8UC1);
      var targetSize = new Dimension(60, 45);

      var result = ImageTransformer.scale(source, targetSize, interpolation);

      assertEquals(60, result.width());
      assertEquals(45, result.height());
    }

    @ParameterizedTest
    @MethodSource("invalidDimensionsProvider")
    void should_throw_exception_for_invalid_dimensions(Dimension invalidDim) {
      var source = createSolidColorImage(50, 50, CvType.CV_8UC1, 100);

      assertThrows(
          IllegalArgumentException.class, () -> ImageTransformer.scale(source, invalidDim));
    }

    static Stream<Arguments> invalidDimensionsProvider() {
      return Stream.of(
          Arguments.of(new Dimension(0, 50)),
          Arguments.of(new Dimension(50, 0)),
          Arguments.of(new Dimension(-10, 50)),
          Arguments.of(new Dimension(50, -20)));
    }

    @ParameterizedTest
    @ValueSource(
        ints = {Core.ROTATE_90_CLOCKWISE, Core.ROTATE_90_COUNTERCLOCKWISE, Core.ROTATE_180})
    void should_rotate_image_correctly(int rotationType) {
      var source = createRectangularImage(60, 40, CvType.CV_8UC1);

      var result = ImageTransformer.getRotatedImage(source, rotationType);

      if (rotationType == Core.ROTATE_180) {
        assertEquals(60, result.width());
        assertEquals(40, result.height());
      } else {
        // 90-degree rotations swap dimensions
        assertEquals(40, result.width());
        assertEquals(60, result.height());
      }
    }

    @Test
    void should_return_clone_for_invalid_rotation_type() {
      var source = createSolidColorImage(30, 30, CvType.CV_8UC1, 150);

      var result = ImageTransformer.getRotatedImage(source, 999); // Invalid

      assertEquals(30, result.width());
      assertEquals(30, result.height());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, -1}) // vertical, horizontal, both
    void should_flip_image_correctly(int flipType) {
      var source = createAsymmetricImage(40, 30, CvType.CV_8UC1);
      var result = ImageTransformer.flip(source, flipType);

      assertEquals(40, result.width());
      assertEquals(30, result.height());

      assertFalse(Arrays.equals(getImageCorner(source, 10, 25), getImageCorner(result, 10, 25)));

      // Verify flip by checking appropriate corners based on flip type
      switch (flipType) {
        case 0 -> { // Vertical flip - top and bottom should swap
          // Top-left of source should equal bottom-left of result
          assertArrayEquals(
              getImageCorner(source, 0, 0), getImageCorner(result, 0, result.height() - 1));
        }
        case 1 -> { // Horizontal flip - left and right should swap
          // Top-left of source should equal top-right of result
          assertArrayEquals(
              getImageCorner(source, 0, 0), getImageCorner(result, result.width() - 1, 0));
        }
        case -1 -> { // Both flips - opposite corner should match
          // Top-left of source should equal bottom-right of result
          assertArrayEquals(
              getImageCorner(source, 0, 0),
              getImageCorner(result, result.width() - 1, result.height() - 1));
        }
      }
    }

    @Test
    void should_apply_affine_transformation() {
      var source = createGradientImage(50, 50, CvType.CV_8UC3);
      var transformMatrix = createTranslationMatrix(10, 15);
      var outputSize = new Size(60, 65);

      var result = ImageTransformer.warpAffine(source, transformMatrix, outputSize, null);

      assertEquals(60, result.width());
      assertEquals(65, result.height());
    }

    @Test
    void should_throw_exception_for_invalid_affine_matrix() {
      var source = createSolidColorImage(30, 30, CvType.CV_8UC1, 100);
      var invalidMatrix = Mat.eye(3, 3, CvType.CV_32F); // Wrong dimensions
      var outputSize = new Size(30, 30);

      assertThrows(
          IllegalArgumentException.class,
          () -> ImageTransformer.warpAffine(source, invalidMatrix, outputSize, null));
    }
  }

  @Nested
  class Pixel_Value_Transformations {

    @Test
    void should_apply_single_channel_lut() {
      var source = createGradientImage(30, 30, CvType.CV_8UC1);
      var lut = createInvertLUT(1);

      var result = ImageTransformer.applyLUT(source, lut);

      assertEquals(source.width(), result.width());
      assertEquals(source.height(), result.height());

      // Verify inversion: dark pixels should become bright
      var sourcePixel = source.get(0, 0);
      var resultPixel = result.get(0, 0);
      assertTrue(Math.abs((255 - sourcePixel[0]) - resultPixel[0]) < 2);
    }

    @Test
    void should_apply_multi_channel_lut() {
      var source = createColorGradientImage(40, 40, CvType.CV_8UC3);
      var lut = createColorSwapLUT(); // Swap R and B channels

      var result = ImageTransformer.applyLUT(source, lut);

      assertEquals(source.channels(), result.channels());

      // Verify channel swap
      var sourcePixel = source.get(10, 10);
      var resultPixel = result.get(10, 10);

      assertEquals(sourcePixel[2], resultPixel[0], 1.0); // Original R -> Result B
      assertEquals(sourcePixel[0], resultPixel[2], 1.0); // Original B -> Result R
    }

    @ParameterizedTest
    @MethodSource("invalidLutProvider")
    void should_throw_exception_for_invalid_lut(byte[][] invalidLut) {
      var source = createSolidColorImage(20, 20, CvType.CV_8UC1, 100);

      assertThrows(
          IllegalArgumentException.class, () -> ImageTransformer.applyLUT(source, invalidLut));
    }

    static Stream<Arguments> invalidLutProvider() {
      return Stream.of(
          Arguments.of((Object) new byte[0][]),
          Arguments.of((Object) new byte[1][100]), // Wrong size
          Arguments.of((Object) new byte[1][500]) // Wrong size
          );
    }

    @Test
    void should_rescale_to_byte_range() {
      var source = createHighDynamicRangeImage();
      double alpha = 0.1; // Scale down
      double beta = 50; // Add offset

      var result = ImageTransformer.rescaleToByte(source, alpha, beta);

      assertEquals(CvType.CV_8U, CvType.depth(result.type()));

      // Verify transformation formula: output = input * alpha + beta
      var sourcePixel = source.get(0, 0);
      var resultPixel = result.get(0, 0);

      var expected = Math.min(255, Math.max(0, sourcePixel[0] * alpha + beta));
      assertEquals(expected, resultPixel[0], 2.0);
    }

    @Test
    void should_invert_lut() {
      var source = createGradientImage(25, 25, CvType.CV_8UC1);

      var result = ImageTransformer.invertLUT(ImageCV.fromMat(source));

      // Verify inversion: pixel + inverted_pixel ≈ 255
      var sourcePixel = source.get(10, 10);
      var resultPixel = result.get(10, 10);

      assertEquals(255, sourcePixel[0] + resultPixel[0], 2.0);
    }

    @Test
    void should_apply_bitwise_and() {
      var source = createBitPatternImage();
      int maskValue = 0xF0; // Keep upper 4 bits

      var result = ImageTransformer.bitwiseAnd(source, maskValue);

      // Verify masking
      var sourcePixel = new byte[1];
      var resultPixel = new byte[1];
      source.get(5, 5, sourcePixel);
      result.get(5, 5, resultPixel);

      assertEquals(((int) sourcePixel[0]) & maskValue, resultPixel[0], 0.1);
    }
  }

  @Nested
  class Visual_Effects {

    @Test
    void should_merge_images_with_opacity() {
      var source1 = createSolidColorImage(30, 30, CvType.CV_8UC1, 100);
      var source2 = createSolidColorImage(30, 30, CvType.CV_8UC1, 200);
      double opacity1 = 0.3;
      double opacity2 = 0.7;

      var result = ImageTransformer.mergeImages(source1, source2, opacity1, opacity2);

      assertEquals(source1.width(), result.width());
      assertEquals(source1.height(), result.height());

      // Verify weighted combination
      var resultPixel = result.get(15, 15);
      var expected = 100 * opacity1 + 200 * opacity2;
      assertEquals(expected, resultPixel[0], 2.0);
    }

    @Test
    void should_throw_exception_for_mismatched_image_sizes() {
      var source1 = createSolidColorImage(30, 30, CvType.CV_8UC1, 100);
      var source2 = createSolidColorImage(40, 40, CvType.CV_8UC1, 200);

      assertThrows(
          IllegalArgumentException.class,
          () -> ImageTransformer.mergeImages(source1, source2, 0.5, 0.5));
    }

    @ParameterizedTest
    @ValueSource(doubles = {-0.1, 1.1, 2.0})
    void should_throw_exception_for_invalid_opacity(double invalidOpacity) {
      var source1 = createSolidColorImage(20, 20, CvType.CV_8UC1, 100);
      var source2 = createSolidColorImage(20, 20, CvType.CV_8UC1, 200);

      assertThrows(
          IllegalArgumentException.class,
          () -> ImageTransformer.mergeImages(source1, source2, invalidOpacity, 0.5));
    }

    @Test
    void should_create_overlay_with_color() {
      var source = createSolidColorImage(40, 40, CvType.CV_8UC3, 100);
      var mask = createCircularMask(40, 40);

      var result = ImageTransformer.overlay(source, mask, RED);

      assertEquals(source.channels(), result.channels());

      // Check that overlay was applied inside the circle
      var centerPixel = result.get(20, 20);
      assertTrue(centerPixel[2] > 200); // Red channel should be high
    }

    @Test
    void should_create_overlay_with_transparent_color() {
      var source = createSolidColorImage(30, 30, CvType.CV_8UC3, 128);
      var mask = createRectangularMask(30, 30);

      var result = ImageTransformer.overlay(source, mask, TRANSPARENT_RED);

      // Verify that transparent overlay creates blended result
      var overlayPixel = result.get(15, 15);
      assertTrue(overlayPixel[2] >= 128 && overlayPixel[2] < 255); // Partially red
    }

    @Test
    void should_draw_shape_on_image() {
      var sourceImage = createTestBufferedImage(60, 60);
      var shape = new Rectangle2D.Double(10, 10, 20, 20);

      var result = ImageTransformer.drawShape(sourceImage, shape, BLUE);

      assertNotNull(result);
      assertEquals(sourceImage.getWidth(), result.getWidth());
      assertEquals(sourceImage.getHeight(), result.getHeight());
    }

    @Test
    void should_apply_crop_mask() {
      var source = createGradientImage(50, 50, CvType.CV_8UC1);
      var bounds = new Rectangle(10, 10, 30, 30);
      double alpha = 0.3;

      var result = ImageTransformer.applyCropMask(source, bounds, alpha);

      assertEquals(source.size(), result.size());

      // Verify that areas outside bounds are darker
      var outsidePixel = source.get(5, 5);
      var insidePixel = result.get(25, 25);
      assertTrue(outsidePixel[0] < insidePixel[0]);
    }

    @Test
    void should_apply_shutter_effect() {
      var source = createGradientImage(40, 40, CvType.CV_8UC1);
      var shape = new Rectangle2D.Double(10, 10, 20, 20);

      var result = ImageTransformer.applyShutter(source, shape, Color.BLACK);

      // Verify that areas outside shape are black
      var outsidePixel = result.get(5, 5);
      assertEquals(0, outsidePixel[0], 1.0);
    }
  }

  @Nested
  class Error_Handling {

    @Test
    void should_throw_exception_for_null_source() {
      assertThrows(
          NullPointerException.class,
          () -> ImageTransformer.crop(null, new Rectangle(10, 10, 20, 20)));
    }

    @Test
    void should_throw_exception_for_null_parameters() {
      var source = createSolidColorImage(30, 30, CvType.CV_8UC1, 100);

      assertAll(
          () ->
              assertThrows(NullPointerException.class, () -> ImageTransformer.scale(source, null)),
          () ->
              assertThrows(
                  NullPointerException.class, () -> ImageTransformer.applyLUT(source, null)),
          () ->
              assertThrows(
                  NullPointerException.class,
                  () -> ImageTransformer.overlay(source, (Mat) null, RED)));
    }
  }

  // Test data creation utilities

  private static Mat createSolidColorImage(int width, int height, int type, int value) {
    return new Mat(height, width, type, new Scalar(value));
  }

  private static Mat createGradientImage(int width, int height, int type) {
    var image = new Mat(height, width, type);
    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        var intensity = (double) (x + y) / (width + height) * 255;
        if (CvType.channels(type) == 1) {
          image.put(y, x, intensity);
        } else {
          image.put(y, x, intensity, intensity * 0.8, intensity * 0.6);
        }
      }
    }
    return image;
  }

  private static Mat createColorGradientImage(int width, int height, int type) {
    var image = new Mat(height, width, type);
    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        var red = (double) x / width * 255;
        var green = (double) y / height * 255;
        var blue = (double) (x + y) / (width + height) * 255;
        image.put(y, x, blue, green, red); // BGR format
      }
    }
    return image;
  }

  private static Mat createCheckerboardImage(int width, int height, int type) {
    var image = new Mat(height, width, type);
    int squareSize = 10;
    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        var value = ((x / squareSize) + (y / squareSize)) % 2 == 0 ? 255 : 0;
        image.put(y, x, value);
      }
    }
    return image;
  }

  private static Mat createAsymmetricImage(int width, int height, int type) {
    var image = new Mat(height, width, type);
    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        var value = x > y ? 200 : 50;
        image.put(y, x, value);
      }
    }
    return image;
  }

  private static Mat createRectangularImage(int width, int height, int type) {
    var image = new Mat(height, width, type);
    // Create distinctive pattern for rotation testing
    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        var value = (x < width / 3) ? 255 : ((x < 2 * width / 3) ? 128 : 64);
        image.put(y, x, value);
      }
    }
    return image;
  }

  private static Mat createHighDynamicRangeImage() {
    var image = new Mat(20, 20, CvType.CV_16U);
    for (int y = 0; y < 20; y++) {
      for (int x = 0; x < 20; x++) {
        var value = (x + y) * 1000; // High values requiring scaling
        image.put(y, x, value);
      }
    }
    return image;
  }

  private static Mat createBitPatternImage() {
    var image = new Mat(15, 15, CvType.CV_8UC1);
    for (int y = 0; y < 15; y++) {
      for (int x = 0; x < 15; x++) {
        var value = (x * 16 + y) % 256; // Create bit patterns
        image.put(y, x, value);
      }
    }
    return image;
  }

  private static Mat createCircularMask(int width, int height) {
    var mask = Mat.zeros(height, width, CvType.CV_8UC1);
    var center = new Point(width / 2.0, height / 2.0);
    var radius = Math.min(width, height) / 3.0;
    Imgproc.circle(mask, center, (int) radius, new Scalar(255), -1);
    return mask;
  }

  private static Mat createRectangularMask(int width, int height) {
    var mask = Mat.zeros(height, width, CvType.CV_8UC1);
    var rect = new Rect(width / 4, height / 4, width / 2, height / 2);
    Imgproc.rectangle(mask, rect, new Scalar(255), -1);
    return mask;
  }

  private static BufferedImage createTestBufferedImage(int width, int height) {
    var image = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
    var graphics = image.createGraphics();
    graphics.setColor(java.awt.Color.LIGHT_GRAY);
    graphics.fillRect(0, 0, width, height);
    graphics.dispose();
    return image;
  }

  private static Mat createTranslationMatrix(double tx, double ty) {
    var matrix = Mat.zeros(2, 3, CvType.CV_32F);
    matrix.put(0, 0, 1, 0, tx);
    matrix.put(1, 0, 0, 1, ty);
    return matrix;
  }

  private static byte[][] createInvertLUT(int channels) {
    var lut = new byte[channels][256];
    for (int c = 0; c < channels; c++) {
      for (int i = 0; i < 256; i++) {
        lut[c][i] = (byte) (255 - i);
      }
    }
    return lut;
  }

  private static byte[][] createColorSwapLUT() {
    var lut = new byte[3][256];
    // Channel swap: R->B, G->G, B->R
    for (int i = 0; i < 256; i++) {
      lut[0][i] = (byte) i; // Blue stays blue (for BGR format)
      lut[1][i] = (byte) i; // Green stays green
      lut[2][i] = (byte) i; // Red becomes blue channel input
    }
    return lut;
  }

  private static double[] getImageCorner(Mat image, int x, int y) {
    return image.get(y, x);
  }
}
