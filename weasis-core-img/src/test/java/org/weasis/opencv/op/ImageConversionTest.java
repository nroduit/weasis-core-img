/*
 * Copyright (c) 2023 Weasis Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.weasis.opencv.op;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BandedSampleModel;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.MultiPixelPackedSampleModel;
import java.awt.image.PixelInterleavedSampleModel;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.util.Vector;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.osgi.OpenCVNativeLoader;
import org.weasis.opencv.data.ImageCV;
import org.weasis.opencv.data.PlanarImage;

@DisplayName("Image Conversion Tests")
class ImageConversionTest {

  @BeforeAll
  @DisplayName("Load OpenCV native library")
  static void loadNativeLib() {
    OpenCVNativeLoader loader = new OpenCVNativeLoader();
    loader.init();
  }

  @Nested
  @DisplayName("BufferedImage Conversion Tests")
  class BufferedImageConversionTests {

    @Test
    @DisplayName("Should return null for null inputs")
    void testToBufferedImageWithNullInputs() {
      assertNull(ImageConversion.toBufferedImage((Mat) null));
      assertNull(ImageConversion.toBufferedImage((PlanarImage) null));
    }

    @Test
    @DisplayName("Should throw exception for unsupported CV types")
    void testToBufferedImageWithUnsupportedTypes() {
      try (ImageCV img = new ImageCV(3, 3, CvType.CV_8UC4, new Scalar(7))) {
        assertThrowsExactly(
            UnsupportedOperationException.class,
            () -> ImageConversion.toBufferedImage((PlanarImage) img));
      }

      try (ImageCV img = new ImageCV(new Size(7, 7), CvType.CV_16UC2, new Scalar(4096))) {
        assertThrowsExactly(
            UnsupportedOperationException.class,
            () -> ImageConversion.toBufferedImage((PlanarImage) img));
      }

      try (ImageCV img = new ImageCV(new Size(7, 7), CvType.CV_16F, new Scalar(-1.56287f))) {
        assertThrowsExactly(
            UnsupportedOperationException.class,
            () -> ImageConversion.toBufferedImage((PlanarImage) img));
      }
    }

    @ParameterizedTest(name = "Convert {0} image type")
    @MethodSource("supportedImageTypes")
    @DisplayName("Should convert supported CV types to BufferedImage")
    void testToBufferedImageWithSupportedTypes(
        String description, int cvType, Scalar value, int expectedBytes) {
      try (ImageCV img = new ImageCV(new Size(7, 7), cvType, value)) {
        testToBufferedImageConversion(img);
        assertEquals(expectedBytes, img.physicalBytes(), "Physical bytes should match expected");
      }
    }

    static Stream<Arguments> supportedImageTypes() {
      return Stream.of(
          Arguments.of("8-bit single channel", CvType.CV_8UC1, new Scalar(255), 49),
          Arguments.of("8-bit three channel", CvType.CV_8UC3, new Scalar(7), 147),
          Arguments.of("16-bit signed single channel", CvType.CV_16SC1, new Scalar(-1024), 98),
          Arguments.of("16-bit unsigned three channel", CvType.CV_16UC3, new Scalar(4096), 294),
          Arguments.of("32-bit signed integer", CvType.CV_32S, new Scalar(-409600), 196),
          Arguments.of("32-bit float", CvType.CV_32F, new Scalar(-1.56287f), 196),
          Arguments.of("64-bit double", CvType.CV_64F, new Scalar(-23566.221548796545), 392));
    }

    private void testToBufferedImageConversion(ImageCV img) {
      int width = img.width();
      int height = img.height();
      int cvType = img.type();
      int channels = CvType.channels(cvType);
      int depth = CvType.depth(cvType);

      BufferedImage bufferedImage = ImageConversion.toBufferedImage((PlanarImage) img);
      assertNotNull(bufferedImage, "BufferedImage should not be null");

      try (ImageCV imageCV = ImageConversion.toMat(bufferedImage)) {
        validateImageProperties(img, imageCV);
        validateImageData(img, imageCV, width, height, channels, depth);
      }
    }

    private void validateImageProperties(ImageCV original, ImageCV converted) {
      assertEquals(
          original.physicalBytes(), converted.physicalBytes(), "Physical bytes should match");
      assertEquals(original.rows(), converted.rows(), "Rows should match");
      assertEquals(original.cols(), converted.cols(), "Columns should match");
      assertEquals(original.type(), converted.type(), "CV type should match");
      assertEquals(original.channels(), converted.channels(), "Channels should match");
      assertEquals(original.isContinuous(), converted.isContinuous(), "Continuity should match");
    }

    private void validateImageData(
        ImageCV img, ImageCV imageCV, int width, int height, int channels, int depth) {
      int dataSize = width * height * channels;

      if (depth <= CvType.CV_8S) {
        validateByteData(img, imageCV, dataSize);
      } else if (depth <= CvType.CV_16S) {
        validateShortData(img, imageCV, dataSize);
      } else if (depth == CvType.CV_32S) {
        validateIntData(img, imageCV, dataSize);
      } else if (depth == CvType.CV_32F) {
        validateFloatData(img, imageCV, dataSize);
      } else if (depth == CvType.CV_64F) {
        validateDoubleData(img, imageCV, dataSize);
      }
    }

    private void validateByteData(ImageCV img, ImageCV imageCV, int dataSize) {
      byte[] data1 = new byte[dataSize];
      byte[] data2 = new byte[dataSize];
      img.get(0, 0, data1);
      imageCV.get(0, 0, data2);
      assertArrayEquals(data1, data2, "Byte data should match");
    }

    private void validateShortData(ImageCV img, ImageCV imageCV, int dataSize) {
      short[] data1 = new short[dataSize];
      short[] data2 = new short[dataSize];
      img.get(0, 0, data1);
      imageCV.get(0, 0, data2);
      assertArrayEquals(data1, data2, "Short data should match");
    }

    private void validateIntData(ImageCV img, ImageCV imageCV, int dataSize) {
      int[] data1 = new int[dataSize];
      int[] data2 = new int[dataSize];
      img.get(0, 0, data1);
      imageCV.get(0, 0, data2);
      assertArrayEquals(data1, data2, "Int data should match");
    }

    private void validateFloatData(ImageCV img, ImageCV imageCV, int dataSize) {
      float[] data1 = new float[dataSize];
      float[] data2 = new float[dataSize];
      img.get(0, 0, data1);
      imageCV.get(0, 0, data2);
      assertArrayEquals(data1, data2, "Float data should match");
    }

    private void validateDoubleData(ImageCV img, ImageCV imageCV, int dataSize) {
      double[] data1 = new double[dataSize];
      double[] data2 = new double[dataSize];
      img.get(0, 0, data1);
      imageCV.get(0, 0, data2);
      assertArrayEquals(data1, data2, "Double data should match");
    }
  }

  @Nested
  @DisplayName("RenderedImage Conversion Tests")
  class RenderedImageConversionTests {

    @Test
    @DisplayName("Should return null for null RenderedImage input")
    void testConvertRenderedImageWithNull() {
      assertNull(
          ImageConversion.convertRenderedImage(null),
          "convertRenderedImage should return null for null input");
    }

    @Test
    @DisplayName("Should return same instance when input is already BufferedImage")
    void testConvertRenderedImageWithBufferedImage() {
      // Given
      BufferedImage original = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
      Graphics2D g2d = original.createGraphics();
      g2d.setColor(Color.BLUE);
      g2d.fillRect(0, 0, 100, 100);
      g2d.dispose();

      // When
      BufferedImage result = ImageConversion.convertRenderedImage(original);

      // Then
      assertSame(
          original,
          result,
          "convertRenderedImage should return the same instance for BufferedImage input");
      assertEquals(100, result.getWidth());
      assertEquals(100, result.getHeight());
    }

    @Test
    @DisplayName("Should convert RenderedImage to BufferedImage preserving dimensions")
    void testConvertRenderedImagePreservesDimensions() {
      // Given
      BufferedImage source = new BufferedImage(150, 200, BufferedImage.TYPE_INT_ARGB);
      RenderedImage renderedImage = createMockRenderedImage(source);

      // When
      BufferedImage result = ImageConversion.convertRenderedImage(renderedImage);

      // Then
      assertNotNull(result, "Result should not be null");
      assertEquals(150, result.getWidth(), "Width should be preserved");
      assertEquals(200, result.getHeight(), "Height should be preserved");
    }

    @Test
    @DisplayName("Should preserve ColorModel properties")
    void testConvertRenderedImagePreservesColorModel() {
      // Given
      BufferedImage source = new BufferedImage(50, 50, BufferedImage.TYPE_INT_RGB);
      RenderedImage renderedImage = createMockRenderedImage(source);

      // When
      BufferedImage result = ImageConversion.convertRenderedImage(renderedImage);

      // Then
      assertNotNull(result, "Result should not be null");
      ColorModel originalColorModel = source.getColorModel();
      ColorModel resultColorModel = result.getColorModel();

      assertEquals(
          originalColorModel.getNumComponents(),
          resultColorModel.getNumComponents(),
          "Number of color components should be preserved");
      assertEquals(
          originalColorModel.hasAlpha(),
          resultColorModel.hasAlpha(),
          "Alpha channel presence should be preserved");
      assertEquals(
          originalColorModel.isAlphaPremultiplied(),
          resultColorModel.isAlphaPremultiplied(),
          "Alpha premultiplication should be preserved");
    }

    @Test
    @DisplayName("Should copy pixel data correctly")
    void testConvertRenderedImageCopiesPixelData() {
      // Given
      BufferedImage source = new BufferedImage(20, 20, BufferedImage.TYPE_INT_RGB);
      Graphics2D g2d = source.createGraphics();
      g2d.setColor(Color.RED);
      g2d.fillRect(0, 0, 10, 10);
      g2d.setColor(Color.GREEN);
      g2d.fillRect(10, 0, 10, 10);
      g2d.setColor(Color.BLUE);
      g2d.fillRect(0, 10, 10, 10);
      g2d.setColor(Color.YELLOW);
      g2d.fillRect(10, 10, 10, 10);
      g2d.dispose();

      RenderedImage renderedImage = createMockRenderedImage(source);

      // When
      BufferedImage result = ImageConversion.convertRenderedImage(renderedImage);

      // Then
      assertNotNull(result, "Result should not be null");

      // Verify pixel data by sampling key points
      assertEquals(source.getRGB(5, 5), result.getRGB(5, 5), "Red quadrant should match");
      assertEquals(source.getRGB(15, 5), result.getRGB(15, 5), "Green quadrant should match");
      assertEquals(source.getRGB(5, 15), result.getRGB(5, 15), "Blue quadrant should match");
      assertEquals(source.getRGB(15, 15), result.getRGB(15, 15), "Yellow quadrant should match");
    }

    @ParameterizedTest(name = "Convert {0} image type")
    @MethodSource("imageTypeProvider")
    @DisplayName("Should handle different BufferedImage types")
    void testConvertRenderedImageWithDifferentTypes(String description, int imageType) {
      // Given
      BufferedImage source = new BufferedImage(30, 30, imageType);
      Graphics2D g2d = source.createGraphics();
      g2d.setColor(Color.MAGENTA);
      g2d.fillOval(5, 5, 20, 20);
      g2d.dispose();

      RenderedImage renderedImage = createMockRenderedImage(source);

      // When
      BufferedImage result = ImageConversion.convertRenderedImage(renderedImage);

      // Then
      assertNotNull(result, "Result should not be null for " + description);
      assertEquals(30, result.getWidth(), "Width should be preserved for " + description);
      assertEquals(30, result.getHeight(), "Height should be preserved for " + description);
      assertEquals(
          source.getData().getDataBuffer().getDataType(),
          result.getData().getDataBuffer().getDataType(),
          "Image type should be preserved for " + description);
    }

    static Stream<Arguments> imageTypeProvider() {
      return Stream.of(
          Arguments.of("TYPE_INT_RGB", BufferedImage.TYPE_INT_RGB),
          Arguments.of("TYPE_INT_ARGB", BufferedImage.TYPE_INT_ARGB),
          Arguments.of("TYPE_INT_ARGB_PRE", BufferedImage.TYPE_INT_ARGB_PRE),
          Arguments.of("TYPE_3BYTE_BGR", BufferedImage.TYPE_3BYTE_BGR),
          Arguments.of("TYPE_4BYTE_ABGR", BufferedImage.TYPE_4BYTE_ABGR),
          Arguments.of("TYPE_BYTE_GRAY", BufferedImage.TYPE_BYTE_GRAY),
          Arguments.of("TYPE_USHORT_GRAY", BufferedImage.TYPE_USHORT_GRAY));
    }

    @Test
    @DisplayName("Should handle empty properties gracefully")
    void testConvertRenderedImageWithEmptyProperties() {
      // Given
      BufferedImage source = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
      RenderedImage renderedImage = createMockRenderedImage(source);

      // When
      BufferedImage result = ImageConversion.convertRenderedImage(renderedImage);

      // Then
      assertNotNull(result, "Result should not be null");
      assertEquals(10, result.getWidth(), "Width should be preserved");
      assertEquals(10, result.getHeight(), "Height should be preserved");
    }

    /**
     * Creates a mock RenderedImage that delegates to a BufferedImage but is not instanceof
     * BufferedImage. This allows testing the conversion path for non-BufferedImage RenderedImage
     * implementations.
     */
    private RenderedImage createMockRenderedImage(BufferedImage source) {
      return new RenderedImage() {
        @Override
        public Vector<RenderedImage> getSources() {
          return source.getSources();
        }

        @Override
        public Object getProperty(String name) {
          return source.getProperty(name);
        }

        @Override
        public String[] getPropertyNames() {
          return source.getPropertyNames();
        }

        @Override
        public ColorModel getColorModel() {
          return source.getColorModel();
        }

        @Override
        public SampleModel getSampleModel() {
          return source.getSampleModel();
        }

        @Override
        public int getWidth() {
          return source.getWidth();
        }

        @Override
        public int getHeight() {
          return source.getHeight();
        }

        @Override
        public int getMinX() {
          return source.getMinX();
        }

        @Override
        public int getMinY() {
          return source.getMinY();
        }

        @Override
        public int getNumXTiles() {
          return source.getNumXTiles();
        }

        @Override
        public int getNumYTiles() {
          return source.getNumYTiles();
        }

        @Override
        public int getMinTileX() {
          return source.getMinTileX();
        }

        @Override
        public int getMinTileY() {
          return source.getMinTileY();
        }

        @Override
        public int getTileWidth() {
          return source.getTileWidth();
        }

        @Override
        public int getTileHeight() {
          return source.getTileHeight();
        }

        @Override
        public int getTileGridXOffset() {
          return source.getTileGridXOffset();
        }

        @Override
        public int getTileGridYOffset() {
          return source.getTileGridYOffset();
        }

        @Override
        public Raster getTile(int tileX, int tileY) {
          return source.getTile(tileX, tileY);
        }

        @Override
        public Raster getData() {
          return source.getData();
        }

        @Override
        public Raster getData(Rectangle rect) {
          return source.getData(rect);
        }

        @Override
        public WritableRaster copyData(WritableRaster raster) {
          return source.copyData(raster);
        }
      };
    }
  }

  @Nested
  @DisplayName("Resource Management Tests")
  class ResourceManagementTests {

    @Test
    @DisplayName("Should handle null release operations gracefully")
    void testReleaseWithNullInputs() {
      ImageConversion.releaseMat(null);
      ImageConversion.releasePlanarImage(null);
    }

    @Test
    @DisplayName("Should properly manage image lifecycle and conversion")
    void testImageLifecycleManagement() {
      try (ImageCV img = new ImageCV(new Size(7, 7), CvType.CV_16UC1, new Scalar(1024))) {
        assertEquals(98, img.physicalBytes());

        BufferedImage bufferedImage = ImageConversion.toBufferedImage((PlanarImage) img);
        BufferedImage byteBufferedImage =
            ImageConversion.convertTo(bufferedImage, BufferedImage.TYPE_BYTE_GRAY);

        try (ImageCV imageCV = ImageConversion.toMat(byteBufferedImage)) {
          assertEquals(49, imageCV.physicalBytes());
          assertEquals(4, imageCV.get(0, 0)[0]);
          assertEquals(7.0, ImageConversion.getBounds(img).getWidth());
        }

        validateImageLifecycle(img);
      }
    }

    private void validateImageLifecycle(ImageCV img) {
      assertFalse(img.isReleased());
      assertFalse(img.isReleasedAfterProcessing());

      img.setReleasedAfterProcessing(true);
      assertTrue(img.isReleasedAfterProcessing());

      ImageConversion.releaseMat(img);
      assertTrue(img.isReleased());
    }
  }

  @Nested
  @DisplayName("Binary Image Conversion Tests")
  class BinaryImageConversionTests {

    @Test
    @DisplayName("Should return null for null RenderedImage")
    void testConvertRenderedImageWithNull() {
      assertNull(ImageConversion.convertRenderedImage(null));
    }

    @ParameterizedTest(name = "Convert binary image with {0} data buffer")
    @MethodSource("binaryImageTypes")
    @DisplayName("Should convert binary images with different data buffer types")
    void testBinaryImageConversion(String description, int dataBufferType) {
      int width = 3;
      int height = 3;

      BufferedImage binaryImage = createBinaryImage(width, height, dataBufferType);
      testBinaryImageConversion(binaryImage);
    }

    static Stream<Arguments> binaryImageTypes() {
      return Stream.of(
          Arguments.of("BYTE", DataBuffer.TYPE_BYTE),
          Arguments.of("USHORT", DataBuffer.TYPE_USHORT),
          Arguments.of("INT", DataBuffer.TYPE_INT));
    }

    private BufferedImage createBinaryImage(int width, int height, int dataBufferType) {
      if (dataBufferType == DataBuffer.TYPE_BYTE) {
        return new BufferedImage(width, height, BufferedImage.TYPE_BYTE_BINARY);
      }

      BufferedImage template = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_BINARY);
      WritableRaster raster = Raster.createPackedRaster(dataBufferType, width, height, 1, 1, null);
      return new BufferedImage(template.getColorModel(), raster, false, null);
    }

    private void testBinaryImageConversion(BufferedImage binaryImage) {
      WritableRaster raster = binaryImage.getRaster();
      setupBinaryTestData(raster);

      try (ImageCV imageCV = ImageConversion.toMat(binaryImage)) {
        validateBinaryConversion(imageCV);
      }
    }

    private void setupBinaryTestData(WritableRaster raster) {
      raster.setSample(1, 1, 0, 1); // Set pixel at (1, 1) to 1 (white)
      raster.setSample(2, 2, 0, 1); // Set pixel at (2, 2) to 1 (white)
    }

    private void validateBinaryConversion(ImageCV imageCV) {
      assertEquals(9, imageCV.physicalBytes());
      assertEquals(3, imageCV.rows());
      assertEquals(3, imageCV.cols());
      assertEquals(CvType.CV_8UC1, imageCV.type());
      assertEquals(1, imageCV.channels());
      assertTrue(imageCV.isContinuous());

      // Validate specific pixel values
      assertEquals(0, imageCV.get(0, 0)[0]);
      assertEquals(1, imageCV.get(1, 1)[0]);
      assertEquals(0, imageCV.get(1, 2)[0]);
      assertEquals(1, imageCV.get(2, 2)[0]);
    }
  }

  @Nested
  @DisplayName("Utility Method Tests")
  class UtilityMethodTests {

    @Test
    @DisplayName("Should correctly identify binary sample models")
    void testIsBinary() {
      assertFalse(ImageConversion.isBinary(new BandedSampleModel(1, 1, 1, 1)));
      assertFalse(
          ImageConversion.isBinary(new PixelInterleavedSampleModel(1, 1, 1, 1, 1, new int[] {0})));
      assertFalse(ImageConversion.isBinary(new MultiPixelPackedSampleModel(1, 1, 1, 2)));
      assertTrue(ImageConversion.isBinary(new MultiPixelPackedSampleModel(1, 1, 1, 1)));
    }

    @ParameterizedTest(name = "Convert CV type {0} to DataBuffer type {1}")
    @MethodSource("cvTypeToDataTypeMapping")
    @DisplayName("Should convert CV types to DataBuffer types correctly")
    void testConvertToDataType(int cvType, int expectedDataType) {
      assertEquals(expectedDataType, ImageConversion.convertToDataType(cvType));
    }

    static Stream<Arguments> cvTypeToDataTypeMapping() {
      return Stream.of(
          Arguments.of(CvType.CV_8U, DataBuffer.TYPE_BYTE),
          Arguments.of(CvType.CV_8S, DataBuffer.TYPE_BYTE),
          Arguments.of(CvType.CV_16U, DataBuffer.TYPE_USHORT),
          Arguments.of(CvType.CV_16S, DataBuffer.TYPE_SHORT),
          Arguments.of(CvType.CV_32S, DataBuffer.TYPE_INT),
          Arguments.of(CvType.CV_32F, DataBuffer.TYPE_FLOAT),
          Arguments.of(CvType.CV_64F, DataBuffer.TYPE_DOUBLE));
    }

    @Test
    @DisplayName("Should throw exception for invalid CV types")
    void testConvertToDataTypeWithInvalidInput() {
      assertThrows(
          UnsupportedOperationException.class, () -> ImageConversion.convertToDataType(-1));
    }
  }

  @Nested
  @DisplayName("Mat Conversion Tests")
  class MatConversionTests {

    @Test
    @DisplayName("Should handle BGR/RGB conversion correctly")
    void testMatConversionWithColorOrdering() {
      // Values: Blue=1, Green=2, Red=3
      try (ImageCV img = new ImageCV(new Size(7, 7), CvType.CV_8UC3, new Scalar(1, 2, 3))) {
        assertEquals(147, img.physicalBytes());

        BufferedImage bufferedImage = ImageConversion.toBufferedImage((PlanarImage) img);
        // BufferedImage is RGB by default, but Mat is BGR
        assertArrayEquals(
            new double[] {3, 2, 1}, bufferedImage.getRaster().getPixel(0, 0, (double[]) null));

        testMatConversionWithRegion(bufferedImage);
        testMatConversionWithInterleavedRaster(bufferedImage);
        testMatConversionWithBandedRaster(bufferedImage);
      }
    }

    private void testMatConversionWithRegion(BufferedImage bufferedImage) {
      try (ImageCV imageCV =
          ImageConversion.toMat(bufferedImage, new Rectangle(0, 0, 3, 3), true, false)) {
        assertEquals(27, imageCV.physicalBytes());
        assertEquals(3, imageCV.rows());
        assertEquals(3, imageCV.cols());
        assertArrayEquals(new double[] {1, 2, 3}, imageCV.get(0, 0));
      }
    }

    private void testMatConversionWithInterleavedRaster(BufferedImage template) {
      WritableRaster rgbRaster =
          Raster.createInterleavedRaster(
              DataBuffer.TYPE_BYTE, 1, 1, 3, 3, new int[] {0, 1, 2}, null);
      setupRasterData(rgbRaster);

      BufferedImage bgrImage = new BufferedImage(template.getColorModel(), rgbRaster, false, null);
      try (ImageCV imageCV = ImageConversion.toMat(bgrImage, null, true, false)) {
        assertEquals(3, imageCV.physicalBytes());
        assertArrayEquals(new double[] {1, 2, 3}, imageCV.get(0, 0));
      }
    }

    private void testMatConversionWithBandedRaster(BufferedImage template) {
      WritableRaster rgbBandRaster = Raster.createBandedRaster(DataBuffer.TYPE_BYTE, 1, 1, 3, null);
      setupRasterData(rgbBandRaster);

      BufferedImage bgrImage =
          new BufferedImage(template.getColorModel(), rgbBandRaster, false, null);
      try (ImageCV imageCV = ImageConversion.toMat(bgrImage, null, true, false)) {
        assertEquals(3, imageCV.physicalBytes());
        assertArrayEquals(new double[] {1, 2, 3}, imageCV.get(0, 0));
      }
    }

    private void setupRasterData(WritableRaster raster) {
      raster.setSample(0, 0, 0, 3); // Red
      raster.setSample(0, 0, 1, 2); // Green
      raster.setSample(0, 0, 2, 1); // Blue
    }
  }
}
