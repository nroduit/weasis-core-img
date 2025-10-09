/*
 * Copyright (c) 2020 Weasis Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.weasis.opencv.op;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.ComponentSampleModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferDouble;
import java.awt.image.DataBufferFloat;
import java.awt.image.DataBufferInt;
import java.awt.image.DataBufferShort;
import java.awt.image.DataBufferUShort;
import java.awt.image.MultiPixelPackedSampleModel;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;
import org.weasis.opencv.data.ImageCV;
import org.weasis.opencv.data.PlanarImage;

/**
 * Utility class for converting between OpenCV Mat objects and Java BufferedImage objects. Provides
 * comprehensive conversion methods supporting various data types, color spaces, and image formats
 * commonly used in medical imaging applications.
 *
 * <p>Key features:
 *
 * <ul>
 *   <li>Bidirectional conversion between Mat and BufferedImage
 *   <li>Support for grayscale and RGB color spaces
 *   <li>Handles multiple data types (byte, short, int, float, double)
 *   <li>Binary image processing capabilities
 *   <li>Memory management utilities
 * </ul>
 *
 * @author Weasis Team
 * @since 1.0
 */
public final class ImageConversion {

  private ImageConversion() {
    // Utility class - prevent instantiation
  }

  // Channel band offsets for color conversions
  private static final int[] RGB_OFFSETS = {0, 1, 2};
  private static final int[] BGR_OFFSETS = {2, 1, 0};
  private static final int[] BGR_RASTER_OFFSETS = {2, 1, 0};
  private static final int[] BANDED_RGB_OFFSETS = {0, 0, 0};

  // Data type mapping from OpenCV to Java AWT
  private static final Map<Integer, Integer> CV_TO_DATABUFFER_TYPE =
      Map.of(
          CvType.CV_8U, DataBuffer.TYPE_BYTE,
          CvType.CV_8S, DataBuffer.TYPE_BYTE,
          CvType.CV_16U, DataBuffer.TYPE_USHORT,
          CvType.CV_16S, DataBuffer.TYPE_SHORT,
          CvType.CV_32S, DataBuffer.TYPE_INT,
          CvType.CV_32F, DataBuffer.TYPE_FLOAT,
          CvType.CV_64F, DataBuffer.TYPE_DOUBLE);

  /**
   * Converts an OpenCV Mat object to a Java BufferedImage.
   *
   * <p>Handles the complete conversion process including extracting matrix properties, creating
   * appropriate ColorModel and WritableRaster, and copying pixel data.
   *
   * <p>Supported formats:
   *
   * <ul>
   *   <li>Single-channel grayscale images (CV_8UC1, CV_16UC1, etc.)
   *   <li>Three-channel RGB images (CV_8UC3, CV_16UC3, etc.)
   *   <li>All OpenCV data types: 8U, 8S, 16U, 16S, 32S, 32F, 64F
   * </ul>
   *
   * @param matrix the OpenCV Mat object to convert, may be null
   * @return a BufferedImage representation of the Mat, or null if input is null
   * @throws UnsupportedOperationException if the Mat has an unsupported format
   */
  public static BufferedImage toBufferedImage(Mat matrix) {
    if (matrix == null) {
      return null;
    }

    int cols = matrix.cols();
    int rows = matrix.rows();
    int type = matrix.type();
    int channels = CvType.channels(type);
    int bpp = (CvType.ELEM_SIZE(type) * 8) / channels;

    int dataType = convertToDataType(type);

    ColorModel colorModel = createColorModel(channels, bpp, dataType);
    WritableRaster raster = createRaster(colorModel, channels, cols, rows, dataType);

    populateRasterFromMat(matrix, raster);

    return new BufferedImage(colorModel, raster, false, null);
  }

  /** Converts a PlanarImage to a BufferedImage by delegating to Mat conversion. */
  public static BufferedImage toBufferedImage(PlanarImage matrix) {
    return matrix == null ? null : toBufferedImage(matrix.toMat());
  }

  /**
   * Releases the native memory associated with an OpenCV Mat object. Important for preventing
   * memory leaks when working with large images.
   */
  public static void releaseMat(Mat mat) {
    if (mat != null) {
      mat.release();
    }
  }

  /** Releases the native memory associated with a PlanarImage object. */
  public static void releasePlanarImage(PlanarImage img) {
    if (img != null) {
      img.release();
    }
  }

  /**
   * Converts an OpenCV data type constant to the corresponding Java AWT DataBuffer type.
   *
   * @param cvType the OpenCV CvType constant (e.g., CvType.CV_8UC3)
   * @return the corresponding DataBuffer type constant
   * @throws UnsupportedOperationException if the CvType is not supported
   */
  public static int convertToDataType(int cvType) {
    int depth = CvType.depth(cvType);
    Integer dataType = CV_TO_DATABUFFER_TYPE.get(depth);
    if (dataType == null) {
      throw new UnsupportedOperationException("Unsupported CvType value: " + cvType);
    }
    return dataType;
  }

