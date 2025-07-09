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
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.opencv.core.Core.MinMaxLocResult;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.osgi.OpenCVNativeLoader;
import org.weasis.opencv.data.ImageCV;

/**
 * Comprehensive test suite for {@link ImageAnalyzer} functionality.
 *
 * <p>Tests statistical analysis operations including min/max calculations, mean/standard deviation
 * computations, and shape-based measurements for medical image analysis.
 *
 * @author Weasis Team
 */
class ImageAnalyzerTest {

  @BeforeAll
  @DisplayName("Load OpenCV native library")
  static void loadNativeLib() {
    OpenCVNativeLoader loader = new OpenCVNativeLoader();
    loader.init();
  }

  @Nested
  @DisplayName("Min/Max Value Detection")
  class MinMaxValueTests {

    @Test
    @DisplayName("Find raw min/max values - 16-bit image")
    void testFindRawMinMaxValues16Bit() {
      try (ImageCV img = new ImageCV(new Size(7, 7), CvType.CV_16UC1, new Scalar(1024))) {
        img.put(3, 3, new short[] {(short) 32000});
        img.put(4, 4, new short[] {(short) 65535});

        MinMaxLocResult result = ImageAnalyzer.findRawMinMaxValues(img, false);

        assertNotNull(result);
        assertEquals(1024, result.minVal, "Minimum value should match expected");
        assertEquals(65535, result.maxVal, "Maximum value should match expected");
        assertEquals(new Point(0, 0), result.minLoc, "Minimum location should be correct");
        assertEquals(new Point(4, 4), result.maxLoc, "Maximum location should be correct");
      }
    }

    @Test
    @DisplayName("Find raw min/max values - 8-bit image with exclusion")
    void testFindRawMinMaxValues8BitExclusion() {
      try (ImageCV img = new ImageCV(new Size(3, 3), CvType.CV_8UC1, new Scalar(128))) {
        MinMaxLocResult result = ImageAnalyzer.findRawMinMaxValues(img, true);

        assertNotNull(result);
        assertEquals(0, result.minVal, "8-bit exclusion should return default min");
        assertEquals(255, result.maxVal, "8-bit exclusion should return default max");
      }
    }

    @Test
    @DisplayName("Find raw min/max values - black image handling")
    void testFindRawMinMaxValuesBlackImage() {
      try (ImageCV img = new ImageCV(new Size(3, 3), CvType.CV_16UC1, new Scalar(0))) {
        MinMaxLocResult result = ImageAnalyzer.findRawMinMaxValues(img, false);

        assertNotNull(result);
        assertEquals(0, result.minVal, "Black image minimum should be 0");
        assertEquals(1, result.maxVal, "Black image maximum adjusted to avoid division by zero");
      }
    }

    @Test
    @DisplayName("Find min/max values with padding exclusion")
    void testFindMinMaxValuesWithPadding() {
      try (ImageCV img = new ImageCV(new Size(5, 5), CvType.CV_16UC1, new Scalar(1000))) {
        img.put(2, 2, new short[] {(short) 2000});
        img.put(3, 3, new short[] {(short) 3000});

        // Exclude padding value 1000
        MinMaxLocResult result = ImageAnalyzer.findMinMaxValues(img, 1000, null);

        assertNotNull(result);
        assertEquals(2000, result.minVal, "Should find minimum excluding padding");
        assertEquals(3000, result.maxVal, "Should find maximum excluding padding");
      }
    }

    @Test
    @DisplayName("Find min/max values with padding range exclusion")
    void testFindMinMaxValuesWithPaddingRange() {
      try (ImageCV img = new ImageCV(new Size(5, 5), CvType.CV_16UC1, new Scalar(1000))) {
        img.put(1, 1, new short[] {(short) 1500});
        img.put(2, 2, new short[] {(short) 2000});
        img.put(3, 3, new short[] {(short) 3000});

        // Exclude padding range 1000-2000
        MinMaxLocResult result = ImageAnalyzer.findMinMaxValues(img, 1000, 2000);

        assertNotNull(result);
        assertEquals(3000, result.minVal, "Should exclude entire padding range");
        assertEquals(3000, result.maxVal, "Should exclude entire padding range");
      }
    }

