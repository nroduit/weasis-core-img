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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Rectangle;
import java.awt.image.BandedSampleModel;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.MultiPixelPackedSampleModel;
import java.awt.image.PixelInterleavedSampleModel;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.osgi.OpenCVNativeLoader;
import org.weasis.opencv.data.ImageCV;
import org.weasis.opencv.data.PlanarImage;

class ImageConversionTest {

  @BeforeAll
  public static void loadNativeLib() {
    // Load the native OpenCV library
    OpenCVNativeLoader loader = new OpenCVNativeLoader();
    loader.init();
  }

  /**
   *
   *
   * <ul>
   *   <li>{@link ImageConversion#convertRenderedImage(RenderedImage)}
   *   <li>{@link ImageConversion#toBufferedImage(Mat)}
   * </ul>
   */
  @Test
  void testToBufferedImage() {
    assertNull(ImageConversion.toBufferedImage((Mat) null));
    assertNull(ImageConversion.toBufferedImage((PlanarImage) null));

    try (ImageCV img = new ImageCV(3, 3, CvType.CV_8UC4, new Scalar(7))) {
      assertThrowsExactly(
          UnsupportedOperationException.class,
          () -> {
            ImageConversion.toBufferedImage((PlanarImage) img);
            ;
          });
    }

    try (ImageCV img = new ImageCV(new Size(4, 4), CvType.CV_8UC1, new Scalar(255))) {
      testToBufferedImage(img);
    }

    try (ImageCV img = new ImageCV(3, 3, CvType.CV_8UC3, new Scalar(7))) {
      testToBufferedImage(img);
    }

    try (ImageCV img = new ImageCV(new Size(7, 7), CvType.CV_16SC1, new Scalar(-1024))) {
      testToBufferedImage(img);
      assertEquals(98, img.physicalBytes());
    }

    try (ImageCV img = new ImageCV(new Size(7, 7), CvType.CV_16UC2, new Scalar(4096))) {
      assertThrowsExactly(
          UnsupportedOperationException.class,
          () -> {
            ImageConversion.toBufferedImage((PlanarImage) img);
            ;
          });
    }

    try (ImageCV img = new ImageCV(new Size(7, 7), CvType.CV_16UC3, new Scalar(4096))) {
      testToBufferedImage(img);
      assertEquals(294, img.physicalBytes());
    }

    try (ImageCV img = new ImageCV(new Size(7, 7), CvType.CV_32S, new Scalar(-409600))) {
      testToBufferedImage(img);
      assertEquals(196, img.physicalBytes());
    }

    try (ImageCV img = new ImageCV(new Size(7, 7), CvType.CV_16F, new Scalar(-1.56287f))) {
      assertThrowsExactly(
          UnsupportedOperationException.class,
          () -> {
            ImageConversion.toBufferedImage((PlanarImage) img);
          });
    }

    try (ImageCV img = new ImageCV(new Size(7, 7), CvType.CV_32F, new Scalar(-1.56287f))) {
      testToBufferedImage(img);
      assertEquals(196, img.physicalBytes());
    }

    try (ImageCV img =
        new ImageCV(new Size(7, 7), CvType.CV_64F, new Scalar(-23566.221548796545))) {
      testToBufferedImage(img);
      assertEquals(392, img.physicalBytes());
    }
  }

  private void testToBufferedImage(ImageCV img) {
    int width = img.width();
    int height = img.height();
    int cvType = img.type();
    int channels = CvType.channels(cvType);
    int depth = CvType.depth(cvType);

    BufferedImage bufferedImage = ImageConversion.toBufferedImage((PlanarImage) img);
    try (ImageCV imageCV = ImageConversion.toMat(bufferedImage)) {
      assertEquals(img.physicalBytes(), imageCV.physicalBytes());
      assertEquals(img.rows(), imageCV.rows());
      assertEquals(img.cols(), imageCV.rows());
      assertEquals(img.type(), imageCV.type());
      assertEquals(img.channels(), imageCV.channels());
      assertEquals(img.isContinuous(), imageCV.isContinuous());

      if (depth <= CvType.CV_8S) {
        byte[] data1 = new byte[width * height * channels];
        img.get(0, 0, data1);
        byte[] data2 = new byte[width * height * channels];
        imageCV.get(0, 0, data2);
        assertArrayEquals(data1, data2);
      } else if (depth <= CvType.CV_16S) {
        short[] data1 = new short[width * height * channels];
        img.get(0, 0, data1);
        short[] data2 = new short[width * height * channels];
        imageCV.get(0, 0, data2);
        assertArrayEquals(data1, data2);
      } else if (depth == CvType.CV_32S) {
        int[] data1 = new int[width * height * channels];
        img.get(0, 0, data1);
        int[] data2 = new int[width * height * channels];
        imageCV.get(0, 0, data2);
        assertArrayEquals(data1, data2);
      } else if (depth == CvType.CV_32F) {
        float[] data1 = new float[width * height * channels];
        img.get(0, 0, data1);
        float[] data2 = new float[width * height * channels];
        imageCV.get(0, 0, data2);
        assertArrayEquals(data1, data2);
      } else if (depth == CvType.CV_64F) {
        double[] data1 = new double[width * height * channels];
        img.get(0, 0, data1);
        double[] data2 = new double[width * height * channels];
        imageCV.get(0, 0, data2);
        assertArrayEquals(data1, data2);
      }
    }
  }

