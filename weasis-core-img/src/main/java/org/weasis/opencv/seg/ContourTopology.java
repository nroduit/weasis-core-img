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

import java.awt.geom.Point2D;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;

/** A contour converted to a {@link Segment}, with the index of its parent contour. */
public class ContourTopology {

  private final Segment segment;
  private final int parent;

  /**
   * @param contour the OpenCV contour
   * @param parent the parent contour index (-1 if no parent)
   */
  public ContourTopology(MatOfPoint contour, int parent) {
    this(toSegment(contour), parent);
  }

  /**
   * @param contour the OpenCV contour
   * @param parent the parent contour index (-1 if no parent)
   */
  public ContourTopology(MatOfPoint2f contour, int parent) {
    this(toSegment(contour), parent);
  }

  /**
   * @param points the contour points
   * @param parent the parent contour index (-1 if no parent)
   */
  public ContourTopology(Point[] points, int parent) {
    this(toSegment(points), parent);
  }

  private ContourTopology(Segment segment, int parent) {
    this.segment = segment;
    this.parent = parent;
  }

  public int getParent() {
    return parent;
  }

  public Segment getSegment() {
    return segment;
  }

  /** Converts a MatOfPoint or MatOfPoint2f contour, or returns null for any other Mat. */
  static Segment toSegment(Mat contour) {
    if (contour instanceof MatOfPoint matOfPoint) {
      return toSegment(matOfPoint);
    }
    if (contour instanceof MatOfPoint2f matOfPoint2f) {
      return toSegment(matOfPoint2f);
    }
    return null;
  }

  // Reads the raw coordinates in one native call, without the intermediate Point[] of toArray()
  static Segment toSegment(MatOfPoint contour) {
    int count = (int) contour.total();
    var segment = newSegment(count);
    if (count > 0) {
      var data = new int[count * 2];
      contour.get(0, 0, data);
      for (int i = 0; i < data.length; i += 2) {
        segment.add(new Point2D.Double(data[i], data[i + 1]));
      }
    }
    return segment;
  }

  static Segment toSegment(MatOfPoint2f contour) {
    int count = (int) contour.total();
    var segment = newSegment(count);
    if (count > 0) {
      var data = new float[count * 2];
      contour.get(0, 0, data);
      for (int i = 0; i < data.length; i += 2) {
        segment.add(new Point2D.Double(data[i], data[i + 1]));
      }
    }
    return segment;
  }

  private static Segment toSegment(Point[] points) {
    var segment = newSegment(points.length);
    for (Point point : points) {
      segment.add(new Point2D.Double(point.x, point.y));
    }
    return segment;
  }

  private static Segment newSegment(int capacity) {
    var segment = new Segment();
    segment.ensureCapacity(capacity);
    return segment;
  }
}
