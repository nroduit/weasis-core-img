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

import java.awt.Color;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(ReplaceUnderscores.class)
class ColorMapSamplerTest {

  private static final float EPS = 1e-3f;

  private static void assertRgba(Rgba expected, Rgba actual) {
    assertAll(
        () -> assertEquals(expected.red(), actual.red(), EPS, "red"),
        () -> assertEquals(expected.green(), actual.green(), EPS, "green"),
        () -> assertEquals(expected.blue(), actual.blue(), EPS, "blue"),
        () -> assertEquals(expected.alpha(), actual.alpha(), EPS, "alpha"));
  }

  @Test
  void linear_interpolation_between_stops() {
    ColorMap map = ColorMap.builder("BW").stop(0, Color.BLACK).stop(1, Color.WHITE).build();

    assertAll(
        () -> assertRgba(Rgba.BLACK, map.sample(0)),
        () -> assertRgba(new Rgba(0.25f, 0.25f, 0.25f, 1f), map.sample(0.25)),
        () -> assertRgba(Rgba.WHITE, map.sample(1)));
  }

  @Test
  void values_outside_the_stops_clamp_by_default() {
    ColorMap map = ColorMap.builder("BW").stop(0.2, Color.BLACK).stop(0.8, Color.WHITE).build();

    assertRgba(Rgba.BLACK, map.sample(-5));
    assertRgba(Rgba.WHITE, map.sample(5));
  }

  @Test
  void outside_colors_override_clamping_and_nan() {
    Rgba low = new Rgba(0f, 0f, 1f, 1f);
    Rgba high = new Rgba(1f, 0f, 0f, 1f);
    Rgba nan = new Rgba(0f, 1f, 0f, 0.5f);
    ColorMap map =
        ColorMap.builder("Out")
            .stop(0.2, Color.BLACK)
            .stop(0.8, Color.WHITE)
            .outside(new OutsideColors(low, high, nan))
            .build();

    assertAll(
        () -> assertRgba(low, map.sample(0.1)),
        () -> assertRgba(high, map.sample(0.9)),
        () -> assertRgba(nan, map.sample(Double.NaN)),
        () -> assertRgba(Rgba.BLACK, map.sample(0.2)),
        () -> assertRgba(Rgba.WHITE, map.sample(0.8)));
    assertRgba(
        Rgba.TRANSPARENT, ColorMap.builder("x").stop(0, Color.RED).build().sample(Double.NaN));
  }

  @Test
  void step_interpolation_holds_the_previous_stop() {
    ColorMap map =
        ColorMap.builder("Bands")
            .type(ColorMapType.QUALITATIVE)
            .interpolation(Interpolation.STEP)
            .stop(0, Color.RED)
            .stop(0.5, Color.GREEN)
            .stop(1, Color.BLUE)
            .build();

    assertAll(
        () -> assertRgba(Rgba.of(Color.RED), map.sample(0.49)),
        () -> assertRgba(Rgba.of(Color.GREEN), map.sample(0.5)),
        () -> assertRgba(Rgba.of(Color.GREEN), map.sample(0.99)),
        () -> assertRgba(Rgba.of(Color.BLUE), map.sample(1)));
  }

  @Test
  void duplicate_positions_make_a_hard_edge() {
    ColorMap map =
        ColorMap.builder("Edge")
            .stop(0, Color.BLACK)
            .stop(0.5, Color.BLACK)
            .stop(0.5, Color.WHITE)
            .stop(1, Color.WHITE)
            .build();

    assertAll(
        () -> assertRgba(Rgba.BLACK, map.sample(0.4999)),
        () -> assertRgba(Rgba.WHITE, map.sample(0.5)),
        () -> assertRgba(Rgba.WHITE, map.sample(0.75)));
  }

  @Test
  void alpha_curve_is_independent_from_the_color_curve() {
    ColorMap map =
        ColorMap.builder("PET")
            .domain(ColorMapDomain.absolute("SUV", 0, 10))
            .stop(0, Color.BLACK)
            .alphaStop(2.5, 0f)
            .alphaStop(2.5, 1f)
            .stop(10, Color.RED)
            .build();

    assertAll(
        () -> assertEquals(0f, map.sample(0).alpha(), EPS, "first alpha stop clamps below"),
        () -> assertEquals(0f, map.sample(2.4).alpha(), EPS),
        () -> assertEquals(1f, map.sample(2.5).alpha(), EPS),
        () -> assertEquals(1f, map.sample(9).alpha(), EPS),
        () -> assertEquals(0.5f, map.sample(5).red(), EPS, "color ramps across the alpha edge"));
  }

  @Test
  void alpha_ramps_linearly_and_defaults_to_opaque() {
    ColorMap ramp = ColorMap.builder("Ramp").stop(0, Color.RED, 0f).stop(1, Color.RED, 1f).build();
    ColorMap opaque = ColorMap.builder("Opaque").stop(0, Color.RED).build();

    assertEquals(0.25f, ramp.sample(0.25).alpha(), EPS);
    assertEquals(1f, opaque.sample(0.7).alpha(), EPS);
  }

  @Test
  void map_without_color_stops_is_white() {
    ColorMap alphaOnly = ColorMap.builder("Alpha").alphaStop(0, 0f).alphaStop(1, 1f).build();

    assertRgba(new Rgba(1f, 1f, 1f, 0.5f), alphaOnly.sample(0.5));
  }

  @Test
  void cyclic_maps_wrap_around_the_domain() {
    ColorMap map =
        ColorMap.builder("Phase")
            .type(ColorMapType.CYCLIC)
            .stop(0, Color.BLACK)
            .stop(1, Color.WHITE)
            .build();

    assertAll(
        () -> assertRgba(map.sample(0.25), map.sample(1.25)),
        () -> assertRgba(map.sample(0.75), map.sample(-0.25)),
        () -> assertRgba(map.sample(0), map.sample(2)));
  }

  @Test
  void material_curve() {
    Material soft = new Material(0.1f, 0.5f, 0.1f);
    Material bone = new Material(0.3f, 0.9f, 0.5f);
    ColorMap map =
        ColorMap.builder("CT")
            .domain(ColorMapDomain.absolute("HU", 0, 1000))
            .stop(new ColorStop(0, Color.BLACK, null, soft, null))
            .stop(new ColorStop(1000, Color.WHITE, null, bone, null))
            .build();
    ColorMapSampler sampler = map.sampler();

    assertAll(
        () -> assertEquals(soft, sampler.material(-10)),
        () -> assertEquals(bone, sampler.material(1000)),
        () -> assertEquals(0.2f, sampler.material(500).ambient(), EPS),
        () -> assertEquals(0.7f, sampler.material(500).diffuse(), EPS),
        () -> assertEquals(0.3f, sampler.material(500).specular(), EPS),
        () ->
            assertEquals(
                Material.DEFAULT,
                ColorMap.builder("x").stop(0, Color.RED).build().sampler().material(0)));
  }

  @Test
  void percent_domain_positions_are_plain_numbers() {
    ColorMap map =
        ColorMap.builder("Dose")
            .domain(ColorMapDomain.percent(ColorMapDomain.REFERENCE_PRESCRIPTION, 0, 120))
            .stop(0, Color.BLUE)
            .stop(100, Color.RED)
            .build();

    assertRgba(new Rgba(0.5f, 0f, 0.5f, 1f), map.sample(50));
    assertEquals(0.5, map.domain().normalize(60), 1e-9);
  }
}
