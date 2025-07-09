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

import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferShort;
import java.awt.image.DataBufferUShort;
import java.util.Arrays;
import java.util.Objects;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.weasis.opencv.op.ImageConversion;

/**
 * The {@code LookupTableCV} class implements a lookup table for fast pixel value transformations in
 * image processing. It allows efficient mapping of input pixel values to output values using
 * pre-computed tables.
 *
 * <p>The lookup table can be initialized with different data types:
 *
 * <ul>
 *   <li>Byte arrays - for 8-bit transformations using {@link DataBufferByte}
 *   <li>Short arrays - for 16-bit transformations using {@link DataBufferUShort} or {@link
 *       DataBufferShort}
 * </ul>
 *
 * <p>Key features include:
 *
 * <ul>
 *   <li>Support for single and multi-band (color) image processing
 *   <li>Configurable offset values for each band
 *   <li>Optional unsigned/signed data interpretation
 *   <li>Automatic band expansion for mismatched channel counts
 *   <li>Bounds checking and value clamping
 * </ul>
 *
 * <p>The lookup operation is optimized for performance and handles various input/output data type
 * combinations. When applying the lookup table to images, the class automatically adapts to the
 * image characteristics including number of channels, bit depth and data type.
 */
public class LookupTableCV {

  private final int[] offsets;
  private final DataBuffer data;
  private final boolean forceReadingUnsigned;

  /**
   * Constructs a LookupTableCV with the provided byte data.
   *
   * @param data the byte data for the lookup table, must not be null
   */
  public LookupTableCV(byte[] data) {
    this(data, 0, false);
  }

  /**
   * Constructs a LookupTableCV with the provided byte data and an offset.
   *
   * @param data the byte data for the lookup table, must not be null
   * @param offset the offset for the band, must be non-negative
   */
  public LookupTableCV(byte[] data, int offset) {
    this(data, offset, false);
  }

  /**
   * Constructs a LookupTableCV with the provided byte data and an offset.
   *
   * @param data the byte data for the lookup table, must not be null
   * @param offset the offset for the band, must be non-negative
   * @param forceReadingUnsigned if true, forces reading values as unsigned
   */
  public LookupTableCV(byte[] data, int offset, boolean forceReadingUnsigned) {
    if (data == null || data.length == 0) {
      throw new IllegalArgumentException("Data array must not be empty.");
    }
    this.offsets = new int[] {offset};
    this.data = new DataBufferByte(data, data.length);
    this.forceReadingUnsigned = forceReadingUnsigned;
  }

  /**
   * Constructs a LookupTableCV with the provided byte data.
   *
   * @param data the byte data for the lookup table, must not be null
   */
  public LookupTableCV(byte[][] data) {
    this(data, new int[data.length], false);
  }

  /**
   * Constructs a LookupTableCV with the provided byte data and an offset for each band.
   *
   * @param data the byte data for the lookup table, must not be null
   * @param offset the offset for each band, must be non-negative
   */
  public LookupTableCV(byte[][] data, int offset) {
    this(data, createFilledArray(data.length, offset), false);
  }

  /**
   * Constructs a LookupTableCV with the provided byte data and offsets for each band.
   *
   * @param data the byte data for the lookup table, must not be null
   * @param offsets the offsets for each band, must not be null
   */
  public LookupTableCV(byte[][] data, int[] offsets) {
    this(data, offsets, false);
  }

  /**
   * Constructs a LookupTableCV with the provided byte data and offsets.
   *
   * @param data the byte data for the lookup table, must not be null or empty
   * @param offsets the offsets for each band, must not be null or empty
   * @param forceReadingUnsigned if true, forces reading values as unsigned
   */
  public LookupTableCV(byte[][] data, int[] offsets, boolean forceReadingUnsigned) {
    if (data == null || data.length == 0 || data[0].length == 0) {
      throw new IllegalArgumentException("Data array must not be empty.");
    }
    if (offsets == null || offsets.length != data.length) {
      throw new IllegalArgumentException("Offsets array must match the number of bands.");
    }
    this.offsets = Arrays.copyOf(offsets, data.length);
    this.data = new DataBufferByte(data, data[0].length);
    this.forceReadingUnsigned = forceReadingUnsigned;
  }

