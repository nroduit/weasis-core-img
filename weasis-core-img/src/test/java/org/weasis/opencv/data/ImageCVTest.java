/*
 * Copyright (c) 2010-2023 Weasis Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.weasis.opencv.data;

import static org.junit.jupiter.api.Assertions.*;

import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Range;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.osgi.OpenCVNativeLoader;

@DisplayNameGeneration(ReplaceUnderscores.class)
class ImageCVTest {

  @BeforeAll
  static void load_opencv_native_library() {
    var loader = new OpenCVNativeLoader();
    loader.init();
  }

  @Nested
  class Physical_Bytes_Calculation {

    @ParameterizedTest
    @MethodSource("image_type_test_data")
    void should_calculate_physical_bytes_correctly(ImageTestData testData) {
      try (var img =
          new ImageCV(testData.width, testData.height, testData.type, testData.fillValue)) {
        var expectedBytes = testData.width * testData.height * testData.expectedElemSize;

        assertAll(
            () -> assertEquals(expectedBytes, img.physicalBytes()),
            () -> assertEquals(testData.expectedChannels, img.channels()),
            () -> assertEquals(testData.width * testData.height, img.total()),
            () -> assertEquals(testData.expectedElemSize, img.elemSize()));
      }
    }

    static Stream<Arguments> image_type_test_data() {
      return Stream.of(
          Arguments.of(new ImageTestData(3, 3, CvType.CV_8UC1, new Scalar(128), 1, 1)),
          Arguments.of(new ImageTestData(4, 4, CvType.CV_8UC3, new Scalar(50, 100, 150), 3, 3)),
          Arguments.of(new ImageTestData(7, 7, CvType.CV_16UC1, new Scalar(65535), 1, 2)),
          Arguments.of(new ImageTestData(2, 3, CvType.CV_16SC2, new Scalar(-1000, 1000), 2, 4)),
          Arguments.of(new ImageTestData(5, 2, CvType.CV_32FC1, new Scalar(3.14159), 1, 4)),
          Arguments.of(new ImageTestData(3, 4, CvType.CV_64FC3, new Scalar(1.0, 2.0, 3.0), 3, 24)));
    }

    @Test
    void should_return_zero_bytes_for_default_constructor() {
      try (var img = new ImageCV()) {
        assertAll(
            () -> assertEquals(0, img.physicalBytes()),
            () -> assertEquals(0, img.total()),
            () -> assertEquals(0, img.width()),
            () -> assertEquals(0, img.height()));
      }
    }

    @Test
    void should_handle_empty_dimensions() {
      try (var img = new ImageCV(new Size(0, 0), CvType.CV_8UC1)) {
        assertEquals(0, img.physicalBytes());
      }
    }

    @Test
    void should_calculate_bytes_for_subregion_correctly() {
      try (var baseImage = new ImageCV(10, 10, CvType.CV_8UC1, new Scalar(255))) {
        try (var subImage = new ImageCV(baseImage, new Rect(2, 2, 4, 3))) {
          assertAll(
              () -> assertEquals(12, subImage.physicalBytes()), // 4x3x1
              () -> assertEquals(4, subImage.width()),
              () -> assertEquals(3, subImage.height()),
              () -> assertEquals(1, subImage.channels()));
        }
      }
    }
  }

  @Nested
  class Resource_Management {

    @Test
    void should_manage_release_lifecycle_correctly() {
      var img = new ImageCV(new Size(5, 5), CvType.CV_8UC1, new Scalar(100));

      // Initial state
      assertAll(
          () -> assertFalse(img.isReleased()), () -> assertFalse(img.isReleasedAfterProcessing()));

      // Mark for release after processing
      img.setReleasedAfterProcessing(true);
      assertAll(
          () -> assertTrue(img.isReleasedAfterProcessing()), () -> assertFalse(img.isReleased()));

      // Release and verify idempotency
      img.release();
      assertTrue(img.isReleased());

      img.release(); // Second call should be safe
      assertTrue(img.isReleased());
    }

    @Test
    void should_handle_close_correctly() {
      var img = new ImageCV(new Size(3, 3), CvType.CV_16UC1);

      assertFalse(img.isReleased());
      img.close();
      assertTrue(img.isReleased());
    }

    @Test
    void should_auto_close_in_try_with_resources() {
      ImageCV capturedImg;

      try (var img = new ImageCV(new Size(2, 2), CvType.CV_32FC1)) {
        capturedImg = img;
        assertFalse(img.isReleased());
      }

      assertTrue(capturedImg.isReleased());
    }
  }

  @Nested
  class Type_Conversions {

    @Test
    void should_convert_to_mat_returning_same_instance() {
      try (var imageCV = new ImageCV(4, 3, CvType.CV_8UC3, new Scalar(1, 2, 3))) {
        var mat = imageCV.toMat();

        assertAll(
            () -> assertSame(imageCV, mat),
            () -> assertEquals(imageCV.width(), mat.width()),
            () -> assertEquals(imageCV.height(), mat.height()),
            () -> assertEquals(imageCV.type(), mat.type()));
      }
    }

    @Test
    void should_convert_to_imageCV_returning_same_instance() {
      try (var original = new ImageCV(2, 3, CvType.CV_16UC1, new Scalar(1000))) {
        var converted = original.toImageCV();

        assertSame(original, converted);
      }
    }

    @Test
    void should_create_imageCV_from_regular_mat() {
      var originalMat = new Mat(3, 4, CvType.CV_8UC1, new Scalar(200));

      try (var converted = ImageCV.fromMat(originalMat)) {
        assertAll(
            () -> assertNotSame(originalMat, converted),
            () -> assertEquals(originalMat.width(), converted.width()),
            () -> assertEquals(originalMat.height(), converted.height()),
            () -> assertEquals(originalMat.type(), converted.type()),
            () -> assertInstanceOf(ImageCV.class, converted));

        // Verify data was copied correctly
        var originalData = new byte[1];
        var convertedData = new byte[1];
        originalMat.get(1, 1, originalData);
        converted.get(1, 1, convertedData);
        assertArrayEquals(originalData, convertedData);
      }

      originalMat.release();
    }

    @Test
    void should_return_same_instance_when_converting_imageCV_to_imageCV() {
      try (var original = new ImageCV(2, 2, CvType.CV_32FC1, new Scalar(3.14))) {
        var converted = ImageCV.fromMat(original);

        assertSame(original, converted);
      }
    }

    @Test
    void should_convert_via_static_toMat_method() {
      try (var imageCV = new ImageCV(new Size(5, 3), CvType.CV_8UC1, new Scalar(42))) {
        var mat = ImageCV.toMat(imageCV);

        assertSame(imageCV, mat);
      }
    }

    @Test
    void should_handle_null_input_in_static_methods() {
      assertAll(
          () -> assertThrows(NullPointerException.class, () -> ImageCV.fromMat(null)),
          () -> assertThrows(NullPointerException.class, () -> ImageCV.toMat(null)));
    }
  }

  @Nested
  class Constructor_Variants {

    @Test
    void should_create_from_dimensions_and_type() {
      try (var img = new ImageCV(6, 4, CvType.CV_16UC2)) {
        assertAll(
            () -> assertEquals(6, img.height()),
            () -> assertEquals(4, img.width()),
            () -> assertEquals(CvType.CV_16UC2, img.type()),
            () -> assertEquals(2, img.channels()));
      }
    }

    @Test
    void should_create_from_size_and_type() {
      var size = new Size(8, 5);
      try (var img = new ImageCV(size, CvType.CV_32FC3)) {
        assertAll(
            () -> assertEquals(8, img.width()),
            () -> assertEquals(5, img.height()),
            () -> assertEquals(CvType.CV_32FC3, img.type()),
            () -> assertEquals(3, img.channels()));
      }
    }

    @Test
    void should_create_with_scalar_initialization() {
      var initValue = new Scalar(50, 100, 200);
      try (var img = new ImageCV(2, 3, CvType.CV_8UC3, initValue)) {
        var pixelData = new byte[3];
        img.get(0, 0, pixelData);

        assertAll(
            () -> assertEquals(50, Byte.toUnsignedInt(pixelData[0])),
            () -> assertEquals(100, Byte.toUnsignedInt(pixelData[1])),
            () -> assertEquals(200, Byte.toUnsignedInt(pixelData[2])));
      }
    }

    @Test
    void should_create_subimage_with_ranges() {
      try (var baseImg = createTestImage(10, 8, CvType.CV_8UC1)) {
        // Row range only
        try (var rowRangeImg = new ImageCV(baseImg, new Range(2, 6))) {
          assertAll(
              () -> assertEquals(10, rowRangeImg.width()),
              () -> assertEquals(4, rowRangeImg.height()));
        }

        // Both row and column ranges
        try (var bothRangeImg = new ImageCV(baseImg, new Range(1, 4), new Range(2, 7))) {
          assertAll(
              () -> assertEquals(5, bothRangeImg.width()),
              () -> assertEquals(3, bothRangeImg.height()));
        }
      }
    }

    @Test
    void should_create_subimage_with_rect() {
      try (var baseImg = createTestImage(12, 10, CvType.CV_8UC1)) {
        var roi = new Rect(3, 2, 4, 5); // x, y, width, height

        try (var roiImg = new ImageCV(baseImg, roi)) {
          assertAll(() -> assertEquals(4, roiImg.width()), () -> assertEquals(5, roiImg.height()));
        }
      }
    }
  }

  @Nested
  class Data_Access_Operations {

    @Test
    void should_read_and_write_single_channel_data() {
      try (var img = new ImageCV(3, 3, CvType.CV_8UC1)) {
        // Write data
        img.put(1, 1, 123);

        // Read as byte array
        var byteData = new byte[1];
        var bytesRead = img.get(1, 1, byteData);

        // Read as double array
        var doubleData = img.get(1, 1);

        assertAll(
            () -> assertEquals(1, bytesRead),
            () -> assertEquals(123, Byte.toUnsignedInt(byteData[0])),
            () -> assertEquals(1, doubleData.length),
            () -> assertEquals(123.0, doubleData[0]));
      }
    }

    @Test
    void should_handle_multi_channel_data_access() {
      var testColor = new Scalar(25, 75, 125);
      try (var img = new ImageCV(4, 4, CvType.CV_8UC3, testColor)) {
        var data = new byte[3];
        var result = img.get(2, 1, data);

        assertAll(
            () -> assertEquals(3, result),
            () -> assertEquals(25, Byte.toUnsignedInt(data[0])),
            () -> assertEquals(75, Byte.toUnsignedInt(data[1])),
            () -> assertEquals(125, Byte.toUnsignedInt(data[2])));
      }
    }

    @Test
    void should_handle_different_data_types() {
      // 16-bit unsigned data
      try (var img16 = new ImageCV(2, 2, CvType.CV_16UC1, new Scalar(30000))) {
        var shortData = new short[1];
        img16.get(0, 0, shortData);
        assertEquals(30000, Short.toUnsignedInt(shortData[0]));
      }

      // 32-bit integer data
      try (var img32i = new ImageCV(2, 2, CvType.CV_32SC1, new Scalar(-50000))) {
        var intData = new int[1];
        img32i.get(0, 0, intData);
        assertEquals(-50000, intData[0]);
      }

      // 32-bit float data
      try (var img32f = new ImageCV(2, 2, CvType.CV_32FC1, new Scalar(3.14159f))) {
        var floatData = new float[1];
        img32f.get(0, 0, floatData);
        assertEquals(3.14159f, floatData[0], 0.00001f);
      }

      // 64-bit double data
      try (var img64f = new ImageCV(2, 2, CvType.CV_64FC1, new Scalar(2.71828))) {
        var doubleData = new double[1];
        img64f.get(0, 0, doubleData);
        assertEquals(2.71828, doubleData[0], 0.00001);
      }
    }

    @Test
    void should_assign_to_another_mat() {
      try (var source = new ImageCV(3, 4, CvType.CV_8UC1, new Scalar(200))) {
        var destination = new Mat();

        source.assignTo(destination);

        assertAll(
            () -> assertEquals(source.width(), destination.width()),
            () -> assertEquals(source.height(), destination.height()),
            () -> assertEquals(source.type(), destination.type()));

        // Verify data was copied
        var sourceData = new byte[1];
        var destData = new byte[1];
        source.get(1, 1, sourceData);
        destination.get(1, 1, destData);
        assertArrayEquals(sourceData, destData);
      }
    }
  }

  // Test utilities
  private static ImageCV createTestImage(int width, int height, int type) {
    var image = new ImageCV(height, width, type);
    fillWithGradientPattern(image);
    return image;
  }

  private static void fillWithGradientPattern(Mat image) {
    for (int y = 0; y < image.height(); y++) {
      for (int x = 0; x < image.width(); x++) {
        var value = (x + y) % 256;
        int type = image.type();

        if (type == CvType.CV_8UC1) {
          image.put(y, x, value);
        } else if (type == CvType.CV_8UC3) {
          image.put(y, x, value, value * 0.8, value * 0.6);
        } else if (type == CvType.CV_16UC1) {
          image.put(y, x, value * 256);
        }
      }
    }
  }

  // Test data record
  record ImageTestData(
      int width,
      int height,
      int type,
      Scalar fillValue,
      int expectedChannels,
      int expectedElemSize) {}
}
