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
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.weasis.opencv.data.ImageCV;
import org.weasis.opencv.data.PlanarImage;

public class ImageConversion {

  private static final Logger LOGGER = LoggerFactory.getLogger(ImageConversion.class);

  private ImageConversion() {}

  /**
   * Converts/writes a Mat into a BufferedImage.
   *
   * @param matrix
   * @return BufferedImage
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

    ColorSpace cs;
    WritableRaster raster;
    ComponentColorModel colorModel;
    int dataType = convertToDataType(type);

    switch (channels) {
      case 1:
        cs = ColorSpace.getInstance(ColorSpace.CS_GRAY);
        colorModel =
            new ComponentColorModel(
                cs, new int[] {bpp}, false, true, Transparency.OPAQUE, dataType);
        raster = colorModel.createCompatibleWritableRaster(cols, rows);
        break;
      case 3:
        cs = ColorSpace.getInstance(ColorSpace.CS_sRGB);
        colorModel =
            new ComponentColorModel(
                cs, new int[] {bpp, bpp, bpp}, false, false, Transparency.OPAQUE, dataType);
        raster =
            Raster.createInterleavedRaster(
                dataType, cols, rows, cols * channels, channels, new int[] {2, 1, 0}, null);
        break;
      default:
        throw new UnsupportedOperationException(
            "No implementation to handle " + channels + " channels");
    }

    DataBuffer buf = raster.getDataBuffer();

    if (buf instanceof DataBufferByte) {
      matrix.get(0, 0, ((DataBufferByte) buf).getData());
    } else if (buf instanceof DataBufferUShort) {
      matrix.get(0, 0, ((DataBufferUShort) buf).getData());
    } else if (buf instanceof DataBufferShort) {
      matrix.get(0, 0, ((DataBufferShort) buf).getData());
    } else if (buf instanceof DataBufferInt) {
      matrix.get(0, 0, ((DataBufferInt) buf).getData());
    } else if (buf instanceof DataBufferFloat) {
      matrix.get(0, 0, ((DataBufferFloat) buf).getData());
    } else if (buf instanceof DataBufferDouble) {
      matrix.get(0, 0, ((DataBufferDouble) buf).getData());
    }
    return new BufferedImage(colorModel, raster, false, null);
  }

  public static BufferedImage toBufferedImage(PlanarImage matrix) {
    if (matrix == null) {
      return null;
    }
    return toBufferedImage(matrix.toMat());
  }

  public static void releaseMat(Mat mat) {
    if (mat != null) {
      mat.release();
    }
  }

  public static void releasePlanarImage(PlanarImage img) {
    if (img != null) {
      img.release();
    }
  }

  public static int convertToDataType(int cvType) {
    switch (CvType.depth(cvType)) {
      case CvType.CV_8U:
      case CvType.CV_8S:
        return DataBuffer.TYPE_BYTE;
      case CvType.CV_16U:
        return DataBuffer.TYPE_USHORT;
      case CvType.CV_16S:
        return DataBuffer.TYPE_SHORT;
      case CvType.CV_32S:
        return DataBuffer.TYPE_INT;
      case CvType.CV_32F:
        return DataBuffer.TYPE_FLOAT;
      case CvType.CV_64F:
        return DataBuffer.TYPE_DOUBLE;
      default:
        throw new java.lang.UnsupportedOperationException("Unsupported CvType value: " + cvType);
    }
  }

  public static ImageCV toMat(RenderedImage img) {
    return toMat(img, null);
  }

  public static ImageCV toMat(RenderedImage img, Rectangle region) {
    return toMat(img, region, true);
  }

  public static ImageCV toMat(RenderedImage img, Rectangle region, boolean toBGR) {
    Raster raster = region == null ? img.getData() : img.getData(region);
    DataBuffer buf = raster.getDataBuffer();
    int[] samples = raster.getSampleModel().getSampleSize();
    int[] offsets;
    if (raster.getSampleModel() instanceof ComponentSampleModel) {
      offsets = ((ComponentSampleModel) raster.getSampleModel()).getBandOffsets();
    } else {
      offsets = new int[samples.length];
      for (int i = 0; i < offsets.length; i++) {
        offsets[i] = i;
      }
    }

    if (isBinary(raster.getSampleModel())) {
      // Sonar false positive: not mandatory to close ImageCV (can be done with finalize())
      ImageCV mat = new ImageCV(raster.getHeight(), raster.getWidth(), CvType.CV_8UC1); // NOSONAR
      mat.put(0, 0, getUnpackedBinaryData(raster, raster.getBounds()));
      return mat;
    }

    if (buf instanceof DataBufferByte) {
      if (Arrays.equals(offsets, new int[] {0, 0, 0})) {

        Mat b = new Mat(raster.getHeight(), raster.getWidth(), CvType.CV_8UC1);
        b.put(0, 0, ((DataBufferByte) buf).getData(2));
        Mat g = new Mat(raster.getHeight(), raster.getWidth(), CvType.CV_8UC1);
        g.put(0, 0, ((DataBufferByte) buf).getData(1));
        ImageCV r = new ImageCV(raster.getHeight(), raster.getWidth(), CvType.CV_8UC1);
        r.put(0, 0, ((DataBufferByte) buf).getData(0));
        List<Mat> mv = toBGR ? Arrays.asList(b, g, r) : Arrays.asList(r, g, b);
        ImageCV dstImg = new ImageCV(raster.getHeight(), raster.getWidth(), CvType.CV_8UC3);
        Core.merge(mv, dstImg);
        return dstImg;
      }

      ImageCV mat =
          new ImageCV(raster.getHeight(), raster.getWidth(), CvType.CV_8UC(samples.length));
      mat.put(0, 0, ((DataBufferByte) buf).getData());
      if (toBGR && Arrays.equals(offsets, new int[] {0, 1, 2})) {
        ImageCV dstImg = new ImageCV();
        Imgproc.cvtColor(mat, dstImg, Imgproc.COLOR_RGB2BGR);
        return dstImg;
      } else if (!toBGR && Arrays.equals(offsets, new int[] {2, 1, 0})) {
        ImageCV dstImg = new ImageCV();
        Imgproc.cvtColor(mat, dstImg, Imgproc.COLOR_BGR2RGB);
        return dstImg;
      }
      return mat;
    } else if (buf instanceof DataBufferUShort) {
      ImageCV mat =
          new ImageCV(raster.getHeight(), raster.getWidth(), CvType.CV_16UC(samples.length));
      mat.put(0, 0, ((DataBufferUShort) buf).getData());
      return mat;
    } else if (buf instanceof DataBufferShort) {
      ImageCV mat =
          new ImageCV(raster.getHeight(), raster.getWidth(), CvType.CV_16SC(samples.length));
      mat.put(0, 0, ((DataBufferShort) buf).getData());
      return mat;
    } else if (buf instanceof DataBufferInt) {
      ImageCV mat =
          new ImageCV(raster.getHeight(), raster.getWidth(), CvType.CV_32SC(samples.length));
      mat.put(0, 0, ((DataBufferInt) buf).getData());
      return mat;
    } else if (buf instanceof DataBufferFloat) {
      ImageCV mat =
          new ImageCV(raster.getHeight(), raster.getWidth(), CvType.CV_32FC(samples.length));
      mat.put(0, 0, ((DataBufferFloat) buf).getData());
      return mat;
    } else if (buf instanceof DataBufferDouble) {
      ImageCV mat =
          new ImageCV(raster.getHeight(), raster.getWidth(), CvType.CV_64FC(samples.length));
      mat.put(0, 0, ((DataBufferDouble) buf).getData());
      return mat;
    }

    return null;
  }

  public static Rectangle getBounds(PlanarImage img) {
    return new Rectangle(0, 0, img.width(), img.height());
  }

  public static BufferedImage convertTo(RenderedImage src, int imageType) {
    BufferedImage dst = new BufferedImage(src.getWidth(), src.getHeight(), imageType);
    Graphics2D big = dst.createGraphics();
    try {
      big.drawRenderedImage(src, AffineTransform.getTranslateInstance(0.0, 0.0));
    } finally {
      big.dispose();
    }
    return dst;
  }

  public static boolean isBinary(SampleModel sm) {
    return sm instanceof MultiPixelPackedSampleModel
        && ((MultiPixelPackedSampleModel) sm).getPixelBitStride() == 1
        && sm.getNumBands() == 1;
  }

  public static BufferedImage convertRenderedImage(RenderedImage img) {
    if (img == null) {
      return null;
    }
    if (img instanceof BufferedImage) {
      return (BufferedImage) img;
    }
    ColorModel cm = img.getColorModel();
    int width = img.getWidth();
    int height = img.getHeight();
    WritableRaster raster = cm.createCompatibleWritableRaster(width, height);
    boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
    Hashtable<String, Object> properties = new Hashtable<>();
    String[] keys = img.getPropertyNames();
    if (keys != null) {
      for (String key : keys) {
        properties.put(key, img.getProperty(key));
      }
    }
    BufferedImage result = new BufferedImage(cm, raster, isAlphaPremultiplied, properties);
    img.copyData(raster);
    return result;
  }

  /**
   * Returns the binary data unpacked into an array of bytes. The line stride will be the width of
   * the <code>Raster</code>.
   *
   * @throws IllegalArgumentException if <code>isBinary()</code> returns <code>false</code> with the
   *     <code>SampleModel</code> of the supplied <code>Raster</code> as argument.
   */
  public static byte[] getUnpackedBinaryData(Raster raster, Rectangle rect) {
    SampleModel sm = raster.getSampleModel();
    if (!isBinary(sm)) {
      throw new IllegalArgumentException("Not a binary raster!");
    }

    int rectX = rect.x;
    int rectY = rect.y;
    int rectWidth = rect.width;
    int rectHeight = rect.height;

    DataBuffer dataBuffer = raster.getDataBuffer();

    int dx = rectX - raster.getSampleModelTranslateX();
    int dy = rectY - raster.getSampleModelTranslateY();

    MultiPixelPackedSampleModel mpp = (MultiPixelPackedSampleModel) sm;
    int lineStride = mpp.getScanlineStride();
    int eltOffset = dataBuffer.getOffset() + mpp.getOffset(dx, dy);
    int bitOffset = mpp.getBitOffset(dx);

    byte[] bdata = new byte[rectWidth * rectHeight];
    int maxY = rectY + rectHeight;
    int maxX = rectX + rectWidth;
    int k = 0;

    if (dataBuffer instanceof DataBufferByte) {
      byte[] data = ((DataBufferByte) dataBuffer).getData();
      for (int y = rectY; y < maxY; y++) {
        int bOffset = eltOffset * 8 + bitOffset;
        for (int x = rectX; x < maxX; x++) {
          byte b = data[bOffset / 8];
          bdata[k++] = (byte) ((b >>> (7 - bOffset & 7)) & 0x0000001);
          bOffset++;
        }
        eltOffset += lineStride;
      }
    } else if (dataBuffer instanceof DataBufferShort || dataBuffer instanceof DataBufferUShort) {
      short[] data =
          dataBuffer instanceof DataBufferShort
              ? ((DataBufferShort) dataBuffer).getData()
              : ((DataBufferUShort) dataBuffer).getData();
      for (int y = rectY; y < maxY; y++) {
        int bOffset = eltOffset * 16 + bitOffset;
        for (int x = rectX; x < maxX; x++) {
          short s = data[bOffset / 16];
          bdata[k++] = (byte) ((s >>> (15 - bOffset % 16)) & 0x0000001);
          bOffset++;
        }
        eltOffset += lineStride;
      }
    } else if (dataBuffer instanceof DataBufferInt) {
      int[] data = ((DataBufferInt) dataBuffer).getData();
      for (int y = rectY; y < maxY; y++) {
        int bOffset = eltOffset * 32 + bitOffset;
        for (int x = rectX; x < maxX; x++) {
          int i = data[bOffset / 32];
          bdata[k++] = (byte) ((i >>> (31 - bOffset % 32)) & 0x0000001);
          bOffset++;
        }
        eltOffset += lineStride;
      }
    }

    return bdata;
  }
}