    @Test
    @DisplayName("MinMaxLoc with rectangular area")
    void testMinMaxLocWithRectangle() {
      try (ImageCV img = new ImageCV(new Size(10, 10), CvType.CV_8UC1, new Scalar(100))) {
        img.put(2, 2, new byte[] {50});
        img.put(7, 7, new byte[] {(byte) 200});

        Rectangle area = new Rectangle(0, 0, 5, 5);
        MinMaxLocResult result =
            ImageAnalyzer.minMaxLoc(ImageConversion.toBufferedImage(img.toMat()), area);

        assertNotNull(result);
        assertEquals(50, result.minVal, "Should find minimum in specified area");
        assertEquals(100, result.maxVal, "Should find maximum in specified area");
      }
    }

    @Test
    @DisplayName("MinMaxLoc with multi-channel image")
    void testMinMaxLocMultiChannel() {
      try (ImageCV img = new ImageCV(new Size(3, 3), CvType.CV_8UC3, new Scalar(10, 20, 30))) {
        img.put(1, 1, new byte[] {5, 25, 35});

        MinMaxLocResult result = ImageAnalyzer.minMaxLoc(img, null);

        assertNotNull(result);
        assertEquals(5, result.minVal, "Should find global minimum across all channels");
        assertEquals(35, result.maxVal, "Should find global maximum across all channels");
      }
    }

    @Test
    @DisplayName("Null input validation")
    void testNullInputValidation() {
      assertThrows(
          NullPointerException.class,
          () -> ImageAnalyzer.findRawMinMaxValues(null, false),
          "Should throw NPE for null PlanarImage");

      assertThrows(
          IllegalArgumentException.class,
          () -> ImageAnalyzer.findMinMaxValues(new Mat()),
          "Should throw IAE for empty Mat");
    }
  }

  @Nested
  @DisplayName("Statistical Analysis")
  class StatisticalAnalysisTests {

    @Test
    @DisplayName("Mean and standard deviation - entire image")
    void testMeanStdDevEntireImage() {
      try (ImageCV img = new ImageCV(new Size(3, 3), CvType.CV_8UC1, new Scalar(100))) {
        img.put(1, 1, new byte[] {(byte) 150});
        img.put(2, 2, new byte[] {50});

        double[][] result = ImageAnalyzer.meanStdDev(img);

        assertNotNull(result);
        assertEquals(1, result[0].length, "Should have one channel");
        assertEquals(5, result.length, "Should have 5 statistical measures");

        assertEquals(50, result[0][0], "Minimum value");
        assertEquals(150, result[1][0], "Maximum value");
        assertEquals(9, result[4][0], "Pixel count should be 9");
        assertEquals(100, result[2][0], 0.1, "Mean should be positive");
        assertTrue(result[3][0] > 0, "Standard deviation should be positive");
      }
    }

    @Test
    @DisplayName("Mean and standard deviation with shape")
    void testMeanStdDevWithShape() {
      try (ImageCV img = new ImageCV(new Size(10, 10), CvType.CV_8UC1, new Scalar(100))) {
        img.put(5, 5, new byte[] {(byte) 200});

        Shape circle = new Ellipse2D.Double(3, 3, 5, 5);
        double[][] result = ImageAnalyzer.meanStdDev(img, circle);

        assertNotNull(result);
        assertEquals(1, result[0].length, "Should have one channel");
        assertEquals(5, result.length, "Should have 5 statistical measures");
        assertEquals(100, result[0][0], "Minimum in circle");
        assertEquals(200, result[1][0], "Maximum in circle");
        assertTrue(result[2][0] > 100.0, "Mean should be slightly above 100");
        assertTrue(result[3][0] > 10, "Standard deviation should be positive");
        assertTrue(
            result[4][0] > 12 && result[4][0] < 18, "Pixel count in circle should be around 15");
      }
    }

