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
 * Utility class for converting between OpenCV Mat objects and Java BufferedImage objects. This
 * class provides comprehensive conversion methods supporting various data types, color spaces, and
 * image formats commonly used in medical imaging applications.
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
public class ImageConversion {

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
   * <p>This method handles the complete conversion process including:
   *
   * <ul>
   *   <li>Extracting matrix properties (dimensions, channels, data type)
   *   <li>Creating appropriate ColorModel and WritableRaster
   *   <li>Copying pixel data from Mat to BufferedImage
   * </ul>
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
   * @throws UnsupportedOperationException if the Mat has an unsupported number of channels
   * @throws UnsupportedOperationException if the Mat has an unsupported data type
   * @see #toBufferedImage(PlanarImage)
   * @see #convertToDataType(int)
   */
  public static BufferedImage toBufferedImage(Mat matrix) {
    if (matrix == null) {
      return null;
    }

    int cols = matrix.cols();
    int rows = matrix.rows();
    int type = matrix.type();
    int elemSize = CvType.ELEM_SIZE(type);
    int channels = CvType.channels(type);
    int bpp = (elemSize * 8) / channels;

    int dataType = convertToDataType(type);

    ColorModel colorModel = createColorModel(channels, bpp, dataType);
    WritableRaster raster = createRaster(colorModel, channels, cols, rows, dataType);

    populateRasterFromMat(matrix, raster);

    return new BufferedImage(colorModel, raster, false, null);
  }

