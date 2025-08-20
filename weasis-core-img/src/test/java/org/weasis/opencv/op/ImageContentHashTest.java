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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.osgi.OpenCVNativeLoader;
import org.weasis.opencv.op.lut.ColorLut;

@DisplayNameGeneration(ReplaceUnderscores.class)
class ImageContentHashTest {

  private static final double IDENTICAL_THRESHOLD = 0.0001;
  private static final int DEFAULT_SIZE = 100;

  @BeforeAll
  @DisplayName("Load OpenCV native library")
  static void load_native_lib() {
    var loader = new OpenCVNativeLoader();
    loader.init();
  }

  @Nested
  class Identical_Image_Comparisons {

    @ParameterizedTest
    @EnumSource(ImageContentHash.class)
    void identical_images_should_have_zero_difference(ImageContentHash algorithm) {
      var image1 = createSolidColorImage(DEFAULT_SIZE, DEFAULT_SIZE, new Scalar(128, 64, 192));
      var image2 = image1.clone();

      compareImages(algorithm, image1, image2);
    }

    @Test
    void scaled_images_should_be_recognized_as_similar() {
      var original = createCheckerboardImage(50, 50);
      var scaled = new Mat();

      Imgproc.resize(original, scaled, new Size(150, 150));

      var averageDiff = ImageContentHash.AVERAGE.compare(original, scaled);
      var phashDiff = ImageContentHash.PHASH.compare(original, scaled);

      assertEquals(
          0.0, averageDiff, IDENTICAL_THRESHOLD, "Average hash should handle scaling well");
      assertEquals(0.0, phashDiff, IDENTICAL_THRESHOLD, "PHash should handle scaling well");
    }
  }

  private static void compareImages(ImageContentHash algorithm, Mat image1, Mat image2) {
    var difference = algorithm.compare(image1, image2);
    if (algorithm == ImageContentHash.RADIAL_VARIANCE) {
      assertEquals(
          1.0,
          difference,
          IDENTICAL_THRESHOLD,
          algorithm + " should return zero difference for identical images");
    } else {
      assertEquals(
          0.0,
          difference,
          IDENTICAL_THRESHOLD,
          algorithm + " should return zero difference for identical images");
    }
  }

  @Nested
  class Different_Image_Comparisons {

    @ParameterizedTest
    @MethodSource("provideImageTransformationTestCases")
    void transformed_images_should_show_expected_differences(
        ImageContentHash algorithm,
        String transformation,
        double expectedDifference,
        double tolerance) {

      var original = createGradientImage(DEFAULT_SIZE, DEFAULT_SIZE);
      var transformed = applyTransformation(original, transformation);

      var actualDifference = algorithm.compare(original, transformed);

      assertEquals(
          expectedDifference,
          actualDifference,
          tolerance,
          String.format(
              "%s should show expected difference for %s transformation",
              algorithm, transformation));
    }

    static Stream<Arguments> provideImageTransformationTestCases() {
      return Stream.of(
          // Algorithm, Transformation, Expected Difference, Tolerance
          Arguments.of(ImageContentHash.MARR_HILDRETH, "color_shift", 400.0, 150.0),
          Arguments.of(ImageContentHash.COLOR_MOMENT, "color_shift", 20.0, 10.0),
          Arguments.of(ImageContentHash.BLOCK_MEAN_ZERO, "color_shift", 180.0, 30.0),
          Arguments.of(ImageContentHash.BLOCK_MEAN_ONE, "color_shift", 670.0, 100.0),
          Arguments.of(ImageContentHash.RADIAL_VARIANCE, "rotation_90", 0.3, 0.1));
    }

    @Test
    void structurally_similar_images_should_have_low_marr_hildreth_difference() {
      var original = createGradientImage(DEFAULT_SIZE, DEFAULT_SIZE);
      var blurred = new Mat();

      Imgproc.GaussianBlur(original, blurred, new Size(3, 3), 0);

      var difference = ImageContentHash.MARR_HILDRETH.compare(original, blurred);

      assertEquals(
          1.0,
          difference,
          IDENTICAL_THRESHOLD,
          "Marr-Hildreth should recognize structurally similar images");
    }
  }

