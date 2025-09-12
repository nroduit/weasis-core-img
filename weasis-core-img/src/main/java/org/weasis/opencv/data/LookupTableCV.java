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
 * The {@code LookupTableCV} class defines a lookup table used for fast pixel value transformations
 * in image processing. It can be initialized with different types of data sources, including single
 * or multi-dimensional byte and short arrays, with optional offsets and signed/unsigned
 * interpretation flags. The lookup table data is stored in a {@link DataBuffer}.
 *
 * <p>This class supports querying various properties of the lookup table data, such as the number
 * of bands, data type, and entry count. It also facilitates performing lookups on image data in
 * both byte and short formats. The lookup operation can handle images with multiple bands and
 * adjusts the lookup table for mismatched bands if necessary.
 *
 * <p>Supported data types for storage include: - {@link DataBufferByte} - {@link DataBufferUShort}
 * - {@link DataBufferShort}
 *
 * <p>The class also provides utility functions to extract raw lookup table data and access offsets
 * for a specific band.
 */
public class LookupTableCV {

  private final int[] offsets;
  private final DataBuffer data;
  private final boolean forceReadingUnsigned;

  public LookupTableCV(byte[] data) {
    this(data, 0);
  }

  public LookupTableCV(byte[] data, int offset) {
    this(data, offset, false);
  }

  public LookupTableCV(byte[] data, int offset, boolean forceReadingUnsigned) {
    this.offsets = new int[1];
    Arrays.fill(offsets, offset);
    this.data = new DataBufferByte(Objects.requireNonNull(data), data.length);
    this.forceReadingUnsigned = forceReadingUnsigned;
  }

  public LookupTableCV(byte[][] data) {
    this(data, new int[data.length]);
  }

  public LookupTableCV(byte[][] data, int offset) {
    this(data, new int[data.length]);
    Arrays.fill(offsets, offset);
  }

  public LookupTableCV(byte[][] data, int[] offsets) {
    this(data, offsets, false);
  }

  public LookupTableCV(byte[][] data, int[] offsets, boolean forceReadingUnsigned) {
    this.offsets = Arrays.copyOf(offsets, data.length);
    this.data = new DataBufferByte(Objects.requireNonNull(data), data[0].length);
    this.forceReadingUnsigned = forceReadingUnsigned;
  }

  public LookupTableCV(short[] data, int offset, boolean isUShort) {
    this(data, offset, isUShort, false);
  }

  public LookupTableCV(short[] data, int offset, boolean isUShort, boolean forceReadingUnsigned) {
    this.offsets = new int[1];
    Arrays.fill(offsets, offset);
    if (isUShort) {
      this.data = new DataBufferUShort(Objects.requireNonNull(data), data.length);
    } else {
      this.data = new DataBufferShort(Objects.requireNonNull(data), data.length);
    }
    this.forceReadingUnsigned = forceReadingUnsigned;
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

    // Ensure table data matches source data
    int numBands = getNumBands();
    int[] tblOffsets = getOffsets();
    byte[][] bTblData = getByteData();
    short[][] sTblData = getShortData();

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
      tblOffsets = new int[channels];
      Arrays.fill(tblOffsets, firstOffset);
      numBands = channels;
    }

    // Process lookup based on the destination type
    int tblDataType = getDataType();

