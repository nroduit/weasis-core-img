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

import java.awt.Rectangle;
import java.awt.geom.Point2D.Double;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.osgi.OpenCVNativeLoader;
import org.weasis.opencv.data.ImageCV;
import org.weasis.opencv.data.PlanarImage;

class RegionTest {
  @BeforeAll
  public static void loadNativeLib() {
    // Load the native OpenCV library
    OpenCVNativeLoader loader = new OpenCVNativeLoader();
    loader.init();
  }

  @Test
  void createsRegionWithRandomIdWhenIdIsNull() {
    Region region = new Region(null);
    Assertions.assertNotNull(region.getId());
  }

  @Test
  void createsRegionWithGivenSegmentList() {
    Segment segment =
        new Segment(
            Arrays.asList(
                new Double(1, 2), new Double(21, 2), new Double(21, 12), new Double(1, 12))); // 200
    Segment child1 =
        new Segment(
            Arrays.asList(
                new Double(5, 4), new Double(15, 4), new Double(15, 10), new Double(5, 10))); // -60
    segment.addChild(child1);
    Segment child2 =
        new Segment(
            Arrays.asList(
                new Double(7, 6), new Double(12, 6), new Double(12, 8), new Double(7, 8))); // 10
    child1.addChild(child2);
    List<Segment> segments = Collections.singletonList(segment);
    Region region = new Region("testId");
    region.setSegmentList(segments);
    assertEquals(segments, region.getSegmentList());
    assertEquals(148.0, region.getArea()); // Approximation of area

    region = new Region("testId");
    region.setSegmentList(segments, 140);
    assertEquals(140.0, region.getNumberOfPixels());
    assertEquals(140.0, region.getArea());
  }

  @Test
  void createsRegionWithEmptySegmentListWhenSegmentListIsNull() {
    Region region = new Region("testId", null);
    assertEquals("testId", region.getId());
    assertTrue(region.getSegmentList().isEmpty());
    assertEquals(0.0, region.getArea());
    Assertions.assertNull(region.getAttributes());
    assertTrue(Region.buildSegmentList((PlanarImage) null, null).isEmpty());
  }

  @Test
  void buildsSegmentListFromBinaryImage() {
    Mat source = Mat.zeros(10, 20, CvType.CV_8UC1);
    Rectangle rect = new Rectangle(4, 3, 9, 5);
    Point[] pts = new Point[5];
    pts[0] = new Point(4, 3);
    pts[1] = new Point(13, 3);
    pts[2] = new Point(13, 8);
    pts[3] = new Point(4, 8);
    pts[4] = new Point(4, 3);

    Imgproc.fillPoly(source, Collections.singletonList(new MatOfPoint(pts)), new Scalar(255));
    int nbPixels = Core.countNonZero(source);
    Assertions.assertEquals(60.0, nbPixels);

    pts = new Point[5];
    pts[0] = new Point(6, 5);
    pts[1] = new Point(10, 5);
    pts[2] = new Point(10, 6);
    pts[3] = new Point(6, 6);
    pts[4] = new Point(6, 5);
    Imgproc.fillPoly(source, Collections.singletonList(new MatOfPoint(pts)), new Scalar(0));
    nbPixels = Core.countNonZero(source);
    Assertions.assertEquals(50.0, nbPixels); // Real area from the number of pixels

    List<Segment> segments = Region.buildSegmentList(ImageCV.toImageCV(source));
    Region region = new Region("testId", segments);
    assertEquals(46.0, region.getArea()); // Polygonal approximation of area
  }
}