    @Test
    @DisplayName("Mean and standard deviation with padding exclusion")
    void testMeanStdDevWithPaddingExclusion() {
      try (ImageCV img = new ImageCV(new Size(5, 5), CvType.CV_8UC1, new Scalar(0))) {
        img.put(2, 2, new byte[] {100});
        img.put(3, 3, new byte[] {(byte) 200});

        // Exclude padding value 0
        double[][] result = ImageAnalyzer.meanStdDev(img, (Shape) null, 0, null);

        assertNotNull(result);
        assertEquals(100, result[0][0], "Minimum after padding exclusion");
        assertEquals(200, result[1][0], "Maximum after padding exclusion");
        assertEquals(2, result[4][0], "Should count only non-padding pixels");
      }
    }

    @Test
    @DisplayName("Mean and standard deviation with shape and padding")
    void testMeanStdDevWithShapeAndPadding() {
      try (ImageCV img = new ImageCV(new Size(6, 6), CvType.CV_8UC1, new Scalar(0))) {
        img.put(2, 2, new byte[] {100});
        img.put(3, 3, new byte[] {(byte) 150});
        img.put(4, 4, new byte[] {(byte) 200});

        Rectangle rect = new Rectangle(1, 1, 4, 4);
        double[][] result = ImageAnalyzer.meanStdDev(img, rect, 0, null);

        assertNotNull(result);
        assertTrue(result[4][0] > 0, "Should process pixels within shape excluding padding");
        assertEquals(100, result[0][0], "Minimum in region excluding padding");
      }
    }

    @Test
    @DisplayName("Multi-channel statistical analysis")
    void testMeanStdDevMultiChannel() {
      try (ImageCV img = new ImageCV(new Size(3, 3), CvType.CV_8UC3, new Scalar(10, 20, 30))) {
        img.put(1, 1, new byte[] {15, 25, 35});

        double[][] result = ImageAnalyzer.meanStdDev(img);

        assertNotNull(result);
        assertEquals(3, result[0].length, "Multi-channel images return multiple channels");
        assertArrayEquals(
            new double[] {10, 20, 30}, result[0], "Minimum should match channel values");
        assertArrayEquals(
            new double[] {15, 25, 35}, result[1], "Maximum should match channel values");
      }
    }

    @Test
    @DisplayName("Mean and standard deviation with null mask")
    void testMeanStdDevWithNullMask() {
      try (ImageCV img = new ImageCV(new Size(5, 5), CvType.CV_8UC1, new Scalar(50))) {
        double[][] result = ImageAnalyzer.meanStdDev(img.toMat(), (Mat) null, null, null);
        assertNotNull(result);
        assertEquals(50, result[2][0], "Mean with null mask should match scalar value");
        assertEquals(0, result[3][0], "Standard deviation should be zero for homogeneous image");
      }
    }

    @Test
    @DisplayName("Mean and standard deviation with padding value and limit")
    void testMeanStdDevWithPaddingValueAndLimit() {
      try (ImageCV img = new ImageCV(new Size(5, 5), CvType.CV_8UC1, new Scalar(0))) {
        img.put(2, 2, new byte[] {100});
        img.put(3, 3, new byte[] {(byte) 200});

        double[][] result = ImageAnalyzer.meanStdDev(img.toMat(), (Mat) null, 0, 50);
        assertNotNull(result);
        assertEquals(100, result[0][0], "Minimum value after padding exclusion mismatch");
        assertEquals(200, result[1][0], "Maximum value after padding exclusion mismatch");
        assertTrue(result[2][0] > 100, "Mean should reflect non-padding values");
      }
    }