  /**
   * Creates an appropriate ColorModel based on the number of channels and data type.
   *
   * <p>This method determines the correct color space and creates a ComponentColorModel with the
   * proper configuration for the given image characteristics.
   *
   * @param channels the number of color channels (1 for grayscale, 3 for RGB)
   * @param bpp bits per pixel per channel
   * @param dataType the Java AWT DataBuffer type constant
   * @return a ColorModel suitable for the specified image format
   * @throws UnsupportedOperationException if the number of channels is not supported
   */
  private static ColorModel createColorModel(int channels, int bpp, int dataType) {
    switch (channels) {
      case 1 -> {
        ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_GRAY);
        return new ComponentColorModel(
            cs, new int[] {bpp}, false, true, Transparency.OPAQUE, dataType);
      }
      case 3 -> {
        ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_sRGB);
        return new ComponentColorModel(
            cs, new int[] {bpp, bpp, bpp}, false, false, Transparency.OPAQUE, dataType);
      }
      default ->
          throw new UnsupportedOperationException(
              "No implementation to handle " + channels + " channels");
    }
  }

  /**
   * Creates an appropriate WritableRaster based on the ColorModel and image specifications.
   *
   * <p>For RGB images, this method creates an interleaved raster with BGR band ordering to match
   * OpenCV's default color format. For grayscale images, it creates a compatible raster using the
   * ColorModel.
   *
   * @param colorModel the ColorModel to create a compatible raster for
   * @param channels the number of color channels
   * @param cols image width in pixels
   * @param rows image height in pixels
   * @param dataType the Java AWT DataBuffer type constant
   * @return a WritableRaster suitable for the specified image format
   * @throws UnsupportedOperationException if the number of channels is not supported
   */
  private static WritableRaster createRaster(
      ColorModel colorModel, int channels, int cols, int rows, int dataType) {
    switch (channels) {
      case 1 -> {
        return colorModel.createCompatibleWritableRaster(cols, rows);
      }
      case 3 -> {
        return Raster.createInterleavedRaster(
            dataType, cols, rows, cols * channels, channels, BGR_RASTER_OFFSETS, null);
      }
      default ->
          throw new UnsupportedOperationException(
              "No implementation to handle " + channels + " channels");
    }
  }

  /**
   * Populates a WritableRaster with pixel data from an OpenCV Mat.
   *
   * <p>This method uses Java 17 pattern matching in switch expressions to handle different
   * DataBuffer types safely and efficiently. It directly copies the raw pixel data from the Mat to
   * the appropriate typed array in the DataBuffer.
   *
   * @param matrix the source OpenCV Mat containing pixel data
   * @param raster the target WritableRaster to populate
   * @throws UnsupportedOperationException if the DataBuffer type is not supported
   */
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

  /**
   * Converts a PlanarImage to a BufferedImage by first converting to Mat.
   *
   * <p>This is a convenience method that delegates to the Mat conversion after extracting the
   * underlying Mat from the PlanarImage. It maintains the same conversion capabilities and format
   * support.
   *
   * @param matrix the PlanarImage to convert, may be null
   * @return a BufferedImage representation of the PlanarImage, or null if input is null
   * @throws UnsupportedOperationException if the underlying Mat format is not supported
   * @see #toBufferedImage(Mat)
   */
  public static BufferedImage toBufferedImage(PlanarImage matrix) {
    if (matrix == null) {
      return null;
    }
    return toBufferedImage(matrix.toMat());
  }

  /**
   * Releases the native memory associated with an OpenCV Mat object. Without calling this method,
   * the native memory will not be freed when the Mat object is garbage collected.
   *
   * <p>This method safely releases the native memory allocated by OpenCV for the Mat, while
   * preserving the Java object itself. It's important to call this method to prevent memory issue
   * when working with large images or many Mat objects.
   *
   * <p>The method is null-safe and can be called multiple times on the same Mat without adverse
   * effects.
   *
   * @param mat the Mat object to release, may be null
   * @see #releasePlanarImage(PlanarImage)
   */
  public static void releaseMat(Mat mat) {
    if (mat != null) {
      mat.release();
    }
  }

  /**
   * Releases the native memory associated with a PlanarImage object.
   *
   * @param img the PlanarImage object to release, may be null
   * @see #releaseMat(Mat)
   */
  public static void releasePlanarImage(PlanarImage img) {
    if (img != null) {
      img.release();
    }
  }

  /**
   * Converts an OpenCV data type constant to the corresponding Java AWT DataBuffer type.
   *
   * <p>This method provides a mapping between OpenCV's CvType constants and Java's DataBuffer type
   * constants, enabling proper creation of BufferedImage objects with the correct underlying data
   * representation.
   *
   * <p>Supported conversions:
   *
   * <ul>
   *   <li>CV_8U, CV_8S → TYPE_BYTE
   *   <li>CV_16U → TYPE_USHORT
   *   <li>CV_16S → TYPE_SHORT
   *   <li>CV_32S → TYPE_INT
   *   <li>CV_32F → TYPE_FLOAT
   *   <li>CV_64F → TYPE_DOUBLE
   * </ul>
   *
   * @param cvType the OpenCV CvType constant (e.g., CvType.CV_8UC3)
   * @return the corresponding DataBuffer type constant
   * @throws UnsupportedOperationException if the CvType is not supported
   * @see DataBuffer
   * @see CvType
   */
  public static int convertToDataType(int cvType) {
    int depth = CvType.depth(cvType);
    Integer dataType = CV_TO_DATABUFFER_TYPE.get(depth);
    if (dataType == null) {
      throw new UnsupportedOperationException("Unsupported CvType value: " + cvType);
    }
    return dataType;
  }

  /**
   * Converts a RenderedImage to an OpenCV Mat with default settings.
   *
   * <p>This convenience method uses default parameters:
   *
   * <ul>
   *   <li>No region restriction (full image)
   *   <li>BGR color format (toBGR = true)
   * </ul>
   *
   * @param img the RenderedImage to convert
   * @return an ImageCV object containing the converted image data
   * @see #toMat(RenderedImage, Rectangle, boolean, boolean)
   */
  public static ImageCV toMat(RenderedImage img) {
    return toMat(img, null);
  }

  /**
   * Converts a specific region of a RenderedImage to an OpenCV Mat with default BGR format.
   *
   * <p>This convenience method converts the specified region with BGR color format, which is the
   * default for most OpenCV operations.
   *
   * @param img the RenderedImage to convert
   * @param region the rectangular region to convert, null for entire image
   * @return an ImageCV object containing the converted image data
   * @see #toMat(RenderedImage, Rectangle, boolean, boolean)
   */
  public static ImageCV toMat(RenderedImage img, Rectangle region) {
    return toMat(img, region, true);
  }

  /**
   * Converts a RenderedImage to an OpenCV Mat with specified color format.
   *
   * <p>This method provides control over the output color format while using default settings for
   * other parameters.
   *
   * @param img the RenderedImage to convert
   * @param region the rectangular region to convert, null for entire image
   * @param toBGR true to convert to BGR format, false to keep original format
   * @return an ImageCV object containing the converted image data
   * @see #toMat(RenderedImage, Rectangle, boolean, boolean)
   */
  public static ImageCV toMat(RenderedImage img, Rectangle region, boolean toBGR) {
    return toMat(img, region, toBGR, false);
  }

  /**
   * Converts a RenderedImage to an OpenCV Mat with full control over conversion parameters.
   *
   * <p>This is the main conversion method that handles all aspects of converting Java's
   * RenderedImage format to OpenCV's Mat format. It supports:
   *
   * <ul>
   *   <li>Region-based conversion for processing image subsets
   *   <li>Color format conversion (RGB ↔ BGR)
   *   <li>Data type conversion and optimization
   *   <li>Binary image handling
   *   <li>Multiple DataBuffer types (byte, short, int, float, double)
   * </ul>
   *
   * <p>The method automatically detects and handles special cases:
   *
   * <ul>
   *   <li>Binary images (1-bit per pixel)
   *   <li>Banded vs. interleaved color layouts
   *   <li>Different band ordering configurations
   * </ul>
   *
   * @param img the RenderedImage to convert, must not be null
   * @param region the rectangular region to convert, null to convert entire image
   * @param toBGR true to ensure output is in BGR format (recommended for OpenCV), false to preserve
   *     original color ordering
   * @param forceShortType true to force unsigned short data to signed short format, false to
   *     preserve original data type (recommended)
   * @return an ImageCV object containing the converted image data, or null if conversion fails
   * @throws IllegalArgumentException if the raster is binary but binary handling fails
   * @see #createBinaryMat(Raster)
   * @see #getBandOffsets(Raster)
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

  /**
   * Creates an OpenCV Mat from binary raster data.
   *
   * <p>Binary images store multiple pixels per byte (1 bit per pixel) and require special handling
   * to unpack into OpenCV's standard 8-bit per pixel format. This method handles the unpacking
   * process and creates a grayscale Mat.
   *
   * @param raster the binary raster containing packed pixel data
   * @return an ImageCV object with unpacked binary data as CV_8UC1 format
   * @see #getUnpackedBinaryData(Raster, Rectangle)
   */
  private static ImageCV createBinaryMat(Raster raster) {
    ImageCV mat = new ImageCV(raster.getHeight(), raster.getWidth(), CvType.CV_8UC1);
    mat.put(0, 0, getUnpackedBinaryData(raster, raster.getBounds()));
    return mat;
  }

  /**
   * Extracts band offset information from a raster's sample model.
   *
   * <p>Band offsets determine how color channels are organized in memory. For ComponentSampleModel,
   * this information is directly available. For other sample models, sequential offsets are
   * assumed.
   *
   * @param raster the raster to analyze
   * @return an array of band offsets indicating color channel organization
   */
  private static int[] getBandOffsets(Raster raster) {
    SampleModel sampleModel = raster.getSampleModel();
    if (sampleModel instanceof ComponentSampleModel model) {
      return model.getBandOffsets();
    }

    int[] samples = sampleModel.getSampleSize();
    int[] offsets = new int[samples.length];
    for (int i = 0; i < offsets.length; i++) {
      offsets[i] = i;
    }
    return offsets;
  }

  private static ImageCV processByteBuffer(
      DataBufferByte bufferByte, Raster raster, int[] samples, int[] offsets, boolean toBGR) {
    if (Arrays.equals(offsets, BANDED_RGB_OFFSETS)) {
      return createBandedRGBMat(bufferByte, raster, toBGR);
    }

    ImageCV mat = new ImageCV(raster.getHeight(), raster.getWidth(), CvType.CV_8UC(samples.length));
    mat.put(0, 0, bufferByte.getData());

    return applyColorConversion(mat, offsets, toBGR);
  }

  private static ImageCV createBandedRGBMat(
      DataBufferByte bufferByte, Raster raster, boolean toBGR) {
    Mat b = new Mat(raster.getHeight(), raster.getWidth(), CvType.CV_8UC1);
    b.put(0, 0, bufferByte.getData(2));
    Mat g = new Mat(raster.getHeight(), raster.getWidth(), CvType.CV_8UC1);
    g.put(0, 0, bufferByte.getData(1));
    ImageCV r = new ImageCV(raster.getHeight(), raster.getWidth(), CvType.CV_8UC1);
    r.put(0, 0, bufferByte.getData(0));
    List<Mat> channels = toBGR ? Arrays.asList(b, g, r) : Arrays.asList(r, g, b);
    ImageCV result = new ImageCV(raster.getHeight(), raster.getWidth(), CvType.CV_8UC3);
    Core.merge(channels, result);
    return result;
  }

  private static ImageCV applyColorConversion(ImageCV mat, int[] offsets, boolean toBGR) {
    if (toBGR && Arrays.equals(offsets, RGB_OFFSETS)) {
      ImageCV result = new ImageCV();
      Imgproc.cvtColor(mat, result, Imgproc.COLOR_RGB2BGR);
      return result;
    } else if (!toBGR && Arrays.equals(offsets, BGR_OFFSETS)) {
      ImageCV result = new ImageCV();
      Imgproc.cvtColor(mat, result, Imgproc.COLOR_BGR2RGB);
      return result;
    }
    return mat;
  }

  private static ImageCV processUShortBuffer(
      DataBufferUShort bufferUShort, Raster raster, int[] samples, boolean forceShortType) {
    int cvType = forceShortType ? CvType.CV_16SC(samples.length) : CvType.CV_16UC(samples.length);
    ImageCV mat = new ImageCV(raster.getHeight(), raster.getWidth(), cvType);
    mat.put(0, 0, bufferUShort.getData());
    return mat;
  }

  private static ImageCV processShortBuffer(
      DataBufferShort bufferShort, Raster raster, int[] samples) {
    ImageCV mat =
        new ImageCV(raster.getHeight(), raster.getWidth(), CvType.CV_16SC(samples.length));
    mat.put(0, 0, bufferShort.getData());
    return mat;
  }

  private static ImageCV processIntBuffer(DataBufferInt bufferInt, Raster raster, int[] samples) {
    ImageCV mat =
        new ImageCV(raster.getHeight(), raster.getWidth(), CvType.CV_32SC(samples.length));
    mat.put(0, 0, bufferInt.getData());
    return mat;
  }

  private static ImageCV processFloatBuffer(
      DataBufferFloat bufferFloat, Raster raster, int[] samples) {
    ImageCV mat =
        new ImageCV(raster.getHeight(), raster.getWidth(), CvType.CV_32FC(samples.length));
    mat.put(0, 0, bufferFloat.getData());
    return mat;
  }

  private static ImageCV processDoubleBuffer(
      DataBufferDouble bufferDouble, Raster raster, int[] samples) {
    ImageCV mat =
        new ImageCV(raster.getHeight(), raster.getWidth(), CvType.CV_64FC(samples.length));
    mat.put(0, 0, bufferDouble.getData());
    return mat;
  }

  /**
   * Calculates and returns the bounding rectangle of a PlanarImage.
   *
   * <p>This utility method provides a convenient way to get the full bounds of a PlanarImage as a
   * Rectangle object, which is often needed for region-based operations and conversions.
   *
   * @param img the PlanarImage to get bounds for
   * @return a Rectangle representing the full image bounds (0, 0, width, height)
   * @see Rectangle
   */
  public static Rectangle getBounds(PlanarImage img) {
    return new Rectangle(0, 0, img.width(), img.height());
  }

  /**
   * Converts a RenderedImage to a BufferedImage with a specific image type.
   *
   * <p>This method creates a new BufferedImage with the specified type and renders the source image
   * into it. This is useful for format conversion, such as converting a complex image to a standard
   * RGB format.
   *
   * <p>The conversion is performed using Java's Graphics2D rendering system, which handles color
   * space conversions and data type transformations automatically.
   *
   * @param src the source RenderedImage to convert
   * @param imageType the target BufferedImage type constant (e.g., BufferedImage.TYPE_INT_RGB)
   * @return a new BufferedImage of the specified type containing the rendered source image
   * @see BufferedImage
   * @see Graphics2D
   */
  public static BufferedImage convertTo(RenderedImage src, int imageType) {
    BufferedImage dst = new BufferedImage(src.getWidth(), src.getHeight(), imageType);
    Graphics2D graphics = dst.createGraphics();
    try {
      graphics.drawRenderedImage(src, AffineTransform.getTranslateInstance(0.0, 0.0));
    } finally {
      graphics.dispose();
    }
    return dst;
  }

  /**
   * Determines if a SampleModel represents binary (1-bit per pixel) data.
   *
   * <p>Binary images use 1 bit per pixel and are typically stored in packed format with multiple
   * pixels per byte. This method identifies such images by checking:
   *
   * <ul>
   *   <li>The SampleModel is a MultiPixelPackedSampleModel
   *   <li>The pixel bit stride is exactly 1
   *   <li>There is exactly 1 band (grayscale)
   * </ul>
   *
   * @param sm the SampleModel to test
   * @return true if the SampleModel represents binary data, false otherwise
   * @see MultiPixelPackedSampleModel
   */
  public static boolean isBinary(SampleModel sm) {
    return sm instanceof MultiPixelPackedSampleModel model
        && model.getPixelBitStride() == 1
        && sm.getNumBands() == 1;
  }

  /**
   * Converts a RenderedImage to a BufferedImage, handling various input types.
   *
   * <p>This method provides a robust conversion that:
   *
   * <ul>
   *   <li>Returns null for null input
   *   <li>Returns the same object if input is already a BufferedImage
   *   <li>Creates a new BufferedImage for other RenderedImage types
   *   <li>Preserves color model, alpha premultiplication, and properties
   * </ul>
   *
   * <p>The conversion process creates a compatible WritableRaster and copies all pixel data and
   * metadata from the source image.
   *
   * @param img the RenderedImage to convert, may be null
   * @return a BufferedImage representation of the input, or null if input is null
   * @see #createImageProperties(RenderedImage)
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
    BufferedImage result = new BufferedImage(cm, raster, isAlphaPremultiplied, properties);
    img.copyData(raster);
    return result;
  }

  private static Hashtable<String, Object> createImageProperties(RenderedImage img) {
    Hashtable<String, Object> properties = new Hashtable<>();
    String[] keys = img.getPropertyNames();
    if (keys != null) {
      for (String key : keys) {
        properties.put(key, img.getProperty(key));
      }
    }
    return properties;
  }

  /**
   * Unpacks binary raster data into a standard byte array format.
   *
   * <p>Binary images store multiple pixels per byte (8 pixels per byte for 1-bit data). This method
   * unpacks the packed binary data into a standard format where each pixel occupies one byte (0 or
   * 1), making it suitable for OpenCV processing.
   *
   * <p>The method supports different underlying data types:
   *
   * <ul>
   *   <li>DataBufferByte (8 pixels per byte)
   *   <li>DataBufferShort/DataBufferUShort (16 pixels per short)
   *   <li>DataBufferInt (32 pixels per int)
   * </ul>
   *
   * <p>The unpacking process accounts for:
   *
   * <ul>
   *   <li>Line stride and alignment
   *   <li>Bit offsets within data elements
   *   <li>Region-based extraction
   * </ul>
   *
   * @param raster the raster containing packed binary data
   * @param rect the rectangular region to unpack
   * @return a byte array with unpacked binary data (0 or 1 per byte)
   * @throws IllegalArgumentException if the raster is not binary format
   * @see #isBinary(SampleModel)
   * @see #unpackBinaryBytes(byte[], Rectangle, int, int, int, byte[])
   * @see #unpackBinaryShorts(short[], Rectangle, int, int, int, byte[])
   * @see #unpackBinaryInts(int[], Rectangle, int, int, int, byte[])
   */
  public static byte[] getUnpackedBinaryData(Raster raster, Rectangle rect) {
    SampleModel sm = raster.getSampleModel();
    if (!isBinary(sm)) {
      throw new IllegalArgumentException("Not a binary raster!");
    }

    DataBuffer dataBuffer = raster.getDataBuffer();

    MultiPixelPackedSampleModel mpp = (MultiPixelPackedSampleModel) sm;
    int dx = rect.x - raster.getSampleModelTranslateX();
    int dy = rect.y - raster.getSampleModelTranslateY();
    int lineStride = mpp.getScanlineStride();
    int eltOffset = dataBuffer.getOffset() + mpp.getOffset(dx, dy);
    int bitOffset = mpp.getBitOffset(dx);

    byte[] result = new byte[rect.width * rect.height];

    if (dataBuffer instanceof DataBufferByte buffer) {
      return unpackBinaryBytes(buffer.getData(), rect, eltOffset, bitOffset, lineStride, result);
    } else if (dataBuffer instanceof DataBufferShort buffer) {
      return unpackBinaryShorts(buffer.getData(), rect, eltOffset, bitOffset, lineStride, result);
    } else if (dataBuffer instanceof DataBufferUShort buffer) {
      return unpackBinaryShorts(buffer.getData(), rect, eltOffset, bitOffset, lineStride, result);
    } else if (dataBuffer instanceof DataBufferInt buffer) {
      return unpackBinaryInts(buffer.getData(), rect, eltOffset, bitOffset, lineStride, result);
    } else {
      return result; // Return empty array for unsupported types
    }
  }

  private static byte[] unpackBinaryBytes(
      byte[] data, Rectangle rect, int eltOffset, int bitOffset, int lineStride, byte[] result) {
    int k = 0;
    int maxY = rect.y + rect.height;
    int maxX = rect.x + rect.width;

    for (int y = rect.y; y < maxY; y++) {
      int bOffset = eltOffset * 8 + bitOffset;
      for (int x = rect.x; x < maxX; x++) {
        byte b = data[bOffset / 8];
        result[k++] = (byte) ((b >>> (7 - (bOffset & 7))) & 0x1);
        bOffset++;
      }
      eltOffset += lineStride;
    }
    return result;
  }

  private static byte[] unpackBinaryShorts(
      short[] data, Rectangle rect, int eltOffset, int bitOffset, int lineStride, byte[] result) {
    int k = 0;
    int maxY = rect.y + rect.height;
    int maxX = rect.x + rect.width;

    for (int y = rect.y; y < maxY; y++) {
      int bOffset = eltOffset * 16 + bitOffset;
      for (int x = rect.x; x < maxX; x++) {
        short s = data[bOffset / 16];
        result[k++] = (byte) ((s >>> (15 - (bOffset % 16))) & 0x1);
        bOffset++;
      }
      eltOffset += lineStride;
    }
    return result;
  }

  private static byte[] unpackBinaryInts(
      int[] data, Rectangle rect, int eltOffset, int bitOffset, int lineStride, byte[] result) {
    int k = 0;
    int maxY = rect.y + rect.height;
    int maxX = rect.x + rect.width;

    for (int y = rect.y; y < maxY; y++) {
      int bOffset = eltOffset * 32 + bitOffset;
      for (int x = rect.x; x < maxX; x++) {
        int i = data[bOffset / 32];
        result[k++] = (byte) ((i >>> (31 - (bOffset % 32))) & 0x1);
        bOffset++;
      }
      eltOffset += lineStride;
    }
    return result;
  }
}
