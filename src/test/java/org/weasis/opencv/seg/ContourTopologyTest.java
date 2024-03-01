/*
 * Copyright (c) 2024 Weasis Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.weasis.opencv.seg;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.geom.Point2D;
import java.util.Arrays;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.osgi.OpenCVNativeLoader;

class ContourTopologyTest {
  @BeforeAll
  public static void loadNativeLib() {
    // Load the native OpenCV library
    OpenCVNativeLoader loader = new OpenCVNativeLoader();
    loader.init();
  }

  @Test
  void createsSegmentFromContour() {
    MatOfPoint contour = new MatOfPoint(new Point(1, 2), new Point(3, 4));
    ContourTopology contourTopology = new ContourTopology(contour, 1);

    Segment segment = contourTopology.getSegment();

    assertEquals(Arrays.asList(new Point2D.Double(1, 2), new Point2D.Double(3, 4)), segment);
    assertEquals(1, contourTopology.getParent());
  }

  @Test
  void createsEmptySegmentFromEmptyContour() {
    MatOfPoint contour = new MatOfPoint();
    ContourTopology contourTopology = new ContourTopology(contour, 0);

    Segment segment = contourTopology.getSegment();

    assertEquals(0, segment.size());
  }
}
