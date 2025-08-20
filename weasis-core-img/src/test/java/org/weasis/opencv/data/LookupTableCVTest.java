/*
 * Copyright (c) 2025 Weasis Team and other contributors.
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
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.opencv.core.CvType;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.osgi.OpenCVNativeLoader;

@DisplayNameGeneration(ReplaceUnderscores.class)
class LookupTableCVTest {

  @BeforeAll
  static void load_opencv_native_library() {
    var loader = new OpenCVNativeLoader();
    loader.init();
  }

  @Nested
  class Constructor_Tests {

    @Test
    void creates_single_band_byte_lut_with_default_parameters() {
      var lutData = createSequentialByteArray(16);
      var lut = new LookupTableCV(lutData);

      assertLutProperties(lut, 1, DataBuffer.TYPE_BYTE, 0, lutData.length);
      assertArrayEquals(lutData, lut.getByteData()[0]);
      assertArrayEquals(lutData, lut.getByteData(0));
      assertArrayEquals(new int[] {0}, lut.getOffsets());
      assertNull(lut.getShortData());
    }

    @Test
    void creates_single_band_byte_lut_with_custom_offset() {
      var lutData = createSequentialByteArray(10);
      int offset = 5;
      var lut = new LookupTableCV(lutData, offset);

      assertLutProperties(lut, 1, DataBuffer.TYPE_BYTE, offset, lutData.length);
      assertEquals(offset, lut.getOffset(0));
    }

    @Test
    void creates_multi_band_byte_lut_with_uniform_offset() {
      var band1 = createSequentialByteArray(8);
      var band2 = createAlternatingByteArray(8);
      var band3 = createInverseByteArray(8);
      var lutData = new byte[][] {band1, band2, band3};
      int offset = 3;

      var lut = new LookupTableCV(lutData, offset);

      assertLutProperties(lut, 3, DataBuffer.TYPE_BYTE, offset, band1.length);
      assertArrayEquals(lutData, lut.getByteData());
      assertArrayEquals(band1, lut.getByteData(0));
      assertArrayEquals(band2, lut.getByteData(1));
      assertNull(lut.getShortData());
    }

    @Test
    void creates_multi_band_byte_lut_with_individual_offsets() {
      var lutData = new byte[][] {createSequentialByteArray(6), createAlternatingByteArray(6)};
      var offsets = new int[] {10, 20};

      var lut = new LookupTableCV(lutData, offsets);

      assertLutProperties(lut, 2, DataBuffer.TYPE_BYTE, 10, 6);
      assertEquals(10, lut.getOffset(0));
      assertEquals(20, lut.getOffset(1));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void creates_single_band_short_lut(boolean isUnsigned) {
      var lutData = createSequentialShortArray(10);
      int offset = 100;

      var lut = new LookupTableCV(lutData, offset, isUnsigned);

      int expectedType = isUnsigned ? DataBuffer.TYPE_USHORT : DataBuffer.TYPE_SHORT;
      assertLutProperties(lut, 1, expectedType, offset, lutData.length);
      assertArrayEquals(lutData, lut.getShortData()[0]);
      assertArrayEquals(lutData, lut.getShortData(0));
      assertNull(lut.getByteData());
    }

    @Test
    void throws_exception_for_null_data() {
      assertAll(
          () -> assertThrows(NullPointerException.class, () -> new LookupTableCV((byte[]) null)),
          () -> assertThrows(NullPointerException.class, () -> new LookupTableCV((byte[][]) null)),
          () ->
              assertThrows(
                  NullPointerException.class, () -> new LookupTableCV((short[]) null, 0, true)));
    }

    @Test
    void throws_exception_for_empty_data() {
      assertAll(
          () -> assertThrows(IllegalArgumentException.class, () -> new LookupTableCV(new byte[0])),
          () ->
              assertThrows(
                  IllegalArgumentException.class,
                  () -> new LookupTableCV(new byte[][] {new byte[0]})));
    }

    @Test
    void throws_exception_for_mismatched_offsets() {
      var lutData = new byte[][] {createSequentialByteArray(4), createAlternatingByteArray(4)};
      var wrongOffsets = new int[] {0}; // Should be length 2

      assertThrows(IllegalArgumentException.class, () -> new LookupTableCV(lutData, wrongOffsets));
    }

    private void assertLutProperties(
        LookupTableCV lut,
        int expectedBands,
        int expectedDataType,
        int expectedOffset,
        int expectedEntries) {
      assertAll(
          () -> assertEquals(expectedBands, lut.getNumBands()),
          () -> assertEquals(expectedDataType, lut.getDataType()),
          () -> assertEquals(expectedOffset, lut.getOffset()),
          () -> assertEquals(expectedEntries, lut.getNumEntries()));
    }
  }

  @Nested
  class Direct_Value_Lookup_Tests {

    @Test
    void performs_direct_byte_lookup_correctly() {
      var lutData = createSequentialByteArray(10);
      var lut = new LookupTableCV(lutData);

      assertEquals(lutData[3] & 0xFF, lut.lookup(0, 3));
      assertEquals(lutData[7] & 0xFF, lut.lookup(0, 7));
    }

    @Test
    void performs_multi_band_direct_lookup_correctly() {
      var band1 = createSequentialByteArray(8);
      var band2 = createAlternatingByteArray(8);
      var lutData = new byte[][] {band1, band2};
      int offset = 5;
      var lut = new LookupTableCV(lutData, offset);

      assertEquals(band1[2] & 0xFF, lut.lookup(0, offset + 2));
      assertEquals(band2[4] & 0xFF, lut.lookup(1, offset + 4));
    }

    @ParameterizedTest
    @MethodSource("shortLutTestData")
    void performs_short_lut_direct_lookup(
        short[] data, boolean isUnsigned, int index, int expected) {
      int offset = 10;
      var lut = new LookupTableCV(data, offset, isUnsigned);

      assertEquals(expected, lut.lookup(0, offset + index));
    }

    static Stream<Arguments> shortLutTestData() {
      var data = new short[] {1000, -2000, 3000, -4000};
      return Stream.of(
          Arguments.of(data, true, 1, data[1] & 0xFFFF),
          Arguments.of(data, true, 3, data[3] & 0xFFFF),
          Arguments.of(data, false, 1, data[1]),
          Arguments.of(data, false, 3, data[3]));
    }

    @Test
    void throws_exception_for_out_of_bounds_lookup() {
      var lutData = createSequentialShortArray(5);
      var lut = new LookupTableCV(lutData, 0, false);

      assertThrows(ArrayIndexOutOfBoundsException.class, () -> lut.lookup(0, -1));
      assertThrows(ArrayIndexOutOfBoundsException.class, () -> lut.lookup(0, lutData.length));
    }
  }

  @Nested
  class Image_Lookup_Tests {

    @Test
    void transforms_single_channel_byte_image_correctly() {
      var lutData = createInverseByteArray(256);
      var lut = new LookupTableCV(lutData);

      try (var sourceImage = new ImageCV(new Size(4, 4), CvType.CV_8UC1, new Scalar(100))) {
        var result = lut.lookup(sourceImage.toMat());

        assertImageProperties(result, CvType.CV_8UC1, 1);

        var pixelValue = new byte[1];
        result.get(2, 2, pixelValue);
        assertEquals(lutData[100] & 0xFF, pixelValue[0] & 0xFF);
      }
    }

    @Test
    void transforms_multi_channel_byte_image_correctly() {
      var lutData = createSequentialByteArray(256);
      var lut = new LookupTableCV(lutData);

      try (var sourceImage =
          new ImageCV(new Size(3, 3), CvType.CV_8UC3, new Scalar(50, 100, 150))) {
        var result = lut.lookup(sourceImage.toMat());

        assertImageProperties(result, CvType.CV_8UC3, 3);

        var pixelValues = new byte[3];
        result.get(1, 1, pixelValues);
        assertArrayEquals(new byte[] {lutData[50], lutData[100], lutData[150]}, pixelValues);
      }
    }

    @Test
    void expands_single_channel_to_multi_channel_correctly() {
      var redChannel = createSequentialByteArray(256);
      var greenChannel = createAlternatingByteArray(256);
      var blueChannel = createInverseByteArray(256);
      var lutData = new byte[][] {redChannel, greenChannel, blueChannel};
      var lut = new LookupTableCV(lutData, 0);

      try (var sourceImage = new ImageCV(new Size(2, 2), CvType.CV_8UC1, new Scalar(128))) {
        var result = lut.lookup(sourceImage.toMat());

        assertImageProperties(result, CvType.CV_8UC3, 3);

        var pixelValues = new byte[3];
        result.get(0, 0, pixelValues);
        assertArrayEquals(
            new byte[] {redChannel[128], greenChannel[128], blueChannel[128]}, pixelValues);
      }
    }

    @ParameterizedTest
    @ValueSource(ints = {2, 3}) // CV_16UC1 = 18, CV_16SC1 = 19
    void transforms_different_source_types_to_byte_output(int sourceType) {
      var lutData = createSequentialByteArray(256);
      var lut = new LookupTableCV(lutData);
      int t = CvType.CV_16UC1;
      try (var sourceImage = new ImageCV(new Size(2, 2), sourceType, new Scalar(200))) {
        var result = lut.lookup(sourceImage.toMat());

        assertImageProperties(result, CvType.CV_8UC1, 1);

        var pixelValue = new byte[1];
        result.get(0, 0, pixelValue);
        assertEquals(lutData[200], pixelValue[0]);
      }
    }

    @Test
    void transforms_byte_to_short_lookup_correctly() {
      var lutData = createSequentialShortArray(256);
      var lut = new LookupTableCV(lutData, 0, true);

      try (var sourceImage = new ImageCV(new Size(3, 3), CvType.CV_8UC1, new Scalar(150))) {
        var result = lut.lookup(sourceImage.toMat());

        assertImageProperties(result, CvType.CV_16UC1, 1);

        var pixelValue = new short[1];
        result.get(1, 1, pixelValue);
        assertArrayEquals(new short[] {lutData[150]}, pixelValue);
      }
    }

    @Test
    void handles_force_reading_unsigned_flag_correctly() {
      // Create a full range LUT (0 to 65535)
      var dataLut = new short[65536];
      for (int i = 0; i < 65536; i++) {
        dataLut[i] = (short) i;
      }

      var normalLut = new LookupTableCV(dataLut, 0, true, false);
      var forcedUnsignedLut = new LookupTableCV(dataLut, 0, true, true);

      try (var sourceImage = new ImageCV(new Size(2, 2), CvType.CV_16SC1, new Scalar(-32768))) {
        var normalResult = normalLut.lookup(sourceImage.toMat());
        var forcedResult = forcedUnsignedLut.lookup(sourceImage.toMat());

        var normalPixel = new short[1];
        var forcedPixel = new short[1];
        normalResult.get(0, 0, normalPixel);
        forcedResult.get(0, 0, forcedPixel);

        assertEquals(0, normalPixel[0]); // Clamped to 0
        assertEquals(-32768, forcedPixel[0]); // Treated as unsigned
      }
    }

    @Test
    void throws_exception_for_unsupported_source_type() {
      var lutData = createSequentialByteArray(10);
      var lut = new LookupTableCV(lutData);

      try (var unsupportedImage = new ImageCV(new Size(2, 2), CvType.CV_64FC1, new Scalar(1.5))) {
        assertThrows(IllegalArgumentException.class, () -> lut.lookup(unsupportedImage.toMat()));
      }
    }

    @Test
    void throws_exception_for_null_source_mat() {
      var lut = new LookupTableCV(createSequentialByteArray(10));
      assertThrows(NullPointerException.class, () -> lut.lookup(null));
    }

    private void assertImageProperties(ImageCV image, int expectedType, int expectedChannels) {
      assertAll(
          () -> assertNotNull(image),
          () -> assertEquals(expectedType, image.type()),
          () -> assertEquals(expectedChannels, image.channels()));
    }
  }

  @Nested
  class Edge_Cases_And_Performance_Tests {

    @Test
    void handles_single_element_lut_correctly() {
      var singleElementLut = new byte[] {42};
      var lut = new LookupTableCV(singleElementLut);

      assertEquals(1, lut.getNumBands());
      assertEquals(1, lut.getNumEntries());
      assertEquals(42 & 0xFF, lut.lookup(0, 0));
    }

    @Test
    void processes_large_images_efficiently() {
      var invertLut = createInverseByteArray(256);
      var lut = new LookupTableCV(invertLut);

      try (var largeImage = new ImageCV(new Size(512, 512), CvType.CV_8UC1, new Scalar(128))) {
        var result = lut.lookup(largeImage.toMat());

        assertNotNull(result);
        assertEquals(CvType.CV_8UC1, result.type());
        assertEquals(512, result.width());
        assertEquals(512, result.height());

        var samplePixel = new byte[1];
        result.get(256, 256, samplePixel);
        assertEquals(invertLut[128] & 0xFF, samplePixel[0] & 0xFF);
      }
    }

    @Test
    void maintains_consistency_across_multiple_lookups() {
      var lutData = createAlternatingByteArray(100);
      var lut = new LookupTableCV(lutData);

      var results =
          IntStream.range(0, 10)
              .mapToObj(
                  i -> {
                    try (var image =
                        new ImageCV(
                            new Size(5, 5), CvType.CV_8UC1, new Scalar(i % lutData.length))) {
                      return lut.lookup(image.toMat());
                    }
                  })
              .toList();

      results.forEach(
          result -> {
            assertNotNull(result);
            assertEquals(CvType.CV_8UC1, result.type());
          });
    }

    @Test
    void handles_maximum_size_unsigned_short_lut() {
      var maxSizeLut = new short[65536];
      for (int i = 0; i < 65536; i++) {
        maxSizeLut[i] = (short) i;
      }

      var lut = new LookupTableCV(maxSizeLut, 0, true, false);

      assertEquals(DataBuffer.TYPE_USHORT, lut.getDataType());
      assertEquals(32767, lut.lookup(0, 32767));
      assertEquals(65535, lut.lookup(0, 65535) & 0xFFFF);
    }
  }

  // Utility methods for creating test data
  private static byte[] createSequentialByteArray(int size) {
    var array = new byte[size];
    for (int i = 0; i < size; i++) {
      array[i] = (byte) i;
    }
    return array;
  }

  private static byte[] createAlternatingByteArray(int size) {
    var array = new byte[size];
    for (int i = 0; i < size; i++) {
      array[i] = (byte) (i % 2 == 0 ? i : -i);
    }
    return array;
  }

  private static byte[] createInverseByteArray(int size) {
    var array = new byte[size];
    for (int i = 0; i < size; i++) {
      array[i] = (byte) (size - 1 - i);
    }
    return array;
  }

  private static short[] createSequentialShortArray(int size) {
    var array = new short[size];
    for (int i = 0; i < size; i++) {
      array[i] = (short) (i * 100);
    }
    return array;
  }
}
