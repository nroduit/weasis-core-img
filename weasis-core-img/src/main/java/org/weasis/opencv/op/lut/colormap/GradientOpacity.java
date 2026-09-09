/*
 * Copyright (c) 2026 Weasis Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.weasis.opencv.op.lut.colormap;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Opacity factor as a function of the normalized gradient magnitude of the volume, {@code 0} in
 * homogeneous regions and {@code 1} across a full-range edge. Multiplies the alpha of a volume
 * preset, so surfaces show and uniform tissue fades: the classic edge emphasis of volume rendering.
 *
 * @param points sorted by magnitude; a magnitude below the first or above the last point clamps
 */
public record GradientOpacity(List<Point> points) {

  /** Magnitude above which a two-point emphasis curve is fully opaque. */
  public static final double EDGE_MAGNITUDE = 0.3;

  public record Point(double magnitude, float factor) {
    public Point {
      if (!(magnitude >= 0.0 && magnitude <= 1.0)) {
        throw new IllegalArgumentException("Magnitude must be in [0, 1]: " + magnitude);
      }
      if (Float.isNaN(factor) || factor < 0f || factor > 1f) {
        throw new IllegalArgumentException("Factor must be in [0, 1]: " + factor);
      }
    }
  }

  public GradientOpacity {
    Objects.requireNonNull(points, "Points cannot be null");
    if (points.isEmpty()) {
      throw new IllegalArgumentException("At least one point is needed");
    }
    var sorted = new ArrayList<>(points);
    sorted.sort(Comparator.comparingDouble(Point::magnitude));
    points = List.copyOf(sorted);
  }

  /**
   * Two-point curve for an emphasis in {@code [0, 1]}: homogeneous regions keep {@code 1 -
   * emphasis} of their opacity, edges keep all of it.
   */
  public static GradientOpacity edgeEmphasis(float emphasis) {
    float kept = 1f - Math.max(0f, Math.min(1f, emphasis));
    return new GradientOpacity(List.of(new Point(0.0, kept), new Point(EDGE_MAGNITUDE, 1f)));
  }

  /** The emphasis a two-point curve was built with: what homogeneous regions lose. */
  public float emphasis() {
    return 1f - points.get(0).factor();
  }

  public float factorAt(double magnitude) {
    Point first = points.get(0);
    if (magnitude <= first.magnitude()) {
      return first.factor();
    }
    for (int i = 1; i < points.size(); i++) {
      Point p = points.get(i);
      if (magnitude <= p.magnitude()) {
        Point q = points.get(i - 1);
        double span = p.magnitude() - q.magnitude();
        float t = span <= 0 ? 1f : (float) ((magnitude - q.magnitude()) / span);
        return q.factor() + (p.factor() - q.factor()) * t;
      }
    }
    return points.get(points.size() - 1).factor();
  }

  /** {@code n} factors sampled evenly over the magnitude range, for a shader uniform. */
  public float[] table(int n) {
    if (n < 2) {
      throw new IllegalArgumentException("At least 2 samples are needed: " + n);
    }
    float[] table = new float[n];
    for (int i = 0; i < n; i++) {
      table[i] = factorAt((double) i / (n - 1));
    }
    return table;
  }
}
