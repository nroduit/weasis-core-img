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
import java.util.stream.Collectors;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;

public class ContourTopology {

  private final Segment segment;
  private final int parent;

  public ContourTopology(MatOfPoint contour, int parent) {
    this(contour.toArray(), parent);
  }

  public ContourTopology(MatOfPoint2f contour, int parent) {
    this(contour.toArray(), parent);
  }

  public ContourTopology(Point[] pts, int parent) {
    this.parent = parent;
    this.segment =
        new Segment(
            Arrays.stream(pts).map(p -> new Point2D.Double(p.x, p.y)).collect(Collectors.toList()));
  }

  public int getParent() {
    return parent;
  }

  public Segment getSegment() {
    return segment;
  }
}
