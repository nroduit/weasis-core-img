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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opencv.core.Core;
import org.opencv.core.CvException;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.osgi.OpenCVNativeLoader;
import org.weasis.core.util.FileUtil;
import org.weasis.opencv.data.ImageCV;
import org.weasis.opencv.data.LookupTableCV;
import org.weasis.opencv.data.PlanarImage;

class ImageProcessorTest {

  @BeforeAll
  public static void loadNativeLib() {
    // Load the native OpenCV library
    OpenCVNativeLoader loader = new OpenCVNativeLoader();
    loader.init();
  }

  /**
   * Method under test:
   *
   * <ul>
   *   <li>{@link ImageProcessor#findRawMinMaxValues(PlanarImage, boolean)}
   *   <li>{@link ImageProcessor#findMinMaxValues(Mat, Integer, Integer)}
   *   <li>{@link ImageProcessor#findMinMaxValues(Mat)}
   *   <li>{@link ImageProcessor#minMaxLoc(RenderedImage, Rectangle)}
   * </ul>
   */
  @Test
  void testFindRawMinMaxValues() throws OutOfMemoryError {
    try (ImageCV img = new ImageCV(new Size(7, 7), CvType.CV_16UC1, new Scalar(1024))) {
      img.put(3, 3, new short[] {(short) 32000});
      img.put(4, 4, new short[] {(short) 65535});
      Core.MinMaxLocResult result = ImageProcessor.findRawMinMaxValues(img, false);
      assertNotNull(result);
      assertEquals(1024, result.minVal);
      assertEquals(65535, result.maxVal);
      assertEquals(new Point(0, 0), result.minLoc);
      assertEquals(new Point(4, 4), result.maxLoc);

      // findMinMaxValues()
      result = ImageProcessor.findMinMaxValues(img, 1024, null);
      assertNotNull(result);
      assertEquals(32000, result.minVal);
      assertEquals(65535, result.maxVal);
      assertEquals(new Point(3, 3), result.minLoc);
      assertEquals(new Point(4, 4), result.maxLoc);

      result = ImageProcessor.findMinMaxValues(img, 1024, 32000);
      assertNotNull(result);
      assertEquals(65535, result.minVal);
      assertEquals(65535, result.maxVal);

      result = ImageProcessor.findMinMaxValues(img, 32000, 1024);
      assertNotNull(result);
      assertEquals(65535, result.minVal);
      assertEquals(65535, result.maxVal);

      result =
          ImageProcessor.minMaxLoc(
              ImageConversion.toBufferedImage((PlanarImage) img), new Rectangle(1, 1, 3, 3));
      assertNotNull(result);
      assertEquals(1024, result.minVal);
      assertEquals(32000, result.maxVal);

      // meanStdDev()
      double[][] resStd = ImageProcessor.meanStdDev(img);
      assertNotNull(resStd);
      assertEquals(1024, resStd[0][0]);
      assertEquals(65535, resStd[1][0]);
      assertEquals(2972.71, resStd[2][0], 0.01);
      assertEquals(10035.75, resStd[3][0], 0.01);
      assertEquals(49, resStd[4][0]);
      resStd = ImageProcessor.meanStdDev(img, new Rectangle(1, 1, 3, 3));
      assertEquals(1024, resStd[0][0]);
      assertEquals(32000, resStd[1][0]);
      assertEquals(4465.77, resStd[2][0], 0.01);
      assertEquals(9734.81, resStd[3][0], 0.01);
      assertEquals(9, resStd[4][0]);
      resStd = ImageProcessor.meanStdDev(img, null, 1024, 32000);
      assertEquals(65535, resStd[0][0]);
      assertEquals(65535, resStd[1][0]);
      assertEquals(65535, resStd[2][0], 0.01);
      assertEquals(0.0, resStd[3][0], 0.01);
      assertEquals(1, resStd[4][0]);
    }

    // Special case: black image
    try (ImageCV img = new ImageCV(new Size(3, 3), CvType.CV_16UC1, new Scalar(0))) {
      Core.MinMaxLocResult result = ImageProcessor.findRawMinMaxValues(img, false);
      assertNotNull(result);
      assertEquals(0, result.minVal);
      assertEquals(1, result.maxVal); // 1 instead of 0 because to avoid division by 0
      assertEquals(new Point(0, 0), result.minLoc);
      assertEquals(new Point(0, 0), result.maxLoc);
    }

    try (ImageCV img = new ImageCV(new Size(3, 3), CvType.CV_8UC1, new Scalar(5))) {
      Core.MinMaxLocResult result = ImageProcessor.findRawMinMaxValues(img, false);
      assertNotNull(result);
      assertEquals(5, result.minVal);
      assertEquals(6, result.maxVal); // 6 instead of 5 because to avoid division by 0

      result = ImageProcessor.findRawMinMaxValues(img, true);
      assertNotNull(result);
      assertEquals(0, result.minVal);
      assertEquals(255, result.maxVal);
    }

    try (ImageCV img = new ImageCV(new Size(3, 3), CvType.CV_8UC3, new Scalar(1, 2, 3))) {
      Core.MinMaxLocResult result = ImageProcessor.findRawMinMaxValues(img, false);
      assertNotNull(result);
      assertEquals(1, result.minVal);
      assertEquals(3, result.maxVal);

      result = ImageProcessor.findRawMinMaxValues(img, true);
      assertNotNull(result);
      assertEquals(0, result.minVal);
      assertEquals(255, result.maxVal);

      // findMinMaxValues()
      result = ImageProcessor.findMinMaxValues(img);
      assertNotNull(result);
      assertEquals(1, result.minVal);
      assertEquals(3, result.maxVal);

      result = ImageProcessor.findMinMaxValues(img, 3, null);
      assertNotNull(result);
      // Do not support multi-band image
      assertEquals(1, result.minVal);
      assertEquals(3, result.maxVal);
    }
  }