    @Test
    @DisplayName("Mean and standard deviation with multi-channel image")
    void testMeanStdDevWithMultiChannelImage() {
      try (ImageCV img = new ImageCV(new Size(3, 3), CvType.CV_8UC3, new Scalar(10, 20, 30))) {
        img.put(1, 1, new byte[] {15, 25, 35});

        double[][] result = ImageAnalyzer.meanStdDev(img.toMat(), (Mat) null, null, null);

        assertNotNull(result);
        assertEquals(3, result[0].length, "Should return results for all channels");
        assertArrayEquals(new double[] {10, 20, 30}, result[0], "Minimum values mismatch");
        assertArrayEquals(new double[] {15, 25, 35}, result[1], "Maximum values mismatch");
      }
    }

    @Test
    @DisplayName("Mean and standard deviation with empty image")
    void testMeanStdDevWithEmptyImage() {
      Mat empty = new Mat();
      assertThrows(
          IllegalArgumentException.class,
          () -> ImageAnalyzer.meanStdDev(empty, (Shape) null, null, null),
          "Should throw IllegalArgumentException for an empty image");
    }
  }

  @Nested
  @DisplayName("Shape and Mask Operations")
  class ShapeMaskTests {

    @Test
    @DisplayName("Transform rectangle to contour")
    void testTransformRectangleToContour() {
      Rectangle rect = new Rectangle(10, 20, 50, 30);
      List<MatOfPoint> contours = ImageAnalyzer.transformShapeToContour(rect, false);

      assertNotNull(contours);
      assertFalse(contours.isEmpty(), "Should generate contours");

      MatOfPoint contour = contours.get(0);
      Point[] points = contour.toArray();
      assertTrue(points.length >= 4, "Rectangle should have at least 4 points");
    }

    @Test
    @DisplayName("Transform circle to contour")
    void testTransformCircleToContour() {
      Ellipse2D circle = new Ellipse2D.Double(0, 0, 100, 100);
      List<MatOfPoint> contours = ImageAnalyzer.transformShapeToContour(circle, true);

      assertNotNull(contours);
      assertFalse(contours.isEmpty(), "Should generate contours for circle");

      MatOfPoint contour = contours.get(0);
      Point[] points = contour.toArray();
      assertTrue(points.length > 8, "Circle should have many points");
    }

    @Test
    @DisplayName("Get mask image with shape")
    void testGetMaskImageWithShape() {
      try (ImageCV img = new ImageCV(new Size(10, 10), CvType.CV_8UC1, new Scalar(100))) {
        Rectangle rect = new Rectangle(2, 2, 6, 6);

        List<Mat> result = ImageAnalyzer.getMaskImage(img, rect, null, null);

        assertNotNull(result);
        assertEquals(2, result.size(), "Should return source and mask");

        Mat srcCropped = result.get(0);
        Mat mask = result.get(1);

        assertEquals(6, srcCropped.width(), "Cropped width should match rectangle");
        assertEquals(6, srcCropped.height(), "Cropped height should match rectangle");
        assertEquals(srcCropped.size(), mask.size(), "Mask should match source size");
      }
    }

    @Test
    @DisplayName("Get mask image with padding exclusion")
    void testGetMaskImageWithPadding() {
      try (ImageCV img = new ImageCV(new Size(5, 5), CvType.CV_8UC1, new Scalar(0))) {
        img.put(2, 2, new byte[] {100});

        List<Mat> result = ImageAnalyzer.getMaskImage(img, null, 0, null);

        assertNotNull(result);
        assertEquals(2, result.size(), "Should return source and mask");

        Mat mask = result.get(1);
        assertEquals(img.size(), mask.size(), "Mask should match original image size");
      }
    }

    @Test
    @DisplayName("Get mask image with empty intersection")
    void testGetMaskImageEmptyIntersection() {
      try (ImageCV img = new ImageCV(new Size(10, 10), CvType.CV_8UC1, new Scalar(100))) {
        Rectangle outsideRect = new Rectangle(20, 20, 5, 5);

        List<Mat> result = ImageAnalyzer.getMaskImage(img, outsideRect, null, null);

        assertTrue(result.isEmpty(), "Should return empty list for non-intersecting shapes");
      }
    }
  }

