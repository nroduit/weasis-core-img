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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.osgi.OpenCVNativeLoader;
import org.weasis.opencv.data.ImageCV;

/**
 * Comprehensive test suite for ImageTransformer operations.
 *
 * <p>This test class provides thorough coverage of all ImageTransformer methods including:
 *
 * <ul>
 *   <li>Geometric transformations (crop, scale, rotate, flip, warp)
 *   <li>Pixel value transformations (LUT, rescale, invert, bitwise operations)
 *   <li>Visual effects (overlay, merge, shutter, crop mask)
 *   <li>Edge cases and error conditions
 *   <li>Memory management and resource cleanup
 * </ul>
 *
 * @author Weasis Team
 */
@Execution(ExecutionMode.CONCURRENT)
class ImageTransformerTest {

  @BeforeAll
  @DisplayName("Load OpenCV native library")
  static void loadNativeLib() {
    OpenCVNativeLoader loader = new OpenCVNativeLoader();
    loader.init();
  }

  @Nested
  @DisplayName("Geometric Transformations")
  class GeometricTransformations {

    @Test
    @DisplayName("Should crop image to specified rectangle")
    void testCrop() {
      // Create test image
      Mat source = createTestImage(100, 100, CvType.CV_8UC3);
      Rectangle cropArea = new Rectangle(20, 30, 40, 35);

      // Perform crop
      ImageCV result = ImageTransformer.crop(source, cropArea);

      // Verify dimensions
      assertEquals(40, result.width());
      assertEquals(35, result.height());
      assertEquals(source.channels(), result.channels());

      // Clean up
      source.release();
      result.release();
    }

    @Test
    @DisplayName("Should handle crop area extending beyond image bounds")
    void testCropBeyondBounds() {
      Mat source = createTestImage(50, 50, CvType.CV_8UC1);
      Rectangle cropArea = new Rectangle(40, 40, 30, 30); // Extends beyond 50x50

      ImageCV result = ImageTransformer.crop(source, cropArea);

      // Should crop to intersection with image bounds
      assertEquals(10, result.width());
      assertEquals(10, result.height());

      source.release();
      result.release();
    }

    @Test
    @DisplayName("Should throw exception for null crop area")
    void testCropNullArea() {
      Mat source = createTestImage(50, 50, CvType.CV_8UC1);

      assertThrows(
          NullPointerException.class,
          () -> {
            ImageTransformer.crop(source, null);
          });

      source.release();
    }

    @Test
    @DisplayName("Should scale image using default interpolation")
    void testScaleDefault() {
      Mat source = createTestImage(100, 100, CvType.CV_8UC3);
      Dimension targetSize = new Dimension(200, 150);

      ImageCV result = ImageTransformer.scale(source, targetSize);

      assertEquals(200, result.width());
      assertEquals(150, result.height());
      assertEquals(source.channels(), result.channels());
      double diff = ImageContentHash.PHASH.compare(result, source);
      assertTrue(diff < 0.001, "Images should be similar after scaling with default interpolation");

      source.release();
      result.release();
    }

    @ParameterizedTest
    @ValueSource(ints = {Imgproc.INTER_NEAREST, Imgproc.INTER_LINEAR, Imgproc.INTER_CUBIC})
    @DisplayName("Should scale with different interpolation methods")
    void testScaleWithInterpolation(int interpolation) {
      Mat source = createTestImage(50, 50, CvType.CV_8UC1);
      Dimension targetSize = new Dimension(100, 75);

      ImageCV result = ImageTransformer.scale(source, targetSize, interpolation);

      assertEquals(100, result.width());
      assertEquals(75, result.height());

      double diff = ImageContentHash.PHASH.compare(result, source);
      assertTrue(
          diff < 0.001,
          "Images should be similar after scaling with interpolation " + interpolation);

      source.release();
      result.release();
    }

