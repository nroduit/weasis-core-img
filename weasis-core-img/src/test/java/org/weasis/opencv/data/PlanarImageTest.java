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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.osgi.OpenCVNativeLoader;

class PlanarImageTest {

  @BeforeAll
  public static void loadNativeLib() {
    // Load the native OpenCV library
    OpenCVNativeLoader loader = new OpenCVNativeLoader();
    loader.init();
  }

  /** Method under test: {@link PlanarImage#toImageCV()} */
  @Test
  void testToImageCV() {
    PlanarImage planarImage =
        new PlanarImage() {
          @Override
          public int channels() {
            return 0;
          }

          @Override
          public int dims() {
            return 0;
          }

          @Override
          public int depth() {
            return 0;
          }

          @Override
          public long elemSize() {
            return 0;
          }

          @Override
          public long elemSize1() {
            return 0;
          }

          @Override
          public void release() {}

          @Override
          public Size size() {
            return null;
          }

          @Override
          public int type() {
            return 0;
          }

          @Override
          public int height() {
            return 0;
          }

          @Override
          public int width() {
            return 0;
          }

          @Override
          public double[] get(int row, int column) {
            return new double[0];
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
          public boolean isHasBeenReleased() {
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
          public long physicalBytes() {
            return 0;
          }
        };

    assertThrowsExactly(IllegalAccessError.class, planarImage::toImageCV);

    assertThrowsExactly(IllegalAccessError.class, planarImage::toMat);

    assertThrowsExactly(IllegalAccessError.class, () -> ImageCV.toMat(planarImage));

    try (TestCV testCV = new TestCV(new Size(3, 3), CvType.CV_16UC3)) {
      ImageCV imgCV = testCV.toImageCV();
      assertEquals(testCV.size(), imgCV.size());
      assertEquals(testCV.dataAddr(), imgCV.dataAddr());
    }
  }

  static class TestCV extends Mat implements PlanarImage {
    public TestCV(Size size, int type) {
      super(size, type);
    }

    @Override
    public long physicalBytes() {
      return total() * elemSize();
    }

    @Override
    public boolean isHasBeenReleased() {
      return false;
    }

    @Override
    public boolean isReleasedAfterProcessing() {
      return false;
    }

    @Override
    public void setReleasedAfterProcessing(boolean releasedAfterProcessing) {}

    @Override
    public void close() {
      release();
    }
  }
}
