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

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.MultiPixelPackedSampleModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
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
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.weasis.opencv.data.ImageCV;
import org.weasis.opencv.data.PlanarImage;
import org.weasis.opencv.natives.NativeLibrary;

@DisplayNameGeneration(ReplaceUnderscores.class)
class ImageConversionTest {

  @BeforeAll
  static void load_opencv_native_library() {
    NativeLibrary.loadLibraryFromLibraryName();
  }

  @Nested
  @DisplayNameGeneration(ReplaceUnderscores.class)
  class BufferedImage_conversion_tests {

    @Test
    void should_return_null_for_null_inputs() {
      assertNull(ImageConversion.toBufferedImage((Mat) null));
      assertNull(ImageConversion.toBufferedImage((PlanarImage) null));
    }

    @Test
    void should_throw_exception_for_unsupported_cv_types() {
      try (var img4Channel = new ImageCV(3, 3, CvType.CV_8UC4, new Scalar(7))) {
        assertThrows(
            UnsupportedOperationException.class,
            () -> ImageConversion.toBufferedImage((PlanarImage) img4Channel));
      }

      try (var img2Channel = new ImageCV(new Size(7, 7), CvType.CV_16UC2, new Scalar(4096))) {
        assertThrows(
            UnsupportedOperationException.class,
            () -> ImageConversion.toBufferedImage((PlanarImage) img2Channel));
      }
    }

    @ParameterizedTest
    @MethodSource("supported_image_types")
    void should_convert_supported_cv_types_to_buffered_image(
        String description, int cvType, Scalar value, int expectedBytes) {
      try (var img = new ImageCV(new Size(7, 7), cvType, value)) {
        test_buffered_image_conversion(img, expectedBytes);
      }
    }

    static Stream<Arguments> supported_image_types() {
      return Stream.of(
          Arguments.of("8-bit single channel", CvType.CV_8UC1, new Scalar(255), 49),
          Arguments.of("8-bit three channel", CvType.CV_8UC3, new Scalar(7), 147),
          Arguments.of("16-bit signed single channel", CvType.CV_16SC1, new Scalar(-1024), 98),
          Arguments.of("16-bit unsigned three channel", CvType.CV_16UC3, new Scalar(4096), 294),
          Arguments.of("32-bit signed integer", CvType.CV_32S, new Scalar(-409600), 196),
          Arguments.of("32-bit float", CvType.CV_32F, new Scalar(-1.56287f), 196),
          Arguments.of("64-bit double", CvType.CV_64F, new Scalar(-23566.221548796545), 392));
    }

    private void test_buffered_image_conversion(ImageCV img, int expectedBytes) {
      int width = img.width();
      int height = img.height();
      int channels = CvType.channels(img.type());
      int depth = CvType.depth(img.type());

      assertEquals(expectedBytes, img.physicalBytes(), "Physical bytes should match expected");

      var bufferedImage = ImageConversion.toBufferedImage((PlanarImage) img);
      assertNotNull(bufferedImage, "BufferedImage should not be null");

      try (var convertedImg = ImageConversion.toMat(bufferedImage)) {
        validate_image_properties(img, convertedImg);
        validate_image_data(img, convertedImg, width, height, channels, depth);
      }
    }

    private void validate_image_properties(ImageCV original, ImageCV converted) {
      assertAll(
          "Image properties",
          () -> assertEquals(original.physicalBytes(), converted.physicalBytes(), "Physical bytes"),
          () -> assertEquals(original.rows(), converted.rows(), "Rows"),
          () -> assertEquals(original.cols(), converted.cols(), "Columns"),
          () -> assertEquals(original.type(), converted.type(), "CV type"),
          () -> assertEquals(original.channels(), converted.channels(), "Channels"),
          () -> assertEquals(original.isContinuous(), converted.isContinuous(), "Continuity"));
    }

    private void validate_image_data(
        ImageCV img, ImageCV converted, int width, int height, int channels, int depth) {
      int dataSize = width * height * channels;

      switch (depth) {
        case CvType.CV_8U, CvType.CV_8S -> validate_byte_data(img, converted, dataSize);
        case CvType.CV_16U, CvType.CV_16S -> validate_short_data(img, converted, dataSize);
        case CvType.CV_32S -> validate_int_data(img, converted, dataSize);
        case CvType.CV_32F -> validate_float_data(img, converted, dataSize);
        case CvType.CV_64F -> validate_double_data(img, converted, dataSize);
      }
    }

