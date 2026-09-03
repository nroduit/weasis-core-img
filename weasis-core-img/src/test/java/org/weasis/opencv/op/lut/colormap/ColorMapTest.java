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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;
import org.weasis.opencv.op.lut.ByteLut;
import org.weasis.opencv.op.lut.ColorLut;

@DisplayNameGeneration(ReplaceUnderscores.class)
class ColorMapTest {

  @Test
  void builder_applies_defaults() {
    ColorMap map = ColorMap.builder("Gray").stop(0, Color.BLACK).stop(1, Color.WHITE).build();

    assertAll(
        () -> assertEquals("Gray", map.name()),
        () -> assertEquals(ColorMapType.SEQUENTIAL, map.type()),
        () -> assertTrue(map.modalities().isEmpty()),
        () -> assertFalse(map.defaultForModality()),
        () -> assertEquals(ColorMapDomain.RELATIVE, map.domain()),
        () -> assertEquals(8, map.bits()),
        () -> assertEquals(256, map.entries()),
        () -> assertEquals(InterpolationSpace.RGB, map.space()),
        () -> assertEquals(Interpolation.LINEAR, map.interpolation()),
        () -> assertEquals(OutsideColors.CLAMP, map.outside()),
        () -> assertEquals(null, map.lighting()),
        () -> assertFalse(map.hasAlpha()),
        () -> assertFalse(map.hasMaterial()));
  }

  @Test
  void absolute_domain_defaults_to_16_bits() {
    ColorMap map =
        ColorMap.builder("HU")
            .domain(ColorMapDomain.absolute("HU", -1000, 3000))
            .stop(-1000, Color.BLACK)
            .stop(3000, Color.WHITE)
            .build();

    assertEquals(16, map.bits());
    assertEquals(65536, map.entries());
  }

  @Test
  void stops_are_sorted_by_position_keeping_authoring_order_for_duplicates() {
    ColorStop late = ColorStop.of(0.5, Color.RED);
    ColorStop lateAgain = ColorStop.of(0.5, Color.GREEN);
    ColorMap map =
        ColorMap.builder("Sorted")
            .stop(1, Color.WHITE)
            .stop(late)
            .stop(0, Color.BLACK)
            .stop(lateAgain)
            .build();

    assertEquals(
        List.of(0.0, 0.5, 0.5, 1.0), map.stops().stream().map(ColorStop::position).toList());
    assertEquals(late, map.stops().get(1));
    assertEquals(lateAgain, map.stops().get(2));
    assertEquals(0.0, map.firstPosition());
    assertEquals(1.0, map.lastPosition());
  }

  @Test
  void invalid_arguments_are_rejected() {
    assertAll(
        () -> assertThrows(IllegalArgumentException.class, () -> ColorMap.builder("x").build()),
        () ->
            assertThrows(
                IllegalArgumentException.class,
                () -> ColorMap.builder("x").stop(0, Color.RED).bits(7).build()),
        () ->
            assertThrows(
                IllegalArgumentException.class,
                () -> ColorMap.builder("x").stop(0, Color.RED).bits(17).build()),
        () ->
            assertThrows(
                IllegalArgumentException.class, () -> new ColorStop(0, null, null, null, null)),
        () -> assertThrows(IllegalArgumentException.class, () -> ColorStop.ofAlpha(0, 1.5f)),
        () ->
            assertThrows(IllegalArgumentException.class, () -> ColorStop.of(Double.NaN, Color.RED)),
        () ->
            assertThrows(
                IllegalArgumentException.class,
                () -> new ColorMapDomain(DomainKind.RELATIVE, null, 0, 2, null)),
        () ->
            assertThrows(IllegalArgumentException.class, () -> ColorMapDomain.absolute("HU", 5, 5)),
        () ->
            assertThrows(
                IllegalArgumentException.class,
                () -> new ColorMapDomain(DomainKind.PERCENT, null, 0, 100, null)),
        () -> assertThrows(IllegalArgumentException.class, () -> new Material(1.2f, 0, 0)),
        () -> assertThrows(IllegalArgumentException.class, () -> new Lighting(true, 0)));
  }

