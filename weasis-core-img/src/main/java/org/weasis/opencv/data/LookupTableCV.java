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
import org.weasis.core.util.MathUtil;
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
    Objects.requireNonNull(data, "Data array must not be null");
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
    this(data, createFilledArray(data.length, offset), false);
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
    Objects.requireNonNull(data, "Data array must not be null");
    this.offsets = new int[] {offset};
    this.data =
        isUShort ? new DataBufferUShort(data, data.length) : new DataBufferShort(data, data.length);
    this.forceReadingUnsigned = forceReadingUnsigned;
  }

  private static void validateByteArrayInput(byte[][] data, int[] offsets) {
    Objects.requireNonNull(data, "Data array must not be null");
    Objects.requireNonNull(offsets, "Offsets array must not be null");

    if (data.length == 0 || data[0].length == 0) {
      throw new IllegalArgumentException("Data array must not be empty");
    }
    if (offsets.length != data.length) {
      throw new IllegalArgumentException("Offsets array must match the number of bands");
    }
  }

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
    return offsets.clone();
  }

  public int getOffset() {
    return offsets[0];
  }

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
    Objects.requireNonNull(src, "Source Mat cannot be null");

    var imageInfo = extractImageInfo(src);
    var sourceData = extractSourceData(src, imageInfo);
    var lookupContext = createLookupContext(imageInfo.channels());

    return performLookup(imageInfo, sourceData, lookupContext);
  }

  /** Encapsulates image properties for easier handling. */
  private record ImageInfo(int width, int height, int channels, int cvType, int srcDataType) {}

  /** Encapsulates lookup table context for processing. */
  private record LookupContext(
      int numBands, int[] offsets, byte[][] byteData, short[][] shortData) {}

  private ImageInfo extractImageInfo(Mat src) {
    int width = src.width();
    int height = src.height();
    int cvType = src.type();
    int channels = CvType.channels(cvType);
    int srcDataType = ImageConversion.convertToDataType(cvType);

    return new ImageInfo(width, height, channels, cvType, srcDataType);
  }

  private Object extractSourceData(Mat src, ImageInfo imageInfo) {
    int totalPixels = imageInfo.width() * imageInfo.height() * imageInfo.channels();

    return switch (CvType.depth(imageInfo.cvType())) {
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
              "Unsupported data type for LUT transformation: " + imageInfo.cvType());
    };
  }

  private LookupContext createLookupContext(int channels) {
    int numBands = getNumBands();
    int[] contextOffsets = getOffsets();
    byte[][] byteData = getByteData();
    short[][] shortData = getShortData();

    if (numBands < channels) {
      if (byteData != null) {
        byteData = expandByteData(byteData, channels);
      } else if (shortData != null) {
        shortData = expandShortData(shortData, channels);
      }
      contextOffsets = createFilledArray(channels, contextOffsets[0]);
      numBands = channels;
    }

    return new LookupContext(numBands, contextOffsets, byteData, shortData);
  }

  private byte[][] expandByteData(byte[][] originalData, int channels) {
    byte[][] expandedData = new byte[channels][];
    Arrays.fill(expandedData, originalData[0]);
    return expandedData;
  }

  private short[][] expandShortData(short[][] originalData, int channels) {
    short[][] expandedData = new short[channels][];
    Arrays.fill(expandedData, originalData[0]);
    return expandedData;
  }

  private ImageCV performLookup(ImageInfo imageInfo, Object sourceData, LookupContext context) {
    return switch (getDataType()) {
      case DataBuffer.TYPE_BYTE -> performByteLookup(imageInfo, sourceData, context);
      case DataBuffer.TYPE_USHORT, DataBuffer.TYPE_SHORT ->
          performShortLookup(imageInfo, sourceData, context);
      default -> throw new IllegalArgumentException("Unsupported LUT transformation data type");
    };
  }

  private ImageCV performByteLookup(ImageInfo imageInfo, Object sourceData, LookupContext context) {
    boolean canReuseSource =
        imageInfo.srcDataType() == DataBuffer.TYPE_BYTE
            && imageInfo.channels() >= context.numBands();

    byte[] dstData =
        canReuseSource
            ? (byte[]) sourceData
            : new byte[imageInfo.width() * imageInfo.height() * context.numBands()];

    applyByteLookup(imageInfo.srcDataType(), sourceData, dstData, context);

    var result =
        new ImageCV(imageInfo.height(), imageInfo.width(), CvType.CV_8UC(context.numBands()));
    result.put(0, 0, dstData);
    return result;
  }

  private ImageCV performShortLookup(
      ImageInfo imageInfo, Object sourceData, LookupContext context) {
    boolean canReuseSource =
        imageInfo.srcDataType() != DataBuffer.TYPE_BYTE
            && imageInfo.channels() >= context.numBands();

    short[] dstData =
        canReuseSource
            ? (short[]) sourceData
            : new short[imageInfo.width() * imageInfo.height() * context.numBands()];

    applyShortLookup(imageInfo.srcDataType(), sourceData, dstData, context);

    int outputType =
        getDataType() == DataBuffer.TYPE_USHORT
            ? CvType.CV_16UC(context.numBands())
            : CvType.CV_16SC(context.numBands());

    var result = new ImageCV(imageInfo.height(), imageInfo.width(), outputType);
    result.put(0, 0, dstData);
    return result;
  }

  private void applyByteLookup(
      int srcDataType, Object sourceData, byte[] dstData, LookupContext context) {
    switch (srcDataType) {
      case DataBuffer.TYPE_BYTE -> lookupByteToByte((byte[]) sourceData, dstData, context);
      case DataBuffer.TYPE_USHORT -> lookupUShortToByte((short[]) sourceData, dstData, context);
      case DataBuffer.TYPE_SHORT -> lookupShortToByte((short[]) sourceData, dstData, context);
      default ->
          throw new IllegalArgumentException(
              "Unsupported LUT conversion from source dataType " + srcDataType);
    }
  }

  private void applyShortLookup(
      int srcDataType, Object sourceData, short[] dstData, LookupContext context) {
    switch (srcDataType) {
      case DataBuffer.TYPE_BYTE -> lookupByteToShort((byte[]) sourceData, dstData, context);
      case DataBuffer.TYPE_USHORT -> lookupUShortToShort((short[]) sourceData, dstData, context);
      case DataBuffer.TYPE_SHORT -> lookupShortToShort((short[]) sourceData, dstData, context);
      default ->
          throw new IllegalArgumentException(
              "Unsupported LUT conversion from source dataType " + srcDataType);
    }
  }

  private static int clampIndex(int pixel, int offset, int maxIndex) {
    int val = pixel - offset;
    return MathUtil.clamp(val, 0, maxIndex);
  }

  private void lookupByteToByte(byte[] srcData, byte[] dstData, LookupContext context) {
    int srcLength = srcData.length;
    int numBands = context.numBands();
    int[] offsets = context.offsets();
    byte[][] tableData = context.byteData();

    for (int i = 0; i < srcLength; i++) {
      int band = i % numBands;
      int pixel = srcData[i] & 0xFF;
      int index = clampIndex(pixel, offsets[band], tableData[band].length - 1);
      dstData[i] = tableData[band][index];
    }
  }

  private void lookupUShortToByte(short[] srcData, byte[] dstData, LookupContext context) {
    int srcLength = srcData.length;
    int numBands = context.numBands();
    int[] offsets = context.offsets();
    byte[][] tableData = context.byteData();

    for (int i = 0; i < srcLength; i++) {
      int band = i % numBands;
      int pixel = srcData[i] & 0xFFFF;
      int index = clampIndex(pixel, offsets[band], tableData[band].length - 1);
      dstData[i] = tableData[band][index];
    }
  }

  private void lookupShortToByte(short[] srcData, byte[] dstData, LookupContext context) {
    int srcLength = srcData.length;
    int numBands = context.numBands();
    int[] offsets = context.offsets();
    byte[][] tableData = context.byteData();
    for (int i = 0; i < srcLength; i++) {
      int band = i % numBands;
      int pixel = forceReadingUnsigned ? (srcData[i] & 0xFFFF) : srcData[i];
      int index = clampIndex(pixel, offsets[band], tableData[band].length - 1);
      dstData[i] = tableData[band][index];
    }
  }

  private void lookupByteToShort(byte[] srcData, short[] dstData, LookupContext context) {
    int srcLength = srcData.length;
    int numBands = context.numBands();
    int[] offsets = context.offsets();
    short[][] tableData = context.shortData();

    for (int i = 0; i < srcLength; i++) {
      int band = i % numBands;
      int pixel = srcData[i] & 0xFF;
      int index = clampIndex(pixel, offsets[band], tableData[band].length - 1);
      dstData[i] = tableData[band][index];
    }
  }

  private void lookupUShortToShort(short[] srcData, short[] dstData, LookupContext context) {
    int srcLength = srcData.length;
    int numBands = context.numBands();
    int[] offsets = context.offsets();
    short[][] tableData = context.shortData();
    for (int i = 0; i < srcLength; i++) {
      int band = i % numBands;
      int pixel = srcData[i] & 0xFFFF;
      int index = clampIndex(pixel, offsets[band], tableData[band].length - 1);
      dstData[i] = tableData[band][index];
    }
  }

  private void lookupShortToShort(short[] srcData, short[] dstData, LookupContext context) {
    int srcLength = srcData.length;
    int numBands = context.numBands();
    int[] offsets = context.offsets();
    short[][] tableData = context.shortData();

    for (int i = 0; i < srcLength; i++) {
      int band = i % numBands;
      int pixel = forceReadingUnsigned ? (srcData[i] & 0xFFFF) : srcData[i];
      int index = clampIndex(pixel, offsets[band], tableData[band].length - 1);
      dstData[i] = tableData[band][index];
    }
  }
}