    private void validate_byte_data(ImageCV img, ImageCV converted, int dataSize) {
      var data1 = new byte[dataSize];
      var data2 = new byte[dataSize];
      img.get(0, 0, data1);
      converted.get(0, 0, data2);
      assertArrayEquals(data1, data2, "Byte data should match");
    }

    private void validate_short_data(ImageCV img, ImageCV converted, int dataSize) {
      var data1 = new short[dataSize];
      var data2 = new short[dataSize];
      img.get(0, 0, data1);
      converted.get(0, 0, data2);
      assertArrayEquals(data1, data2, "Short data should match");
    }

    private void validate_int_data(ImageCV img, ImageCV converted, int dataSize) {
      var data1 = new int[dataSize];
      var data2 = new int[dataSize];
      img.get(0, 0, data1);
      converted.get(0, 0, data2);
      assertArrayEquals(data1, data2, "Int data should match");
    }

    private void validate_float_data(ImageCV img, ImageCV converted, int dataSize) {
      var data1 = new float[dataSize];
      var data2 = new float[dataSize];
      img.get(0, 0, data1);
      converted.get(0, 0, data2);
      assertArrayEquals(data1, data2, "Float data should match");
    }

    private void validate_double_data(ImageCV img, ImageCV converted, int dataSize) {
      var data1 = new double[dataSize];
      var data2 = new double[dataSize];
      img.get(0, 0, data1);
      converted.get(0, 0, data2);
      assertArrayEquals(data1, data2, "Double data should match");
    }
  }

  @Nested
  @DisplayNameGeneration(ReplaceUnderscores.class)
  class RenderedImage_conversion_tests {

    @Test
    void should_return_null_for_null_rendered_image_input() {
      assertNull(ImageConversion.convertRenderedImage(null));
    }

    @Test
    void should_return_same_instance_when_input_is_already_buffered_image() {
      var original = create_test_image(100, 100, BufferedImage.TYPE_INT_RGB, Color.BLUE);
      var result = ImageConversion.convertRenderedImage(original);

      assertSame(original, result, "Should return same BufferedImage instance");
      assertEquals(100, result.getWidth());
      assertEquals(100, result.getHeight());
    }

    @Test
    void should_convert_rendered_image_preserving_dimensions() {
      var source = create_test_image(150, 200, BufferedImage.TYPE_INT_ARGB, Color.GREEN);
      var result = ImageConversion.convertRenderedImage(source);

      assertAll(
          "Dimension preservation",
          () -> assertNotNull(result, "Result should not be null"),
          () -> assertEquals(150, result.getWidth(), "Width should be preserved"),
          () -> assertEquals(200, result.getHeight(), "Height should be preserved"));
    }

    @Test
    void should_preserve_color_model_properties() {
      var source = create_test_image(50, 50, BufferedImage.TYPE_INT_RGB, Color.RED);
      var result = ImageConversion.convertRenderedImage(source);

      assertNotNull(result);
      var originalColorModel = source.getColorModel();
      var resultColorModel = result.getColorModel();

      assertAll(
          "ColorModel properties",
          () ->
              assertEquals(
                  originalColorModel.getNumComponents(), resultColorModel.getNumComponents()),
          () -> assertEquals(originalColorModel.hasAlpha(), resultColorModel.hasAlpha()),
          () ->
              assertEquals(
                  originalColorModel.isAlphaPremultiplied(),
                  resultColorModel.isAlphaPremultiplied()));
    }

    @Test
    void should_copy_pixel_data_correctly() {
      var source = create_colored_quadrant_image(20, 20);
      var result = ImageConversion.convertRenderedImage(source);

      assertNotNull(result);

      // Verify pixel data by sampling key points
      assertAll(
          "Pixel data integrity",
          () -> assertEquals(source.getRGB(5, 5), result.getRGB(5, 5), "Red quadrant"),
          () -> assertEquals(source.getRGB(15, 5), result.getRGB(15, 5), "Green quadrant"),
          () -> assertEquals(source.getRGB(5, 15), result.getRGB(5, 15), "Blue quadrant"),
          () -> assertEquals(source.getRGB(15, 15), result.getRGB(15, 15), "Yellow quadrant"));
    }

