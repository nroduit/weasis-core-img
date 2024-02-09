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

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Dimension;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Double;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opencv.osgi.OpenCVNativeLoader;

class SegmentTest {
  @BeforeAll
  public static void loadNativeLib() {
    // Load the native OpenCV library
    OpenCVNativeLoader loader = new OpenCVNativeLoader();
    loader.init();
  }

  @Test
  void createsSegmentWithEmptyPoints() {
    Segment segment = new Segment(Collections.emptyList());
    assertTrue(segment.isEmpty());
  }

  @Test
  void createsSegmentWithGivenPoints() {
    List<Point2D> points =
        Arrays.asList(new Double(1, 2), new Double(21, 2), new Double(21, 12), new Double(1, 12));
    Segment segment = new Segment(points);
    assertEquals(points, segment);
  }

  @Test
  void createsSegmentWithForcedClosure() {
    List<Point2D> points =
        Arrays.asList(
            new Point2D.Double(1, 2), new Point2D.Double(11, 4), new Point2D.Double(6, 9));
    Segment segment = new Segment(points, true);
    assertEquals(4, segment.size());
    assertEquals(points.get(0), segment.get(segment.size() - 1));
  }

  @Test
  void addsChildSegment() {
    float[] pPts = new float[] {1.0f, 2.0f, 11.0f, 4.0f, 6.0f, 9.0f};
    float[] cPts = new float[] {2.0f, 3.0f, 3.0f, 4.0f};
    Segment parent = new Segment(pPts);
    Segment child = new Segment(cPts);
    parent.addChild(child);
    assertTrue(parent.getChildren().contains(child));

    Segment parent2 = new Segment(pPts);
    parent2.addChild(new Segment(cPts));
    assertEquals(parent, parent2);
  }

  @Test
  void convertsFloatArrayToDoubleArray() {
    float[] floatArray = {1.0f, 2.0f, 3.0f};
    double[] expected = {1.0, 2.0, 3.0};
    double[] result = Segment.convertFloatToDouble(floatArray);
    assertArrayEquals(expected, result);
  }

  @Test
  void setPointsWithNullInverseTransform() {
    Segment segment = new Segment();
    float[] points = {1.0f, 2.0f, 3.0f, 4.0f};
    segment.setPoints(points, false, null);
    assertEquals(2, segment.size());
    assertEquals(new Point2D.Double(1.0, 2.0), segment.get(0));
    assertEquals(new Point2D.Double(3.0, 4.0), segment.get(1));
  }

  @Test
  void setPointsWithInverseTransform() {
    Segment segment = new Segment();
    float[] points = {1.0f, 2.0f, 3.0f, 4.0f};
    AffineTransform inverse = AffineTransform.getScaleInstance(2.0, 2.0);
    segment.setPoints(points, inverse, false, null);
    assertEquals(2, segment.size());
    assertEquals(new Point2D.Double(2.0, 4.0), segment.get(0));
    assertEquals(new Point2D.Double(6.0, 8.0), segment.get(1));

    segment.setPoints(new double[] {1.0, 2.0, 3.0, 4.0}, inverse, false, null);
    assertEquals(new Point2D.Double(2.0, 4.0), segment.get(0));
    assertEquals(new Point2D.Double(6.0, 8.0), segment.get(1));
  }

  @Test
  void setPointsWithForceClose() {
    Segment segment = new Segment();
    float[] points = {1.0f, 2.0f, 3.0f, 4.0f};
    segment.setPoints(points, true, null);
    assertEquals(3, segment.size());
    assertEquals(new Point2D.Double(1.0, 2.0), segment.get(0));
    assertEquals(new Point2D.Double(3.0, 4.0), segment.get(1));
    assertEquals(new Point2D.Double(1.0, 2.0), segment.get(2));
  }

  @Test
  void setPointsWithDimension() {
    double[] points = {0.5, 0.5, 1.5, 1.5};
    Dimension dim = new Dimension(200, 200);
    Segment segment = new Segment(points, null, false, dim);
    segment.setPoints(points, false, dim);
    assertEquals(2, segment.size());
    assertEquals(new Point2D.Double(100.0, 100.0), segment.get(0));
    assertEquals(new Point2D.Double(300.0, 300.0), segment.get(1));
  }
}
