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
   * Applies this lookup table to the source image. The table is applied band by band; a single-band
   * table is applied to every channel, and a multi-band table applied to a single-channel image
   * produces one channel per band.
   *
   * @param src source image matrix (8 or 16 bits)
   * @return transformed image
   * @throws IllegalArgumentException if the table and the image both have several bands, in
   *     different numbers
   */
  public ImageCV lookup(Mat src) {
    Objects.requireNonNull(src, "Source Mat cannot be null.");

    int width = src.width();
    int height = src.height();
    int channels = src.channels();
    int srcDataType = ImageConversion.convertToDataType(src.type());
    int mask = srcDataType == DataBuffer.TYPE_SHORT && !forceReadingUnsigned ? 0xFFFFFFFF : 0xFFFF;

    Object sourceData = extractSourceData(src, width * height * channels);
    LutContext context = prepareLutContext(channels);
    int pixels = width * height;

    if (context.byteData() != null) {
      byte[] dstData =
          sourceData instanceof byte[] bytes && channels == context.numBands()
              ? bytes
              : new byte[pixels * context.numBands()];
      if (sourceData instanceof byte[] bytes) {
        lookupByteToByte(bytes, dstData, context);
      } else {
        lookupShortToByte((short[]) sourceData, dstData, context, mask);
      }
      return toMat(height, width, CvType.CV_8UC(context.numBands()), dstData);
    }

    short[] dstData =
        sourceData instanceof short[] shorts && channels == context.numBands()
            ? shorts
            : new short[pixels * context.numBands()];
    if (sourceData instanceof byte[] bytes) {
      lookupByteToShort(bytes, dstData, context);
    } else {
      lookupShortToShort((short[]) sourceData, dstData, context, mask);
    }
    int depth = getDataType() == DataBuffer.TYPE_USHORT ? CvType.CV_16U : CvType.CV_16S;
    return toMat(height, width, CvType.makeType(depth, context.numBands()), dstData);
  }

  private static Object extractSourceData(Mat src, int size) {
    int depth = CvType.depth(src.type());
    if (depth == CvType.CV_8U || depth == CvType.CV_8S) {
      byte[] byteData = new byte[size];
      src.get(0, 0, byteData);
      return byteData;
    }
    if (depth == CvType.CV_16U || depth == CvType.CV_16S) {
      short[] shortData = new short[size];
      src.get(0, 0, shortData);
      return shortData;
    }
    throw new IllegalArgumentException(
        "Unsupported dataType for LUT transformation: " + CvType.typeToString(src.type()));
  }

  private static ImageCV toMat(int height, int width, int type, byte[] data) {
    var dst = new ImageCV(height, width, type);
    dst.put(0, 0, data);
    return dst;
  }

  private static ImageCV toMat(int height, int width, int type, short[] data) {
    var dst = new ImageCV(height, width, type);
    dst.put(0, 0, data);
    return dst;
  }

  // A table with fewer bands than the image channels is repeated for every channel
  private LutContext prepareLutContext(int channels) {
    int numBands = getNumBands();
    if (numBands != channels && numBands != 1 && channels != 1) {
      throw new IllegalArgumentException(
          "A " + numBands + "-band table cannot be applied to a " + channels + "-channel image");
    }
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
    return new LutContext(numBands, tblOffsets, bTblData, sTblData);
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

  private static int clampIndex(int pixel, int offset, int maxIndex) {
    return Math.max(0, Math.min(pixel - offset, maxIndex));
  }

  // The source has either one channel (read once per band) or one channel per band
  private static int sourceChannels(int srcLength, int dstLength, int numBands) {
    return srcLength < dstLength ? 1 : numBands;
  }

  private static void lookupByteToByte(byte[] src, byte[] dst, LutContext ctx) {
    int numBands = ctx.numBands();
    int srcChannels = sourceChannels(src.length, dst.length, numBands);
    for (int b = 0; b < numBands; b++) {
      byte[] table = ctx.byteData()[b];
      int offset = ctx.offsets()[b];
      int maxIndex = table.length - 1;
      for (int s = srcChannels == 1 ? 0 : b, d = b;
          d < dst.length;
          s += srcChannels, d += numBands) {
        dst[d] = table[clampIndex(src[s] & 0xFF, offset, maxIndex)];
      }
    }
  }

  private static void lookupShortToByte(short[] src, byte[] dst, LutContext ctx, int mask) {
    int numBands = ctx.numBands();
    int srcChannels = sourceChannels(src.length, dst.length, numBands);
    for (int b = 0; b < numBands; b++) {
      byte[] table = ctx.byteData()[b];
      int offset = ctx.offsets()[b];
      int maxIndex = table.length - 1;
      for (int s = srcChannels == 1 ? 0 : b, d = b;
          d < dst.length;
          s += srcChannels, d += numBands) {
        dst[d] = table[clampIndex(src[s] & mask, offset, maxIndex)];
      }
    }
  }

  private static void lookupByteToShort(byte[] src, short[] dst, LutContext ctx) {
    int numBands = ctx.numBands();
    int srcChannels = sourceChannels(src.length, dst.length, numBands);
    for (int b = 0; b < numBands; b++) {
      short[] table = ctx.shortData()[b];
      int offset = ctx.offsets()[b];
      int maxIndex = table.length - 1;
      for (int s = srcChannels == 1 ? 0 : b, d = b;
          d < dst.length;
          s += srcChannels, d += numBands) {
        dst[d] = table[clampIndex(src[s] & 0xFF, offset, maxIndex)];
      }
    }
  }

  private static void lookupShortToShort(short[] src, short[] dst, LutContext ctx, int mask) {
    int numBands = ctx.numBands();
    int srcChannels = sourceChannels(src.length, dst.length, numBands);
    for (int b = 0; b < numBands; b++) {
      short[] table = ctx.shortData()[b];
      int offset = ctx.offsets()[b];
      int maxIndex = table.length - 1;
      for (int s = srcChannels == 1 ? 0 : b, d = b;
          d < dst.length;
          s += srcChannels, d += numBands) {
        dst[d] = table[clampIndex(src[s] & mask, offset, maxIndex)];
      }
    }
  }

  private record LutContext(int numBands, int[] offsets, byte[][] byteData, short[][] shortData) {}
}