    @ParameterizedTest
    @MethodSource("image_type_provider")
    void should_handle_different_buffered_image_types(String description, int imageType) {
      var source = create_test_image(30, 30, imageType, Color.MAGENTA);
      var result = ImageConversion.convertRenderedImage(source);

      assertAll(
          "Different image types",
          () -> assertNotNull(result, "Result should not be null for " + description),
          () -> assertEquals(30, result.getWidth(), "Width for " + description),
          () -> assertEquals(30, result.getHeight(), "Height for " + description),
          () ->
              assertEquals(
                  source.getData().getDataBuffer().getDataType(),
                  result.getData().getDataBuffer().getDataType(),
                  "Data type for " + description));
    }

    static Stream<Arguments> image_type_provider() {
      return Stream.of(
          Arguments.of("TYPE_INT_RGB", BufferedImage.TYPE_INT_RGB),
          Arguments.of("TYPE_INT_ARGB", BufferedImage.TYPE_INT_ARGB),
          Arguments.of("TYPE_INT_ARGB_PRE", BufferedImage.TYPE_INT_ARGB_PRE),
          Arguments.of("TYPE_3BYTE_BGR", BufferedImage.TYPE_3BYTE_BGR),
          Arguments.of("TYPE_4BYTE_ABGR", BufferedImage.TYPE_4BYTE_ABGR),
          Arguments.of("TYPE_BYTE_GRAY", BufferedImage.TYPE_BYTE_GRAY),
          Arguments.of("TYPE_USHORT_GRAY", BufferedImage.TYPE_USHORT_GRAY));
    }

    private BufferedImage create_test_image(int width, int height, int type, Color color) {
      var image = new BufferedImage(width, height, type);
      var g2d = image.createGraphics();
      try {
        g2d.setColor(color);
        g2d.fillRect(0, 0, width, height);
      } finally {
        g2d.dispose();
      }
      return image;
    }

    private BufferedImage create_colored_quadrant_image(int width, int height) {
      var image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
      var g2d = image.createGraphics();
      try {
        int halfWidth = width / 2;
        int halfHeight = height / 2;

        g2d.setColor(Color.RED);
        g2d.fillRect(0, 0, halfWidth, halfHeight);
        g2d.setColor(Color.GREEN);
        g2d.fillRect(halfWidth, 0, halfWidth, halfHeight);
        g2d.setColor(Color.BLUE);
        g2d.fillRect(0, halfHeight, halfWidth, halfHeight);
        g2d.setColor(Color.YELLOW);
        g2d.fillRect(halfWidth, halfHeight, halfWidth, halfHeight);
      } finally {
        g2d.dispose();
      }
      return image;
    }
  }

  @Nested
  @DisplayNameGeneration(ReplaceUnderscores.class)
  class Resource_management_tests {

    @Test
    void should_handle_null_release_operations_gracefully() {
      assertAll(
          "Null release operations",
          () -> assertDoesNotThrow(() -> ImageConversion.releaseMat(null)),
          () -> assertDoesNotThrow(() -> ImageConversion.releasePlanarImage(null)));
    }

    @Test
    void should_properly_manage_image_lifecycle_and_conversion() {
      try (var img = new ImageCV(new Size(7, 7), CvType.CV_16UC1, new Scalar(1024))) {
        assertEquals(98, img.physicalBytes());

        var bufferedImage = ImageConversion.toBufferedImage((PlanarImage) img);
        var byteBufferedImage =
            ImageConversion.convertTo(bufferedImage, BufferedImage.TYPE_BYTE_GRAY);

        try (var imageCV = ImageConversion.toMat(byteBufferedImage)) {
          assertAll(
              "Image lifecycle validation",
              () -> assertEquals(49, imageCV.physicalBytes()),
              () -> assertEquals(4, imageCV.get(0, 0)[0]),
              () -> assertEquals(7.0, ImageConversion.getBounds(img).getWidth()));
        }

        validate_image_lifecycle(img);
      }
    }