    @Test
    @DisplayName("Should throw exception for invalid dimensions")
    void testScaleInvalidDimensions() {
      Mat source = createTestImage(50, 50, CvType.CV_8UC1);

      assertThrows(
          IllegalArgumentException.class,
          () -> {
            ImageTransformer.scale(source, new Dimension(0, 50));
          });

      assertThrows(
          IllegalArgumentException.class,
          () -> {
            ImageTransformer.scale(source, new Dimension(50, -10));
          });

      source.release();
    }

    @ParameterizedTest
    @ValueSource(
        ints = {Core.ROTATE_90_CLOCKWISE, Core.ROTATE_90_COUNTERCLOCKWISE, Core.ROTATE_180})
    @DisplayName("Should rotate image correctly")
    void testRotation(int rotationType) {
      Mat source = createTestImage(100, 80, CvType.CV_8UC3);

      ImageCV result = ImageTransformer.getRotatedImage(source, rotationType);
      if (rotationType == Core.ROTATE_180) {
        assertEquals(100, result.width());
        assertEquals(80, result.height());
      } else {
        // 90-degree rotations swap dimensions
        assertEquals(80, result.width());
        assertEquals(100, result.height());
      }

      source.release();
      result.release();
    }

    @Test
    @DisplayName("Should handle invalid rotation type")
    void testInvalidRotation() {
      Mat source = createTestImage(50, 50, CvType.CV_8UC1);

      ImageCV result = ImageTransformer.getRotatedImage(source, 5); // Invalid

      // Should return clone of original
      assertEquals(50, result.width());
      assertEquals(50, result.height());

      source.release();
      result.release();
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, -1})
    @DisplayName("Should flip image correctly")
    void testFlip(int flipType) {
      Mat source = createTestImage(60, 40, CvType.CV_8UC1);

      ImageCV result = ImageTransformer.flip(source, flipType);

      // Dimensions should remain the same
      assertEquals(60, result.width());
      assertEquals(40, result.height());

      double diff = ImageContentHash.PHASH.compare(result, source);
      assertTrue(diff > 2, "Images should differ after flipping with type " + flipType);

      source.release();
      result.release();
    }

    @Test
    @DisplayName("Should apply affine transformation")
    void testWarpAffine() {
      Mat source = createTestImage(100, 100, CvType.CV_8UC3);
      Mat transformMatrix = Mat.eye(2, 3, CvType.CV_32F);
      Size outputSize = new Size(120, 90);

      ImageCV result = ImageTransformer.warpAffine(source, transformMatrix, outputSize, null);

      assertEquals(120, result.width());
      assertEquals(90, result.height());

      double diff = ImageContentHash.PHASH.compare(result, source);
      assertTrue(diff > 2, "Images should differ after affine transformation");

      source.release();
      transformMatrix.release();
      result.release();
    }

