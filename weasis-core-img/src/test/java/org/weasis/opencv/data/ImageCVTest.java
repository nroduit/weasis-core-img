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

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Range;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.osgi.OpenCVNativeLoader;

@DisplayName("ImageCV Tests")
class ImageCVTest {
  @BeforeAll
  @DisplayName("Load OpenCV Native Library")
  static void loadNativeLib() {
    // Load the native OpenCV library
    OpenCVNativeLoader loader = new OpenCVNativeLoader();
    loader.init();
  }

  @Nested
  @DisplayName("Physical Bytes Calculation Tests")
  class PhysicalBytesTests {
    @Test
    @DisplayName("Should calculate correct physical bytes for 3-channel 8-bit image")
    void shouldCalculatePhysicalBytesFor3Channel8BitImage() {
      try (ImageCV img = new ImageCV(3, 3, CvType.CV_8UC3, new Scalar(7))) {
        assertEquals(27, img.physicalBytes(), "3x3 CV_8UC3 should be 27 bytes");
        assertEquals(3, img.channels());
        assertEquals(9, img.total());
        assertEquals(3, img.elemSize());
      }
    }

    @Test
    @DisplayName("Should calculate correct physical bytes for single-channel 8-bit image")
    void shouldCalculatePhysicalBytesForSingleChannel8BitImage() {
      try (ImageCV img = new ImageCV(new Size(4, 4), CvType.CV_8UC1, new Scalar(255))) {
        assertEquals(16, img.physicalBytes(), "4x4 CV_8UC1 should be 16 bytes");
        assertEquals(1, img.channels());
        assertEquals(16, img.total());
        assertEquals(1, img.elemSize());
      }
    }

    @Test
    @DisplayName("Should calculate correct physical bytes for 16-bit signed image")
    void shouldCalculatePhysicalBytesFor16BitSignedImage() {
      try (ImageCV img = new ImageCV(new Size(7, 7), CvType.CV_16SC1, new Scalar(-1024))) {
        assertEquals(98, img.physicalBytes(), "7x7 CV_16SC1 should be 98 bytes");
        assertEquals(1, img.channels());
        assertEquals(49, img.total());
        assertEquals(2, img.elemSize());
      }
    }

    @Test
    @DisplayName("Should return zero bytes for default constructor")
    void shouldReturnZeroBytesForDefaultConstructor() {
      try (ImageCV img = new ImageCV()) {
        assertEquals(0, img.physicalBytes(), "Empty image should have 0 bytes");
        assertEquals(0, img.total());
      }
    }

    @Test
    @DisplayName("Should return zero bytes for empty image dimensions")
    void shouldReturnZeroBytesForEmptyImageDimensions() {
      try (ImageCV img = new ImageCV(new Size(0, 0), CvType.CV_8UC1)) {
        assertEquals(0, img.physicalBytes(), "0x0 image should have 0 bytes");
        assertEquals(0, img.total());
      }
    }

    @Test
    @DisplayName("Should calculate correct bytes for subregion image")
    void shouldCalculateCorrectBytesForSubregionImage() {
      try (ImageCV baseImage = new ImageCV(5, 5, CvType.CV_8UC1, new Scalar(4))) {
        try (ImageCV rangedImage = new ImageCV(baseImage, new Rect(1, 1, 2, 2))) {
          assertEquals(4, rangedImage.physicalBytes(), "2x2 subregion should be 4 bytes");
          assertEquals(2, rangedImage.width());
          assertEquals(2, rangedImage.height());
        }
      }
    }
  }

  @Nested
  @DisplayName("Resource Management Tests")
  class ResourceManagementTests {

    @Test
    @DisplayName("Should manage release state correctly")
    void shouldManageReleaseStateCorrectly() {
      ImageCV img = new ImageCV(new Size(3, 3), CvType.CV_8UC1, new Scalar(100));

      assertFalse(img.isReleased(), "Image should not be released initially");
      assertFalse(
          img.isReleasedAfterProcessing(),
          "Image should not be marked for release after processing initially");

      img.setReleasedAfterProcessing(true);
      assertTrue(
          img.isReleasedAfterProcessing(), "Image should be marked for release after processing");
      assertFalse(img.isReleased(), "Image should not be released yet");

      img.release();
      assertTrue(img.isReleased(), "Image should be released");

      // Test double release safety
      img.release();
      assertTrue(img.isReleased(), "Image should remain released after double release");
    }

    @Test
    @DisplayName("Should handle close() properly")
    void shouldHandleCloseCorrectly() {
      ImageCV img = new ImageCV(new Size(2, 2), CvType.CV_8UC1);

      assertFalse(img.isReleased(), "Image should not be released before close");

      img.close();
      assertTrue(img.isReleased(), "Image should be released after close");
    }

    @Test
    @DisplayName("Should work correctly with try-with-resources")
    void shouldWorkCorrectlyWithTryWithResources() {
      ImageCV img;
      try (ImageCV autoCloseImg = new ImageCV(new Size(2, 2), CvType.CV_8UC1)) {
        img = autoCloseImg;
        assertFalse(img.isReleased(), "Image should not be released inside try block");
      }
      assertTrue(img.isReleased(), "Image should be released after try block");
    }
  }

