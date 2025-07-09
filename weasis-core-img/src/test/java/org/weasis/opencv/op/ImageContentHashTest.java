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

import java.awt.Dimension;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;
import org.opencv.osgi.OpenCVNativeLoader;
import org.weasis.opencv.data.ImageCV;
import org.weasis.opencv.op.lut.ColorLut;

public class ImageContentHashTest {

  @BeforeAll
  @DisplayName("Load OpenCV native library")
  static void loadNativeLib() {
    OpenCVNativeLoader loader = new OpenCVNativeLoader();
    loader.init();
  }

  @Test
  public void testCompare_AverageHash_SimilarImages() {
    // Arrange
    Mat source = ImageTransformerTest.createTestImage(100, 100, CvType.CV_8UC3);

    ImageCV result = ImageTransformer.scale(source, new Dimension(300, 300));
    assertEquals(300, result.width());
    assertEquals(300, result.height());

    // Act
    double diff = ImageContentHash.AVERAGE.compare(result, source);

    // Assert
    assertEquals(0.0, diff, 0.0001, "Average hash should be identical for the same image");

    result.release();
    source.release();
  }

  @Test
  public void testCompare_Phash_SameImages() {
    // Arrange
    Mat source = ImageTransformerTest.createTestImage(100, 100, CvType.CV_8UC3);

    ImageCV result = ImageTransformer.scale(source, new Dimension(300, 300));
    assertEquals(300, result.width());
    assertEquals(300, result.height());

    // Act
    double diff = ImageContentHash.PHASH.compare(result, source);

    // Assert
    assertEquals(0.0, diff, 0.0001, "PHash should be identical for the same image");

    result.release();
    source.release();
  }

  @Test
  public void testCompare_MarrHildrethHash_SimilarImages() {
    // Arrange
    Mat source = ImageTransformerTest.createTestImage(100, 100, CvType.CV_8UC3);
    Mat source2 = ImageTransformerTest.createTestImage(100, 100, CvType.CV_8UC3);
    Imgproc.GaussianBlur(source2, source2, new org.opencv.core.Size(3, 3), 0);
    Mat differentImage = ImageTransformer.applyLUT(source, ColorLut.HUE.getByteLut().lutTable());

    // Act
    double diff = ImageContentHash.MARR_HILDRETH.compare(source, source2);
    double diff2 = ImageContentHash.MARR_HILDRETH.compare(source, differentImage);

    // Assert
    assertEquals(0.0, diff, 0.0001, "Marr-Hildreth hash should be identical for similar images");
    assertEquals(
        400.0, diff2, 50, "Marr-Hildreth hash should be different for different color images");

    source.release();
    source2.release();
  }

  @Test
  public void testCompare_ColorMomentHash_DifferentImages() {
    // Arrange
    Mat source = ImageTransformerTest.createTestImage(100, 100, CvType.CV_8UC3);
    Mat source2 = ImageTransformerTest.createTestImage(100, 100, CvType.CV_8UC3);
    Mat differentImage = ImageTransformer.applyLUT(source, ColorLut.HUE.getByteLut().lutTable());

    // Act
    double diff = ImageContentHash.COLOR_MOMENT.compare(source, source2);
    double diff2 = ImageContentHash.COLOR_MOMENT.compare(source, differentImage);

    // Assert
    assertEquals(0.0, diff, 0.0001, "Color Moment hash should be identical for the same image");
    assertEquals(
        20.0, diff2, 10, "Color Moment hash should be different for different color images");

    source.release();
    differentImage.release();
  }

  @Test
  public void testCompare_BlockMean_Zero_DifferentImages() {
    // Arrange
    Mat source = ImageTransformerTest.createTestImage(100, 100, CvType.CV_8UC3);
    Mat source2 = ImageTransformerTest.createTestImage(100, 100, CvType.CV_8UC3);
    Mat differentImage = ImageTransformer.applyLUT(source, ColorLut.HUE.getByteLut().lutTable());

    // Act
    double diff = ImageContentHash.BLOCK_MEAN_ZERO.compare(source, source2);
    double diff2 = ImageContentHash.BLOCK_MEAN_ZERO.compare(source, differentImage);

    // Assert
    assertEquals(0.0, diff, 0.0001, "Block Mean Zero hash should be identical for the same image");
    assertEquals(
        180, diff2, 30, "Block Mean Zero hash should be different for different color images");

    source.release();
    differentImage.release();
  }

  @Test
  public void testCompare_BlockMean_One_DifferentImages() {
    // Arrange
    Mat source = ImageTransformerTest.createTestImage(100, 100, CvType.CV_8UC3);
    Mat source2 = ImageTransformerTest.createTestImage(100, 100, CvType.CV_8UC3);
    Mat differentImage = ImageTransformer.applyLUT(source, ColorLut.HUE.getByteLut().lutTable());

    // Act
    double diff = ImageContentHash.BLOCK_MEAN_ONE.compare(source, source2);
    double diff2 = ImageContentHash.BLOCK_MEAN_ONE.compare(source, differentImage);

    // Assert
    assertEquals(0.0, diff, 0.0001, "Block Mean One hash should be identical for the same image");
    assertEquals(
        670, diff2, 100, "Block Mean One hash should be different for different color images");

    source.release();
    differentImage.release();
  }

  @Test
  public void testCompare_RadialVariance_SimilarImages() {
    // Arrange
    Mat source = ImageTransformerTest.createTestImage(100, 100, CvType.CV_8UC3);
    ImageCV result = ImageTransformer.getRotatedImage(source, Core.ROTATE_90_CLOCKWISE);

    // Ac
    double diff = ImageContentHash.RADIAL_VARIANCE.compare(result, source);

    // Assert
    assertEquals(0.3, diff, 0.1, "Radial Variance hash should be different for rotated images");

    result.release();
    source.release();
  }
}
