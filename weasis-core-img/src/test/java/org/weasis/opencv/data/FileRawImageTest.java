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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Paths;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opencv.core.CvType;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.osgi.OpenCVNativeLoader;
import org.weasis.core.util.FileUtil;

class FileRawImageTest {
  @BeforeAll
  static void loadNativeLib() {
    // Load the native OpenCV library
    OpenCVNativeLoader loader = new OpenCVNativeLoader();
    loader.init();
  }

  /**
   * Method under test:
   *
   * <ul>
   *   <li>{@link FileRawImage#FileRawImage(File)}
   *   <li>{@link FileRawImage#file()}
   *   <li>{@link FileRawImage#read()}
   *   <li>{@link FileRawImage#write(PlanarImage)}
   * </ul>
   */
  @Test
  void testWriteAndRead() {
    File file = Paths.get(System.getProperty("java.io.tmpdir"), "fileRawImage.wcv").toFile();
    FileRawImage rawImg = new FileRawImage(file);
    assertSame(file, rawImg.file());

    try (ImageCV img = new ImageCV(new Size(3, 3), CvType.CV_16UC3, new Scalar(3, 4, 5))) {
      assertTrue(rawImg.write(img));
    }

    try (ImageCV img = rawImg.read()) {
      assertNotNull(img);
      assertEquals(3, img.width());
      assertEquals(3, img.height());
      assertEquals(3, img.channels());
      assertEquals(CvType.CV_16UC3, img.type());
      assertEquals(2, img.depth());
      assertEquals(6, img.elemSize());
      assertEquals(2, img.elemSize1());
      assertEquals(54, img.physicalBytes());
      assertEquals(54, img.total() * img.elemSize());
      short[] data = new short[3];
      img.get(1, 1, data);
      assertArrayEquals(new short[] {3, 4, 5}, data);
    }

    FileUtil.delete(file);
  }
}