  /**
   * Constructs a LookupTableCV with the provided short data.
   *
   * @param data the short data for the lookup table, must not be null
   * @param offset the offset for the band, must be non-negative
   * @param isUShort if true, interprets the data as unsigned short
   */
  public LookupTableCV(short[] data, int offset, boolean isUShort) {
    this(data, offset, isUShort, false);
  }

  /**
   * Constructs a LookupTableCV with the provided short data and an offset.
   *
   * @param data the short data for the lookup table, must not be null
   * @param offset the offset for the band, must be non-negative
   * @param isUShort if true, interprets the data as unsigned short
   * @param forceReadingUnsigned if true, forces reading values as unsigned. Bug in some libraries
   *     that do not handle signed short correctly
   */
  public LookupTableCV(short[] data, int offset, boolean isUShort, boolean forceReadingUnsigned) {
    this.offsets = new int[] {offset};
    this.data =
        isUShort
            ? new DataBufferUShort(Objects.requireNonNull(data), data.length)
            : new DataBufferShort(Objects.requireNonNull(data), data.length);
    this.forceReadingUnsigned = forceReadingUnsigned;
  }

  // Utility method to create filled arrays
  private static int[] createFilledArray(int length, int value) {
    int[] array = new int[length];
    Arrays.fill(array, value);
    return array;
  }

  public DataBuffer getData() {
    return data;
  }

  public byte[][] getByteData() {
    return data instanceof DataBufferByte buffer ? buffer.getBankData() : null;
  }

  public byte[] getByteData(int band) {
    return data instanceof DataBufferByte buffer ? buffer.getData(band) : null;
  }

  public short[][] getShortData() {
    if (data instanceof DataBufferUShort bufferUShort) {
      return bufferUShort.getBankData();
    } else if (data instanceof DataBufferShort bufferShort) {
      return bufferShort.getBankData();
    } else {
      return null;
    }
  }

  public short[] getShortData(int band) {
    if (data instanceof DataBufferUShort bufferUShort) {
      return bufferUShort.getData(band);
    } else if (data instanceof DataBufferShort bufferShort) {
      return bufferShort.getData(band);
    } else {
      return null;
    }
  }

  public int[] getOffsets() {
    return offsets;
  }

  public int getOffset() {
    return offsets[0];
  }

  /** Returns the index offset of entry 0 for a specific band. */
  public int getOffset(int band) {
    return offsets[band];
  }

  public int getNumBands() {
    return data.getNumBanks();
  }

  public int getNumEntries() {
    return data.getSize();
  }

  public int getDataType() {
    return data.getDataType();
  }

  public int lookup(int band, int value) {
    return data.getElem(band, value - offsets[band]);
  }

  public ImageCV lookup(Mat src) {
    Objects.requireNonNull(src, "Source Mat cannot be null.");

    int width = src.width();
    int height = src.height();
    int cvType = src.type();
    int channels = CvType.channels(cvType);
    int srcDataType = ImageConversion.convertToDataType(cvType);

    Object sourceData = initializeSourceData(src, width, height, channels, cvType);

    // Prepare lookup table data for processing
    LookupData lookupData = prepareLookupData(channels);

    // Process lookup based on the destination type
    return switch (getDataType()) {
      case DataBuffer.TYPE_BYTE ->
          performByteLookup(
              srcDataType,
              width,
              height,
              lookupData.numBands,
              channels,
              sourceData,
              lookupData.byteData,
              lookupData.offsets);
      case DataBuffer.TYPE_USHORT, DataBuffer.TYPE_SHORT ->
          performShortLookup(
              srcDataType,
              width,
              height,
              lookupData.numBands,
              channels,
              sourceData,
              lookupData.shortData,
              lookupData.offsets);
      default -> throw new IllegalArgumentException("Unsupported LUT transformation.");
    };
  }