  @Nested
  @DisplayName("Error Handling and Edge Cases")
  class ErrorHandlingTests {

    @Test
    @DisplayName("Null shape handling")
    void testNullShapeHandling() {
      try (ImageCV img = new ImageCV(new Size(5, 5), CvType.CV_8UC1, new Scalar(100))) {
        double[][] result = ImageAnalyzer.meanStdDev(img, (Shape) null);

        assertNotNull(result);
        assertEquals(25, result[4][0], "Should process entire image when shape is null");
      }
    }

    @Test
    @DisplayName("Small image handling")
    void testSmallImageHandling() {
      try (ImageCV img = new ImageCV(new Size(1, 1), CvType.CV_8UC1, new Scalar(42))) {
        MinMaxLocResult result = ImageAnalyzer.findMinMaxValues(img);

        assertNotNull(result);
        assertEquals(42, result.minVal);
        assertEquals(42, result.maxVal);
        assertEquals(new Point(0, 0), result.minLoc);
        assertEquals(new Point(0, 0), result.maxLoc);
      }
    }

    @Test
    @DisplayName("Large value range handling")
    void testLargeValueRangeHandling() {
      try (ImageCV img = new ImageCV(new Size(3, 3), CvType.CV_32FC1, new Scalar(-1000000.0))) {
        img.put(1, 1, new float[] {1000000.0f});

        MinMaxLocResult result = ImageAnalyzer.findMinMaxValues(img);

        assertNotNull(result);
        assertEquals(-1000000.0, result.minVal, 0.1);
        assertEquals(1000000.0, result.maxVal, 0.1);
      }
    }

    @Test
    @DisplayName("Invalid padding range handling")
    void testInvalidPaddingRangeHandling() {
      try (ImageCV img = new ImageCV(new Size(5, 5), CvType.CV_8UC1, new Scalar(100))) {
        img.put(1, 1, new byte[] {0});
        img.put(2, 2, new byte[] {50});
        img.put(3, 3, new byte[] {75});
        img.put(4, 4, new byte[] {(byte) 150});
        // Test with paddingValue > paddingLimit (invalid range)
        MinMaxLocResult result = ImageAnalyzer.findMinMaxValues(img, 75, 0);

        assertNotNull(result);
        // Should still work, just with swapped range internally
        assertEquals(100, result.minVal);
        assertEquals(150, result.maxVal);
      }
    }
  }

  @Nested
  @DisplayName("Performance and Memory Tests")
  class PerformanceTests {

    @Test
    @DisplayName("Large image processing")
    void testLargeImageProcessing() {
      // Test with reasonably large image to ensure no memory issues
      try (ImageCV img = new ImageCV(new Size(1000, 1000), CvType.CV_8UC1, new Scalar(128))) {
        img.put(500, 500, new byte[] {(byte) 255});
        img.put(0, 0, new byte[] {0});

        MinMaxLocResult result = ImageAnalyzer.findMinMaxValues(img);

        assertNotNull(result);
        assertEquals(0, result.minVal);
        assertEquals(255, result.maxVal);
        assertEquals(1000000, img.total(), "Should handle large images");
      }
    }

    @Test
    @DisplayName("Memory cleanup verification")
    void testMemoryCleanup() {
      // Create and process multiple images to test memory management
      for (int i = 0; i < 10; i++) {
        ImageCV testImg;
        try (ImageCV img =
            new ImageCV(new Size(1000, 1000), CvType.CV_16UC1, new Scalar(i * 1000))) {
          double[][] stats = ImageAnalyzer.meanStdDev(img);
          assertNotNull(stats);
          assertEquals(i * 1000, stats[2][0], 0.1, "Mean should match scalar value");
          testImg = img; // Keep reference to ensure it is closed after loop
        }
        assertTrue(testImg.isReleased(), "Image should be closed after processing");
        assertTrue(testImg.empty(), "Mat should be empty after release");
      }
      // If we get here without OutOfMemoryError, memory management is working
      assertTrue(true, "Memory management test passed");
    }
  }
}