  /** Converts a RenderedImage to an OpenCV Mat with default settings (full image, BGR format). */
  public static ImageCV toMat(RenderedImage img) {
    return toMat(img, null, true, false);
  }

  /** Converts a specific region of a RenderedImage to an OpenCV Mat with default BGR format. */
  public static ImageCV toMat(RenderedImage img, Rectangle region) {
    return toMat(img, region, true, false);
  }

  /** Converts a RenderedImage to an OpenCV Mat with specified color format. */
  public static ImageCV toMat(RenderedImage img, Rectangle region, boolean toBGR) {
    return toMat(img, region, toBGR, false);
  }

  /**
   * Converts a RenderedImage to an OpenCV Mat with full control over conversion parameters.
   *
   * <p>Main conversion method that handles all aspects of converting Java's RenderedImage format to
   * OpenCV's Mat format, including region-based conversion, color format conversion, and binary
   * image handling.
   *
   * @param img the RenderedImage to convert, must not be null
   * @param region the rectangular region to convert, null to convert entire image
   * @param toBGR true to ensure output is in BGR format (recommended for OpenCV)
   * @param forceShortType true to force unsigned short data to signed short format
   * @return an ImageCV object containing the converted image data, or null if conversion fails
   */
  public static ImageCV toMat(
      RenderedImage img, Rectangle region, boolean toBGR, boolean forceShortType) {
    Raster raster = region == null ? img.getData() : img.getData(region);
    if (isBinary(raster.getSampleModel())) {
      return createBinaryMat(raster);
    }
    DataBuffer buf = raster.getDataBuffer();
    int[] samples = raster.getSampleModel().getSampleSize();
    int[] offsets = getBandOffsets(raster);

    if (buf instanceof DataBufferByte bufferByte) {
      return processByteBuffer(bufferByte, raster, samples, offsets, toBGR);
    } else if (buf instanceof DataBufferUShort bufferUShort) {
      return processUShortBuffer(bufferUShort, raster, samples, forceShortType);
    } else if (buf instanceof DataBufferShort bufferShort) {
      return processShortBuffer(bufferShort, raster, samples);
    } else if (buf instanceof DataBufferInt bufferInt) {
      return processIntBuffer(bufferInt, raster, samples);
    } else if (buf instanceof DataBufferFloat bufferFloat) {
      return processFloatBuffer(bufferFloat, raster, samples);
    } else if (buf instanceof DataBufferDouble bufferDouble) {
      return processDoubleBuffer(bufferDouble, raster, samples);
    } else {
      return null;
    }
  }

  /** Calculates and returns the bounding rectangle of a PlanarImage. */
  public static Rectangle getBounds(PlanarImage img) {
    return new Rectangle(0, 0, img.width(), img.height());
  }

  /**
   * Converts a RenderedImage to a BufferedImage with a specific image type. Uses Graphics2D
   * rendering for format conversion.
   */
  public static BufferedImage convertTo(RenderedImage src, int imageType) {
    var dst = new BufferedImage(src.getWidth(), src.getHeight(), imageType);
    Graphics2D graphics = dst.createGraphics();
    try {
      graphics.drawRenderedImage(src, AffineTransform.getTranslateInstance(0.0, 0.0));
    } finally {
      graphics.dispose();
    }
    return dst;
  }

  /** Determines if a SampleModel represents binary (1-bit per pixel) data. */
  public static boolean isBinary(SampleModel sm) {
    return sm instanceof MultiPixelPackedSampleModel model
        && model.getPixelBitStride() == 1
        && sm.getNumBands() == 1;
  }

  /**
   * Converts a RenderedImage to a BufferedImage, handling various input types. Returns null for
   * null input, same object if already BufferedImage, or creates new instance.
   */
  public static BufferedImage convertRenderedImage(RenderedImage img) {
    if (img == null) {
      return null;
    }
    if (img instanceof BufferedImage bufferedImage) {
      return bufferedImage;
    }
    ColorModel cm = img.getColorModel();
    int width = img.getWidth();
    int height = img.getHeight();
    WritableRaster raster = cm.createCompatibleWritableRaster(width, height);
    boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
    var properties = createImageProperties(img);
    var result = new BufferedImage(cm, raster, isAlphaPremultiplied, properties);
    img.copyData(raster);
    return result;
  }