  /** Method under test: {@link ImageProcessor#applyLUT(Mat, byte[][])} */
  @Test
  void testApplyLUT() {
    byte[][] multiColor = new byte[3][256];
    int[] r = {
      255, 0, 255, 0, 255, 128, 64, 255, 0, 128, 236, 189, 250, 154, 221, 255, 128, 255, 0, 128,
      228, 131, 189, 0, 36, 66, 40, 132, 156, 135, 98, 194, 217, 251, 255, 0
    };
    int[] g = {
      3, 255, 245, 0, 0, 0, 128, 128, 0, 0, 83, 228, 202, 172, 160, 128, 128, 200, 187, 88, 93, 209,
      89, 255, 137, 114, 202, 106, 235, 85, 216, 226, 182, 247, 195, 173
    };
    int[] b = {
      0, 0, 55, 255, 255, 0, 64, 0, 128, 128, 153, 170, 87, 216, 246, 128, 64, 188, 236, 189, 39,
      96, 212, 255, 176, 215, 204, 221, 255, 70, 182, 84, 172, 176, 142, 95
    };
    for (int i = 0; i < 256; i++) {
      int p = i % 36;
      multiColor[0][i] = (byte) b[p];
      multiColor[1][i] = (byte) g[p];
      multiColor[2][i] = (byte) r[p];
    }

    try (ImageCV img = new ImageCV(new Size(16, 16), CvType.CV_8UC1, new Scalar(0))) {
      byte[] values = new byte[256];
      for (int i = 0; i < values.length; i++) {
        values[i] = (byte) i;
      }
      img.put(0, 0, values);
      try (ImageCV result = ImageProcessor.applyLUT(img, multiColor)) {
        assertNotNull(result);
        byte[] data = new byte[3 * r.length];
        result.get(0, 0, data);

        byte[] expected = new byte[3 * r.length];
        for (int i = 0; i < r.length; i++) {
          expected[i * 3] = (byte) b[i];
          expected[i * 3 + 1] = (byte) g[i];
          expected[i * 3 + 2] = (byte) r[i];
        }
        assertArrayEquals(expected, data);

        // Compare also with LookupTableCV
        LookupTableCV lut = new LookupTableCV(multiColor);
        try (ImageCV result2 = lut.lookup(img)) {
          assertNotNull(result2);
          byte[] data2 = new byte[3 * r.length];
          result2.get(0, 0, data2);
          assertArrayEquals(expected, data2);
        }
      }

      byte[][] oneBandLut = new byte[1][256];
      oneBandLut[0] = multiColor[0];

      try (ImageCV result = ImageProcessor.applyLUT(img, oneBandLut)) {
        assertNotNull(result);
        byte[] data = new byte[3 * r.length];
        result.get(0, 0, data);

        byte[] expected = new byte[3 * b.length];
        for (int i = 0; i < r.length; i++) {
          expected[i * 3] = (byte) b[i];
          expected[i * 3 + 1] = (byte) b[i];
          expected[i * 3 + 2] = (byte) b[i];
        }
        assertArrayEquals(expected, data);
      }
    }
  }