    private void validate_image_lifecycle(ImageCV img) {
      assertFalse(img.isReleased());
      assertFalse(img.isReleasedAfterProcessing());

      img.setReleasedAfterProcessing(true);
      assertTrue(img.isReleasedAfterProcessing());

      ImageConversion.releaseMat(img);
      assertTrue(img.isReleased());
    }
  }

  @Nested
  @DisplayNameGeneration(ReplaceUnderscores.class)
  class Binary_image_conversion_tests {

    @ParameterizedTest
    @MethodSource("binary_image_types")
    void should_convert_binary_images_with_different_data_buffer_types(int dataBufferType) {
      int width = 3;
      int height = 3;

      var binaryImage = create_binary_image(width, height, dataBufferType);
      test_binary_image_conversion(binaryImage);
    }

    static Stream<Arguments> binary_image_types() {
      return Stream.of(
          Arguments.of(DataBuffer.TYPE_BYTE),
          Arguments.of(DataBuffer.TYPE_USHORT),
          Arguments.of(DataBuffer.TYPE_INT));
    }

    private BufferedImage create_binary_image(int width, int height, int dataBufferType) {
      if (dataBufferType == DataBuffer.TYPE_BYTE) {
        return new BufferedImage(width, height, BufferedImage.TYPE_BYTE_BINARY);
      }

      var template = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_BINARY);
      var raster = Raster.createPackedRaster(dataBufferType, width, height, 1, 1, null);
      return new BufferedImage(template.getColorModel(), raster, false, null);
    }

    private void test_binary_image_conversion(BufferedImage binaryImage) {
      var raster = binaryImage.getRaster();
      setup_binary_test_data(raster);

      try (var imageCV = ImageConversion.toMat(binaryImage)) {
        validate_binary_conversion(imageCV);
      }
    }

    private void setup_binary_test_data(WritableRaster raster) {
      raster.setSample(1, 1, 0, 1); // Set pixel at (1, 1) to 1 (white)
      raster.setSample(2, 2, 0, 1); // Set pixel at (2, 2) to 1 (white)
    }

