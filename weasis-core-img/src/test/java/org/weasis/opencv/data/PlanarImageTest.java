/*
 * Copyright (c) 2023 Weasis Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.weasis.opencv.data;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.osgi.OpenCVNativeLoader;

@DisplayNameGeneration(ReplaceUnderscores.class)
class PlanarImageTest {

  @BeforeAll
  static void load_native_library() {
    var loader = new OpenCVNativeLoader();
    loader.init();
  }

  @Nested
  class Conversion_Tests {

    @Test
    void should_convert_ImageCV_to_itself() {
      try (var imageCV = createTestImageCV(10, 8, CvType.CV_8UC3)) {
        var result = imageCV.toImageCV();

        assertSame(imageCV, result);
        assertEquals(imageCV.size(), result.size());
        assertEquals(imageCV.type(), result.type());
      }
    }

    @Test
    void should_convert_Mat_to_ImageCV() {
      var mat = createTestMat(5, 7, CvType.CV_16UC1);
      var result = ImageCV.fromMat(mat);

      assertNotSame(mat, result);
      assertEquals(mat.size(), result.size());
      assertEquals(mat.type(), result.type());
      assertEquals(mat.channels(), result.channels());

      result.close();
    }

    @Test
    void should_convert_Mat_to_Mat() {
      var mat = createTestMat(4, 6, CvType.CV_32FC2);
      var result = ImageCV.fromMat(mat);

      assertNotSame(mat, result); // Different instance
      assertEquals(mat.size(), result.size());
      assertEquals(mat.type(), result.type());
    }

    @Test
    void should_throw_when_converting_unsupported_implementation_to_ImageCV() {
      var unsupportedImage = new UnsupportedPlanarImage();

      var exception =
          assertThrows(UnsupportedOperationException.class, unsupportedImage::toImageCV);
    }

    @Test
    void should_throw_when_converting_unsupported_implementation_to_Mat() {
      var unsupportedImage = new UnsupportedPlanarImage();

      var exception = assertThrows(UnsupportedOperationException.class, unsupportedImage::toMat);
    }
  }

  @Nested
  class Static_Factory_Tests {

    @Test
    void should_convert_PlanarImage_to_Mat_via_static_method() {
      try (var imageCV = createTestImageCV(3, 4, CvType.CV_8UC1)) {
        var result = ImageCV.toMat(imageCV);

        assertSame(imageCV, result);
        assertEquals(imageCV.size(), result.size());
        assertEquals(imageCV.type(), result.type());
      }
    }

    @Test
    void should_throw_when_static_conversion_with_unsupported_type() {
      var unsupportedImage = new UnsupportedPlanarImage();

      var exception =
          assertThrows(UnsupportedOperationException.class, () -> ImageCV.toMat(unsupportedImage));
    }
  }

  @Nested
  class Resource_Management_Tests {

    @Test
    void should_handle_resource_lifecycle_correctly() {
      var testImage = new TestPlanarImage(new Size(5, 5), CvType.CV_8UC1);

      assertFalse(testImage.isReleased());
      assertFalse(testImage.isReleasedAfterProcessing());

      testImage.setReleasedAfterProcessing(true);
      assertTrue(testImage.isReleasedAfterProcessing());

      testImage.close();
      assertTrue(testImage.isReleased());
    }

    @Test
    void should_calculate_physical_bytes_correctly() {
      try (var imageCV = createTestImageCV(10, 15, CvType.CV_8UC3)) {
        var expectedBytes = imageCV.total() * imageCV.elemSize();
        assertEquals(expectedBytes, imageCV.physicalBytes());
        assertEquals(450, imageCV.physicalBytes()); // 10*15*3*1
      }
    }
  }

  @Nested
  class Data_Access_Tests {

    @Test
    void should_access_pixel_data_correctly() {
      try (var imageCV = createTestImageCV(3, 3, CvType.CV_8UC3)) {
        // Set test data
        imageCV.put(1, 1, 100, 150, 200);

        var pixelData = new byte[3];
        var bytesRead = imageCV.get(1, 1, pixelData);

        assertEquals(3, bytesRead);
        assertArrayEquals(new byte[] {100, (byte) 150, (byte) 200}, pixelData);
      }
    }

    @Test
    void should_get_double_array_from_pixel() {
      try (var imageCV = createTestImageCV(2, 2, CvType.CV_64FC1)) {
        imageCV.put(0, 0, 3.14159);

        var result = imageCV.get(0, 0);

        assertNotNull(result);
        assertEquals(1, result.length);
        assertEquals(3.14159, result[0], 0.00001);
      }
    }
  }

  // Test utilities
  private static ImageCV createTestImageCV(int width, int height, int type) {
    var image = new ImageCV(height, width, type);
    fillWithTestPattern(image);
    return image;
  }

  private static Mat createTestMat(int width, int height, int type) {
    var image = new Mat(height, width, type);
    fillWithTestPattern(image);
    return image;
  }

  private static void fillWithTestPattern(Mat image) {
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
        } else if (type == CvType.CV_32FC2) {
          image.put(y, x, value / 255.0, (value * 2) / 255.0);
        } else if (type == CvType.CV_64FC1) {
          image.put(y, x, value / 255.0);
        }
      }
    }
  }

  // Helper assertion method
  private static void assertThat(String actual) {
    new StringAssert(actual);
  }

  private record StringAssert(String value) {
    public void contains(String expected) {
      assertTrue(
          value.contains(expected), "Expected '%s' to contain '%s'".formatted(value, expected));
    }
  }

  // Test implementations
  static class TestPlanarImage extends Mat implements PlanarImage {
    private boolean releasedAfterProcessing;
    private boolean released;

    public TestPlanarImage(Size size, int type) {
      super(size, type);
    }

    @Override
    public long physicalBytes() {
      return total() * elemSize();
    }

    @Override
    public void release() {
      if (!released) {
        super.release();
        released = true;
      }
    }

    @Override
    public boolean isReleased() {
      return released;
    }

    @Override
    public boolean isReleasedAfterProcessing() {
      return releasedAfterProcessing;
    }

    @Override
    public void setReleasedAfterProcessing(boolean releasedAfterProcessing) {
      this.releasedAfterProcessing = releasedAfterProcessing;
    }

    @Override
    public void close() {
      release();
    }

    @Override
    public boolean isHasBeenReleased() {
      return isReleased();
    }
  }

  /** Minimal implementation that doesn't support conversion */
  static class UnsupportedPlanarImage implements PlanarImage {
    @Override
    public int channels() {
      return 1;
    }

    @Override
    public int dims() {
      return 2;
    }

    @Override
    public int depth() {
      return CvType.CV_8U;
    }

    @Override
    public long elemSize() {
      return 1;
    }

    @Override
    public long elemSize1() {
      return 1;
    }

    @Override
    public Size size() {
      return new Size(1, 1);
    }

    @Override
    public int type() {
      return CvType.CV_8UC1;
    }

    @Override
    public int height() {
      return 1;
    }

    @Override
    public int width() {
      return 1;
    }

    @Override
    public double[] get(int row, int column) {
      return new double[] {0};
    }

    @Override
    public int get(int i, int j, byte[] pixelData) {
      return 0;
    }

    @Override
    public int get(int i, int j, short[] data) {
      return 0;
    }

    @Override
    public int get(int i, int j, int[] data) {
      return 0;
    }

    @Override
    public int get(int i, int j, float[] data) {
      return 0;
    }

    @Override
    public int get(int i, int j, double[] data) {
      return 0;
    }

    @Override
    public void assignTo(Mat dstImg) {}

    @Override
    public void release() {}

    @Override
    public boolean isReleased() {
      return false;
    }

    @Override
    public boolean isReleasedAfterProcessing() {
      return false;
    }

    @Override
    public void setReleasedAfterProcessing(boolean releasedAfterProcessing) {}

    @Override
    public void close() {}

    @Override
    public boolean isHasBeenReleased() {
      return false;
    }

    @Override
    public long physicalBytes() {
      return 1;
    }
  }
}
