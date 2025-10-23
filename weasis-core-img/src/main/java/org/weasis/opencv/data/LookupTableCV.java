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
 * Defines a lookup table for fast pixel value transformations in image processing.
 *
 * <p>Supports byte and short data types with configurable offsets and signed/unsigned
 * interpretation. Handles multi-band images and adjusts lookup tables for mismatched bands.
 *
 * @see DataBufferByte
 * @see DataBufferUShort
 * @see DataBufferShort
 */
public final class LookupTableCV {

  public static final String NULL_DATA_ARRAY_MESSAGE = "Data array must not be null";

  private final int[] offsets;
  private final DataBuffer data;
  private final boolean forceReadingUnsigned;

  public LookupTableCV(byte[] data) {
    this(data, 0, false);
  }

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
    Objects.requireNonNull(data, NULL_DATA_ARRAY_MESSAGE);
    if (data.length == 0) {
      throw new IllegalArgumentException("Data array must not be empty");
    }
    this.offsets = new int[] {offset};
    this.data = new DataBufferByte(data, data.length);
    this.forceReadingUnsigned = forceReadingUnsigned;
  }

  public LookupTableCV(byte[][] data) {
    this(data, new int[data.length], false);
  }

  public LookupTableCV(byte[][] data, int offset) {
    this(data, createOffsets(data.length, offset), false);
  }

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
    validateByteArrayInput(data, offsets);
    this.offsets = Arrays.copyOf(offsets, data.length);
    this.data = new DataBufferByte(data, data[0].length);
    this.forceReadingUnsigned = forceReadingUnsigned;
  }

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
    Objects.requireNonNull(data, NULL_DATA_ARRAY_MESSAGE);
    if (data.length == 0) {
      throw new IllegalArgumentException("Data array must not be empty");
    }
    this.offsets = new int[] {offset};
    this.data =
        isUShort ? new DataBufferUShort(data, data.length) : new DataBufferShort(data, data.length);
    this.forceReadingUnsigned = forceReadingUnsigned;
  }

  private static void validateByteArrayInput(byte[][] data, int[] offsets) {
    Objects.requireNonNull(data, NULL_DATA_ARRAY_MESSAGE);
    Objects.requireNonNull(offsets, "Offsets array must not be null");

    if (data.length == 0 || data[0].length == 0) {
      throw new IllegalArgumentException("Data array must not be empty");
    }
    if (offsets.length != data.length) {
      throw new IllegalArgumentException("Offsets array must match the number of bands");
    }
  }

  private static int[] createOffsets(int length, int offset) {
    int[] offsets = new int[length];
    Arrays.fill(offsets, offset);
    return offsets;
  }

  public DataBuffer getData() {
    return data;
  }

  public byte[][] getByteData() {
    if (data instanceof DataBufferByte buffer) {
      return buffer.getBankData();
    }
    return null;
  }

  public byte[] getByteData(int band) {
    if (data instanceof DataBufferByte buffer) {
      return buffer.getData(band);
    }
    return null;
  }

  public short[][] getShortData() {
    if (data instanceof DataBufferUShort bufferUShort) {
      return bufferUShort.getBankData();
    } else if (data instanceof DataBufferShort bufferShort) {
      return bufferShort.getBankData();
    }
    return null;
  }

  public short[] getShortData(int band) {
    if (data instanceof DataBufferUShort bufferUShort) {
      return bufferUShort.getData(band);
    } else if (data instanceof DataBufferShort bufferShort) {
      return bufferShort.getData(band);
    }
    return null;
  }

  public int[] getOffsets() {
    return Arrays.copyOf(offsets, offsets.length);
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

  /**
   * Applies this lookup table to the source image.
   *
   * @param src source image matrix
   * @return transformed image
   */
  public ImageCV lookup(Mat src) {
    Objects.requireNonNull(src, "Source Mat cannot be null.");

    int width = src.width();
    int height = src.height();
    int cvType = src.type();
    int channels = CvType.channels(cvType);
    int srcDataType = ImageConversion.convertToDataType(cvType);

    Object sourceData = extractSourceData(src, width, height, channels, cvType);

    LutContext context = prepareLutContext(channels);

    if (context.isTableDataByte()) {
      return applyByteLookup(srcDataType, width, height, context, sourceData);
    } else {
      return applyShortLookup(srcDataType, width, height, context, sourceData);
    }
  }

  private Object extractSourceData(Mat src, int width, int height, int channels, int cvType) {
    int depth = CvType.depth(cvType);
    int size = width * height * channels;

    if (depth == CvType.CV_8U || depth == CvType.CV_8S) {
      byte[] byteData = new byte[size];
      src.get(0, 0, byteData);
      return byteData;
    } else if (depth == CvType.CV_16U || depth == CvType.CV_16S) {
      short[] shortData = new short[size];
      src.get(0, 0, shortData);
      return shortData;
    } else {
      throw new IllegalArgumentException(
          "Unsupported dataType for LUT transformation: " + CvType.typeToString(cvType));
    }
  }

  private LutContext prepareLutContext(int channels) {
    int numBands = getNumBands();
    int[] tblOffsets = getOffsets();
    byte[][] bTblData = getByteData();
    short[][] sTblData = getShortData();

    if (numBands < channels) {
      if (bTblData != null) {
        bTblData = expandToChannels(bTblData[0], channels);
      } else if (sTblData != null) {
        sTblData = expandToChannels(sTblData[0], channels);
      }
      tblOffsets = createOffsets(channels, tblOffsets[0]);
      numBands = channels;
    }

    return new LutContext(numBands, channels, tblOffsets, bTblData, sTblData);
  }

  private static byte[][] expandToChannels(byte[] bandData, int channels) {
    byte[][] expanded = new byte[channels][];
    Arrays.fill(expanded, bandData);
    return expanded;
  }

  private static short[][] expandToChannels(short[] bandData, int channels) {
    short[][] expanded = new short[channels][];
    Arrays.fill(expanded, bandData);
    return expanded;
  }

  private ImageCV applyByteLookup(
      int srcDataType, int width, int height, LutContext ctx, Object sourceData) {

    boolean isSourceByte = srcDataType == DataBuffer.TYPE_BYTE;
    byte[] dstData =
        isSourceByte && ctx.channels >= ctx.numBands
            ? (byte[]) sourceData
            : new byte[width * height * ctx.numBands];

    if (srcDataType == DataBuffer.TYPE_BYTE) {
      lookupByteToByte((byte[]) sourceData, dstData, ctx);
    } else if (srcDataType == DataBuffer.TYPE_USHORT) {
      lookupShortToByte((short[]) sourceData, dstData, ctx, 0xFFFF);
    } else if (srcDataType == DataBuffer.TYPE_SHORT) {
      int mask = forceReadingUnsigned ? 0xFFFF : 0xFFFFFFFF;
      lookupShortToByte((short[]) sourceData, dstData, ctx, mask);
    } else {
      throw new IllegalArgumentException(
          "Unsupported LUT conversion from source dataType: " + srcDataType);
    }

    ImageCV dst = new ImageCV(height, width, CvType.CV_8UC(ctx.numBands));
    dst.put(0, 0, dstData);
    return dst;
  }

  private ImageCV applyShortLookup(
      int srcDataType, int width, int height, LutContext ctx, Object sourceData) {

    boolean isSourceByte = srcDataType == DataBuffer.TYPE_BYTE;
    short[] dstData =
        !isSourceByte && ctx.channels >= ctx.numBands
            ? (short[]) sourceData
            : new short[width * height * ctx.numBands];

    if (srcDataType == DataBuffer.TYPE_BYTE) {
      lookupByteToShort((byte[]) sourceData, dstData, ctx);
    } else if (srcDataType == DataBuffer.TYPE_USHORT) {
      lookupShortToShort((short[]) sourceData, dstData, ctx, 0xFFFF);
    } else if (srcDataType == DataBuffer.TYPE_SHORT) {
      int mask = forceReadingUnsigned ? 0xFFFF : 0xFFFFFFFF;
      lookupShortToShort((short[]) sourceData, dstData, ctx, mask);
    } else {
      throw new IllegalArgumentException(
          "Unsupported LUT conversion from source dataType: " + srcDataType);
    }

    int cvType =
        getDataType() == DataBuffer.TYPE_USHORT
            ? CvType.CV_16UC(ctx.channels)
            : CvType.CV_16SC(ctx.channels);
    ImageCV dst = new ImageCV(height, width, cvType);
    dst.put(0, 0, dstData);
    return dst;
  }

  private static int clampIndex(int pixel, int offset, int maxLength) {
    return Math.max(0, Math.min(pixel - offset, maxLength));
  }

  private void lookupByteToByte(byte[] srcData, byte[] dstData, LutContext ctx) {
    if (srcData.length < dstData.length) {
      expandChannelsByteToByte(srcData, dstData, ctx);
    } else {
      processPerBandByteToByte(srcData, dstData, ctx.numBands, ctx.byteData, ctx.offsets);
    }
  }

  private void lookupShortToByte(short[] srcData, byte[] dstData, LutContext ctx, int mask) {
    if (srcData.length < dstData.length) {
      expandChannelsShortToByte(srcData, dstData, ctx, mask);
    } else {
      processPerBandShortToByte(srcData, dstData, ctx.numBands, ctx.byteData, ctx.offsets, mask);
    }
  }

  private void lookupByteToShort(byte[] srcData, short[] dstData, LutContext ctx) {
    if (srcData.length < dstData.length) {
      expandChannelsByteToShort(srcData, dstData, ctx);
    } else {
      processPerBandByteToShort(srcData, dstData, ctx.numBands, ctx.shortData, ctx.offsets);
    }
  }

  private void lookupShortToShort(short[] srcData, short[] dstData, LutContext ctx, int mask) {
    if (srcData.length < dstData.length) {
      expandChannelsShortToShort(srcData, dstData, ctx, mask);
    } else {
      processPerBandShortToShort(srcData, dstData, ctx.numBands, ctx.shortData, ctx.offsets, mask);
    }
  }

  // Expand channels methods
  private void expandChannelsByteToByte(byte[] srcData, byte[] dstData, LutContext ctx) {
    for (int i = 0; i < srcData.length; i++) {
      int pixel = srcData[i] & 0xFF;
      for (int b = 0; b < ctx.numBands; b++) {
        int index = clampIndex(pixel, ctx.offsets[b], ctx.byteData[b].length - 1);
        dstData[i * ctx.numBands + b] = ctx.byteData[b][index];
      }
    }
  }

  private void expandChannelsShortToByte(
      short[] srcData, byte[] dstData, LutContext ctx, int mask) {
    for (int i = 0; i < srcData.length; i++) {
      int pixel = srcData[i] & mask;
      for (int b = 0; b < ctx.numBands; b++) {
        int index = clampIndex(pixel, ctx.offsets[b], ctx.byteData[b].length - 1);
        dstData[i * ctx.numBands + b] = ctx.byteData[b][index];
      }
    }
  }

  private void expandChannelsByteToShort(byte[] srcData, short[] dstData, LutContext ctx) {
    for (int i = 0; i < srcData.length; i++) {
      int pixel = srcData[i] & 0xFF;
      for (int b = 0; b < ctx.numBands; b++) {
        int index = clampIndex(pixel, ctx.offsets[b], ctx.shortData[b].length - 1);
        dstData[i * ctx.numBands + b] = ctx.shortData[b][index];
      }
    }
  }

  private void expandChannelsShortToShort(
      short[] srcData, short[] dstData, LutContext ctx, int mask) {
    for (int i = 0; i < srcData.length; i++) {
      int pixel = srcData[i] & mask;
      for (int b = 0; b < ctx.numBands; b++) {
        int index = clampIndex(pixel, ctx.offsets[b], ctx.shortData[b].length - 1);
        dstData[i * ctx.numBands + b] = ctx.shortData[b][index];
      }
    }
  }

  // Process per band methods
  private static void processPerBandByteToByte(
      byte[] srcData, byte[] dstData, int numBands, byte[][] tables, int[] offsets) {
    for (int b = 0; b < numBands; b++) {
      byte[] table = tables[b];
      int offset = offsets[b];
      int maxLength = table.length - 1;
      for (int i = b; i < srcData.length; i += numBands) {
        dstData[i] = table[clampIndex(srcData[i] & 0xFF, offset, maxLength)];
      }
    }
  }

  private static void processPerBandShortToByte(
      short[] srcData, byte[] dstData, int numBands, byte[][] tables, int[] offsets, int mask) {
    for (int b = 0; b < numBands; b++) {
      byte[] table = tables[b];
      int offset = offsets[b];
      int maxLength = table.length - 1;
      for (int i = b; i < srcData.length; i += numBands) {
        dstData[i] = table[clampIndex(srcData[i] & mask, offset, maxLength)];
      }
    }
  }

  private static void processPerBandByteToShort(
      byte[] srcData, short[] dstData, int numBands, short[][] tables, int[] offsets) {
    for (int b = 0; b < numBands; b++) {
      short[] table = tables[b];
      int offset = offsets[b];
      int maxLength = table.length - 1;
      for (int i = b; i < srcData.length; i += numBands) {
        dstData[i] = table[clampIndex(srcData[i] & 0xFF, offset, maxLength)];
      }
    }
  }

  private static void processPerBandShortToShort(
      short[] srcData, short[] dstData, int numBands, short[][] tables, int[] offsets, int mask) {
    for (int b = 0; b < numBands; b++) {
      short[] table = tables[b];
      int offset = offsets[b];
      int maxLength = table.length - 1;
      for (int i = b; i < srcData.length; i += numBands) {
        dstData[i] = table[clampIndex(srcData[i] & mask, offset, maxLength)];
      }
    }
  }

  private record LutContext(
      int numBands, int channels, int[] offsets, byte[][] byteData, short[][] shortData) {

    boolean isTableDataByte() {
      return byteData != null;
    }

    @Override
    public boolean equals(Object o) {
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      LutContext that = (LutContext) o;
      return numBands() == that.numBands()
          && channels() == that.channels()
          && Objects.deepEquals(offsets(), that.offsets())
          && Objects.deepEquals(byteData(), that.byteData())
          && Objects.deepEquals(shortData(), that.shortData());
    }

    @Override
    public int hashCode() {
      return Objects.hash(
          numBands(),
          channels(),
          Arrays.hashCode(offsets()),
          Arrays.deepHashCode(byteData()),
          Arrays.deepHashCode(shortData()));
    }

    @Override
    public String toString() {
      return "LutContext{"
          + "numBands="
          + numBands
          + ", channels="
          + channels
          + ", offsets="
          + Arrays.toString(offsets)
          + ", byteData="
          + Arrays.deepToString(byteData)
          + ", shortData="
          + Arrays.deepToString(shortData)
          + '}';
    }
  }
}