  /**
   * Method under test:
   *
   * <ul>
   *   <li>{{@link ImageProcessor#rescaleToByte(Mat, double, double)}
   *   <li>{@link ImageProcessor#invertLUT(ImageCV)}
   *   <li>{@link ImageProcessor#bitwiseAnd(Mat, int)}
   * </ul>
   */
  @Test
  void testRescaleToByte() {
    double level = 1024.0;
    double window = 32768;
    double low = level - window / 2.0;
    double high = level + window / 2.0;
    double range = high - low;
    if (range < 1.0) {
      range = 1.0;
    }
    double slope = 255.0 / range;
    double yInt = 255.0 - slope * high;

    try (ImageCV img = new ImageCV(new Size(16, 16), CvType.CV_16UC1, new Scalar(0))) {
      short[] values = new short[256];
      for (int i = 0; i < values.length; i++) {
        values[i] = (short) (i * 256 + i);
      }
      img.put(0, 0, values);

      // rescaleToByte()
      try (ImageCV result = ImageProcessor.rescaleToByte(img.toMat(), slope, yInt)) {
        assertNotNull(result);
        byte[] data = new byte[256];
        result.get(0, 0, data);
        assertEquals(120, data[0]);
        assertEquals(-106, data[15]);
        assertEquals(-2, data[67]);
      }

      // invertLUT()
      try (ImageCV result = ImageProcessor.invertLUT(img)) {
        assertNotNull(result);
        short[] data = new short[256];
        result.get(0, 0, data);
        assertEquals(values[255], data[0]);
        assertEquals(values[128], data[127]);
        assertEquals(values[63], data[192]);
        assertEquals(values[0], data[255]);
      }

      // bitwiseAnd()
      int mask = 32768;
      try (ImageCV result = ImageProcessor.bitwiseAnd(img, mask)) {
        assertNotNull(result);
        short[] data = new short[256];
        result.get(0, 0, data);
        assertEquals((values[0] & 0xFFFF) & mask, data[0] & 0xFFFF);
        assertEquals((values[127] & 0xFFFF) & mask, data[127] & 0xFFFF);
        assertEquals((values[192] & 0xFFFF) & mask, data[192] & 0xFFFF);
        assertEquals((values[255] & 0xFFFF) & mask, data[255] & 0xFFFF);
      }
    }
  }