  /**
   * Unpacks binary raster data into a standard byte array format. Converts packed binary data
   * (multiple pixels per byte) to one byte per pixel format.
   */
  public static byte[] getUnpackedBinaryData(Raster raster, Rectangle rect) {
    SampleModel sm = raster.getSampleModel();
    if (!isBinary(sm)) {
      throw new IllegalArgumentException("Not a binary raster!");
    }

    var mpp = (MultiPixelPackedSampleModel) sm;
    DataBuffer dataBuffer = raster.getDataBuffer();

    int dx = rect.x - raster.getSampleModelTranslateX();
    int dy = rect.y - raster.getSampleModelTranslateY();
    int lineStride = mpp.getScanlineStride();
    int eltOffset = dataBuffer.getOffset() + mpp.getOffset(dx, dy);
    int bitOffset = mpp.getBitOffset(dx);

    byte[] result = new byte[rect.width * rect.height];

    if (dataBuffer instanceof DataBufferByte buffer) {
      return unpackBinaryData(buffer.getData(), rect, eltOffset, bitOffset, lineStride, result, 8);
    } else if (dataBuffer instanceof DataBufferShort buffer) {
      return unpackBinaryData(buffer.getData(), rect, eltOffset, bitOffset, lineStride, result, 16);
    } else if (dataBuffer instanceof DataBufferUShort buffer) {
      return unpackBinaryData(buffer.getData(), rect, eltOffset, bitOffset, lineStride, result, 16);
    } else if (dataBuffer instanceof DataBufferInt buffer) {
      return unpackBinaryData(buffer.getData(), rect, eltOffset, bitOffset, lineStride, result, 32);
    } else {
      return result; // Return empty array for unsupported types
    }
  }

  // ============================== PRIVATE METHODS ==============================

  private static ColorModel createColorModel(int channels, int bpp, int dataType) {
    return switch (channels) {
      case 1 ->
          new ComponentColorModel(
              ColorSpace.getInstance(ColorSpace.CS_GRAY),
              new int[] {bpp},
              false,
              true,
              Transparency.OPAQUE,
              dataType);
      case 3 ->
          new ComponentColorModel(
              ColorSpace.getInstance(ColorSpace.CS_sRGB),
              new int[] {bpp, bpp, bpp},
              false,
              false,
              Transparency.OPAQUE,
              dataType);
      default ->
          throw new UnsupportedOperationException(
              "No implementation to handle " + channels + " channels");
    };
  }

  private static WritableRaster createRaster(
      ColorModel colorModel, int channels, int cols, int rows, int dataType) {
    return switch (channels) {
      case 1 -> colorModel.createCompatibleWritableRaster(cols, rows);
      case 3 ->
          Raster.createInterleavedRaster(
              dataType, cols, rows, cols * channels, channels, BGR_RASTER_OFFSETS, null);
      default ->
          throw new UnsupportedOperationException(
              "No implementation to handle " + channels + " channels");
    };
  }

  private static void populateRasterFromMat(Mat matrix, WritableRaster raster) {
    DataBuffer buf = raster.getDataBuffer();

    if (buf instanceof DataBufferByte bufferByte) {
      matrix.get(0, 0, bufferByte.getData());
    } else if (buf instanceof DataBufferUShort bufferUShort) {
      matrix.get(0, 0, bufferUShort.getData());
    } else if (buf instanceof DataBufferShort bufferShort) {
      matrix.get(0, 0, bufferShort.getData());
    } else if (buf instanceof DataBufferInt bufferInt) {
      matrix.get(0, 0, bufferInt.getData());
    } else if (buf instanceof DataBufferFloat bufferFloat) {
      matrix.get(0, 0, bufferFloat.getData());
    } else if (buf instanceof DataBufferDouble bufferDouble) {
      matrix.get(0, 0, bufferDouble.getData());
    } else {
      throw new UnsupportedOperationException(
          "Unsupported DataBuffer type: " + buf.getClass().getSimpleName());
    }
  }

  private static ImageCV createBinaryMat(Raster raster) {
    var mat = new ImageCV(raster.getHeight(), raster.getWidth(), CvType.CV_8UC1);
    mat.put(0, 0, getUnpackedBinaryData(raster, raster.getBounds()));
    return mat;
  }

  private static int[] getBandOffsets(Raster raster) {
    SampleModel sampleModel = raster.getSampleModel();
    if (sampleModel instanceof ComponentSampleModel model) {
      return model.getBandOffsets();
    }

    int[] samples = sampleModel.getSampleSize();
    int[] offsets = new int[samples.length];
    Arrays.setAll(offsets, i -> i);
    return offsets;
  }

  private static ImageCV processByteBuffer(
      DataBufferByte bufferByte, Raster raster, int[] samples, int[] offsets, boolean toBGR) {
    if (Arrays.equals(offsets, BANDED_RGB_OFFSETS)) {
      return createBandedRGBMat(bufferByte, raster, toBGR);
    }
    var mat = new ImageCV(raster.getHeight(), raster.getWidth(), CvType.CV_8UC(samples.length));
    mat.put(0, 0, bufferByte.getData());
    return applyColorConversion(mat, offsets, toBGR);
  }