  /** Method under test: {@link ImageConversion#releaseMat(Mat)} */
  /**
   * Method under test:
   *
   * <ul>
   *   <li>{@link ImageConversion#convertTo(RenderedImage, int)}
   *   <li>{@link ImageConversion#releaseMat(Mat)}
   *   <li>{@link ImageConversion#releasePlanarImage(PlanarImage)}
   *   <li>{@link ImageConversion#getBounds(PlanarImage)}
   * </ul>
   */
  @Test
  void testReleaseMat() {
    ImageConversion.releaseMat(null);
    ImageConversion.releasePlanarImage(null);

    try (ImageCV img = new ImageCV(new Size(7, 7), CvType.CV_16UC1, new Scalar(1024))) {
      assertEquals(98, img.physicalBytes());
      BufferedImage bufferedImage = ImageConversion.toBufferedImage((PlanarImage) img);
      BufferedImage byteBufferedImage =
          ImageConversion.convertTo(bufferedImage, BufferedImage.TYPE_BYTE_GRAY);
      try (ImageCV imageCV = ImageConversion.toMat(byteBufferedImage)) {
        assertEquals(49, imageCV.physicalBytes());
        assertEquals(4, imageCV.get(0, 0)[0]);
        assertEquals(7, ImageConversion.getBounds(img).getWidth());
      }

      assertFalse(img.isHasBeenReleased());
      assertFalse(img.isReleasedAfterProcessing());
      img.setReleasedAfterProcessing(true);
      assertTrue(img.isReleasedAfterProcessing());
      ImageConversion.releaseMat(img);
      assertTrue(img.isHasBeenReleased());
    }
  }

  /**
   * Method under test:
   *
   * <ul>
   *   <li>{@link ImageConversion#convertRenderedImage(RenderedImage)}
   *   <li>{@link ImageConversion#toBufferedImage(Mat)}
   *   <li>{@link ImageConversion#getUnpackedBinaryData(Raster, Rectangle)}
   * </ul>
   */
  @Test
  void testToBinaryBufferedImage() {
    assertNull(ImageConversion.convertRenderedImage(null));
    int width = 3;
    int height = 3;
    BufferedImage binaryImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_BINARY);
    testToBinaryBufferedImage(ImageConversion.convertRenderedImage(binaryImage));

    WritableRaster raster =
        Raster.createPackedRaster(DataBuffer.TYPE_USHORT, width, height, 1, 1, null);
    binaryImage = new BufferedImage(binaryImage.getColorModel(), raster, false, null);
    testToBinaryBufferedImage(binaryImage);

    raster = Raster.createPackedRaster(DataBuffer.TYPE_INT, width, height, 1, 1, null);
    binaryImage = new BufferedImage(binaryImage.getColorModel(), raster, false, null);
    testToBinaryBufferedImage(binaryImage);

