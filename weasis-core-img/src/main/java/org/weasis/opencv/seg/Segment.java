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
import java.util.List;
import java.util.Objects;

public class Segment extends ArrayList<Point2D> {
  private final List<Segment> children = new ArrayList<>();

  public Segment() {
    super();
  }

  public Segment(List<Point2D> point2DList) {
    this(point2DList, false);
  }

  public Segment(List<Point2D> point2DList, boolean forceClose) {
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

  public void setPoints(List<Point2D> point2DList, boolean forceClose) {
    if (point2DList != null && !point2DList.isEmpty()) {
      addAll(point2DList);
      if (forceClose && !point2DList.get(0).equals(point2DList.get(point2DList.size() - 1))) {
        add((Point2D.Double) point2DList.get(0).clone());
      }
    }
  }

  public void setPoints(float[] points, boolean forceClose, Dimension dim) {
    setPoints(points, null, forceClose, dim);
  }

  public void setPoints(
      float[] points, AffineTransform inverse, boolean forceClose, Dimension dim) {
    double[] pts;
    Objects.requireNonNull(points);
    if (inverse == null) {
      pts = convertFloatToDouble(points);
    } else {
      double[] dstPoints = new double[points.length];
      inverse.transform(points, 0, dstPoints, 0, points.length / 2);
      pts = dstPoints;
    }
    addPoints(pts, forceClose, dim);
  }

  public void setPoints(double[] points, boolean forceClose, Dimension dim) {
    setPoints(points, null, forceClose, dim);
  }

  public void setPoints(
      double[] points, AffineTransform inverse, boolean forceClose, Dimension dim) {
    double[] pts;
    if (inverse == null) {
      pts = points;
    } else {
      double[] dstPoints = new double[points.length];
      inverse.transform(points, 0, dstPoints, 0, points.length / 2);
      pts = dstPoints;
    }
    addPoints(pts, forceClose, dim);
  }

  protected void addPoints(double[] pts, boolean forceClose, Dimension dim) {
    clear();
    if (pts == null) {
      return;
    }
    int size = pts.length / 2;
    if (size >= 2) {
      boolean resize = dim != null && dim.width > 0 && dim.height > 0;
      for (int i = 0; i < size; i++) {
        double x = resize ? pts[i * 2] * dim.width : pts[i * 2];
        double y = resize ? pts[i * 2 + 1] * dim.height : pts[i * 2 + 1];
        add(new Point2D.Double(x, y));
      }
      if (forceClose && !get(0).equals(get(size - 1))) {
        add((Point2D.Double) get(0).clone());
      }
    }
  }

  public List<Segment> getChildren() {
    return children;
  }

  public void addChild(Segment child) {
    children.add(child);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }
    Segment point2DS = (Segment) o;
    return Objects.equals(children, point2DS.children);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), children);
  }

  public static double[] convertFloatToDouble(float[] floatArray) {
    double[] doubleArray = new double[floatArray.length];
    for (int i = 0; i < floatArray.length; i++) {
      doubleArray[i] = floatArray[i];
    }
    return doubleArray;
  }
}
