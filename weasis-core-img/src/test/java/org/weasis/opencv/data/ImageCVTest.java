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
import org.junit.jupiter.api.Test;
import org.opencv.core.CvType;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.osgi.OpenCVNativeLoader;

class ImageCVTest {
  @BeforeAll
  static void loadNativeLib() {
    // Load the native OpenCV library
    OpenCVNativeLoader loader = new OpenCVNativeLoader();
    loader.init();
  }

  @Test
  void physicalBytes() {
    try (ImageCV img = new ImageCV(3, 3, CvType.CV_8UC3, new Scalar(7))) {
      assertEquals(27, img.physicalBytes());
      assertEquals(img, ImageCV.toImageCV(img.toMat()));
    }

    try (ImageCV img = new ImageCV(new Size(4, 4), CvType.CV_8UC1, new Scalar(255))) {
      assertEquals(16, img.physicalBytes());
      assertEquals(img, ImageCV.toMat(img));
      assertEquals(img, img.toImageCV());
    }

    try (ImageCV img = new ImageCV(new Size(7, 7), CvType.CV_16SC1, new Scalar(-1024))) {
      assertEquals(98, img.physicalBytes());
      assertFalse(img.isHasBeenReleased());
      assertFalse(img.isReleasedAfterProcessing());
      img.setReleasedAfterProcessing(true);
      assertTrue(img.isReleasedAfterProcessing());
      img.release();
      assertTrue(img.isHasBeenReleased());
    }
  }
}
