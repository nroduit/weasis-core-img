/*
 * Copyright (c) 2023 Weasis Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.weasis.opencv.op;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.weasis.opencv.data.ImageCV;
import org.weasis.opencv.natives.NativeLibrary;

@DisplayNameGeneration(ReplaceUnderscores.class)
class ImageAnalyzerTest {

  @BeforeAll
  static void load_native_library() {
    NativeLibrary.loadLibraryFromLibraryName();
  }

  // Test data builders
  private static final class TestImages {

    static ImageCV uniform(Size size, int type, double... values) {
      return new ImageCV(size, type, new Scalar(values));
    }

    static ImageCV gradient(Size size, int type) {
      var img = new ImageCV(size, type);
      var pixels = new byte[(int) img.total() * img.channels()];
      for (int i = 0; i < pixels.length; i++) {
        pixels[i] = (byte) (i % 256);
      }
      img.put(0, 0, pixels);
      return img;
    }

    static ImageCV withPixels(Size size, int type, double background, PixelValue... pixels) {
      var img = uniform(size, type, background);
      for (var pixel : pixels) {
        switch (img.depth()) {
          case CvType.CV_8U -> img.put(pixel.row, pixel.col, new byte[] {(byte) pixel.value});
          case CvType.CV_16U -> img.put(pixel.row, pixel.col, new short[] {(short) pixel.value});
          case CvType.CV_32F -> img.put(pixel.row, pixel.col, new float[] {(float) pixel.value});
          default -> throw new IllegalArgumentException("Unsupported type: " + type);
        }
      }
      return img;
    }

    static ImageCV multiChannel(Size size, double... channelValues) {
      return new ImageCV(size, CvType.CV_8UC3, new Scalar(channelValues));
    }
  }

  private record PixelValue(int row, int col, double value) {
    static PixelValue at(int row, int col, double value) {
      return new PixelValue(row, col, value);
    }
  }

  private static final class TestShapes {
    static Rectangle rect(int x, int y, int width, int height) {
      return new Rectangle(x, y, width, height);
    }

    static Shape circle(double x, double y, double diameter) {
      return new Ellipse2D.Double(x, y, diameter, diameter);
    }

    static Shape outsideBounds(Size imageSize) {
      return rect((int) imageSize.width + 10, (int) imageSize.height + 10, 5, 5);
    }
  }

  @Nested
  class Min_Max_Value_Detection {

    @ParameterizedTest
    @CsvSource({
      "1024, 32000, 65535, CV_16UC1",
      "128, 200, 255, CV_8UC1",
      "0, 1000, 4095, CV_16UC1"
    })
    void find_raw_min_max_values_with_different_bit_depths(
        int background, int midValue, int maxValue, String typeStr) {

      var type =
          switch (typeStr) {
            case "CV_8UC1" -> CvType.CV_8UC1;
            case "CV_16UC1" -> CvType.CV_16UC1;
            default -> throw new IllegalArgumentException("Unknown type: " + typeStr);
          };

      try (var img =
          TestImages.withPixels(
              new Size(5, 5),
              type,
              background,
              PixelValue.at(2, 2, midValue),
              PixelValue.at(3, 3, maxValue))) {

        var result = ImageAnalyzer.findRawMinMaxValues(img, false);

        assertAll(
            () -> assertEquals(background, result.minVal),
            () -> assertEquals(maxValue, result.maxVal),
            () -> assertEquals(new Point(0, 0), result.minLoc),
            () -> assertEquals(new Point(3, 3), result.maxLoc));
      }
    }

    @Test
    void find_raw_min_max_values_8_bit_optimization() {
      try (var img = TestImages.uniform(new Size(3, 3), CvType.CV_8UC1, 128)) {
        var result = ImageAnalyzer.findRawMinMaxValues(img, true);

        assertAll(() -> assertEquals(0, result.minVal), () -> assertEquals(255, result.maxVal));
      }
    }

    @Test
    void find_raw_min_max_values_handles_equal_values() {
      try (var img = TestImages.uniform(new Size(3, 3), CvType.CV_16UC1, 0)) {
        var result = ImageAnalyzer.findRawMinMaxValues(img, false);

        assertAll(
            () -> assertEquals(0, result.minVal),
            () -> assertEquals(1, result.maxVal) // Adjusted to prevent division by zero
            );
      }
    }

    @ParameterizedTest
    @ValueSource(ints = {1000, 1500, 2000})
    void find_min_max_values_excludes_padding_values(int paddingValue) {
      try (var img =
          TestImages.withPixels(
              new Size(5, 5),
              CvType.CV_16UC1,
              paddingValue,
              PixelValue.at(2, 2, 2000),
              PixelValue.at(3, 3, 3000))) {

        var result = ImageAnalyzer.findMinMaxValues(img, paddingValue, null);

        assertAll(() -> assertTrue(result.minVal >= 2000), () -> assertEquals(3000, result.maxVal));
      }
    }

    @Test
    void find_min_max_values_excludes_padding_range() {
      try (var img =
          TestImages.withPixels(
              new Size(5, 5),
              CvType.CV_16UC1,
              1000,
              PixelValue.at(1, 1, 1500),
              PixelValue.at(2, 2, 2000),
              PixelValue.at(3, 3, 3000))) {

        var result = ImageAnalyzer.findMinMaxValues(img, 1000, 2000);

        assertAll(() -> assertEquals(3000, result.minVal), () -> assertEquals(3000, result.maxVal));
      }
    }

    @Test
    void min_max_loc_with_rectangular_area() {
      try (var img =
          TestImages.withPixels(
              new Size(10, 10),
              CvType.CV_8UC1,
              100,
              PixelValue.at(2, 2, 50), // Inside area
              PixelValue.at(7, 7, 200))) { // Outside area

        var area = TestShapes.rect(0, 0, 5, 5);
        var renderedImage = ImageConversion.toBufferedImage(img.toMat());
        var result = ImageAnalyzer.minMaxLoc(renderedImage, area);

        assertAll(
            () -> assertEquals(50, result.minVal),
            () -> assertEquals(100, result.maxVal) // 200 is outside the area
            );
      }
    }

    @Test
    void min_max_loc_multi_channel_finds_global_extremes() {
      try (var img = TestImages.multiChannel(new Size(3, 3), 10, 20, 30)) {
        img.put(1, 1, new byte[] {5, 25, 35});

        var result = ImageAnalyzer.minMaxLoc(img, null);

        assertAll(() -> assertEquals(5, result.minVal), () -> assertEquals(35, result.maxVal));
      }
    }

    @ParameterizedTest
    @MethodSource("nullInputProvider")
    void throws_exception_for_null_inputs(
        Class<? extends Exception> expectedEx, Executable operation) {
      assertThrows(expectedEx, operation);
    }

    private static Stream<Arguments> nullInputProvider() {
      return Stream.of(
          Arguments.of(
              NullPointerException.class,
              (Executable) () -> ImageAnalyzer.findRawMinMaxValues(null, false)),
          Arguments.of(
              IllegalArgumentException.class,
              (Executable) () -> ImageAnalyzer.findMinMaxValues(new Mat())),
          Arguments.of(
              NullPointerException.class,
              (Executable)
                  () -> ImageAnalyzer.minMaxLoc((java.awt.image.RenderedImage) null, null)));
    }
  }

  @Nested
  class Statistical_Analysis {

    @Test
    void mean_std_dev_entire_image_with_known_values() {
      try (var img =
          TestImages.withPixels(
              new Size(3, 3),
              CvType.CV_8UC1,
              100,
              PixelValue.at(1, 1, 150),
              PixelValue.at(2, 2, 50))) {

        var result = ImageAnalyzer.meanStdDev(img);

        assertAll(
            () -> assertNotNull(result),
            () -> assertEquals(5, result.length), // [min, max, mean, stdDev, pixelCount]
            () -> assertEquals(1, result[0].length), // Single channel
            () -> assertEquals(50, result[0][0]), // Min
            () -> assertEquals(150, result[1][0]), // Max
            () -> assertEquals(9, result[4][0]), // Pixel count
            () -> assertTrue(result[2][0] > 0), // Mean should be positive
            () -> assertTrue(result[3][0] > 0) // StdDev should be positive
            );
      }
    }

    @Test
    void mean_std_dev_with_circular_region() {
      try (var img =
          TestImages.withPixels(new Size(10, 10), CvType.CV_8UC1, 100, PixelValue.at(5, 5, 200))) {

        var circle = TestShapes.circle(3, 3, 5);
        var result = ImageAnalyzer.meanStdDev(img, circle);

        assertAll(
            () -> assertNotNull(result),
            () -> assertEquals(100, result[0][0]), // Min in circle
            () -> assertEquals(200, result[1][0]), // Max in circle
            () -> assertTrue(result[2][0] > 100.0), // Mean should be above background
            () -> assertTrue(result[4][0] > 10 && result[4][0] < 20) // Reasonable pixel count
            );
      }
    }

    @Test
    void mean_std_dev_excludes_padding_values() {
      try (var img =
          TestImages.withPixels(
              new Size(5, 5),
              CvType.CV_8UC1,
              0,
              PixelValue.at(2, 2, 100),
              PixelValue.at(3, 3, 200))) {

        var result = ImageAnalyzer.meanStdDev(img, (Shape) null, 0, null);

        assertAll(
            () -> assertEquals(100, result[0][0]), // Min after exclusion
            () -> assertEquals(200, result[1][0]), // Max after exclusion
            () -> assertEquals(2, result[4][0]) // Only non-padding pixels counted
            );
      }
    }

    @Test
    void mean_std_dev_with_shape_and_padding_exclusion() {
      try (var img =
          TestImages.withPixels(
              new Size(6, 6),
              CvType.CV_8UC1,
              0,
              PixelValue.at(2, 2, 100),
              PixelValue.at(3, 3, 150),
              PixelValue.at(4, 4, 200))) {

        var region = TestShapes.rect(1, 1, 4, 4);
        var result = ImageAnalyzer.meanStdDev(img, region, 0, null);

        assertAll(
            () -> assertTrue(result[4][0] > 0), // Some pixels processed
            () -> assertEquals(100, result[0][0]) // Min in region excluding padding
            );
      }
    }

    @Test
    void mean_std_dev_multi_channel_analysis() {
      try (var img = TestImages.multiChannel(new Size(3, 3), 10, 20, 30)) {
        img.put(1, 1, new byte[] {15, 25, 35});

        var result = ImageAnalyzer.meanStdDev(img);

        assertAll(
            () -> assertEquals(3, result[0].length), // Three channels
            () -> assertArrayEquals(new double[] {10, 20, 30}, result[0]), // Min values
            () -> assertArrayEquals(new double[] {15, 25, 35}, result[1]) // Max values
            );
      }
    }

    @Test
    void mean_std_dev_with_homogeneous_image_has_zero_std_dev() {
      try (var img = TestImages.uniform(new Size(5, 5), CvType.CV_8UC1, 50)) {
        var result = ImageAnalyzer.meanStdDev(img.toMat(), (Mat) null, null, null);

        assertAll(
            () -> assertEquals(50, result[2][0]), // Mean matches scalar
            () -> assertEquals(0, result[3][0], 0.0001) // Zero standard deviation
            );
      }
    }

    @ParameterizedTest
    @CsvSource({"0, 50, 100, 200", "10, 20, 150, 250"})
    void mean_std_dev_with_padding_range_exclusion(
        int paddingStart, int paddingEnd, int value1, int value2) {

      try (var img =
          TestImages.withPixels(
              new Size(5, 5),
              CvType.CV_8UC1,
              paddingStart,
              PixelValue.at(2, 2, value1),
              PixelValue.at(3, 3, value2))) {

        var result = ImageAnalyzer.meanStdDev(img.toMat(), (Mat) null, paddingStart, paddingEnd);

        assertAll(
            () -> assertTrue(result[0][0] >= value1), // Min should be >= value1
            () -> assertEquals(value2, result[1][0]), // Max should be value2
            () -> assertTrue(result[2][0] > value1) // Mean should reflect non-padding values
            );
      }
    }

    @Test
    void mean_std_dev_throws_for_empty_image() {
      var emptyMat = new Mat();
      assertThrows(
          IllegalArgumentException.class,
          () -> ImageAnalyzer.meanStdDev(emptyMat, (Shape) null, null, null));
    }
  }

  @Nested
  class Shape_And_Mask_Operations {

    @ParameterizedTest
    @MethodSource("shapeProvider")
    void transform_shape_to_contour(Shape shape, int expectedMinPoints, String description) {
      var contours = ImageAnalyzer.transformShapeToContour(shape, false);

      assertAll(
          () -> assertNotNull(contours, description),
          () -> assertFalse(contours.isEmpty(), "Should generate contours for " + description),
          () -> {
            var contour = contours.get(0);
            var points = contour.toArray();
            assertTrue(
                points.length >= expectedMinPoints,
                "Should have at least " + expectedMinPoints + " points for " + description);
          });
    }

    private static Stream<Arguments> shapeProvider() {
      return Stream.of(
          Arguments.of(TestShapes.rect(10, 20, 50, 30), 4, "rectangle"),
          Arguments.of(TestShapes.circle(0, 0, 100), 8, "circle"));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void transform_shape_respects_coordinate_system(boolean keepImageCoordinates) {
      var rect = TestShapes.rect(10, 20, 50, 30);
      var contours = ImageAnalyzer.transformShapeToContour(rect, keepImageCoordinates);

      assertNotNull(contours);
      assertFalse(contours.isEmpty());

      var points = contours.get(0).toArray();
      if (keepImageCoordinates) {
        assertTrue(points[0].x >= 10, "Should preserve original coordinates");
      } else {
        assertTrue(points[0].x >= 0 && points[0].x < 50, "Should translate to shape bounds");
      }
    }

    @Test
    void get_mask_image_with_shape_creates_proper_crop_and_mask() {
      try (var img = TestImages.uniform(new Size(10, 10), CvType.CV_8UC1, 100)) {
        var region = TestShapes.rect(2, 2, 6, 6);

        var result = ImageAnalyzer.getMaskImage(img, region, null, null);

        assertAll(
            () -> assertEquals(2, result.size(), "Should return source and mask"),
            () -> {
              var croppedSrc = result.get(0);
              var mask = result.get(1);
              assertEquals(6, croppedSrc.width());
              assertEquals(6, croppedSrc.height());
              assertEquals(croppedSrc.size(), mask.size());
            });
      }
    }

    @Test
    void get_mask_image_with_padding_exclusion() {
      try (var img =
          TestImages.withPixels(new Size(5, 5), CvType.CV_8UC1, 0, PixelValue.at(2, 2, 100))) {

        var result = ImageAnalyzer.getMaskImage(img, null, 0, null);

        assertAll(
            () -> assertEquals(2, result.size()),
            () -> assertEquals(img.size(), result.get(1).size()));
      }
    }

    @Test
    void get_mask_image_returns_empty_for_no_intersection() {
      try (var img = TestImages.uniform(new Size(10, 10), CvType.CV_8UC1, 100)) {
        var outsideShape = TestShapes.outsideBounds(new Size(10, 10));

        var result = ImageAnalyzer.getMaskImage(img, outsideShape, null, null);

        assertTrue(result.isEmpty());
      }
    }
  }

  @Nested
  class Error_Handling_And_Edge_Cases {

    @Test
    void handles_null_shape_by_processing_entire_image() {
      try (var img = TestImages.uniform(new Size(5, 5), CvType.CV_8UC1, 100)) {
        var result = ImageAnalyzer.meanStdDev(img, (Shape) null);

        assertEquals(25, result[4][0]); // Should process all 25 pixels
      }
    }

    @Test
    void handles_single_pixel_image() {
      try (var img = TestImages.uniform(new Size(1, 1), CvType.CV_8UC1, 42)) {
        var result = ImageAnalyzer.findMinMaxValues(img);

        assertAll(
            () -> assertEquals(42, result.minVal),
            () -> assertEquals(42, result.maxVal),
            () -> assertEquals(new Point(0, 0), result.minLoc),
            () -> assertEquals(new Point(0, 0), result.maxLoc));
      }
    }

    @Test
    void handles_extreme_value_ranges() {
      try (var img =
          TestImages.withPixels(
              new Size(3, 3), CvType.CV_32FC1, -1_000_000.0f, PixelValue.at(1, 1, 1_000_000.0f))) {

        var result = ImageAnalyzer.findMinMaxValues(img);

        assertAll(
            () -> assertEquals(-1_000_000.0, result.minVal, 0.1),
            () -> assertEquals(1_000_000.0, result.maxVal, 0.1));
      }
    }

    @Test
    void normalizes_invalid_padding_ranges() {
      try (var img =
          TestImages.withPixels(
              new Size(5, 5),
              CvType.CV_8UC1,
              100,
              PixelValue.at(1, 1, 0),
              PixelValue.at(2, 2, 50),
              PixelValue.at(3, 3, 75),
              PixelValue.at(4, 4, 150))) {

        // paddingValue > paddingLimit (should be swapped internally)
        var result = ImageAnalyzer.findMinMaxValues(img, 75, 0);

        assertAll(() -> assertEquals(100, result.minVal), () -> assertEquals(150, result.maxVal));
      }
    }

    @Test
    void handles_null_padding_value_gracefully() {
      try (var img = TestImages.uniform(new Size(3, 3), CvType.CV_8UC1, 100)) {
        var result1 = ImageAnalyzer.findMinMaxValues(img, null, 50);
        var result2 = ImageAnalyzer.findMinMaxValues(img);

        assertAll(
            () -> assertEquals(result2.minVal, result1.minVal),
            () -> assertEquals(result2.maxVal, result1.maxVal));
      }
    }
  }

  @Nested
  class Performance_And_Memory_Tests {

    @ParameterizedTest
    @ValueSource(ints = {100, 500, 1000})
    void handles_large_images_efficiently(int size) {
      try (var img =
          TestImages.withPixels(
              new Size(size, size),
              CvType.CV_8UC1,
              128,
              PixelValue.at(size / 2, size / 2, 255),
              PixelValue.at(0, 0, 0))) {

        var result = ImageAnalyzer.findMinMaxValues(img);

        assertAll(
            () -> assertEquals(0, result.minVal),
            () -> assertEquals(255, result.maxVal),
            () -> assertEquals(size * size, img.total()));
      }
    }

    @Test
    void memory_management_with_multiple_operations() {
      var imageCount = 10;
      var imageSize = new Size(1000, 1000);

      for (int i = 0; i < imageCount; i++) {
        ImageCV testImg;
        try (var img = TestImages.uniform(imageSize, CvType.CV_16UC1, i * 1000)) {
          var stats = ImageAnalyzer.meanStdDev(img);
          assertNotNull(stats);
          assertEquals(i * 1000, stats[2][0], 0.1);
          testImg = img;
        }
        assertTrue(testImg.isReleased());
        assertTrue(testImg.empty());
      }
    }

    @Test
    void concurrent_access_safety() {
      var sharedImage = TestImages.gradient(new Size(100, 100), CvType.CV_8UC1);

      try (sharedImage) {
        var results =
            Stream.generate(
                    () ->
                        (Runnable)
                            () -> {
                              var result = ImageAnalyzer.meanStdDev(sharedImage);
                              assertNotNull(result);
                            })
                .limit(10)
                .parallel()
                .map(
                    task -> {
                      task.run();
                      return "completed";
                    })
                .toList();

        assertEquals(10, results.size());
      }
    }
  }
}
