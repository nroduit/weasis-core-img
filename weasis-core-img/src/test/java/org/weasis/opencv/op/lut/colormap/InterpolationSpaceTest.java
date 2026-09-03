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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@DisplayNameGeneration(ReplaceUnderscores.class)
class InterpolationSpaceTest {

  private static final float EPS = 0.02f;
  private static final Rgba RED = Rgba.of(Color.RED);
  private static final Rgba BLUE = Rgba.of(Color.BLUE);

  @ParameterizedTest
  @EnumSource(InterpolationSpace.class)
  void ends_are_exact_and_result_is_opaque(InterpolationSpace space) {
    Rgba start = space.mix(RED, BLUE, 0);
    Rgba end = space.mix(RED, BLUE, 1);
    assertAll(
        () -> assertEquals(1f, start.red(), EPS),
        () -> assertEquals(0f, start.blue(), EPS),
        () -> assertEquals(0f, end.red(), EPS),
        () -> assertEquals(1f, end.blue(), EPS),
        () -> assertEquals(1f, space.mix(RED, BLUE, 0.5).alpha()));
  }

  @ParameterizedTest
  @EnumSource(InterpolationSpace.class)
  void gray_ramp_stays_gray(InterpolationSpace space) {
    Rgba mid = space.mix(Rgba.BLACK, Rgba.WHITE, 0.5);
    assertAll(
        () -> assertEquals(mid.red(), mid.green(), EPS),
        () -> assertEquals(mid.green(), mid.blue(), EPS),
        () -> assertTrue(mid.red() > 0.2f && mid.red() < 0.8f));
  }

  @org.junit.jupiter.api.Test
  void rgb_midpoint_is_the_component_average() {
    Rgba mid = InterpolationSpace.RGB.mix(RED, BLUE, 0.5);
    assertAll(
        () -> assertEquals(0.5f, mid.red(), EPS),
        () -> assertEquals(0f, mid.green(), EPS),
        () -> assertEquals(0.5f, mid.blue(), EPS));
  }

  @org.junit.jupiter.api.Test
  void hsl_takes_the_shortest_hue_arc_and_keeps_saturation() {
    Rgba mid = InterpolationSpace.HSL.mix(RED, BLUE, 0.5);
    // Red (0°) to blue (240°) is shortest through magenta (300°), not green.
    assertAll(
        () -> assertEquals(1f, mid.red(), EPS),
        () -> assertEquals(0f, mid.green(), EPS),
        () -> assertEquals(1f, mid.blue(), EPS));
    Rgba fromGray = InterpolationSpace.HSL.mix(Rgba.WHITE, RED, 0.5);
    assertAll(
        () -> assertEquals(1f, fromGray.red(), EPS, "hue borrowed from the colored end"),
        () -> assertEquals(0.5f, fromGray.green(), EPS),
        () -> assertEquals(0.5f, fromGray.blue(), EPS));
  }

  @org.junit.jupiter.api.Test
  void lab_round_trip_is_stable() {
    Rgba[] samples = {RED, BLUE, Rgba.of(Color.ORANGE), Rgba.of(new Color(30, 120, 200))};
    for (Rgba c : samples) {
      Rgba same = InterpolationSpace.LAB.mix(c, c, 0.5);
      assertAll(
          () -> assertEquals(c.red(), same.red(), EPS),
          () -> assertEquals(c.green(), same.green(), EPS),
          () -> assertEquals(c.blue(), same.blue(), EPS));
    }
  }
}