    if (tblDataType == DataBuffer.TYPE_BYTE) {
      return performByteLookup(
          srcDataType, width, height, numBands, channels, sourceData, bTblData, tblOffsets);
    } else if (tblDataType == DataBuffer.TYPE_USHORT || tblDataType == DataBuffer.TYPE_SHORT) {
      return performShortLookup(
          srcDataType, width, height, numBands, channels, sourceData, sTblData, tblOffsets);
    }
    throw new IllegalArgumentException("Unsupported LUT transformation.");
  }

  private Object initializeSourceData(Mat src, int width, int height, int channels, int cvType) {
    switch (CvType.depth(cvType)) {
      case CvType.CV_8U, CvType.CV_8S -> {
        byte[] byteData = new byte[width * height * channels];
        src.get(0, 0, byteData);
        return byteData;
      }
      case CvType.CV_16U, CvType.CV_16S -> {
        short[] shortData = new short[width * height * channels];
        src.get(0, 0, shortData);
        return shortData;
      }
      default ->
          throw new IllegalArgumentException(
              "Not supported dataType for LUT transformation: " + src);
    }
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

    if (isSourceByte) {
      lookup((byte[]) sourceData, dstData, tblOffsets, tblData);
    } else if (srcDataType == DataBuffer.TYPE_USHORT) {
      lookupU((short[]) sourceData, dstData, tblOffsets, tblData);
    } else if (srcDataType == DataBuffer.TYPE_SHORT) {
      lookup((short[]) sourceData, dstData, tblOffsets, tblData);
    } else {
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

    if (isSourceByte) {
      lookup((byte[]) sourceData, dstData, tblOffsets, tblData);
    } else if (srcDataType == DataBuffer.TYPE_USHORT) {
      lookupU((short[]) sourceData, dstData, tblOffsets, tblData);
    } else if (srcDataType == DataBuffer.TYPE_SHORT) {
      lookup((short[]) sourceData, dstData, tblOffsets, tblData);
    } else {
      throw new IllegalArgumentException(
          "Not supported LUT conversion from source dataType " + srcDataType);
    }

    ImageCV dst =
        new ImageCV(
            height,
            width,
            getDataType() == DataBuffer.TYPE_USHORT
                ? CvType.CV_16UC(channels)
                : CvType.CV_16SC(channels));
    dst.put(0, 0, dstData);
    return dst;
  }

  private static int index(int pixel, int offset, int length) {
    int val = pixel - offset;
    if (val < 0) {
      val = 0;
    } else if (val > length) {
      val = length;
    }
    return val;
  }

  // byte to byte
  private void lookup(byte[] srcData, byte[] dstData, int[] tblOffsets, byte[][] tblData) {
    int bOffset = tblData.length;

    if (srcData.length < dstData.length) {
      for (int i = 0; i < srcData.length; i++) {
        int val = (srcData[i] & 0xFF);
        for (int b = 0; b < bOffset; b++) {
          dstData[i * bOffset + b] = tblData[b][index(val, tblOffsets[b], tblData[b].length - 1)];
        }
      }
    } else {
      for (int b = 0; b < bOffset; b++) {
        byte[] t = tblData[b];
        int tblOffset = tblOffsets[b];
        int maxLength = t.length - 1;

        for (int i = b; i < srcData.length; i += bOffset) {
          dstData[i] = t[index((srcData[i] & 0xFF), tblOffset, maxLength)];
        }
      }
    }
  }

  // ushort to byte
  private void lookupU(short[] srcData, byte[] dstData, int[] tblOffsets, byte[][] tblData) {
    lookupByte(srcData, dstData, tblOffsets, tblData, 0xFFFF);
  }

  // short to byte
  private void lookup(short[] srcData, byte[] dstData, int[] tblOffsets, byte[][] tblData) {
    int mask = forceReadingUnsigned ? 0xFFFF : 0xFFFFFFFF;
    lookupByte(srcData, dstData, tblOffsets, tblData, mask);
  }

  private static void lookupByte(
      short[] srcData, byte[] dstData, int[] tblOffsets, byte[][] tblData, int mask) {
    int bOffset = tblData.length;
    if (srcData.length < dstData.length) {
      for (int i = 0; i < srcData.length; i++) {
        int val = srcData[i] & mask;
        for (int b = 0; b < bOffset; b++) {
          dstData[i * bOffset + b] = tblData[b][index(val, tblOffsets[b], tblData[b].length - 1)];
        }
      }
    } else {
      for (int b = 0; b < bOffset; b++) {
        byte[] t = tblData[b];
        int tblOffset = tblOffsets[b];
        int maxLength = t.length - 1;

        for (int i = b; i < srcData.length; i += bOffset) {
          dstData[i] = t[index((srcData[i] & mask), tblOffset, maxLength)];
        }
      }
    }
  }

  // byte to short or ushort
  private void lookup(byte[] srcData, short[] dstData, int[] tblOffsets, short[][] tblData) {
    int bOffset = tblData.length;
    if (srcData.length < dstData.length) {
      for (int i = 0; i < srcData.length; i++) {
        int val = (srcData[i] & 0xFF);
        for (int b = 0; b < bOffset; b++) {
          dstData[i * bOffset + b] = tblData[b][index(val, tblOffsets[b], tblData[b].length - 1)];
        }
      }
    } else {
      for (int b = 0; b < bOffset; b++) {
        short[] t = tblData[b];
        int tblOffset = tblOffsets[b];
        int maxLength = t.length - 1;

        for (int i = b; i < srcData.length; i += bOffset) {
          dstData[i] = t[index((srcData[i] & 0xFF), tblOffset, maxLength)];
        }
      }
    }
  }

  // ushort to short or ushort
  private void lookupU(short[] srcData, short[] dstData, int[] tblOffsets, short[][] tblData) {
    lookupShort(srcData, dstData, tblOffsets, tblData, 0xFFFF);
  }

  // short to short or ushort
  private void lookup(short[] srcData, short[] dstData, int[] tblOffsets, short[][] tblData) {
    int mask = forceReadingUnsigned ? 0xFFFF : 0xFFFFFFFF;
    lookupShort(srcData, dstData, tblOffsets, tblData, mask);
  }

  private static void lookupShort(
      short[] srcData, short[] dstData, int[] tblOffsets, short[][] tblData, int mask) {
    int bOffset = tblData.length;
    if (srcData.length < dstData.length) {
      for (int i = 0; i < srcData.length; i++) {
        int val = (srcData[i] & mask);
        for (int b = 0; b < bOffset; b++) {
          dstData[i * bOffset + b] = tblData[b][index(val, tblOffsets[b], tblData[b].length - 1)];
        }
      }
    } else {
      for (int b = 0; b < bOffset; b++) {
        short[] t = tblData[b];
        int tblOffset = tblOffsets[b];
        int maxLength = t.length - 1;

        for (int i = b; i < srcData.length; i += bOffset) {
          dstData[i] = t[index((srcData[i] & mask), tblOffset, maxLength)];
        }
      }
    }
  }
}
