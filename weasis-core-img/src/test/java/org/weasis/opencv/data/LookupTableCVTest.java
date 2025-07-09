/*
 * Copyright (c) 2020 Weasis Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.weasis.opencv.data;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.image.DataBuffer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.opencv.core.CvType;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.osgi.OpenCVNativeLoader;

@DisplayName("LookupTableCV Tests")
class LookupTableCVTest {

  // Test data constants
  private static final byte[] BYTE_C1 = {1, -1, 1, -1, 0, 127, -128, -9, 9};
  private static final byte[][] BYTE_C3 = {BYTE_C1, {1, -7, 7, -1, 0, -111, 111, -11, 11}, BYTE_C1};
  private static final short[] SHORT_C1 = {-8126, -4096, -1024, -512, 0, 10, 512, 1024, 4096};

  @BeforeAll
  @DisplayName("Load OpenCV native library")
  static void loadNativeLib() {
    // Load the native OpenCV library
    OpenCVNativeLoader loader = new OpenCVNativeLoader();
    loader.init();
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Single Band Byte LUT - Basic Constructor")
    void testSingleBandByteLut() {
      LookupTableCV lut = new LookupTableCV(BYTE_C1);

      // Verify LUT properties
      assertLutProperties(lut, 1, DataBuffer.TYPE_BYTE, 0, BYTE_C1.length);
      assertArrayEquals(BYTE_C1, lut.getByteData()[0]);
      assertArrayEquals(BYTE_C1, lut.getByteData(0));
      assertArrayEquals(new int[] {0}, lut.getOffsets());
      assertNull(lut.getShortData());

      // Verify out of bounds access
      assertThrows(ArrayIndexOutOfBoundsException.class, () -> lut.getByteData(1));
    }

    @Test
    @DisplayName("Single Band Byte LUT - With Offset")
    void testSingleBandByteLutWithOffset() {
      int offset = 5;
      LookupTableCV lut = new LookupTableCV(BYTE_C1, offset);

      assertLutProperties(lut, 1, DataBuffer.TYPE_BYTE, offset, BYTE_C1.length);
      assertEquals(offset, lut.getOffset(0));
    }

    @Test
    @DisplayName("Multi-Band Byte LUT - With Offset")
    void testMultiBandByteLut() {
      int offset = 2;
      LookupTableCV lut = new LookupTableCV(BYTE_C3, offset);

      assertLutProperties(lut, 3, DataBuffer.TYPE_BYTE, offset, BYTE_C1.length);
      assertArrayEquals(BYTE_C3, lut.getByteData());
      assertArrayEquals(BYTE_C1, lut.getByteData(0));
      assertEquals(3, lut.getData().getNumBanks());
      assertNull(lut.getShortData());
    }

    @Test
    @DisplayName("Unsigned Short LUT")
    void testUnsignedShortLut() {
      int offset = 2;
      LookupTableCV lut = new LookupTableCV(SHORT_C1, offset, true);

      assertLutProperties(lut, 1, DataBuffer.TYPE_USHORT, offset, SHORT_C1.length);
      assertArrayEquals(SHORT_C1, lut.getShortData()[0]);
      assertArrayEquals(SHORT_C1, lut.getShortData(0));
      assertNull(lut.getByteData());
      assertNull(lut.getByteData(0));
    }

    @Test
    @DisplayName("Signed Short LUT")
    void testSignedShortLut() {
      int offset = 2;
      LookupTableCV lut = new LookupTableCV(SHORT_C1, offset, false);

      assertLutProperties(lut, 1, DataBuffer.TYPE_SHORT, offset, SHORT_C1.length);
      assertArrayEquals(SHORT_C1, lut.getShortData()[0]);
      assertArrayEquals(SHORT_C1, lut.getShortData(0));
      assertNull(lut.getByteData());
    }

    @Test
    @DisplayName("Constructor with Null Data")
    void testConstructorWithNullData() {
      assertThrows(Exception.class, () -> new LookupTableCV((byte[]) null));
      assertThrows(Exception.class, () -> new LookupTableCV((byte[][]) null));
      assertThrows(Exception.class, () -> new LookupTableCV((short[]) null, 0, true));
    }

    private void assertLutProperties(
        LookupTableCV lut,
        int expectedBands,
        int expectedDataType,
        int expectedOffset,
        int expectedEntries) {
      assertAll(
          "LUT Properties",
          () -> assertEquals(expectedBands, lut.getNumBands()),
          () -> assertEquals(expectedDataType, lut.getDataType()),
          () -> assertEquals(expectedOffset, lut.getOffset()),
          () -> assertEquals(expectedEntries, lut.getNumEntries()));
    }
  }

  @Nested
  @DisplayName("Lookup Operation Tests")
  class LookupOperationTests {

    @Test
    @DisplayName("Direct Value Lookup")
    void testDirectValueLookup() {
      LookupTableCV lut = new LookupTableCV(BYTE_C1);

      assertEquals(BYTE_C1[3] & 0xFF, lut.lookup(0, 3));
      assertEquals(BYTE_C1[5] & 0xFF, lut.lookup(0, 5));
    }

    @Test
    @DisplayName("Multi-Band Direct Lookup")
    void testMultiBandDirectLookup() {
      LookupTableCV lut = new LookupTableCV(BYTE_C3, 2);

      assertEquals(BYTE_C3[1][3] & 0xFF, lut.lookup(1, lut.getOffset() + 3));
      assertEquals(BYTE_C3[1][5] & 0xFF, lut.lookup(1, lut.getOffset() + 5));
    }

    @Test
    @DisplayName("Short LUT Direct Lookup")
    void testShortLutDirectLookup() {
      LookupTableCV unsignedLut = new LookupTableCV(SHORT_C1, 2, true);
      LookupTableCV signedLut = new LookupTableCV(SHORT_C1, 2, false);

      assertEquals(SHORT_C1[3] & 0xFFFF, unsignedLut.lookup(0, unsignedLut.getOffset() + 3));
      assertEquals(SHORT_C1[5] & 0xFFFF, unsignedLut.lookup(0, unsignedLut.getOffset() + 5));

      assertEquals(SHORT_C1[3], signedLut.lookup(0, signedLut.getOffset() + 3));
      assertEquals(SHORT_C1[5], signedLut.lookup(0, signedLut.getOffset() + 5));
    }

    @Test
    @DisplayName("Out of Bounds Lookup")
    void testOutOfBoundsLookup() {
      LookupTableCV lut = new LookupTableCV(SHORT_C1, 2, false);

      assertThrows(ArrayIndexOutOfBoundsException.class, () -> lut.lookup(0, lut.getOffset() - 15));
    }
  }

  @Nested
  @DisplayName("Image Lookup Tests")
  class ImageLookupTests {

    @Test
    @DisplayName("Single Channel Byte to Byte Lookup")
    void testSingleChannelByteToBytes() {
      LookupTableCV lut = new LookupTableCV(BYTE_C1);

      try (ImageCV img = new ImageCV(new Size(3, 3), CvType.CV_8UC1, new Scalar(5))) {
        ImageCV result = lut.lookup(img.toMat());

        assertImageProperties(result, CvType.CV_8UC1, 1);

        byte[] data = new byte[1];
        result.get(1, 1, data);
        assertEquals(BYTE_C1[5] & 0xFF, data[0]);
      }
    }

    @Test
    @DisplayName("Multi-Channel Byte to Byte Lookup")
    void testMultiChannelByteToBytes() {
      LookupTableCV lut = new LookupTableCV(BYTE_C1);

      try (ImageCV img = new ImageCV(new Size(3, 3), CvType.CV_8UC3, new Scalar(4, 5, 6))) {
        ImageCV result = lut.lookup(img.toMat());

        assertImageProperties(result, CvType.CV_8UC3, 3);

        byte[] data = new byte[3];
        result.get(1, 1, data);
        assertArrayEquals(new byte[] {BYTE_C1[4], BYTE_C1[5], BYTE_C1[6]}, data);
      }
    }

    @Test
    @DisplayName("Single Channel to Multi-Channel Expansion")
    void testChannelExpansion() {
      LookupTableCV lut = new LookupTableCV(BYTE_C3, 2);

      try (ImageCV img =
          new ImageCV(new Size(3, 3), CvType.CV_8UC1, new Scalar(lut.getOffset() + 5))) {
        ImageCV result = lut.lookup(img.toMat());

        assertImageProperties(result, CvType.CV_8UC3, 3);

        byte[] data = new byte[3];
        result.get(1, 1, data);
        assertArrayEquals(new byte[] {BYTE_C3[0][5], BYTE_C3[1][5], BYTE_C3[2][5]}, data);
      }
    }

    @ParameterizedTest(name = "Source Type: {0}")
    @DisplayName("Different Source Types to Byte Output")
    @ValueSource(ints = {2, 3}) // CV_16UC1 = 18, CV_16SC1 = 19
    void testDifferentSourceTypesToByte(int sourceType) {
      LookupTableCV lut = new LookupTableCV(BYTE_C1);

      try (ImageCV img = new ImageCV(new Size(3, 3), sourceType, new Scalar(5))) {
        ImageCV result = lut.lookup(img.toMat());
        assertImageProperties(result, CvType.CV_8UC1, 1);

        byte[] data = new byte[1];
        result.get(1, 1, data);
        assertEquals(BYTE_C1[5] & 0xFF, data[0] & 0xFF);
      }
    }

    @Test
    @DisplayName("Byte to Short Lookup")
    void testByteToShortLookup() {
      LookupTableCV lut = new LookupTableCV(SHORT_C1, 2, true);

      try (ImageCV img =
          new ImageCV(new Size(3, 3), CvType.CV_8UC1, new Scalar(lut.getOffset() + 5))) {
        ImageCV result = lut.lookup(img.toMat());

        assertImageProperties(result, CvType.CV_16UC1, 1);

        short[] data = new short[1];
        result.get(1, 1, data);
        assertArrayEquals(new short[] {SHORT_C1[5]}, data);
      }
    }

    @Test
    @DisplayName("Multi-Channel Short to Short Lookup")
    void testMultiChannelShortToShort() {
      LookupTableCV lut = new LookupTableCV(SHORT_C1, 2, false);

      try (ImageCV img =
          new ImageCV(
              new Size(3, 3),
              CvType.CV_16SC3,
              new Scalar(lut.getOffset() + 3, lut.getOffset() + 4, lut.getOffset() + 5))) {
        ImageCV result = lut.lookup(img.toMat());

        assertImageProperties(result, CvType.CV_16SC3, 3);

        short[] data = new short[3];
        result.get(1, 1, data);
        assertArrayEquals(new short[] {SHORT_C1[3], SHORT_C1[4], SHORT_C1[5]}, data);
      }
    }

    @Test
    @DisplayName("Unsupported Source Type")
    void testUnsupportedSourceType() {
      LookupTableCV lut = new LookupTableCV(BYTE_C3, 2);

      try (ImageCV img = new ImageCV(new Size(3, 3), CvType.CV_64FC1, new Scalar(3.5f))) {
        assertThrows(IllegalArgumentException.class, () -> lut.lookup(img.toMat()));
      }
    }

    @Test
    @DisplayName("Null Source Mat")
    void testNullSourceMat() {
      LookupTableCV lut = new LookupTableCV(BYTE_C1);
      assertThrows(NullPointerException.class, () -> lut.lookup(null));
    }

    private void assertImageProperties(ImageCV image, int expectedType, int expectedChannels) {
      assertAll(
          "Image Properties",
          () -> assertNotNull(image),
          () -> assertEquals(expectedType, image.type()),
          () -> assertEquals(expectedChannels, image.channels()));
    }
  }

  @Nested
  @DisplayName("Boundary and Edge Cases")
  class BoundaryTests {

    @Test
    @DisplayName("Maximum Size LUT Operations")
    void testMaxSizeLutOperations() {
      short[] dataLut = new short[65536];
      for (int i = 0; i < 65536; i++) {
        dataLut[i] = (short) i;
      }

      testUnsignedLutOperations(dataLut);
      testSignedLutOperations(dataLut);
    }

    @Test
    @DisplayName("Force Reading Unsigned Flag")
    void testForceReadingUnsigned() {
      short[] dataLut = new short[65536];
      for (int i = 0; i < 65536; i++) {
        dataLut[i] = (short) i;
      }

      LookupTableCV lut1 = new LookupTableCV(dataLut, 0, true, false);
      LookupTableCV lut2 = new LookupTableCV(dataLut, 0, true, true);

      try (ImageCV img = new ImageCV(new Size(3, 3), CvType.CV_16SC1, new Scalar(-32768))) {
        ImageCV result1 = lut1.lookup(img.toMat());
        ImageCV result2 = lut2.lookup(img.toMat());

        short[] data1 = new short[1];
        short[] data2 = new short[1];
        result1.get(1, 1, data1);
        result2.get(1, 1, data2);

        assertEquals(0, data1[0]);
        assertEquals(-32768, data2[0]);
      }
    }

    private void testUnsignedLutOperations(short[] dataLut) {
      LookupTableCV lut = new LookupTableCV(dataLut, 0, true, false);

      assertEquals(DataBuffer.TYPE_USHORT, lut.getDataType());
      assertThrows(ArrayIndexOutOfBoundsException.class, () -> lut.lookup(0, -1));
      assertEquals(32767, lut.lookup(0, 32767));
      assertEquals(65535, lut.lookup(0, 65535));

      try (ImageCV img = new ImageCV(new Size(3, 3), CvType.CV_16SC1, new Scalar(-32768))) {
        ImageCV result = lut.lookup(img.toMat());

        assertEquals(CvType.CV_16UC1, result.type());

        short[] data = new short[1];
        result.get(1, 1, data);
        assertEquals(0, data[0]);
      }
    }

    private void testSignedLutOperations(short[] dataLut) {
      LookupTableCV lut = new LookupTableCV(dataLut, 0, false);

      assertEquals(DataBuffer.TYPE_SHORT, lut.getDataType());
      assertThrows(ArrayIndexOutOfBoundsException.class, () -> lut.lookup(0, -1));
      assertThrows(ArrayIndexOutOfBoundsException.class, () -> lut.lookup(0, 65536));

      assertEquals(0, lut.lookup(0, 0));
      assertEquals(32767, lut.lookup(0, 32767));
      assertEquals(-32768, lut.lookup(0, (-32768 & 0xFFFF)));
      assertEquals(-1, lut.lookup(0, (0xFFFF)));
    }

    @Test
    @DisplayName("Empty Arrays")
    void testEmptyArrays() {
      byte[] emptyByteArray = new byte[0];
      assertThrows(IllegalArgumentException.class, () -> new LookupTableCV(emptyByteArray));
    }

    @Test
    @DisplayName("Single Element Arrays")
    void testSingleElementArrays() {
      byte[] singleByte = {42};
      LookupTableCV lut = new LookupTableCV(singleByte);

      assertEquals(1, lut.getNumBands());
      assertEquals(1, lut.getNumEntries());
      assertEquals(42 & 0xFF, lut.lookup(0, 0));
    }
  }

  @Nested
  @DisplayName("Performance and Memory Tests")
  class PerformanceTests {

    @Test
    @DisplayName("Large Image Processing")
    void testLargeImageProcessing() {
      byte[] simpleLut = new byte[256];
      for (int i = 0; i < 256; i++) {
        simpleLut[i] = (byte) (255 - i); // Invert LUT
      }

      LookupTableCV lut = new LookupTableCV(simpleLut);

      try (ImageCV largeImg = new ImageCV(new Size(1024, 1024), CvType.CV_8UC1, new Scalar(100))) {
        ImageCV result = lut.lookup(largeImg.toMat());

        assertNotNull(result);
        assertEquals(CvType.CV_8UC1, result.type());
        assertEquals(1024, result.width());
        assertEquals(1024, result.height());

        byte[] sample = new byte[1];
        result.get(500, 500, sample);
        assertEquals(simpleLut[100] & 0xFF, sample[0] & 0xFF);
      }
    }

    @Test
    @DisplayName("Multiple Lookups with Same LUT")
    void testMultipleLookups() {
      LookupTableCV lut = new LookupTableCV(BYTE_C1);

      for (int i = 0; i < 10; i++) {
        try (ImageCV img =
            new ImageCV(new Size(10, 10), CvType.CV_8UC1, new Scalar(i % BYTE_C1.length))) {
          ImageCV result = lut.lookup(img.toMat());
          assertNotNull(result);
          assertEquals(CvType.CV_8UC1, result.type());
        }
      }
    }

    @Nested
    @DisplayName("Unit Tests for Lookup Method")
    class LookupMethodTests {

      @Test
      @DisplayName("Valid Byte Lookup")
      void testValidByteLookup() {
        byte[] data = {0, 1, 2, 3, 4, 5};
        LookupTableCV lut = new LookupTableCV(data);

        assertEquals(0, lut.lookup(0, 0));
        assertEquals(3, lut.lookup(0, 3));
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> lut.lookup(0, 10));
      }

      @Test
      @DisplayName("Valid Multi-Band Byte Lookup")
      void testValidMultiBandByteLookup() {
        byte[][] data = {
          {0, 1, 2},
          {3, 4, 5}
        };
        int[] offsets = {0, 0};
        LookupTableCV lut = new LookupTableCV(data, offsets);

        assertEquals(1, lut.lookup(0, 1));
        assertEquals(5, lut.lookup(1, 2));
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> lut.lookup(1, 10));
      }

      @Test
      @DisplayName("Valid Short Lookup")
      void testValidShortLookup() {
        short[] data = {100, 200, 300, 400};
        LookupTableCV lut = new LookupTableCV(data, 0, false);

        assertEquals(100, lut.lookup(0, 0));
        assertEquals(400, lut.lookup(0, 3));
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> lut.lookup(0, 4));
      }

      @Test
      @DisplayName("Valid Unsigned Short Lookup")
      void testValidUnsignedShortLookup() {
        short[] data = {100, 200, 300, 400};
        LookupTableCV lut = new LookupTableCV(data, 0, true);

        assertEquals(100, lut.lookup(0, 0));
        assertEquals(400, lut.lookup(0, 3));
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> lut.lookup(0, 4));
      }

      @Test
      @DisplayName("Offset Handling in Lookup")
      void testLookupWithOffset() {
        byte[] data = {10, 20, 30, 40};
        int offset = 2;
        LookupTableCV lut = new LookupTableCV(data, offset);

        assertEquals(10, lut.lookup(0, 2));
        assertEquals(20, lut.lookup(0, 3));
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> lut.lookup(0, 1));
      }

      @Test
      @DisplayName("Invalid Band in Lookup")
      void testInvalidBandLookup() {
        byte[][] data = {
          {0, 1, 2},
          {3, 4, 5}
        };
        LookupTableCV lut = new LookupTableCV(data);

        assertThrows(ArrayIndexOutOfBoundsException.class, () -> lut.lookup(2, 1));
      }

      @Test
      @DisplayName("Null Source Mat Throws Exception")
      void testNullSourceMatThrows() {
        LookupTableCV lut = new LookupTableCV(new byte[] {10, 20, 30});
        assertThrows(NullPointerException.class, () -> lut.lookup(null));
      }
    }
  }
}