  // Helper class to encapsulate lookup data preparation
  private static class LookupData {
    final int numBands;
    final int[] offsets;
    final byte[][] byteData;
    final short[][] shortData;

    LookupData(int numBands, int[] offsets, byte[][] byteData, short[][] shortData) {
      this.numBands = numBands;
      this.offsets = offsets;
      this.byteData = byteData;
      this.shortData = shortData;
    }
  }

  private LookupData prepareLookupData(int channels) {
    int numBands = getNumBands();
    int[] tblOffsets = getOffsets();
    byte[][] bTblData = getByteData();
    short[][] sTblData = getShortData();

    // Expand lookup table data if needed
    if (numBands < channels) {
      if (bTblData != null) {
        byte[] bandData = bTblData[0];
        bTblData = new byte[channels][];
        Arrays.fill(bTblData, bandData);
      } else if (sTblData != null) {
        short[] bandData = sTblData[0];
        sTblData = new short[channels][];
        Arrays.fill(sTblData, bandData);
      }
      int firstOffset = tblOffsets[0];
      tblOffsets = createFilledArray(channels, firstOffset);
      numBands = channels;
    }

    return new LookupData(numBands, tblOffsets, bTblData, sTblData);
  }

  private Object initializeSourceData(Mat src, int width, int height, int channels, int cvType) {
    int totalPixels = width * height * channels;
    return switch (CvType.depth(cvType)) {
      case CvType.CV_8U, CvType.CV_8S -> {
        byte[] byteData = new byte[totalPixels];
        src.get(0, 0, byteData);
        yield byteData;
      }
      case CvType.CV_16U, CvType.CV_16S -> {
        short[] shortData = new short[totalPixels];
        src.get(0, 0, shortData);
        yield shortData;
      }
      default ->
          throw new IllegalArgumentException(
              "Not supported dataType for LUT transformation: " + src);
    };
  }

  private ImageCV performByteLookup(
      int srcDataType,
      int width,
      int height,
      int numBands,
      int channels,
      Object sourceData,
      byte[][] tblData,
      int[] tblOffsets) {
    boolean isSourceByte = srcDataType == DataBuffer.TYPE_BYTE;
    byte[] dstData =
        isSourceByte && channels >= numBands
            ? (byte[]) sourceData
            : new byte[width * height * numBands];

    switch (srcDataType) {
      case DataBuffer.TYPE_BYTE -> lookup((byte[]) sourceData, dstData, tblOffsets, tblData);
      case DataBuffer.TYPE_USHORT -> lookupU((short[]) sourceData, dstData, tblOffsets, tblData);
      case DataBuffer.TYPE_SHORT -> lookup((short[]) sourceData, dstData, tblOffsets, tblData);
      default ->
          throw new IllegalArgumentException(
              "Not supported LUT conversion from source dataType " + srcDataType);
    }

    ImageCV dst = new ImageCV(height, width, CvType.CV_8UC(numBands));
    dst.put(0, 0, dstData);
    return dst;
  }

  private ImageCV performShortLookup(
      int srcDataType,
      int width,
      int height,
      int numBands,
      int channels,
      Object sourceData,
      short[][] tblData,
      int[] tblOffsets) {
    boolean isSourceByte = srcDataType == DataBuffer.TYPE_BYTE;
    short[] dstData =
        (!isSourceByte && channels >= numBands)
            ? (short[]) sourceData
            : new short[width * height * numBands];

    switch (srcDataType) {
      case DataBuffer.TYPE_BYTE -> lookup((byte[]) sourceData, dstData, tblOffsets, tblData);
      case DataBuffer.TYPE_USHORT -> lookupU((short[]) sourceData, dstData, tblOffsets, tblData);
      case DataBuffer.TYPE_SHORT -> lookup((short[]) sourceData, dstData, tblOffsets, tblData);
      default ->
          throw new IllegalArgumentException(
              "Not supported LUT conversion from source dataType " + srcDataType);
    }

    int outputType =
        getDataType() == DataBuffer.TYPE_USHORT
            ? CvType.CV_16UC(numBands)
            : CvType.CV_16SC(numBands);

    ImageCV dst = new ImageCV(height, width, outputType);
    dst.put(0, 0, dstData);
    return dst;
  }