    // Create a RenderedImage using the SingleTileRenderedImage
    SingleTileRenderedImage singleTileRenderedImage =
        new SingleTileRenderedImage(raster, binaryImage.getColorModel());
    testToBinaryBufferedImage(ImageConversion.convertRenderedImage(singleTileRenderedImage));
  }

  private void testToBinaryBufferedImage(BufferedImage binaryImage) {
    WritableRaster raster = binaryImage.getRaster();
    raster.setSample(1, 1, 0, 1); // Set pixel at (1, 1) to 1 (white)
    raster.setSample(2, 2, 0, 1);
    try (ImageCV imageCV = ImageConversion.toMat(binaryImage)) {
      assertEquals(9, imageCV.physicalBytes());
      assertEquals(3, imageCV.rows());
      assertEquals(3, imageCV.rows());
      assertEquals(CvType.CV_8UC1, imageCV.type());
      assertEquals(1, imageCV.channels());
      assertTrue(imageCV.isContinuous());
      assertEquals(0, imageCV.get(0, 0)[0]);
      assertEquals(1, imageCV.get(1, 1)[0]);
      assertEquals(0, imageCV.get(1, 2)[0]);
      assertEquals(1, imageCV.get(2, 2)[0]);
    }
  }

  /** Method under test: {@link ImageConversion#isBinary(SampleModel)} */
  @Test
  void testIsBinary() {
    assertFalse(ImageConversion.isBinary(new BandedSampleModel(1, 1, 1, 1)));
    assertFalse(
        ImageConversion.isBinary(new PixelInterleavedSampleModel(1, 1, 1, 1, 1, new int[] {0})));
    assertFalse(ImageConversion.isBinary(new MultiPixelPackedSampleModel(1, 1, 1, 2)));
    assertTrue(ImageConversion.isBinary(new MultiPixelPackedSampleModel(1, 1, 1, 1)));
  }

  /** Method under test: {@link ImageConversion#convertToDataType(int)} */
  @Test
  void testConvertToDataType() {
    assertEquals(0, ImageConversion.convertToDataType(1));
    assertEquals(1, ImageConversion.convertToDataType(2));
    assertEquals(2, ImageConversion.convertToDataType(3));
    assertEquals(3, ImageConversion.convertToDataType(4));
    assertEquals(4, ImageConversion.convertToDataType(5));
    assertEquals(5, ImageConversion.convertToDataType(6));
    assertThrows(UnsupportedOperationException.class, () -> ImageConversion.convertToDataType(-1));
  }

  /**
   * Method under test: {@link ImageConversion#toMat(RenderedImage, Rectangle, boolean, boolean)}
   */
  @Test
  void testToMat() {
    // Values: Blue=1, Green=2, Red=3
    try (ImageCV img = new ImageCV(new Size(7, 7), CvType.CV_8UC3, new Scalar(1, 2, 3))) {
      assertEquals(147, img.physicalBytes());
      BufferedImage bufferedImage = ImageConversion.toBufferedImage((PlanarImage) img);
      // Reverse the order of the bands (Mat is BGR) and BufferedImage is RGB by default
      assertArrayEquals(
          new double[] {3, 2, 1}, bufferedImage.getRaster().getPixel(0, 0, (double[]) null));

      try (ImageCV imageCV =
          ImageConversion.toMat(bufferedImage, new Rectangle(0, 0, 3, 3), true, false)) {
        assertEquals(27, imageCV.physicalBytes());
        assertEquals(3, imageCV.rows());
        assertEquals(3, imageCV.rows());
        assertArrayEquals(new double[] {1, 2, 3}, imageCV.get(0, 0));
      }

      // Create a BufferedImage with BGR color model
      WritableRaster rgbRaster =
          Raster.createInterleavedRaster(
              DataBuffer.TYPE_BYTE, 1, 1, 3, 3, new int[] {0, 1, 2}, null);
      rgbRaster.setSample(0, 0, 0, 3);
      rgbRaster.setSample(0, 0, 1, 2);
      rgbRaster.setSample(0, 0, 2, 1);
      BufferedImage brgImage =
          new BufferedImage(bufferedImage.getColorModel(), rgbRaster, false, null);
      try (ImageCV imageCV = ImageConversion.toMat(brgImage, null, true, false)) {
        assertEquals(3, imageCV.physicalBytes());
        assertArrayEquals(new double[] {1, 2, 3}, imageCV.get(0, 0));
      }

      // Create a band BufferedImage with BGR color model
      WritableRaster rgbBandRaster = Raster.createBandedRaster(DataBuffer.TYPE_BYTE, 1, 1, 3, null);
      rgbBandRaster.setSample(0, 0, 0, 3);
      rgbBandRaster.setSample(0, 0, 1, 2);
      rgbBandRaster.setSample(0, 0, 2, 1);
      brgImage = new BufferedImage(bufferedImage.getColorModel(), rgbBandRaster, false, null);
      try (ImageCV imageCV = ImageConversion.toMat(brgImage, null, true, false)) {
        assertEquals(3, imageCV.physicalBytes());
        assertArrayEquals(new double[] {1, 2, 3}, imageCV.get(0, 0));
      }
    }
  }
}