  @Nested
  @DisplayName("Conversion Tests")
  class ConversionTests {

    @Test
    @DisplayName("Should convert to Mat correctly")
    void shouldConvertToMatCorrectly() {
      try (ImageCV img = new ImageCV(3, 3, CvType.CV_8UC1, new Scalar(42))) {
        Mat mat = img.toMat();
        assertSame(img, mat, "toMat should return the same instance for ImageCV");
        assertEquals(img.width(), mat.width());
        assertEquals(img.height(), mat.height());
        assertEquals(img.type(), mat.type());
      }
    }

    @Test
    @DisplayName("Should convert to ImageCV correctly")
    void shouldConvertToImageCVCorrectly() {
      try (ImageCV original = new ImageCV(3, 3, CvType.CV_8UC1, new Scalar(42))) {
        ImageCV converted = original.toImageCV();
        assertSame(original, converted, "toImageCV should return the same instance for ImageCV");
      }
    }

    @Test
    @DisplayName("Should convert static toMat correctly")
    void shouldConvertStaticToMatCorrectly() {
      try (ImageCV img = new ImageCV(new Size(4, 4), CvType.CV_8UC1, new Scalar(255))) {
        Mat mat = ImageCV.toMat(img);
        assertSame(img, mat, "Static toMat should return the same instance for ImageCV");
      }
    }

    @Test
    @DisplayName("Should convert static fromMat correctly for ImageCV")
    void shouldConvertStaticFromMatCorrectlyForImageCV() {
      try (ImageCV original = new ImageCV(3, 3, CvType.CV_8UC3, new Scalar(7))) {
        ImageCV converted = ImageCV.fromMat(original);
        assertSame(
            original,
            converted,
            "fromMat should return the same instance when input is already ImageCV");
      }
    }

    @Test
    @DisplayName("Should convert static fromMat correctly for regular Mat")
    void shouldConvertStaticFromMatCorrectlyForRegularMat() {
      Mat originalMat = new Mat(2, 2, CvType.CV_8UC1, new Scalar(128));
      try (ImageCV converted = ImageCV.fromMat(originalMat)) {
        assertEquals(originalMat.width(), converted.width());
        assertEquals(originalMat.height(), converted.height());
        assertEquals(originalMat.type(), converted.type());
        assertNotSame(originalMat, converted, "fromMat should create new instance for regular Mat");
      }
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Should create image with rows and columns")
    void shouldCreateImageWithRowsAndColumns() {
      try (ImageCV img = new ImageCV(5, 3, CvType.CV_8UC1)) {
        assertEquals(5, img.height());
        assertEquals(3, img.width());
        assertEquals(CvType.CV_8UC1, img.type());
      }
    }

    @Test
    @DisplayName("Should create image with Size")
    void shouldCreateImageWithSize() {
      Size size = new Size(4, 6);
      try (ImageCV img = new ImageCV(size, CvType.CV_16UC1)) {
        assertEquals(4, img.width());
        assertEquals(6, img.height());
        assertEquals(CvType.CV_16UC1, img.type());
      }
    }

    @Test
    @DisplayName("Should create image with Range constructors")
    void shouldCreateImageWithRangeConstructors() {
      try (ImageCV baseImg = new ImageCV(10, 10, CvType.CV_8UC1, new Scalar(50))) {
        try (ImageCV rowRangeImg = new ImageCV(baseImg, new Range(2, 8))) {
          assertEquals(10, rowRangeImg.width());
          assertEquals(6, rowRangeImg.height());
        }

        try (ImageCV bothRangeImg = new ImageCV(baseImg, new Range(1, 5), new Range(2, 7))) {
          assertEquals(5, bothRangeImg.width());
          assertEquals(4, bothRangeImg.height());
        }
      }
    }
  }

  @Nested
  @DisplayName("Data Access Tests")
  class DataAccessTests {

    @Test
    @DisplayName("Should access pixel data correctly")
    void shouldAccessPixelDataCorrectly() {
      try (ImageCV img = new ImageCV(3, 3, CvType.CV_8UC1, new Scalar(100))) {
        byte[] data = new byte[1];
        int result = img.get(1, 1, data);
        assertEquals(1, result, "Should return 1 for successful read");
        assertEquals(100, Byte.toUnsignedInt(data[0]), "Pixel value should match");
      }
    }

    @Test
    @DisplayName("Should handle multi-channel data access")
    void shouldHandleMultiChannelDataAccess() {
      Scalar color = new Scalar(50, 100, 150);
      try (ImageCV img = new ImageCV(2, 2, CvType.CV_8UC3, color)) {
        byte[] data = new byte[3];
        int result = img.get(0, 0, data);
        assertEquals(3, result, "Should return 3 for 3-channel read");
        assertEquals(50, Byte.toUnsignedInt(data[0]), "B channel should match");
        assertEquals(100, Byte.toUnsignedInt(data[1]), "G channel should match");
        assertEquals(150, Byte.toUnsignedInt(data[2]), "R channel should match");
      }
    }
  }
}