  private static ImageCV createBandedRGBMat(
      DataBufferByte bufferByte, Raster raster, boolean toBGR) {
    var b = new Mat(raster.getHeight(), raster.getWidth(), CvType.CV_8UC1);
    b.put(0, 0, bufferByte.getData(2));
    var g = new Mat(raster.getHeight(), raster.getWidth(), CvType.CV_8UC1);
    g.put(0, 0, bufferByte.getData(1));
    var r = new ImageCV(raster.getHeight(), raster.getWidth(), CvType.CV_8UC1);
    r.put(0, 0, bufferByte.getData(0));

    List<Mat> channels = toBGR ? List.of(b, g, r) : List.of(r, g, b);
    var result = new ImageCV(raster.getHeight(), raster.getWidth(), CvType.CV_8UC3);
    Core.merge(channels, result);
    return result;
  }

  private static ImageCV applyColorConversion(ImageCV mat, int[] offsets, boolean toBGR) {
    if (toBGR && Arrays.equals(offsets, RGB_OFFSETS)) {
      var result = new ImageCV();
      Imgproc.cvtColor(mat, result, Imgproc.COLOR_RGB2BGR);
      return result;
    } else if (!toBGR && Arrays.equals(offsets, BGR_OFFSETS)) {
      var result = new ImageCV();
      Imgproc.cvtColor(mat, result, Imgproc.COLOR_BGR2RGB);
      return result;
    }
    return mat;
  }

  private static ImageCV processUShortBuffer(
      DataBufferUShort bufferUShort, Raster raster, int[] samples, boolean forceShortType) {
    int cvType = forceShortType ? CvType.CV_16SC(samples.length) : CvType.CV_16UC(samples.length);
    var mat = new ImageCV(raster.getHeight(), raster.getWidth(), cvType);
    mat.put(0, 0, bufferUShort.getData());
    return mat;
  }

  private static ImageCV processShortBuffer(
      DataBufferShort bufferShort, Raster raster, int[] samples) {
    var mat = new ImageCV(raster.getHeight(), raster.getWidth(), CvType.CV_16SC(samples.length));
    mat.put(0, 0, bufferShort.getData());
    return mat;
  }

  private static ImageCV processIntBuffer(DataBufferInt bufferInt, Raster raster, int[] samples) {
    var mat = new ImageCV(raster.getHeight(), raster.getWidth(), CvType.CV_32SC(samples.length));
    mat.put(0, 0, bufferInt.getData());
    return mat;
  }

  private static ImageCV processFloatBuffer(
      DataBufferFloat bufferFloat, Raster raster, int[] samples) {
    var mat = new ImageCV(raster.getHeight(), raster.getWidth(), CvType.CV_32FC(samples.length));
    mat.put(0, 0, bufferFloat.getData());
    return mat;
  }

  private static ImageCV processDoubleBuffer(
      DataBufferDouble bufferDouble, Raster raster, int[] samples) {
    var mat = new ImageCV(raster.getHeight(), raster.getWidth(), CvType.CV_64FC(samples.length));
    mat.put(0, 0, bufferDouble.getData());
    return mat;
  }

  private static Hashtable<String, Object> createImageProperties(RenderedImage img) {
    var properties = new Hashtable<String, Object>();
    String[] keys = img.getPropertyNames();
    if (keys != null) {
      Arrays.stream(keys).forEach(key -> properties.put(key, img.getProperty(key)));
    }
    return properties;
  }

  /** Generic method to unpack binary data from different array types. */
  private static <T> byte[] unpackBinaryData(
      T data,
      Rectangle rect,
      int eltOffset,
      int bitOffset,
      int lineStride,
      byte[] result,
      int bitsPerElement) {
    int k = 0;
    int maxY = rect.y + rect.height;
    int maxX = rect.x + rect.width;

    for (int y = rect.y; y < maxY; y++) {
      int bOffset = eltOffset * bitsPerElement + bitOffset;
      for (int x = rect.x; x < maxX; x++) {
        int value = extractBitValue(data, bOffset, bitsPerElement);
        result[k++] = (byte) (value & 0x1);
        bOffset++;
      }
      eltOffset += lineStride;
    }
    return result;
  }

  private static <T> int extractBitValue(T data, int bOffset, int bitsPerElement) {
    int elementIndex = bOffset / bitsPerElement;
    int bitIndex = bOffset % bitsPerElement;

    if (data instanceof byte[] bytes) {
      return bytes[elementIndex] >>> (7 - bitIndex);
    } else if (data instanceof short[] shorts) {
      return shorts[elementIndex] >>> (15 - bitIndex);
    } else if (data instanceof int[] ints) {
      return ints[elementIndex] >>> (31 - bitIndex);
    }
    return 0;
  }
}
