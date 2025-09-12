/*
 * Copyright (c) 2023 Weasis Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.weasis.opencv.data;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

import java.awt.image.DataBuffer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.osgi.OpenCVNativeLoader;

class LookupTableCVTest {
  static final byte[] BYTE_C1 = new byte[] {1, -1, 1, -1, 0, 127, -128, -9, 9};
  static final byte[][] BYTE_C3 =
      new byte[][] {BYTE_C1, {1, -7, 7, -1, 0, -111, 111, -11, 11}, BYTE_C1};
  static final short[] SHORT_C1 = new short[] {-8126, -4096, -1024, -512, 0, 10, 512, 1024, 4096};

  @BeforeAll
  static void loadNativeLib() {
    // Load the native OpenCV library
    OpenCVNativeLoader loader = new OpenCVNativeLoader();
    loader.init();
  }

  /**
   * Test byte lookup table with 1 band with byte image with 1 band and 3 bands Methods under test:
   *
   * <ul>
   *   <li>{@link LookupTableCV#LookupTableCV(byte[])}
   *   <li>{@link LookupTableCV#getNumBands()}
   *   <li>{@link LookupTableCV#getDataType()}
   *   <li>{@link LookupTableCV#getOffsets()}
   *   <li>{@link LookupTableCV#getOffset(int)}
   *   <li>{@link LookupTableCV#getNumEntries()}
   *   <li>{@link LookupTableCV#getByteData()}
   *   <li>{@link LookupTableCV#getByteData(int)}
   *   <li>{@link LookupTableCV#getShortData()}
   *   <li>{@link LookupTableCV#lookup(int, int)}
   *   <li>{@link LookupTableCV#lookup(Mat)}
   * </ul>
   */
  @Test
  void testConstructor() {
    LookupTableCV lut = new LookupTableCV(BYTE_C1);
    assertEquals(1, lut.getNumBands());
    assertEquals(DataBuffer.TYPE_BYTE, lut.getDataType());
    assertEquals(0, lut.getOffset());
    assertEquals(0, lut.getOffset(0));
    assertEquals(9, lut.getNumEntries());
    assertArrayEquals(BYTE_C1, lut.getByteData()[0]);
    assertArrayEquals(BYTE_C1, lut.getByteData(0));
    assertArrayEquals(new int[1], lut.getOffsets());
    assertThrowsExactly(
        ArrayIndexOutOfBoundsException.class,
        () -> {
          lut.getByteData(1);
        },
        "Index 1 out of bounds for length 1");
    assertNull(lut.getShortData());

    assertEquals(BYTE_C1[3] & 0xFF, lut.lookup(0, 3));
    assertEquals(BYTE_C1[5] & 0xFF, lut.lookup(0, 5));

    try (ImageCV img = new ImageCV(new Size(3, 3), CvType.CV_8UC1, new Scalar(5))) {
      ImageCV outImg = lut.lookup(img.toMat());
      assertNotNull(outImg);
      assertEquals(CvType.CV_8UC1, outImg.type());
      assertEquals(1, outImg.channels());
      byte[] data = new byte[1];
      outImg.get(1, 1, data);
      assertEquals(BYTE_C1[5] & 0xFF, data[0]);
    }

    try (ImageCV img = new ImageCV(new Size(3, 3), CvType.CV_8UC3, new Scalar(4, 5, 6))) {
      ImageCV outImg = lut.lookup(img.toMat());
      assertNotNull(outImg);
      assertEquals(CvType.CV_8UC3, outImg.type());
      assertEquals(3, outImg.channels());
      byte[] data = new byte[3];
      outImg.get(1, 1, data);
      assertArrayEquals(new byte[] {BYTE_C1[4], BYTE_C1[5], BYTE_C1[6]}, data);
    }

    try (ImageCV img = new ImageCV(new Size(3, 3), CvType.CV_16UC1, new Scalar(5))) {
      ImageCV outImg = lut.lookup(img.toMat());
      assertNotNull(outImg);
      assertEquals(CvType.CV_8UC1, outImg.type());
      assertEquals(1, outImg.channels());
      byte[] data = new byte[1];
      outImg.get(1, 1, data);
      assertEquals(BYTE_C1[5] & 0xFF, data[0]);
    }

    try (ImageCV img = new ImageCV(new Size(3, 3), CvType.CV_16SC1, new Scalar(5))) {
      ImageCV outImg = lut.lookup(img.toMat());
      assertNotNull(outImg);
      assertEquals(CvType.CV_8UC1, outImg.type());
      assertEquals(1, outImg.channels());
      byte[] data = new byte[1];
      outImg.get(1, 1, data);
      assertEquals(BYTE_C1[5], data[0]);
    }

    try (ImageCV img = new ImageCV(new Size(3, 3), CvType.CV_16UC3, new Scalar(3, 4, 5))) {
      ImageCV outImg = lut.lookup(img.toMat());
      assertNotNull(outImg);
      assertEquals(CvType.CV_8UC3, outImg.type());
      assertEquals(3, outImg.channels());
      byte[] data = new byte[3];
      outImg.get(1, 1, data);
      assertArrayEquals(new byte[] {BYTE_C1[3], BYTE_C1[4], BYTE_C1[5]}, data);
    }
  }

  /**
   * Test byte lookup table with 3 bands and offset with byte image with 1 band and 3 bands Methods
   * under test:
   *
   * <ul>
   *   <li>{@link LookupTableCV#LookupTableCV(byte[][], int)}
   *   <li>{@link LookupTableCV#getNumBands()}
   *   <li>{@link LookupTableCV#getDataType()}
   *   <li>{@link LookupTableCV#getOffsets()}
   *   <li>{@link LookupTableCV#getOffset(int)}
   *   <li>{@link LookupTableCV#getNumEntries()}
   *   <li>{@link LookupTableCV#getByteData()}
   *   <li>{@link LookupTableCV#getByteData(int)}
   *   <li>{@link LookupTableCV#getShortData()}
   *   <li>{@link LookupTableCV#lookup(int, int)}
   *   <li>{@link LookupTableCV#lookup(Mat)}
   * </ul>
   */
  @Test
  void testConstructor2() {
    LookupTableCV lut = new LookupTableCV(BYTE_C3, 2);
    assertEquals(3, lut.getNumBands());
    assertEquals(DataBuffer.TYPE_BYTE, lut.getDataType());
    assertEquals(2, lut.getOffset());
    assertEquals(2, lut.getOffset(1));
    assertEquals(9, lut.getNumEntries());
    assertArrayEquals(BYTE_C3, lut.getByteData());
    assertArrayEquals(BYTE_C1, lut.getByteData(0));

    assertEquals(3, lut.getData().getNumBanks());
    assertNull(lut.getShortData());
    assertEquals(BYTE_C3[1][3] & 0xFF, lut.lookup(1, lut.getOffset() + 3));
    assertEquals(BYTE_C3[1][5] & 0xFF, lut.lookup(1, lut.getOffset() + 5));

    try (ImageCV img =
        new ImageCV(new Size(3, 3), CvType.CV_8UC1, new Scalar(lut.getOffset() + 5))) {
      ImageCV outImg = lut.lookup(img.toMat());
      assertNotNull(outImg);
      // When LUT has 3 channels, the output image has 3 channels even if the input image has 1
      assertEquals(CvType.CV_8UC3, outImg.type());
      assertEquals(3, outImg.channels());
      byte[] data = new byte[3];
      outImg.get(1, 1, data);
      assertArrayEquals(new byte[] {BYTE_C3[0][5], BYTE_C3[1][5], BYTE_C3[2][5]}, data);
    }

    try (ImageCV img =
        new ImageCV(
            new Size(3, 3),
            CvType.CV_8UC3,
            new Scalar(lut.getOffset() + 4, lut.getOffset() + 5, lut.getOffset() + 6))) {
      ImageCV outImg = lut.lookup(img.toMat());
      assertNotNull(outImg);
      assertEquals(CvType.CV_8UC3, outImg.type());
      assertEquals(3, outImg.channels());
      byte[] data = new byte[3];
      outImg.get(1, 1, data);
      assertArrayEquals(new byte[] {BYTE_C3[0][4], BYTE_C3[1][5], BYTE_C3[2][6]}, data);
    }

    try (ImageCV img = new ImageCV(new Size(3, 3), CvType.CV_64FC1, new Scalar(3.5f))) {

      assertThrowsExactly(
          IllegalArgumentException.class,
          () -> {
            lut.lookup(img.toMat());
          });
    }

    try (ImageCV img =
        new ImageCV(new Size(3, 3), CvType.CV_16UC1, new Scalar(lut.getOffset() + 5))) {
      ImageCV outImg = lut.lookup(img.toMat());
      assertNotNull(outImg);
      assertEquals(CvType.CV_8UC3, outImg.type());
      assertEquals(3, outImg.channels());
      byte[] data = new byte[3];
      outImg.get(1, 1, data);
      assertArrayEquals(new byte[] {BYTE_C3[0][5], BYTE_C3[1][5], BYTE_C3[2][5]}, data);
    }

    try (ImageCV img =
        new ImageCV(new Size(3, 3), CvType.CV_16SC1, new Scalar(lut.getOffset() + 5))) {
      ImageCV outImg = lut.lookup(img.toMat());
      assertNotNull(outImg);
      assertEquals(CvType.CV_8UC3, outImg.type());
      assertEquals(3, outImg.channels());
      byte[] data = new byte[3];
      outImg.get(1, 1, data);
      assertArrayEquals(new byte[] {BYTE_C3[0][5], BYTE_C3[1][5], BYTE_C3[2][5]}, data);
    }

    try (ImageCV img =
        new ImageCV(
            new Size(3, 3),
            CvType.CV_16UC3,
            new Scalar(lut.getOffset() + 3, lut.getOffset() + 4, lut.getOffset() + 5))) {
      ImageCV outImg = lut.lookup(img.toMat());
      assertNotNull(outImg);
      assertEquals(CvType.CV_8UC3, outImg.type());
      assertEquals(3, outImg.channels());
      byte[] data = new byte[3];
      outImg.get(1, 1, data);
      assertArrayEquals(new byte[] {BYTE_C3[0][3], BYTE_C3[1][4], BYTE_C3[2][5]}, data);
    }
  }

  /**
   * Test unsigned short lookup table with 1 band and offset with byte image with 1 band and 3 bands
   * Methods under test:
   *
   * <ul>
   *   <li>{@link LookupTableCV#LookupTableCV(short[], int, boolean)}
   *   <li>{@link LookupTableCV#getNumBands()}
   *   <li>{@link LookupTableCV#getDataType()}
   *   <li>{@link LookupTableCV#getOffsets()}
   *   <li>{@link LookupTableCV#getOffset(int)}
   *   <li>{@link LookupTableCV#getNumEntries()}
   *   <li>{@link LookupTableCV#getShortData(int)}
   *   <li>{@link LookupTableCV#getShortData()}
   *   <li>{@link LookupTableCV#getByteData()}
   *   <li>{@link LookupTableCV#lookup(int, int)}
   *   <li>{@link LookupTableCV#lookup(Mat)}
   * </ul>
   */
  @Test
  void testConstructor4() {
    LookupTableCV lut = new LookupTableCV(SHORT_C1, 2, true);
    assertEquals(1, lut.getNumBands());
    assertEquals(DataBuffer.TYPE_USHORT, lut.getDataType());
    assertEquals(2, lut.getOffset());
    assertEquals(2, lut.getOffset(0));
    assertEquals(9, lut.getNumEntries());
    assertArrayEquals(SHORT_C1, lut.getShortData()[0]);
    assertArrayEquals(SHORT_C1, lut.getShortData(0));

    assertNull(lut.getByteData());
    assertNull(lut.getByteData(0));
    assertEquals(SHORT_C1[3] & 0xFFFF, lut.lookup(0, lut.getOffset() + 3));
    assertEquals(SHORT_C1[5] & 0xFFFF, lut.lookup(0, lut.getOffset() + 5));

    try (ImageCV img =
        new ImageCV(new Size(3, 3), CvType.CV_8UC1, new Scalar(lut.getOffset() + 5))) {
      ImageCV outImg = lut.lookup(img.toMat());
      assertNotNull(outImg);
      assertEquals(CvType.CV_16UC1, outImg.type());
      assertEquals(1, outImg.channels());
      short[] data = new short[1];
      outImg.get(1, 1, data);
      assertArrayEquals(new short[] {SHORT_C1[5]}, data);
    }

    try (ImageCV img =
        new ImageCV(
            new Size(3, 3),
            CvType.CV_8UC3,
            new Scalar(lut.getOffset() + 3, lut.getOffset() + 4, lut.getOffset() + 5))) {
      ImageCV outImg = lut.lookup(img.toMat());
      assertNotNull(outImg);
      assertEquals(CvType.CV_16UC3, outImg.type());
      assertEquals(3, outImg.channels());
      short[] data = new short[3];
      outImg.get(1, 1, data);
      assertArrayEquals(new short[] {SHORT_C1[3], SHORT_C1[4], SHORT_C1[5]}, data);
    }

    try (ImageCV img =
        new ImageCV(new Size(3, 3), CvType.CV_16UC1, new Scalar(lut.getOffset() + 5))) {
      ImageCV outImg = lut.lookup(img.toMat());
      assertNotNull(outImg);
      // When LUT has 3 channels, the output image has 3 channels even if the input image has 1
      assertEquals(CvType.CV_16UC1, outImg.type());
      assertEquals(1, outImg.channels());
      // ushort is stored in a short array
      short[] data = new short[1];
      outImg.get(1, 1, data);
      assertArrayEquals(new short[] {SHORT_C1[5]}, data);
    }

    try (ImageCV img =
        new ImageCV(
            new Size(3, 3),
            CvType.CV_16UC3,
            new Scalar(lut.getOffset() + 3, lut.getOffset() + 4, lut.getOffset() + 5))) {
      ImageCV outImg = lut.lookup(img.toMat());
      assertNotNull(outImg);
      assertEquals(CvType.CV_16UC3, outImg.type());
      assertEquals(3, outImg.channels());
      // ushort is stored in a short array
      short[] data = new short[3];
      outImg.get(1, 1, data);
      assertArrayEquals(new short[] {SHORT_C1[3], SHORT_C1[4], SHORT_C1[5]}, data);
    }
  }

  /**
   * Test signed short lookup table with 1 band and offset with byte image with 1 band and 3 bands
   * Methods under test:
   *
   * <ul>
   *   <li>{@link LookupTableCV#LookupTableCV(short[], int, boolean)}
   *   <li>{@link LookupTableCV#getNumBands()}
   *   <li>{@link LookupTableCV#getDataType()}
   *   <li>{@link LookupTableCV#getOffsets()}
   *   <li>{@link LookupTableCV#getOffset(int)}
   *   <li>{@link LookupTableCV#getNumEntries()}
   *   <li>{@link LookupTableCV#getShortData(int)}
   *   <li>{@link LookupTableCV#getShortData()}
   *   <li>{@link LookupTableCV#getByteData()}
   *   <li>{@link LookupTableCV#lookup(int, int)}
   *   <li>{@link LookupTableCV#lookup(Mat)}
   * </ul>
   */
  @Test
  void testConstructor5() {
    LookupTableCV lut = new LookupTableCV(SHORT_C1, 2, false);
    assertEquals(1, lut.getNumBands());
    assertEquals(DataBuffer.TYPE_SHORT, lut.getDataType());
    assertEquals(2, lut.getOffset());
    assertEquals(2, lut.getOffset(0));
    assertEquals(9, lut.getNumEntries());
    assertArrayEquals(SHORT_C1, lut.getShortData()[0]);
    assertArrayEquals(SHORT_C1, lut.getShortData(0));

    assertNull(lut.getByteData());
    assertEquals(SHORT_C1[3], lut.lookup(0, lut.getOffset() + 3));
    assertEquals(SHORT_C1[5], lut.lookup(0, lut.getOffset() + 5));
    assertThrowsExactly(
        ArrayIndexOutOfBoundsException.class,
        () -> {
          lut.lookup(0, lut.getOffset() + -15);
        });

    try (ImageCV img =
        new ImageCV(new Size(3, 3), CvType.CV_16SC1, new Scalar(lut.getOffset() + 5))) {
      ImageCV outImg = lut.lookup(img.toMat());
      assertNotNull(outImg);
      // When LUT has 3 channels, the output image has 3 channels even if the input image has 1
      assertEquals(CvType.CV_16SC1, outImg.type());
      assertEquals(1, outImg.channels());
      short[] data = new short[1];
      outImg.get(1, 1, data);
      assertArrayEquals(new short[] {SHORT_C1[5]}, data);
    }

    try (ImageCV img =
        new ImageCV(
            new Size(3, 3),
            CvType.CV_16SC3,
            new Scalar(lut.getOffset() + 3, lut.getOffset() + 4, lut.getOffset() + 5))) {
      ImageCV outImg = lut.lookup(img.toMat());
      assertNotNull(outImg);
      assertEquals(CvType.CV_16SC3, outImg.type());
      assertEquals(3, outImg.channels());
      short[] data = new short[3];
      outImg.get(1, 1, data);
      assertArrayEquals(new short[] {SHORT_C1[3], SHORT_C1[4], SHORT_C1[5]}, data);
    }
  }

  /**
   * Test short lookup table limit for signed and unsigned data Methods under test:
   *
   * <ul>
   *   <li>{@link LookupTableCV#LookupTableCV(short[], int, boolean)}
   *   <li>{@link LookupTableCV#lookup(int, int)}
   *   <li>{@link LookupTableCV#lookup(Mat)}
   * </ul>
   */
  @Test
  void testLutLimit() {
    short[] dataLut = new short[65536];
    for (int i = 0; i < 65536; i++) {
      dataLut[i] = (short) i;
    }

    // signed
    LookupTableCV lut = new LookupTableCV(dataLut, 0, true, false);
    assertEquals(DataBuffer.TYPE_USHORT, lut.getDataType());
    assertThrowsExactly(
        ArrayIndexOutOfBoundsException.class,
        () -> {
          lut.lookup(0, -1);
        });
    assertEquals(32767, lut.lookup(0, 32767));
    assertEquals(65535, lut.lookup(0, 65535));

    try (ImageCV img = new ImageCV(new Size(3, 3), CvType.CV_16SC1, new Scalar(-32768))) {
      ImageCV outImg = lut.lookup(img.toMat());
      assertNotNull(outImg);
      // Get unsigned image because lut is unsigned
      assertEquals(CvType.CV_16UC1, outImg.type());
      assertEquals(1, outImg.channels());
      short[] data = new short[1];
      outImg.get(1, 1, data);
      assertArrayEquals(new short[] {0}, data);
    }

    LookupTableCV lut2 = new LookupTableCV(dataLut, 0, true, true);
    assertEquals(DataBuffer.TYPE_USHORT, lut2.getDataType());
    assertThrowsExactly(
        ArrayIndexOutOfBoundsException.class,
        () -> {
          lut2.lookup(0, -1);
        });
    assertEquals(32767, lut2.lookup(0, 32767));
    assertEquals(65535, lut2.lookup(0, 65535));

    try (ImageCV img = new ImageCV(new Size(3, 3), CvType.CV_16SC1, new Scalar(-32768))) {
      ImageCV outImg = lut2.lookup(img.toMat());
      assertNotNull(outImg);
      // Get unsigned image because lut is unsigned
      assertEquals(CvType.CV_16UC1, outImg.type());
      assertEquals(1, outImg.channels());
      short[] data = new short[1];
      outImg.get(1, 1, data);
      assertArrayEquals(new short[] {-32768}, data);
    }

    // unsigned
    LookupTableCV lut3 = new LookupTableCV(dataLut, 0, false);
    assertEquals(DataBuffer.TYPE_SHORT, lut3.getDataType());
    assertThrowsExactly(
        ArrayIndexOutOfBoundsException.class,
        () -> {
          lut3.lookup(0, -1);
        });
    assertThrowsExactly(
        ArrayIndexOutOfBoundsException.class,
        () -> {
          lut3.lookup(0, 65536);
        });

    assertEquals(0, lut3.lookup(0, 0));
    assertEquals(32767, lut3.lookup(0, 32767));
    assertEquals(-32768, lut3.lookup(0, (-32768 & 0xFFFF)));
    assertEquals(-1, lut3.lookup(0, (-1 & 0xFFFF)));
  }
}
