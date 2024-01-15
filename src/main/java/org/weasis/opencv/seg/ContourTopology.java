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
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;

public class ContourTopology {

  private final Segment segment;
  private final int parent;

  public ContourTopology(MatOfPoint contour, int parent) {
    this.parent = parent;
    Point[] pts = contour.toArray();
    this.segment = new Segment();
    for (Point p : pts) {
      segment.add(new Point2D.Double(p.x, p.y));
    }
  }

  public int getParent() {
    return parent;
  }

  public Segment getSegment() {
    return segment;
  }
}