  /**
   * Method under test:
   *
   * <ul>
   *   <li>{@link ImageProcessor#crop(Mat, Rectangle)}
   *   <li>{{@link ImageProcessor#scale(Mat, Dimension)}
   *   <li>{@link ImageProcessor#scale(Mat, Dimension, Integer)}
   *   <li>{@link ImageProcessor#flip(Mat, int)}
   *   <li>{@link ImageProcessor#getRotatedImage(Mat, int)}
   *   <li>{@link ImageProcessor#warpAffine(Mat, Mat, Size, Integer)}
   * </ul>
   */
  @Test
  void testGeometricTransformations() {
    try (ImageCV img = new ImageCV(new Size(16, 16), CvType.CV_16UC1, new Scalar(0))) {
      short[] values = new short[256];
      for (int i = 0; i < values.length; i++) {
        values[i] = (short) (i * 256 + i);
      }
      img.put(0, 0, values);

      // crop()
      try (ImageCV result = ImageProcessor.crop(img, new Rectangle(2, 2, 12, 12))) {
        assertNotNull(result);
        assertEquals(12, result.width());
        assertEquals(12, result.height());
        assertEquals(1, result.channels());
        assertEquals(CvType.CV_16UC1, result.type());
        assertEquals(2, result.depth());
        assertEquals(288, result.physicalBytes());
      }
      // invalid area
      try (ImageCV result = ImageProcessor.crop(img, new Rectangle(2, 2, 12, 0))) {
        assertNotNull(result);
        assertEquals(16, result.width());
        assertEquals(16, result.height());
        assertEquals(512, result.physicalBytes());
      }

      // scale()
      try (ImageCV result = ImageProcessor.scale(img, new Dimension(50, 50))) {
        assertNotNull(result);
        assertEquals(50, result.width());
        assertEquals(50, result.height());
        assertEquals(5000, result.physicalBytes());
      }
      assertThrowsExactly(
          IllegalArgumentException.class,
          () -> {
            ImageProcessor.scale(img, new Dimension(50, 0));
          });
      assertThrowsExactly(
          IllegalArgumentException.class,
          () -> {
            ImageProcessor.scale(img, new Dimension(50, 0), Imgproc.INTER_LANCZOS4);
          });
      try (ImageCV result = ImageProcessor.scale(img, new Dimension(50, 50), Imgproc.INTER_CUBIC)) {
        assertNotNull(result);
        assertEquals(50, result.width());
        assertEquals(50, result.height());
        assertEquals(5000, result.physicalBytes());
      }

      // flip()
      try (ImageCV result = ImageProcessor.flip(img, 1)) {
        assertNotNull(result);
        short[] data = new short[256];
        result.get(0, 0, data);
        assertEquals(values[15], data[0]);
        assertEquals(values[255 - 15], data[255]);
      }
      try (ImageCV result = ImageProcessor.flip(img, 0)) {
        assertNotNull(result);
        short[] data = new short[256];
        result.get(0, 0, data);
        assertEquals(values[255 - 15], data[0]);
        assertEquals(values[15], data[255]);
      }
      try (ImageCV result = ImageProcessor.flip(img, -1)) {
        assertNotNull(result);
        short[] data = new short[256];
        result.get(0, 0, data);
        assertEquals(values[255], data[0]);
        assertEquals(values[0], data[255]);
      }

      // getRotatedImage()
      try (ImageCV result = ImageProcessor.getRotatedImage(img, -1)) {
        assertNotNull(result);
        assertEquals(img.width(), result.width());
        assertEquals(img.height(), result.height());
      }
      try (ImageCV result = ImageProcessor.getRotatedImage(img, Core.ROTATE_180)) {
        assertNotNull(result);
        short[] data = new short[256];
        result.get(0, 0, data);
        assertEquals(values[255], data[0]);
        assertEquals(values[0], data[255]);
      }
      try (ImageCV result = ImageProcessor.getRotatedImage(img, Core.ROTATE_90_CLOCKWISE)) {
        assertNotNull(result);
        short[] data = new short[256];
        result.get(0, 0, data);
        assertEquals(values[0], data[15]);
        assertEquals(values[255], data[255 - 15]);
      }
      try (ImageCV result = ImageProcessor.getRotatedImage(img, Core.ROTATE_90_COUNTERCLOCKWISE)) {
        assertNotNull(result);
        short[] data = new short[256];
        result.get(0, 0, data);
        assertEquals(values[255], data[15]);
        assertEquals(values[0], data[255 - 15]);
      }

      // warpAffine() with identity Matrix
      Mat mat = new Mat(2, 3, CvType.CV_64FC1);
      mat.put(0, 0, 1.0, 0.0, 0.0, 0.0, 1.0, 0.0);
      try (ImageCV result = ImageProcessor.warpAffine(img, mat, img.size(), null)) {
        assertNotNull(result);
        assertEquals(img.size(), result.size());
        assertEquals(img.physicalBytes(), result.physicalBytes());
      }
      try (ImageCV result = ImageProcessor.warpAffine(img, mat, new Size(50, 50), null)) {
        assertNotNull(result);
        assertEquals(50, result.width());
        assertEquals(50, result.height());
      }
    }
  }