  @Nested
  class Edge_Cases_And_Validation {

    @ParameterizedTest
    @EnumSource(ImageContentHash.class)
    void null_images_should_throw_exception(ImageContentHash algorithm) {
      var validImage = createSolidColorImage(10, 10, Scalar.all(100));

      assertThrows(
          NullPointerException.class,
          () -> algorithm.compare(null, validImage),
          "Should throw exception for null first image");

      assertThrows(
          NullPointerException.class,
          () -> algorithm.compare(validImage, null),
          "Should throw exception for null second image");
    }

    @ParameterizedTest
    @EnumSource(ImageContentHash.class)
    void empty_images_should_throw_exception(ImageContentHash algorithm) {
      var validImage = createSolidColorImage(10, 10, Scalar.all(100));
      var emptyImage = new Mat();

      assertThrows(
          IllegalArgumentException.class,
          () -> algorithm.compare(emptyImage, validImage),
          "Should throw exception for empty first image");

      assertThrows(
          IllegalArgumentException.class,
          () -> algorithm.compare(validImage, emptyImage),
          "Should throw exception for empty second image");
    }

    @Test
    void different_image_sizes_should_be_handled_correctly() {
      var smallImage = createSolidColorImage(50, 50, Scalar.all(128));
      var largeImage = createSolidColorImage(200, 200, Scalar.all(128));

      compareImages(ImageContentHash.AVERAGE, smallImage, largeImage);
      // All algorithms should handle different sizes gracefully
      for (var algorithm : ImageContentHash.values()) {
        compareImages(algorithm, smallImage, largeImage);
      }
    }
  }

  // ===================== Helper Methods =====================

  private static Mat createSolidColorImage(int width, int height, Scalar color) {
    var image = new Mat(height, width, CvType.CV_8UC3);
    image.setTo(color);
    return image;
  }

  private static Mat createCheckerboardImage(int width, int height) {
    var image = new Mat(height, width, CvType.CV_8UC3);
    var tileSize = Math.max(width, height) / 8;

    for (int i = 0; i < height; i++) {
      for (int j = 0; j < width; j++) {
        var tileI = i / tileSize;
        var tileJ = j / tileSize;
        var color =
            ((tileI + tileJ) % 2 == 0) ? new double[] {0, 0, 0} : new double[] {255, 255, 255};
        image.put(i, j, color);
      }
    }
    return image;
  }

  private static Mat createGradientImage(int width, int height) {
    var image = new Mat(height, width, CvType.CV_8UC3);

    for (int i = 0; i < height; i++) {
      for (int j = 0; j < width; j++) {
        var red = (int) (255.0 * j / width);
        var green = (int) (255.0 * i / height);
        var blue = (int) (255.0 * (i + j) / (width + height));
        image.put(i, j, blue, green, red); // BGR format
      }
    }
    return image;
  }

  private static Mat applyTransformation(Mat original, String transformation) {
    return switch (transformation) {
      case "color_shift" -> {
        var transformed = new Mat();
        ImageTransformer.applyLUT(original, ColorLut.HUE.getByteLut().lutTable())
            .assignTo(transformed);
        yield transformed;
      }
      case "rotation_90" -> {
        var rotated = ImageTransformer.getRotatedImage(original, Core.ROTATE_90_CLOCKWISE);
        var mat = new Mat();
        rotated.assignTo(mat);
        rotated.release();
        yield mat;
      }
      case "gaussian_blur" -> {
        var blurred = new Mat();
        Imgproc.GaussianBlur(original, blurred, new Size(5, 5), 1.5);
        yield blurred;
      }
      default -> throw new IllegalArgumentException("Unknown transformation: " + transformation);
    };
  }
}
