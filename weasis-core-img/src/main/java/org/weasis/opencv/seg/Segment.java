/*
 * Copyright (c) 2023 Weasis Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.weasis.opencv.seg;

import java.awt.Dimension;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class Segment extends ArrayList<Point2D> {
  private final List<Segment> children = new ArrayList<>();

  public Segment() {
    super();
  }

  public Segment(Collection<? extends Point2D> point2DList) {
    this(point2DList, false);
  }

  public Segment(Collection<? extends Point2D> point2DList, boolean forceClose) {
    super();
    setPoints(point2DList, forceClose);
  }

  public Segment(float[] points) {
    this(points, null, false, null);
  }

  public Segment(float[] points, AffineTransform inverse, boolean forceClose, Dimension dim) {
    setPoints(points, inverse, forceClose, dim);
  }

  public Segment(double[] pts) {
    this(pts, null, false, null);
  }

  public Segment(double[] pts, AffineTransform inverse, boolean forceClose, Dimension dim) {
    setPoints(pts, inverse, forceClose, dim);
  }

  public void setPoints(Collection<? extends Point2D> point2DList, boolean forceClose) {
    clear();
    if (point2DList != null && !point2DList.isEmpty()) {
      addAll(point2DList);
      if (forceClose && shouldCloseSegment(point2DList)) {
        Point2D firstPoint = point2DList.iterator().next();
        add(new Point2D.Double(firstPoint.getX(), firstPoint.getY()));
      }
    }
  }

  public void setPoints(float[] points, boolean forceClose, Dimension dim) {
    setPoints(points, null, forceClose, dim);
  }

  public void setPoints(
      float[] points, AffineTransform inverse, boolean forceClose, Dimension dim) {
    Objects.requireNonNull(points, "Points array cannot be null");
    double[] transformedPoints = transformPoints(convertFloatToDouble(points), inverse);
    clear();
    addPoints(transformedPoints, forceClose, dim);
  }

  public void setPoints(double[] points, boolean forceClose, Dimension dim) {
    setPoints(points, null, forceClose, dim);
  }

  public void setPoints(
      double[] points, AffineTransform inverse, boolean forceClose, Dimension dim) {
    Objects.requireNonNull(points, "Points array cannot be null");
    double[] transformedPoints = transformPoints(points, inverse);
    clear();
    addPoints(transformedPoints, forceClose, dim);
  }

  private double[] transformPoints(double[] points, AffineTransform transform) {
    if (transform == null) {
      return points;
    }
    double[] transformedPoints = new double[points.length];
    transform.transform(points, 0, transformedPoints, 0, points.length / 2);
    return transformedPoints;
  }

  protected void addPoints(double[] pts, boolean forceClose, Dimension dim) {
    if (pts == null || pts.length < 4) { // Need at least 2 points (4 coordinates)
      return;
    }
    int pointCount = pts.length / 2;
    ensureCapacity(pointCount + (forceClose ? 1 : 0)); // Pre-allocate capacity

    boolean shouldResize = dim != null && dim.width > 0 && dim.height > 0;

    for (int i = 0; i < pointCount; i++) {
      double x = shouldResize ? pts[i * 2] * dim.width : pts[i * 2];
      double y = shouldResize ? pts[i * 2 + 1] * dim.height : pts[i * 2 + 1];
      add(new Point2D.Double(x, y));
    }

    var first = get(0);
    if (forceClose && !first.equals(get(pointCount - 1))) {
      add(new Point2D.Double(first.getX(), first.getY()));
    }
  }

  private boolean shouldCloseSegment(Collection<? extends Point2D> points) {
    if (points.size() < 2) {
      return false;
    }
    Point2D first = points.iterator().next();
    Point2D last = null;
    for (Point2D point : points) {
      last = point;
    }
    return !first.equals(last);
  }

  public List<Segment> getChildren() {
    return children;
  }

  public void addChild(Segment child) {
    if (child != null && child != this) { // Prevent null and self-reference
      children.add(child);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Segment segment)) return false;
    if (!super.equals(o)) return false;
    return Objects.equals(children, segment.children);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), children);
  }

  public static double[] convertFloatToDouble(float[] floatArray) {
    if (floatArray == null) {
      return null;
    }
    double[] doubleArray = new double[floatArray.length];
    for (int i = 0; i < floatArray.length; i++) {
      doubleArray[i] = floatArray[i];
    }
    return doubleArray;
  }
}