  /**
   * Method under test:
   *
   * <ul>
   *   <li>{@link ImageProcessor#overlay(Mat, Mat, Color)}
   *   <li>{{@link ImageProcessor#overlay(Mat, RenderedImage, Color)}
   *   <li>{@link ImageProcessor#applyShutter(Mat, RenderedImage, Color)}
   *   <li>{@link ImageProcessor#applyCropMask(Mat, Rectangle, double)}
   *   <li>{@link ImageProcessor#applyShutter(Mat, Shape, Color)}
   *   <li>{@link ImageProcessor#drawShape(RenderedImage, Shape, Color)}
   * </ul>
   */
  @Test
  void testOverlays() {
    int w = 16;
    int h = 16;
    ImageCV overlay = new ImageCV(h, w, CvType.CV_8UC1, new Scalar(0));
    overlay.put(0, 7, new byte[] {(byte) 255});
    overlay.put(1, 0, new byte[] {(byte) 255});

    try (ImageCV img = new ImageCV(new Size(w, h), CvType.CV_8UC3, new Scalar(1, 2, 3))) {
      try (ImageCV result = ImageProcessor.overlay(img, overlay, Color.WHITE)) {
        assertNotNull(result);
        byte[] data = new byte[3 * 256];
        result.get(0, 0, data);
        assertEquals(1, data[0]);
        assertEquals(2, data[1]);
        assertEquals(3, data[2]);
        assertEquals(1, data[18]);
        assertEquals(2, data[19]);
        assertEquals(3, data[20]);
        assertEquals((byte) 255, data[21]);
        assertEquals((byte) 255, data[22]);
        assertEquals((byte) 255, data[23]);
        assertEquals((byte) 255, data[48]);
        assertEquals((byte) 255, data[49]);
        assertEquals((byte) 255, data[50]);
      }
    }

    try (ImageCV img = new ImageCV(new Size(w, h), CvType.CV_16UC1, new Scalar(1024))) {
      try (ImageCV result = ImageProcessor.overlay(img, overlay, Color.WHITE)) {
        assertNotNull(result);
        short[] data = new short[256];
        result.get(0, 0, data);
        assertEquals(1024, data[0]);
        assertEquals(1024, data[6]);
        assertEquals((short) 65535, data[7]);
        assertEquals((short) 65535, data[16]);
      }

      // applyShutter()
      try (ImageCV result =
          ImageProcessor.applyShutter(
              img, ImageConversion.toBufferedImage((PlanarImage) overlay), Color.WHITE)) {
        assertNotNull(result);
        short[] data = new short[256];
        result.get(0, 0, data);
        assertEquals(1024, data[0]);
        assertEquals(1024, data[6]);
        assertEquals((short) 65535, data[7]);
        assertEquals((short) 65535, data[16]);
      }

      // applyCropMask()
      try (ImageCV result = ImageProcessor.applyCropMask(img, new Rectangle(4, -5, 20, 20), 0.5)) {
        assertNotNull(result);
        short[] data = new short[256];
        result.get(0, 0, data);
        assertEquals(512, data[0]);
        assertEquals(512, data[1]);
        assertEquals(1024, data[4]);
        assertEquals(1024, data[5]);
      }

      // applyShutter()
      try (ImageCV result =
          ImageProcessor.applyShutter(img, new Rectangle(4, -5, 20, 20), Color.WHITE)) {
        assertNotNull(result);
        short[] data = new short[256];
        result.get(0, 0, data);
        assertEquals((short) 65535, data[0]);
        assertEquals((short) 65535, data[1]);
        assertEquals(1024, data[4]);
        assertEquals(1024, data[5]);
      }

      // drawShape()
      BufferedImage result =
          ImageProcessor.drawShape(
              ImageConversion.toBufferedImage((PlanarImage) img),
              new Rectangle(4, -5, 20, 20),
              Color.WHITE);
      assertNotNull(result);
      Raster data = result.getData();
      assertEquals(1024, data.getSample(0, 0, 0));
      assertEquals(1024, data.getSample(1, 0, 0));
      assertEquals(65535, data.getSample(4, 0, 0));
      assertEquals(65535, data.getSample(5, 0, 0));
    }
  }

