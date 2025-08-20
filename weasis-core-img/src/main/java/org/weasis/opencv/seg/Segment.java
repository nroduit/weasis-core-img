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
import java.util.*;

/**
 * Represents a geometric segment containing a series of 2D points. Extends ArrayList to provide
 * direct point manipulation while supporting hierarchical structures through child segments.
 */
public class Segment extends ArrayList<Point2D> {
  protected final List<Segment> children = new ArrayList<>();

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

  /**
   * Sets points from a collection, optionally closing the segment by adding the first point at the
   * end.
   *
   * @param point2DList the collection of points to set
   * @param forceClose whether to close the segment by duplicating the first point
   */
  public void setPoints(Collection<? extends Point2D> point2DList, boolean forceClose) {
    clear();
    if (point2DList == null || point2DList.isEmpty()) {
      return;
    }
    addAll(point2DList);
    if (forceClose && isOpenSegment()) {
      Point2D firstPoint = get(0);
      add(new Point2D.Double(firstPoint.getX(), firstPoint.getY()));
    }
  }

  public void setPoints(float[] points, boolean forceClose, Dimension dim) {
    setPoints(points, null, forceClose, dim);
  }

  /** Sets points from a float array with optional transform and dimension scaling. */
  public void setPoints(
      float[] points, AffineTransform inverse, boolean forceClose, Dimension dim) {
    Objects.requireNonNull(points, "Points array cannot be null");
    setPoints(convertFloatToDouble(points), inverse, forceClose, dim);
  }

  public void setPoints(double[] points, boolean forceClose, Dimension dim) {
    setPoints(points, null, forceClose, dim);
  }

  /** Sets points from a double array with optional transform and dimension scaling. */
  public void setPoints(
      double[] points, AffineTransform inverse, boolean forceClose, Dimension dim) {
    Objects.requireNonNull(points, "Points array cannot be null");
    if (points.length < 4) { // Need at least 2 points (4 coordinates)
      clear();
      return;
    }

    double[] transformedPoints = applyTransform(points, inverse);
    clear();
    addPointsFromArray(transformedPoints, forceClose, dim);
  }

  private double[] applyTransform(double[] points, AffineTransform transform) {
    if (transform == null) {
      return points;
    }
    double[] transformedPoints = new double[points.length];
    transform.transform(points, 0, transformedPoints, 0, points.length / 2);
    return transformedPoints;
  }

  private void addPointsFromArray(double[] pts, boolean forceClose, Dimension dim) {
    int pointCount = pts.length / 2;
    ensureCapacity(pointCount + (forceClose ? 1 : 0));

    boolean shouldScale = isValidDimension(dim);

    for (int i = 0; i < pointCount; i++) {
      double x = shouldScale ? pts[i * 2] * dim.width : pts[i * 2];
      double y = shouldScale ? pts[i * 2 + 1] * dim.height : pts[i * 2 + 1];
      add(new Point2D.Double(x, y));
    }

    if (forceClose && isOpenSegment()) {
      Point2D first = get(0);
      add(new Point2D.Double(first.getX(), first.getY()));
    }
  }

  private boolean isValidDimension(Dimension dim) {
    return dim != null && dim.width > 0 && dim.height > 0;
  }

  private boolean isOpenSegment() {
    return size() >= 2 && !get(0).equals(get(size() - 1));
  }

  /** Returns an unmodifiable view of the child segments. */
  public List<Segment> getChildren() {
    return List.copyOf(children);
  }

  /** Adds a child segment, preventing null references and self-references. */
  public void addChild(Segment child) {
    if (child != null && child != this) {
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

  /** Converts a float array to a double array. */
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