  private static int clampIndex(int pixel, int offset, int maxIndex) {
    int val = pixel - offset;
    return Math.max(0, Math.min(val, maxIndex));
  }

  // Simplified lookup methods using generic approach
  private void lookup(byte[] srcData, byte[] dstData, int[] tblOffsets, byte[][] tblData) {
    performLookup(srcData, dstData, tblOffsets, tblData, 0xFF);
  }

  private void lookupU(short[] srcData, byte[] dstData, int[] tblOffsets, byte[][] tblData) {
    performLookup(srcData, dstData, tblOffsets, tblData, 0xFFFF);
  }

  private void lookup(short[] srcData, byte[] dstData, int[] tblOffsets, byte[][] tblData) {
    int mask = forceReadingUnsigned ? 0xFFFF : 0xFFFFFFFF;
    performLookup(srcData, dstData, tblOffsets, tblData, mask);
  }

  private void lookup(byte[] srcData, short[] dstData, int[] tblOffsets, short[][] tblData) {
    performLookup(srcData, dstData, tblOffsets, tblData, 0xFF);
  }

  private void lookupU(short[] srcData, short[] dstData, int[] tblOffsets, short[][] tblData) {
    performLookup(srcData, dstData, tblOffsets, tblData, 0xFFFF);
  }

  private void lookup(short[] srcData, short[] dstData, int[] tblOffsets, short[][] tblData) {
    int mask = forceReadingUnsigned ? 0xFFFF : 0xFFFFFFFF;
    performLookup(srcData, dstData, tblOffsets, tblData, mask);
  }

  // Generic lookup method that handles both byte and short data
  private <S, D, T> void performLookup(
      S srcData, D dstData, int[] tblOffsets, T[] tblData, int mask) {
    int numBands = tblData.length;
    int srcLength = getSrcLength(srcData);
    int dstLength = getDstLength(dstData);

    if (srcLength < dstLength) {
      // Expand channels: one source value maps to multiple destination values
      for (int i = 0; i < srcLength; i++) {
        int val = getSrcValue(srcData, i) & mask;
        for (int b = 0; b < numBands; b++) {
          int index = clampIndex(val, tblOffsets[b], getTableLength(tblData[b]) - 1);
          setDstValue(dstData, i * numBands + b, getTableValue(tblData[b], index));
        }
      }
    } else {
      // Process each band separately
      for (int b = 0; b < numBands; b++) {
        T table = tblData[b];
        int tblOffset = tblOffsets[b];
        int maxLength = getTableLength(table) - 1;

        for (int i = b; i < srcLength; i += numBands) {
          int val = getSrcValue(srcData, i) & mask;
          int index = clampIndex(val, tblOffset, maxLength);
          setDstValue(dstData, i, getTableValue(table, index));
        }
      }
    }
  }

  // Helper methods for generic lookup
  private int getSrcLength(Object srcData) {
    return srcData instanceof byte[] b ? b.length : ((short[]) srcData).length;
  }

  private int getDstLength(Object dstData) {
    return dstData instanceof byte[] b ? b.length : ((short[]) dstData).length;
  }

  private int getSrcValue(Object srcData, int index) {
    return srcData instanceof byte[] b ? b[index] : ((short[]) srcData)[index];
  }

  private void setDstValue(Object dstData, int index, int value) {
    if (dstData instanceof byte[] b) {
      b[index] = (byte) value;
    } else {
      ((short[]) dstData)[index] = (short) value;
    }
  }

  private int getTableLength(Object table) {
    return table instanceof byte[] b ? b.length : ((short[]) table).length;
  }

  private int getTableValue(Object table, int index) {
    return table instanceof byte[] b ? b[index] : ((short[]) table)[index];
  }
}