  /**
   * Method under test:
   *
   * <ul>
   *   <li>{@link ImageProcessor#mergeImages(Mat, Mat, double, double)}
   *   <li>{@link ImageProcessor#buildThumbnail(PlanarImage, Dimension, boolean)}
   * </ul>
   */
  @Test
  void testMergeImages() {
    int w = 16;
    int h = 16;
    ImageCV overlay = new ImageCV(h, w, CvType.CV_8UC3, new Scalar(0));
    overlay.put(0, 7, new byte[] {(byte) 255, (byte) 255, (byte) 255});
    overlay.put(1, 0, new byte[] {(byte) 255, (byte) 255, (byte) 255});

    try (ImageCV img = new ImageCV(new Size(w, h), CvType.CV_8UC3, new Scalar(1, 2, 3))) {
      // mergeImages()
      try (ImageCV result = ImageProcessor.mergeImages(img, overlay, 1.0, 1.0)) {
        assertNotNull(result);
        byte[] data = new byte[3 * 256];
        result.get(0, 0, data);
        assertEquals(1, data[0]);
        assertEquals(2, data[1]);
        assertEquals(3, data[2]);
        assertEquals(1, data[18]);
        assertEquals(2, data[19]);
        assertEquals(3, data[20]);
        assertEquals((byte) 255, data[21]);
        assertEquals((byte) 255, data[22]);
        assertEquals((byte) 255, data[23]);
        assertEquals((byte) 255, data[48]);
        assertEquals((byte) 255, data[49]);
        assertEquals((byte) 255, data[50]);
      }
    }

    try (ImageCV img = new ImageCV(new Size(w, h), CvType.CV_16UC1, new Scalar(1024))) {
      // mergeImages()
      assertThrowsExactly(
          CvException.class, () -> ImageProcessor.mergeImages(img, overlay, 1.0, 1.0));

      // buildThumbnail()
      assertThrowsExactly(
          IllegalArgumentException.class,
          () -> ImageProcessor.buildThumbnail(img, new Dimension(w, 0), false));
      try (ImageCV result = ImageProcessor.buildThumbnail(img, new Dimension(w, 3), false)) {
        assertNotNull(result);
        assertEquals(w, result.width());
        assertEquals(3, result.height());
      }
      try (ImageCV result = ImageProcessor.buildThumbnail(img, new Dimension(w, 3), true)) {
        assertNotNull(result);
        assertEquals(3, result.width());
        assertEquals(3, result.height());
      }
    }
  }