    private void validate_binary_conversion(ImageCV imageCV) {
      assertAll(
          "Binary image conversion",
          () -> assertEquals(9, imageCV.physicalBytes()),
          () -> assertEquals(3, imageCV.rows()),
          () -> assertEquals(3, imageCV.cols()),
          () -> assertEquals(CvType.CV_8UC1, imageCV.type()),
          () -> assertEquals(1, imageCV.channels()),
          () -> assertTrue(imageCV.isContinuous()),
          // Validate specific pixel values
          () -> assertEquals(0, imageCV.get(0, 0)[0]),
          () -> assertEquals(1, imageCV.get(1, 1)[0]),
          () -> assertEquals(0, imageCV.get(1, 2)[0]),
          () -> assertEquals(1, imageCV.get(2, 2)[0]));
    }
  }

  @Nested
  @DisplayNameGeneration(ReplaceUnderscores.class)
  class Utility_method_tests {

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 4, 8})
    void should_correctly_identify_non_binary_sample_models_with_different_bit_strides(
        int bitStride) {
      var sampleModel = new MultiPixelPackedSampleModel(DataBuffer.TYPE_BYTE, 10, 10, bitStride);
      boolean isBinary = bitStride == 1;
      assertEquals(isBinary, ImageConversion.isBinary(sampleModel));
    }

    @ParameterizedTest
    @MethodSource("cv_type_to_data_type_mapping")
    void should_convert_cv_types_to_data_buffer_types_correctly(int cvType, int expectedDataType) {
      assertEquals(expectedDataType, ImageConversion.convertToDataType(cvType));
    }

    static Stream<Arguments> cv_type_to_data_type_mapping() {
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
    void should_throw_exception_for_invalid_cv_types() {
      assertThrows(
          UnsupportedOperationException.class, () -> ImageConversion.convertToDataType(-1));
    }

    @Test
    void should_calculate_bounds_correctly() {
      try (var img = new ImageCV(new Size(100, 50), CvType.CV_8UC1)) {
        var bounds = ImageConversion.getBounds(img);
        assertAll(
            "Bounds calculation",
            () -> assertEquals(0, bounds.x),
            () -> assertEquals(0, bounds.y),
            () -> assertEquals(100, bounds.width),
            () -> assertEquals(50, bounds.height));
      }
    }
  }

  @Nested
  @DisplayNameGeneration(ReplaceUnderscores.class)
  class Mat_conversion_tests {

    @Test
    void should_handle_bgr_rgb_conversion_correctly() {
      // Values: Blue=1, Green=2, Red=3
      try (var img = new ImageCV(new Size(7, 7), CvType.CV_8UC3, new Scalar(1, 2, 3))) {
        assertEquals(147, img.physicalBytes());

        var bufferedImage = ImageConversion.toBufferedImage((PlanarImage) img);
        // BufferedImage is RGB by default, but Mat is BGR
        assertArrayEquals(
            new double[] {3, 2, 1}, bufferedImage.getRaster().getPixel(0, 0, (double[]) null));

        test_mat_conversion_with_region(bufferedImage);
        test_mat_conversion_with_different_raster_types(bufferedImage);
      }
    }

    private void test_mat_conversion_with_region(BufferedImage bufferedImage) {
      try (var imageCV =
          ImageConversion.toMat(bufferedImage, new Rectangle(0, 0, 3, 3), true, false)) {
        assertAll(
            "Region conversion",
            () -> assertEquals(27, imageCV.physicalBytes()),
            () -> assertEquals(3, imageCV.rows()),
            () -> assertEquals(3, imageCV.cols()),
            () -> assertArrayEquals(new double[] {1, 2, 3}, imageCV.get(0, 0)));
      }
    }

    private void test_mat_conversion_with_different_raster_types(BufferedImage template) {
      // Test interleaved raster
      var rgbRaster =
          Raster.createInterleavedRaster(
              DataBuffer.TYPE_BYTE, 1, 1, 3, 3, new int[] {0, 1, 2}, null);
      setup_raster_data(rgbRaster, 3, 2, 1); // RGB values

      var rgbImage = new BufferedImage(template.getColorModel(), rgbRaster, false, null);
      try (var imageCV = ImageConversion.toMat(rgbImage, null, true, false)) {
        assertEquals(3, imageCV.physicalBytes());
        assertArrayEquals(new double[] {1, 2, 3}, imageCV.get(0, 0)); // Should be BGR
      }

      // Test banded raster
      var bandedRaster = Raster.createBandedRaster(DataBuffer.TYPE_BYTE, 1, 1, 3, null);
      setup_raster_data(bandedRaster, 3, 2, 1); // RGB values

      var bandedImage = new BufferedImage(template.getColorModel(), bandedRaster, false, null);
      try (var bandedImageCV = ImageConversion.toMat(bandedImage, null, true, false)) {
        assertEquals(3, bandedImageCV.physicalBytes());
        assertArrayEquals(new double[] {1, 2, 3}, bandedImageCV.get(0, 0)); // Should be BGR
      }
    }

    private void setup_raster_data(WritableRaster raster, int red, int green, int blue) {
      raster.setSample(0, 0, 0, red); // Red channel
      raster.setSample(0, 0, 1, green); // Green channel
      raster.setSample(0, 0, 2, blue); // Blue channel
    }

    @Test
    void should_handle_different_conversion_modes() {
      var image = create_test_color_image();

      // Test different toBGR modes
      try (var bgrMat = ImageConversion.toMat(image, null, true);
          var rgbMat = ImageConversion.toMat(image, null, false)) {

        assertNotNull(bgrMat);
        assertNotNull(rgbMat);
        assertEquals(bgrMat.physicalBytes(), rgbMat.physicalBytes());

        assertArrayEquals(new double[] {255, 128, 64}, rgbMat.get(0, 0));
        assertArrayEquals(new double[] {64, 128, 255}, bgrMat.get(0, 0));
      }
    }

    private BufferedImage create_test_color_image() {
      var image = new BufferedImage(10, 10, BufferedImage.TYPE_3BYTE_BGR);
      var g2d = image.createGraphics();
      try {
        g2d.setColor(new Color(255, 128, 64)); // RGB
        g2d.fillRect(0, 0, 10, 10);
      } finally {
        g2d.dispose();
      }
      return image;
    }
  }
}
