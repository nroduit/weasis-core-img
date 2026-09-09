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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.util.List;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(ReplaceUnderscores.class)
class ColorMapEditsTest {

  private static final ColorMap HU =
      ColorMap.builder("HU")
          .domain(ColorMapDomain.absolute("HU", 0, 1000))
          .stop(0, Color.BLACK, 0f)
          .stop(500, Color.RED)
          .stop(1000, Color.WHITE, 1f)
          .build();

  private static List<Double> positions(ColorMap map) {
    return map.stops().stream().map(ColorStop::position).toList();
  }

  @Test
  void rescaled_moves_stops_and_domain() {
    ColorMap out = ColorMapEdits.rescaled(HU, -1000, 3000);

    assertAll(
        () -> assertEquals(List.of(-1000.0, 1000.0, 3000.0), positions(out)),
        () -> assertEquals(-1000.0, out.domain().min()),
        () -> assertEquals(3000.0, out.domain().max()),
        () -> assertEquals("HU", out.domain().unit()),
        () -> assertThrows(IllegalArgumentException.class, () -> ColorMapEdits.rescaled(HU, 5, 5)));
  }

  @Test
  void anchored_spreads_a_relative_map_over_physical_values_and_keeps_absolute_ones() {
    ColorMap relative =
        ColorMap.builder("Rainbow")
            .stop(0, Color.BLACK)
            .stop(0.5, Color.RED)
            .stop(1, Color.WHITE)
            .build();

    ColorMap out = ColorMapEdits.anchored(relative, "HU", -1000, 3000);

    assertAll(
        () -> assertEquals(DomainKind.ABSOLUTE, out.domain().kind()),
        () -> assertEquals("HU", out.domain().unit()),
        () -> assertEquals(List.of(-1000.0, 1000.0, 3000.0), positions(out)),
        () -> assertSame(HU, ColorMapEdits.anchored(HU, "HU", 0, 1)));
  }

  @Test
  void rescaled_relative_map_stays_in_unit_range() {
    ColorMap map = ColorMap.builder("R").stop(0, Color.BLACK).stop(1, Color.WHITE).build();
    ColorMap out = ColorMapEdits.rescaled(map, -0.5, 0.5);

    assertEquals(List.of(0.0, 0.5), positions(out));
    assertEquals(ColorMapDomain.RELATIVE, out.domain());
  }

  @Test
  void rescaled_rejects_a_relative_range_empty_after_clamping() {
    ColorMap map = ColorMap.builder("R").stop(0, Color.BLACK).stop(1, Color.WHITE).build();

    assertThrows(IllegalArgumentException.class, () -> ColorMapEdits.rescaled(map, 1.2, 1.5));
  }

  @Test
  void discretized_keeps_one_step_per_band() {
    ColorMap out = ColorMapEdits.discretized(HU, 4);

    assertAll(
        () -> assertEquals(Interpolation.STEP, out.interpolation()),
        () -> assertEquals(List.of(0.0, 250.0, 500.0, 750.0, 1000.0), positions(out)),
        () ->
            assertEquals(
                HU.sample(125).toColor().getRGB() & 0xffffff,
                out.stops().get(0).color().getRGB() & 0xffffff),
        () -> assertEquals(HU.sample(875).alpha(), out.stops().get(3).alpha(), 1e-6f),
        () -> assertEquals(out.sample(0), out.sample(249)),
        () -> assertThrows(IllegalArgumentException.class, () -> ColorMapEdits.discretized(HU, 1)));
  }

  @Test
  void threshold_rewrites_only_the_alpha_curve() {
    ColorMap out = ColorMapEdits.withThreshold(HU, 200, 400);

    assertAll(
        () -> assertEquals(ColorMapType.TRANSFER, out.type()),
        () -> assertEquals(0f, out.sample(100).alpha()),
        () -> assertEquals(0.5f, out.sample(300).alpha(), 1e-6f),
        () -> assertEquals(1f, out.sample(900).alpha()),
        () ->
            assertEquals(
                HU.sample(500).withAlpha(1f).toColor(), out.sample(500).withAlpha(1f).toColor()),
        () -> assertEquals(1f, ColorMapEdits.withThreshold(HU, 300, 300).sample(300).alpha()),
        () -> assertEquals(0f, ColorMapEdits.withThreshold(HU, 300, 300).sample(299.9).alpha()),
        () ->
            assertThrows(
                IllegalArgumentException.class, () -> ColorMapEdits.withThreshold(HU, 5, 4)));
  }