    @Test
    @DisplayName("Should throw exception for invalid affine matrix")
    void testInvalidAffineMatrix() {
      Mat source = createTestImage(50, 50, CvType.CV_8UC1);
      Mat invalidMatrix = Mat.eye(3, 3, CvType.CV_32F); // Wrong size
      Size outputSize = new Size(50, 50);

      assertThrows(
          IllegalArgumentException.class,
          () -> {
            ImageTransformer.warpAffine(source, invalidMatrix, outputSize, null);
          });

      source.release();
      invalidMatrix.release();
    }
  }

  @Nested
  @DisplayName("Pixel Value Transformations")
  class PixelValueTransformations {

    @Test
    @DisplayName("Should apply LUT transformation")
    void testApplyLUT() {
      Mat source = createTestImage(50, 50, CvType.CV_8UC1);
      byte[][] lut = createTestLUT(1);

      ImageCV result = ImageTransformer.applyLUT(source, lut);

      assertEquals(source.width(), result.width());
      assertEquals(source.height(), result.height());
      assertEquals(source.channels(), result.channels());

      double diff = ImageContentHash.COLOR_MOMENT.compare(result, source);
      assertTrue(diff > 25, "Images should differ after LUT application");

      source.release();
      result.release();
    }

    @Test
    @DisplayName("Should apply multi-channel LUT")
    void testApplyMultiChannelLUT() {
      Mat source = createTestImage(50, 50, CvType.CV_8UC3);
      byte[][] lut = createTestLUT(3);

      ImageCV result = ImageTransformer.applyLUT(source, lut);

      assertEquals(source.width(), result.width());
      assertEquals(source.height(), result.height());

      source.release();
      result.release();
    }

    @Test
    @DisplayName("Should throw exception for invalid LUT")
    void testInvalidLUT() {
      Mat source = createTestImage(50, 50, CvType.CV_8UC1);

      // Empty LUT
      assertThrows(
          IllegalArgumentException.class,
          () -> {
            ImageTransformer.applyLUT(source, new byte[0][]);
          });

      // Wrong LUT size
      assertThrows(
          IllegalArgumentException.class,
          () -> {
            ImageTransformer.applyLUT(source, new byte[1][100]); // Should be 256
          });

      source.release();
    }

    @Test
    @DisplayName("Should rescale to byte range")
    void testRescaleToByte() {
      Mat source = new Mat(50, 50, CvType.CV_16UC1);
      source.setTo(Scalar.all(1000)); // 16-bit values

      ImageCV result = ImageTransformer.rescaleToByte(source, 0.1, 50);
      // Rescaled value should be (1000 * 0.1) + 50 = 150
      assertEquals(150, result.get(0, 0)[0], "Rescaled pixel value should be correct");

      assertEquals(CvType.CV_8U, CvType.depth(result.type()));
      assertEquals(50, result.width());
      assertEquals(50, result.height());

      source.release();
      result.release();
    }

    @Test
    @DisplayName("Should invert pixel values")
    void testInvertLUT() {
      ImageCV source = new ImageCV(50, 50, CvType.CV_8UC1);
      source.setTo(Scalar.all(100));

      ImageCV result = ImageTransformer.invertLUT(source);
      // Inverted value should be 255 - 100 = 155
      assertEquals(155, result.get(0, 0)[0], "Inverted pixel value should be correct");
      assertEquals(source.width(), result.width());
      assertEquals(source.height(), result.height());

      source.release();
      result.release();
    }

    @Test
    @DisplayName("Should perform bitwise AND operation")
    void testBitwiseAnd() {
      Mat source = new Mat(50, 50, CvType.CV_8UC1);
      source.setTo(Scalar.all(255));

      ImageCV result = ImageTransformer.bitwiseAnd(source, 0x0F);
      // Result should be 255 & 0x0F = 15
      assertEquals(15, result.get(0, 0)[0], "Bitwise AND result should be correct");

      assertEquals(source.width(), result.width());
      assertEquals(source.height(), result.height());

      source.release();
      result.release();
    }
  }

  @Nested
  @DisplayName("Visual Effects")
  class VisualEffects {

    @Test
    @DisplayName("Should merge two images with opacity")
    void testMergeImages() {
      Mat source1 = createTestImage(100, 100, CvType.CV_8UC3);
      Mat source2 = createTestImage(100, 100, CvType.CV_8UC3);

      ImageCV result = ImageTransformer.mergeImages(source1, source2, 0.7, 0.3);
      // Result should have same value as a weighted sum of source1 and source2
      double diff = ImageContentHash.COLOR_MOMENT.compare(result, source1);
      assertTrue(diff <= 0.000001, "Merged image should be similar to source1 with opacity 0.7");

      source1.release();
      source2.release();
      result.release();
    }

    @Test
    @DisplayName("Should throw exception for mismatched image sizes")
    void testMergeImagesSizeMismatch() {
      Mat source1 = createTestImage(100, 100, CvType.CV_8UC3);
      Mat source2 = createTestImage(50, 50, CvType.CV_8UC3);

      assertThrows(
          IllegalArgumentException.class,
          () -> {
            ImageTransformer.mergeImages(source1, source2, 0.5, 0.5);
          });

      source1.release();
      source2.release();
    }

    @Test
    @DisplayName("Should throw exception for invalid opacity values")
    void testMergeImagesInvalidOpacity() {
      Mat source1 = createTestImage(50, 50, CvType.CV_8UC3);
      Mat source2 = createTestImage(50, 50, CvType.CV_8UC3);

      assertThrows(
          IllegalArgumentException.class,
          () -> {
            ImageTransformer.mergeImages(source1, source2, -0.1, 0.5);
          });

      assertThrows(
          IllegalArgumentException.class,
          () -> {
            ImageTransformer.mergeImages(source1, source2, 0.5, 1.1);
          });

      source1.release();
      source2.release();
    }

    @Test
    @DisplayName("Should create overlay with Mat mask with a 16-bit unsigned image")
    void testOverlayWithMaskOn16BitUnsignedImage() {
      Mat source = createTestImage(50, 50, CvType.CV_16UC1);
      Mat mask = createTestMask(50, 50);
      Color overlayColor = Color.PINK;

      ImageCV result = ImageTransformer.overlay(source, mask, overlayColor);
      short[] pix = new short[1];
      // Result should have overlay color where mask is non-zero
      mask.get(0, 0, new byte[50 * 50]);
      for (int i = 0; i < mask.rows(); i++) {
        for (int j = 0; j < mask.cols(); j++) {
          if (mask.get(i, j)[0] > 0) {
            result.get(i, j, pix);
            assertEquals(65535, pix[0] & 0xFFFF, "Pixel value should match with the overlay color");
          }
        }
      }

      source.release();
      mask.release();
      result.release();
    }

    @Test
    @DisplayName("Should create overlay with Mat mask with a 16-bit image")
    void testOverlayWithMaskOn16BitImage() {
      Mat source = createTestImage(50, 50, CvType.CV_16SC1);
      Mat mask = createTestMask(50, 50);
      Color overlayColor = Color.PINK;

      ImageCV result = ImageTransformer.overlay(source, mask, overlayColor);
      short[] pix = new short[1];
      // Result should have overlay color where mask is non-zero
      mask.get(0, 0, new byte[50 * 50]);
      for (int i = 0; i < mask.rows(); i++) {
        for (int j = 0; j < mask.cols(); j++) {
          if (mask.get(i, j)[0] > 0) {
            result.get(i, j, pix);
            assertEquals(32767, pix[0] & 0xFFFF, "Pixel value should match with the overlay color");
          }
        }
      }

      source.release();
      mask.release();
      result.release();
    }

    @Test
    @DisplayName("Should create overlay with Mat mask")
    void testOverlayWithMat() {
      Mat source = createTestImage(100, 100, CvType.CV_8UC3);
      Mat mask = createTestMask(100, 100);
      Color overlayColor = Color.PINK;

      ImageCV result = ImageTransformer.overlay(source, mask, overlayColor);
      byte[] pix = new byte[3];
      // Result should have overlay color where mask is non-zero
      mask.get(0, 0, new byte[100 * 100]);
      for (int i = 0; i < mask.rows(); i++) {
        for (int j = 0; j < mask.cols(); j++) {
          if (mask.get(i, j)[0] > 0) {
            result.get(i, j, pix);
            assertEquals(
                overlayColor,
                new Color(pix[2] & 0xFF, pix[1] & 0xFF, pix[0] & 0xFF),
                "Pixel value should match with the overlay color");
          }
        }
      }

      source.release();
      mask.release();
      result.release();
    }

    @Test
    @DisplayName("Should create overlay with RenderedImage mask")
    void testOverlayWithRenderedImage() {
      Mat source = createTestImage(100, 100, CvType.CV_8UC3);
      BufferedImage maskImage = new BufferedImage(100, 100, BufferedImage.TYPE_BYTE_GRAY);
      Color overlayColor = Color.BLUE;

      ImageCV result = ImageTransformer.overlay(source, maskImage, overlayColor);

      byte[] pix = new byte[3];
      // Result should have overlay color where mask is non-zero
      for (int i = 0; i < maskImage.getHeight(); i++) {
        for (int j = 0; j < maskImage.getWidth(); j++) {
          int maskValue = maskImage.getRGB(j, i) & 0xFF;
          if (maskValue > 0) {
            // Check if the pixel in the result matches the overlay color
            result.get(i, j, pix);
            assertEquals(
                overlayColor,
                new Color(pix[2] & 0xFF, pix[1] & 0xFF, pix[0] & 0xFF),
                "Pixel value should match with the overlay color");
          }
        }
      }

      source.release();
      result.release();
    }

    @Test
    @DisplayName("Should draw shape on image")
    void testDrawShape() {
      BufferedImage source = new BufferedImage(100, 100, BufferedImage.TYPE_3BYTE_BGR);
      Shape shape = new Ellipse2D.Double(25, 25, 50, 50);
      Color drawColor = Color.GREEN;

      BufferedImage result = ImageTransformer.drawShape(source, shape, drawColor);
      assertEquals(
          drawColor, new Color(result.getRGB(40, 40)), "Shape area should match draw color");

      assertEquals(source.getWidth(), result.getWidth());
      assertEquals(source.getHeight(), result.getHeight());
    }

    @Test
    @DisplayName("Should apply crop mask with alpha")
    void testApplyCropMask() {
      ImageCV source = new ImageCV(100, 100, CvType.CV_8UC3);
      source.setTo(Scalar.all(255)); // Fill with white
      Rectangle bounds = new Rectangle(25, 25, 50, 50);
      double alpha = 0.5;

      ImageCV result = ImageTransformer.applyCropMask(source, bounds, alpha);
      byte[] pix = new byte[3];
      result.get(20, 20, pix);
      assertEquals(
          Math.round(255 * alpha), pix[0] & 0xFF, "Pixel value should match alpha applied");
      assertEquals(
          Math.round(255 * alpha), pix[1] & 0xFF, "Pixel value should match alpha applied");
      assertEquals(
          Math.round(255 * alpha), pix[2] & 0xFF, "Pixel value should match alpha applied");

      result.get(30, 30, pix);
      assertEquals(
          Color.WHITE,
          new Color(pix[2] & 0xFF, pix[1] & 0xFF, pix[0] & 0xFF),
          "Pixel value should match with the original image");

      assertEquals(source.width(), result.width());
      assertEquals(source.height(), result.height());

      source.release();
      result.release();
    }

    @Test
    @DisplayName("Should throw exception for invalid alpha in crop mask")
    void testApplyCropMaskInvalidAlpha() {
      Mat source = createTestImage(50, 50, CvType.CV_8UC1);
      Rectangle bounds = new Rectangle(10, 10, 30, 30);

      assertThrows(
          IllegalArgumentException.class,
          () -> {
            ImageTransformer.applyCropMask(source, bounds, -0.1);
          });

      assertThrows(
          IllegalArgumentException.class,
          () -> {
            ImageTransformer.applyCropMask(source, bounds, 1.1);
          });

      source.release();
    }

    @Test
    @DisplayName("Should apply shutter with shape")
    void testApplyShutterWithShape() {
      ImageCV source = new ImageCV(100, 100, CvType.CV_8UC3);
      source.setTo(Scalar.all(255)); // Fill with white
      Shape shutterShape = new Rectangle2D.Double(20, 20, 60, 60);
      Color shutterColor = Color.BLACK;

      ImageCV result = ImageTransformer.applyShutter(source, shutterShape, shutterColor);

      byte[] pix = new byte[3];
      result.get(15, 15, pix);
      assertEquals(
          shutterColor,
          new Color(pix[2] & 0xFF, pix[1] & 0xFF, pix[0] & 0xFF),
          "Pixel value should match with the shutter color");

      result.get(30, 30, pix);
      assertEquals(
          Color.WHITE,
          new Color(pix[2] & 0xFF, pix[1] & 0xFF, pix[0] & 0xFF),
          "Pixel value should match with the original image");

      assertEquals(100, result.width());
      assertEquals(100, result.height());

      source.release();
      result.release();
    }

    @Test
    @DisplayName("Should apply shutter with RenderedImage")
    void testApplyShutterWithRenderedImage() {
      ImageCV source = new ImageCV(100, 100, CvType.CV_8UC3);
      source.setTo(Scalar.all(255)); // Fill with white
      BufferedImage shutterMask = new BufferedImage(100, 100, BufferedImage.TYPE_BYTE_GRAY);
      Shape shutterShape = new Rectangle2D.Double(20, 20, 60, 60);
      Graphics2D g2d = shutterMask.createGraphics();
      g2d.setColor(Color.WHITE);
      g2d.fill(shutterShape);
      g2d.dispose();

      Color shutterColor = Color.PINK;

      ImageCV result = ImageTransformer.applyShutter(source, shutterMask, shutterColor);
      byte[] pix = new byte[3];
      result.get(15, 15, pix);
      assertEquals(
          Color.WHITE,
          new Color(pix[2] & 0xFF, pix[1] & 0xFF, pix[0] & 0xFF),
          "Pixel value should match with the original image");
      result.get(30, 30, pix);
      assertEquals(
          shutterColor,
          new Color(pix[2] & 0xFF, pix[1] & 0xFF, pix[0] & 0xFF),
          "Pixel value should match with the shutter color");

      assertEquals(100, result.width());
      assertEquals(100, result.height());

      source.release();
      result.release();
    }
  }

  @Nested
  @DisplayName("Error Handling and Edge Cases")
  class ErrorHandlingAndEdgeCases {

    @Test
    @DisplayName("Should handle null source image gracefully")
    void testNullSourceImage() {
      assertThrows(
          NullPointerException.class,
          () -> {
            ImageTransformer.crop(null, new Rectangle(10, 10, 20, 20));
          });

      assertThrows(
          NullPointerException.class,
          () -> {
            ImageTransformer.scale(null, new Dimension(50, 50));
          });

      assertThrows(
          NullPointerException.class,
          () -> {
            ImageTransformer.flip(null, 1);
          });
    }

    @Test
    @DisplayName("Should handle empty image")
    void testEmptyImage() {
      Mat emptyImage = new Mat();

      assertThrows(
          IllegalArgumentException.class,
          () -> {
            ImageTransformer.crop(emptyImage, new Rectangle(0, 0, 10, 10));
          });

      emptyImage.release();
    }

    @Test
    @DisplayName("Should handle very small crop areas")
    void testVerySmallCropArea() {
      Mat source = createTestImage(100, 100, CvType.CV_8UC1);
      Rectangle tinyArea = new Rectangle(50, 50, 1, 1);

      // Should return clone instead of submat for image smaller than 2x2
      ImageCV result = ImageTransformer.crop(source, tinyArea);

      assertEquals(source.width(), result.width());
      assertEquals(source.height(), result.height());

      source.release();
      result.release();
    }

    @Test
    @DisplayName("Should handle single pixel operations")
    void testSinglePixelOperations() {
      Mat singlePixel = new Mat(1, 1, CvType.CV_8UC3);
      singlePixel.setTo(Scalar.all(128));

      // Scale up
      ImageCV scaled = ImageTransformer.scale(singlePixel, new Dimension(10, 10));
      assertEquals(10, scaled.width());
      assertEquals(10, scaled.height());

      // Rotate
      ImageCV rotated = ImageTransformer.getRotatedImage(singlePixel, Core.ROTATE_90_CLOCKWISE);
      assertEquals(1, rotated.width());
      assertEquals(1, rotated.height());

      singlePixel.release();
      scaled.release();
      rotated.release();
    }
  }

  @Nested
  @DisplayName("Performance and Memory Management")
  class PerformanceAndMemoryManagement {

    @Test
    @DisplayName("Should handle large image operations")
    void testLargeImageOperations() {
      // Create a reasonably large test image
      Mat largeImage = createTestImage(1200, 1000, CvType.CV_8UC3);

      try {
        // Test scaling
        ImageCV scaled = ImageTransformer.scale(largeImage, new Dimension(2400, 2000));
        assertEquals(2400, scaled.width());
        assertEquals(2000, scaled.height());
        scaled.release();

        // Test rotation
        ImageCV rotated = ImageTransformer.getRotatedImage(largeImage, Core.ROTATE_90_CLOCKWISE);
        assertEquals(1000, rotated.width());
        assertEquals(1200, rotated.height());
        rotated.release();

      } finally {
        largeImage.release();
      }
    }

    @Test
    @DisplayName("Should handle concurrent operations safely")
    void testConcurrentOperations() throws Exception {
      ExecutorService executor = Executors.newFixedThreadPool(4);
      int numTasks = 10;
      Future<?>[] futures = new Future[numTasks];

      try {
        for (int i = 0; i < numTasks; i++) {
          futures[i] =
              executor.submit(
                  () -> {
                    Mat source = createTestImage(100, 100, CvType.CV_8UC3);
                    try {
                      ImageCV result = ImageTransformer.scale(source, new Dimension(200, 200));
                      assertEquals(200, result.width());
                      assertEquals(200, result.height());
                      result.release();
                    } finally {
                      source.release();
                    }
                  });
        }

        // Wait for all tasks to complete
        for (Future<?> future : futures) {
          future.get(5, TimeUnit.SECONDS);
        }

      } finally {
        executor.shutdown();
      }
    }

    @Test
    @DisplayName("Should properly release resources")
    void testResourceManagement() {
      Mat source = createTestImage(100, 100, CvType.CV_8UC3);
      ImageCV result = ImageTransformer.scale(source, new Dimension(50, 50));

      // Test that we can manually release resources
      assertDoesNotThrow(
          () -> {
            result.release();
            source.release();
          });

      // Verify that the images have been released
      assertTrue(result.isReleased(), "Result image should be released");
      assertTrue(result.empty(), "Result image should be empty after release");
      assertTrue(source.empty(), "Source image should be empty after release");
    }
  }

  // Helper methods

  /** Creates a test image with specified dimensions and type. */
  static Mat createTestImage(int width, int height, int type) {
    Mat image = new Mat(height, width, type);
    // Fill with gradient pattern for better testing
    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        double value = (x + y) % 256;
        if (type == CvType.CV_8UC3) {
          image.put(y, x, value, value * 0.8, value * 0.6);
        } else {
          image.put(y, x, value);
        }
      }
    }
    return image;
  }

  /** Creates a test mask with a circular pattern. */
  private Mat createTestMask(int width, int height) {
    Mat mask = Mat.zeros(height, width, CvType.CV_8UC1);
    int centerX = width / 2;
    int centerY = height / 2;
    int radius = Math.min(width, height) / 4;

    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        double distance = Math.sqrt((x - centerX) * (x - centerX) + (y - centerY) * (y - centerY));
        if (distance <= radius) {
          mask.put(y, x, 255);
        }
      }
    }
    return mask;
  }

  /** Creates a test LUT for the specified number of channels. */
  private byte[][] createTestLUT(int channels) {
    byte[][] lut = new byte[channels][256];
    for (int ch = 0; ch < channels; ch++) {
      for (int i = 0; i < 256; i++) {
        // Create an inverted LUT for testing
        lut[ch][i] = (byte) (255 - i);
      }
    }
    return lut;
  }
}