  /**
   *
   *
   * <ul>
   *   <li>{@link ImageProcessor#readImage(File, List)}
   *   <li>{@link ImageProcessor#readImageWithCvException(File, List)}
   *   <li>{@link ImageProcessor#writeImage(Mat, File)}
   *   <li>{@link ImageProcessor#writePNG(Mat, File)}
   *   <li>{@link ImageProcessor#writeImage(Mat, File, MatOfInt)}
   *   <li>{@link ImageProcessor#writeImage(RenderedImage, File)}
   * </ul>
   */
  @Test
  void testReadImageWithCvException() throws IOException {
    Path readOnlyFile = Paths.get(System.getProperty("java.io.tmpdir"), "testReadOnlyFile.wcv");
    FileUtil.delete(readOnlyFile);
    Files.createFile(readOnlyFile);
    Set<PosixFilePermission> permissions = new HashSet<>();
    permissions.add(PosixFilePermission.OWNER_READ);
    permissions.add(PosixFilePermission.GROUP_READ);
    permissions.add(PosixFilePermission.OTHERS_READ);
    Files.setPosixFilePermissions(readOnlyFile, permissions);

    assertFalse(ImageProcessor.writeImage((Mat) null, readOnlyFile.toFile()));
    assertFalse(ImageProcessor.writeImage(null, readOnlyFile.toFile(), null));
    assertFalse(ImageProcessor.writePNG(null, readOnlyFile.toFile()));
    assertFalse(ImageProcessor.writeThumbnail(null, readOnlyFile.toFile(), 2));
    assertFalse(ImageProcessor.writeImage((RenderedImage) null, readOnlyFile.toFile()));
    FileUtil.delete(readOnlyFile);

    Path emptyFile = Paths.get(System.getProperty("java.io.tmpdir"), "testEmptyFile.wcv");
    FileUtil.delete(emptyFile);
    Files.createFile(emptyFile);
    assertNull(ImageProcessor.readImageWithCvException(new File("noFile"), null));
    assertNull(ImageProcessor.readImage(emptyFile.toFile(), null));
    assertThrowsExactly(
        CvException.class,
        () -> {
          ImageProcessor.readImageWithCvException(emptyFile.toFile(), null);
        });
    FileUtil.delete(emptyFile);

    File file = Paths.get(System.getProperty("java.io.tmpdir"), "testRawImage.wcv").toFile();
    try (ImageCV img = new ImageCV(new Size(3, 3), CvType.CV_8UC3, new Scalar(1, 2, 3))) {
      assertTrue(ImageProcessor.writeImage(img.toMat(), file));
      readImage(file);

      assertTrue(ImageProcessor.writeImage(img.toMat(), file, new MatOfInt()));
      readImage(file);

      assertTrue(ImageProcessor.writeImage(ImageConversion.toBufferedImage(img.toMat()), file));
      readImage(file);

      File pngFile = Paths.get(System.getProperty("java.io.tmpdir"), "testImage.png").toFile();
      assertTrue(ImageProcessor.writePNG(img.toMat(), pngFile));
      readImage(pngFile);
      FileUtil.delete(pngFile);

      File jpgFile = Paths.get(System.getProperty("java.io.tmpdir"), "testImage.jpg").toFile();
      assertTrue(ImageProcessor.writeThumbnail(img.toMat(), jpgFile, 2));
      try (ImageCV thumb = ImageProcessor.readImage(jpgFile, null)) {
        assertNotNull(thumb);
        assertEquals(2, thumb.width());
        assertEquals(2, thumb.height());
        assertEquals(3, thumb.channels());
        assertEquals(CvType.CV_8UC3, thumb.type());
        assertEquals(12, thumb.physicalBytes());
      }
      FileUtil.delete(jpgFile);

      Path wrongExtension = Paths.get(System.getProperty("java.io.tmpdir"), "testWrongExt.txt");
      assertFalse(ImageProcessor.writeImage(img.toMat(), wrongExtension.toFile()));
      assertFalse(Files.isReadable(wrongExtension)); // File has been deleted when exception occurs

      assertFalse(ImageProcessor.writeImage(img.toMat(), wrongExtension.toFile(), new MatOfInt()));
      assertFalse(Files.isReadable(wrongExtension)); // File has been deleted when exception occurs

      assertFalse(
          ImageProcessor.writeImage(
              ImageConversion.toBufferedImage(img.toMat()), wrongExtension.toFile()));
      assertFalse(Files.isReadable(wrongExtension)); // File has been deleted when exception occurs

      assertFalse(ImageProcessor.writePNG(img.toMat(), wrongExtension.toFile()));
      assertFalse(Files.isReadable(wrongExtension)); // File has been deleted when exception occurs

      assertFalse(ImageProcessor.writeThumbnail(img.toMat(), wrongExtension.toFile(), 2));
      assertFalse(Files.isReadable(wrongExtension)); // File has been deleted when exception occurs
    }
    FileUtil.delete(file);
  }

  private void readImage(File file) {
    try (ImageCV img = ImageProcessor.readImage(file, null)) {
      assertNotNull(img);
      assertEquals(3, img.width());
      assertEquals(3, img.height());
      assertEquals(3, img.channels());
      assertEquals(CvType.CV_8UC3, img.type());
      assertEquals(27, img.physicalBytes());
      assertEquals(27, img.total() * img.elemSize());
      byte[] data = new byte[3];
      img.get(1, 1, data);
      assertArrayEquals(new byte[] {1, 2, 3}, data);
    }
  }
}