  @Test
  void modality_scope() {
    ColorMap all = ColorMap.builder("All").stop(0, Color.RED).build();
    ColorMap pet = ColorMap.builder("PET").modalities("PT", "NM").stop(0, Color.RED).build();

    assertAll(
        () -> assertTrue(all.appliesTo("CT")),
        () -> assertTrue(all.appliesTo(null)),
        () -> assertTrue(pet.appliesTo("PT")),
        () -> assertFalse(pet.appliesTo("CT")),
        () -> assertFalse(pet.appliesTo(null)),
        () -> assertEquals(Set.of("PT", "NM"), pet.modalities()));
  }

  @Test
  void reversed_mirrors_colors_but_keeps_alpha() {
    ColorMap map =
        ColorMap.builder("Hot")
            .domain(ColorMapDomain.absolute("SUV", 0, 10))
            .stop(0, Color.BLACK, 0f)
            .alphaStop(2.5, 1f)
            .stop(10, Color.RED)
            .build();

    ColorMap reversed = map.reversed();

    assertAll(
        () -> assertEquals(Rgba.of(Color.RED).withAlpha(0f), reversed.sample(0)),
        () -> assertEquals(3, reversed.stops().size(), "split stop is merged back"),
        () -> assertEquals(1f, reversed.sample(2.5).alpha()),
        () -> assertEquals(1f, reversed.sample(10).alpha()),
        () -> assertEquals(Rgba.BLACK, reversed.sample(10)),
        () -> assertEquals(map, reversed.reversed()));
  }

  @Test
  void reversed_keeps_material_with_the_color_and_groups_with_both() {
    ColorStop stop = new ColorStop(0, Color.BLACK, 0f, Material.DEFAULT, "Air");
    ColorMap map = ColorMap.builder("M").stop(stop).stop(1, Color.WHITE).build();

    ColorMap reversed = map.reversed();
    ColorStop mirrored = reversed.stops().get(2);
    ColorStop alphaPart = reversed.stops().get(1);

    assertAll(
        () -> assertEquals(1.0, mirrored.position()),
        () -> assertEquals(Material.DEFAULT, mirrored.material()),
        () -> assertEquals("Air", mirrored.group()),
        () -> assertEquals(0f, alphaPart.alpha()),
        () -> assertEquals(null, alphaPart.material()),
        () -> assertEquals(map, reversed.reversed()));
  }

  @Test
  void from_byte_lut_round_trips_every_built_in_lut() {
    for (ColorLut lut : ColorLut.values()) {
      ByteLut original = lut.getByteLut();
      ByteLut compiled = ColorMapCompiler.toByteLut(ColorMap.fromByteLut(original));
      byte[][] expected =
          original.lutTable() != null ? original.lutTable() : ColorLut.GRAY.getByteLut().lutTable();
      assertEquals(original.name(), compiled.name());
      assertTrue(Arrays.deepEquals(expected, compiled.lutTable()), lut.name());
    }
  }

  @Test
  void from_byte_lut_is_a_sampled_relative_map() {
    ColorMap map = ColorMap.fromByteLut(ColorLut.HUE.getByteLut());

    assertAll(
        () -> assertEquals(Interpolation.SAMPLED, map.interpolation()),
        () -> assertEquals(ColorMapDomain.RELATIVE, map.domain()),
        () -> assertEquals(256, map.stops().size()),
        () -> assertEquals(0.0, map.firstPosition()),
        () -> assertEquals(1.0, map.lastPosition()));
  }

  @Test
  void to_builder_copies_everything() {
    ColorMap map =
        ColorMap.builder("Full")
            .type(ColorMapType.TRANSFER)
            .modalities("CT")
            .defaultForModality(true)
            .domain(ColorMapDomain.fixed("HU", -100, 500))
            .bits(12)
            .space(InterpolationSpace.LAB)
            .interpolation(Interpolation.STEP)
            .stop(new ColorStop(-100, Color.BLACK, 0f, Material.DEFAULT, "Air"))
            .stop(500, Color.WHITE, 1f)
            .outside(OutsideColors.TRANSPARENT)
            .lighting(Lighting.DEFAULT)
            .build();

    assertEquals(map, map.toBuilder().build());
    assertEquals("Renamed", map.withName("Renamed").name());
    assertTrue(map.hasAlpha());
    assertTrue(map.hasMaterial());
  }
}
