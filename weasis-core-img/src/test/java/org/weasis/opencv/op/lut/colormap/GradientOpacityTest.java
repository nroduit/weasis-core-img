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

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;
import org.weasis.opencv.op.lut.colormap.GradientOpacity.Point;

@DisplayNameGeneration(ReplaceUnderscores.class)
class GradientOpacityTest {

  @Test
  void edge_emphasis_keeps_edges_and_fades_homogeneous_regions() {
    GradientOpacity g = GradientOpacity.edgeEmphasis(0.8f);
    float[] table = g.table(31);
    assertAll(
        () -> assertEquals(0.2f, g.factorAt(0.0), 1e-6f),
        () -> assertEquals(0.6f, g.factorAt(0.15), 1e-6f),
        () -> assertEquals(1f, g.factorAt(0.3), 1e-6f),
        () -> assertEquals(1f, g.factorAt(0.9), 1e-6f),
        () -> assertEquals(0.8f, g.emphasis(), 1e-6f),
        () -> assertEquals(31, table.length),
        () -> assertEquals(0.2f, table[0], 1e-6f),
        () -> assertEquals(1f, table[30], 1e-6f));
  }

  @Test
  void points_are_sorted_and_validated() {
    GradientOpacity g =
        new GradientOpacity(List.of(new Point(1.0, 0.5f), new Point(0.0, 1f), new Point(0.5, 0f)));
    assertAll(
        () -> assertEquals(0.0, g.points().get(0).magnitude()),
        () -> assertEquals(0f, g.factorAt(0.5), 1e-6f),
        () -> assertEquals(0.25f, g.factorAt(0.75), 1e-6f),
        () -> assertThrows(IllegalArgumentException.class, () -> new Point(2.0, 1f)),
        () -> assertThrows(IllegalArgumentException.class, () -> new Point(0.0, 1.5f)),
        () -> assertThrows(IllegalArgumentException.class, () -> new GradientOpacity(List.of())),
        () -> assertThrows(IllegalArgumentException.class, () -> g.table(1)),
        () -> assertNull(Lighting.DEFAULT.gradientOpacity()),
        () -> assertEquals(g, Lighting.DEFAULT.withGradientOpacity(g).gradientOpacity()));
  }
}