  @Test
  void palette_replaces_colors_and_keeps_alpha() {
    ColorMap palette =
        ColorMap.builder("BlueGreen")
            .space(InterpolationSpace.LAB)
            .stop(0, Color.BLUE)
            .stop(1, Color.GREEN)
            .build();
    ColorMap thresholded = ColorMapEdits.withThreshold(HU, 200, 400);

    ColorMap out = ColorMapEdits.withPalette(thresholded, palette);

    assertAll(
        () -> assertEquals("HU", out.name()),
        () -> assertEquals(HU.domain(), out.domain()),
        () -> assertEquals(InterpolationSpace.LAB, out.space()),
        () -> assertEquals(Color.BLUE, out.sample(0).withAlpha(1f).toColor()),
        () -> assertEquals(Color.GREEN, out.sample(1000).withAlpha(1f).toColor()),
        () -> assertEquals(0f, out.sample(100).alpha()),
        () -> assertEquals(1f, out.sample(900).alpha()));
  }

  @Test
  void stop_edits() {
    ColorMap added = ColorMapEdits.withStop(HU, ColorStop.of(250, Color.GREEN));
    ColorMap replaced = ColorMapEdits.withStop(HU, 1, ColorStop.of(600, Color.YELLOW));
    ColorMap removed = ColorMapEdits.withoutStop(HU, 1);
    ColorMap single = ColorMap.builder("one").stop(0, Color.RED).build();

    assertAll(
        () -> assertEquals(List.of(0.0, 250.0, 500.0, 1000.0), positions(added)),
        () -> assertEquals(List.of(0.0, 600.0, 1000.0), positions(replaced)),
        () -> assertEquals(List.of(0.0, 1000.0), positions(removed)),
        () ->
            assertThrows(
                IllegalArgumentException.class, () -> ColorMapEdits.withoutStop(single, 0)));
  }

  @Test
  void evenly_spaced_spreads_over_the_range() {
    ColorMap map =
        ColorMap.builder("List")
            .domain(ColorMapDomain.absolute("HU", 0, 300))
            .stop(0, Color.RED)
            .stop(1, Color.GREEN)
            .stop(2, Color.BLUE)
            .stop(3, Color.WHITE)
            .build();

    assertEquals(List.of(0.0, 100.0, 200.0, 300.0), positions(ColorMapEdits.evenlySpaced(map)));
    assertTrue(ColorMapEdits.evenlySpaced(map).stops().get(1).color().equals(Color.GREEN));
  }

  @Test
  void material_presets_are_valid_and_distinct() {
    var seen = new java.util.HashSet<Material>();
    for (MaterialPreset preset : MaterialPreset.values()) {
      assertTrue(seen.add(preset.material()), preset.name());
      assertTrue(preset.specularPower() > 0f);
    }
  }

  @Test
  void discretized_keeps_top_band_in_range_and_outside_transparency() {
    ColorMap full =
        ColorMap.builder("HU")
            .domain(ColorMapDomain.absolute("HU", 0, 1000))
            .outside(OutsideColors.TRANSPARENT)
            .stop(0, Color.BLACK)
            .stop(1000, Color.WHITE)
            .build();
    ColorMap partial =
        ColorMap.builder("HU")
            .domain(ColorMapDomain.absolute("HU", 0, 1000))
            .outside(OutsideColors.TRANSPARENT)
            .stop(300, Color.RED)
            .stop(1000, Color.WHITE)
            .build();

    ColorMap fullBands = ColorMapEdits.discretized(full, 4);
    ColorMap partialBands = ColorMapEdits.discretized(partial, 4);

    assertAll(
        () -> assertEquals(1f, fullBands.sample(900).alpha()),
        () -> assertEquals(fullBands.sample(750), fullBands.sample(1000)),
        () -> assertEquals(0f, partialBands.sample(125).alpha()),
        () -> assertEquals(1f, partialBands.sample(900).alpha()));
  }

  @Test
  void with_palette_keeps_materials_and_alpha() {
    ColorMap map =
        ColorMap.builder("CT")
            .type(ColorMapType.TRANSFER)
            .domain(ColorMapDomain.absolute("HU", -1000, 1000))
            .stop(new ColorStop(-100, Color.BLACK, 0f, MaterialPreset.BONE.material(), null))
            .stop(new ColorStop(500, Color.WHITE, null, MaterialPreset.SKIN.material(), null))
            .build();
    ColorMap palette =
        ColorMap.builder("BlueGreen").stop(0, Color.BLUE).stop(1, Color.GREEN).build();

    ColorMap out = ColorMapEdits.withPalette(map, palette);

    assertAll(
        () -> assertEquals(MaterialPreset.BONE.material(), out.sampler().material(-500)),
        () -> assertEquals(MaterialPreset.SKIN.material(), out.sampler().material(600)),
        () -> assertEquals(map.sample(200).alpha(), out.sample(200).alpha(), 1e-6f));
  }
}
