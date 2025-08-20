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
import java.util.Arrays;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;

/**
 * Represents a contour topology containing a segment and its parent relationship. This class
 * encapsulates the hierarchical structure of contours in image processing.
 */
public class ContourTopology {

  private final Segment segment;
  private final int parent;

  /**
   * Creates a ContourTopology from a MatOfPoint contour.
   *
   * @param contour the OpenCV MatOfPoint contour
   * @param parent the parent contour index (-1 if no parent)
   */
  public ContourTopology(MatOfPoint contour, int parent) {
    this(contour.toArray(), parent);
  }

  /**
   * Creates a ContourTopology from a MatOfPoint2f contour.
   *
   * @param contour the OpenCV MatOfPoint2f contour
   * @param parent the parent contour index (-1 if no parent)
   */
  public ContourTopology(MatOfPoint2f contour, int parent) {
    this(contour.toArray(), parent);
  }

  /**
   * Creates a ContourTopology from an array of OpenCV Points.
   *
   * @param points the array of OpenCV Points
   * @param parent the parent contour index (-1 if no parent)
   */
  public ContourTopology(Point[] points, int parent) {
    this.parent = parent;
    this.segment = createSegment(points);
  }

  public int getParent() {
    return parent;
  }

  public Segment getSegment() {
    return segment;
  }

  private Segment createSegment(Point[] points) {
    return new Segment(Arrays.stream(points).map(this::convertToPoint2D).toList());
  }

  private Point2D.Double convertToPoint2D(Point point) {
    return new Point2D.Double(point.x, point.y);
  }
}
