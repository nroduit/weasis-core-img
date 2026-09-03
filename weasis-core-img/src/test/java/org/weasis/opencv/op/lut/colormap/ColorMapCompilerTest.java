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
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.awt.Color;
import java.util.function.DoubleUnaryOperator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.weasis.opencv.data.ImageCV;
import org.weasis.opencv.data.LookupTableCV;
import org.weasis.opencv.natives.NativeLibrary;
import org.weasis.opencv.op.lut.ByteLut;

@DisplayNameGeneration(ReplaceUnderscores.class)
class ColorMapCompilerTest {

  /** The CT "Grayscale" volume preset of the 3D viewer, expressed as stops. */
  private static final ColorMap CT_GRAY =
      ColorMap.builder("Grayscale")
          .type(ColorMapType.TRANSFER)
          .modalities("CT")
          .domain(ColorMapDomain.absolute("HU", -100, 2047))
          .stop(-100, Color.BLACK, 0f)
          .stop(500, Color.WHITE, 0.875f)
          .stop(2047, Color.WHITE, 0.875f)
          .lighting(Lighting.DEFAULT)
          .build();

  @BeforeAll
  static void loadNativeLib() {
    NativeLibrary.loadLibraryFromLibraryName();
  }

  @Test
  void byte_lut_has_256_bgr_entries_named_after_the_map() {
    ColorMap map = ColorMap.builder("Ramp").stop(0, Color.BLACK).stop(1, Color.RED).build();
    ByteLut lut = ColorMapCompiler.toByteLut(map);

    assertAll(
        () -> assertEquals("Ramp", lut.name()),
        () -> assertSame(map, lut.source()),
        () -> assertSame(map, ColorMap.fromByteLut(lut)),
        () -> assertEquals(3, lut.lutTable().length),
        () -> assertEquals(256, lut.lutTable()[0].length),
        () -> assertEquals(0, lut.lutTable()[2][0]),
        () -> assertEquals((byte) 128, lut.lutTable()[2][128]),
        () -> assertEquals((byte) 255, lut.lutTable()[2][255]),
        () -> assertEquals(0, lut.lutTable()[0][255], "blue plane stays zero"));
  }

  @Test
  void abgr_planes_carry_alpha_first() {
    ColorMap map = ColorMap.builder("A").stop(0, Color.BLUE, 0f).stop(1, Color.BLUE, 1f).build();
    byte[][] abgr = ColorMapCompiler.toAbgr(map, 256);

    assertAll(
        () -> assertEquals(4, abgr.length),
        () -> assertEquals(0, abgr[0][0]),
        () -> assertEquals((byte) 255, abgr[0][255]),
        () -> assertEquals((byte) 255, abgr[1][0], "blue"),
        () -> assertEquals(0, abgr[2][0], "green"),
        () -> assertEquals(0, abgr[3][0], "red"));
  }

  @Test
  void rgba_texture_matches_the_3d_preset_at_integer_hu() {
    byte[] texture = ColorMapCompiler.toRgba(CT_GRAY, -100, 2047, DoubleUnaryOperator.identity());

    assertEquals(2148 * 4, texture.length);
    int at200 = (200 + 100) * 4;
    assertAll(
        () -> assertEquals(0, texture[0]),
        () -> assertEquals(0, texture[3], "alpha at -100 HU"),
        () -> assertEquals((byte) 128, texture[at200], "half way between -100 and 500"),
        () -> assertEquals((byte) 112, texture[at200 + 3], "alpha 0.4375"),
        () -> assertEquals((byte) 255, texture[(500 + 100) * 4]),
        () -> assertEquals((byte) 223, texture[(500 + 100) * 4 + 3], "alpha 0.875"),
        () -> assertEquals((byte) 223, texture[texture.length - 1]));
  }

  @Test
  void window_to_domain_rescales_the_default_range() {
    DoubleUnaryOperator op = ColorMapCompiler.windowToDomain(CT_GRAY, 0, 1000);

    assertAll(
        () -> assertEquals(-100, op.applyAsDouble(0), 1e-9),
        () -> assertEquals(2047, op.applyAsDouble(1000), 1e-9),
        () -> assertEquals(973.5, op.applyAsDouble(500), 1e-9),
        () ->
            assertThrows(
                IllegalArgumentException.class,
                () -> ColorMapCompiler.windowToDomain(CT_GRAY, 10, 10)));
  }

  @Test
  void lookup_table_is_indexed_by_stored_value() {
    ColorMap map =
        ColorMap.builder("Hot")
            .domain(ColorMapDomain.absolute("HU", -1000, 1000))
            .stop(-1000, Color.BLACK, 0f)
            .stop(1000, Color.RED, 1f)
            .build();
    LookupTableCV bgr =
        ColorMapCompiler.toLookupTable(map, -1000, 1000, DoubleUnaryOperator.identity(), false);
    LookupTableCV bgra =
        ColorMapCompiler.toLookupTable(map, -1000, 1000, DoubleUnaryOperator.identity(), true);

    assertAll(
        () -> assertEquals(3, bgr.getNumBands()),
        () -> assertEquals(4, bgra.getNumBands()),
        () -> assertEquals(2001, bgr.getNumEntries()),
        () -> assertEquals(-1000, bgr.getOffset()),
        () -> assertEquals(0, bgr.lookup(2, -1000), "red at the bottom"),
        () -> assertEquals(128, bgr.lookup(2, 0), "red half way"),
        () -> assertEquals(255, bgr.lookup(2, 1000)),
        () -> assertEquals(0, bgr.lookup(0, 1000), "blue stays zero"),
        () -> assertEquals(128, bgra.lookup(3, 0), "alpha half way"));
  }

  @Test
  void lookup_table_colorizes_a_16_bit_image_in_one_pass() {
    ColorMap map =
        ColorMap.builder("Ramp")
            .domain(ColorMapDomain.absolute("HU", -1000, 1000))
            .stop(-1000, Color.BLACK)
            .stop(1000, Color.WHITE)
            .build();
    LookupTableCV table =
        ColorMapCompiler.toLookupTable(map, -1000, 1000, DoubleUnaryOperator.identity(), false);
    Mat src = new Mat(1, 3, CvType.CV_16SC1);
    src.put(0, 0, new short[] {-1000, 0, 1000});

    ImageCV dst = table.lookup(src);
    byte[] pixels = new byte[9];
    dst.get(0, 0, pixels);
    src.release();
    dst.release();

    assertEquals(CvType.CV_8UC3, dst.type());
    assertArrayEquals(
        new byte[] {
          0, 0, 0, (byte) 128, (byte) 128, (byte) 128, (byte) 255, (byte) 255, (byte) 255
        },
        pixels);
  }

  @Test
  void input_ranges_are_validated() {
    ColorMap map = ColorMap.builder("x").stop(0, Color.RED).build();
    DoubleUnaryOperator id = DoubleUnaryOperator.identity();

    assertAll(
        () ->
            assertThrows(
                IllegalArgumentException.class, () -> ColorMapCompiler.sample(map, 5, 4, id)),
        () ->
            assertThrows(
                IllegalArgumentException.class,
                () -> ColorMapCompiler.sample(map, -40000, 40000, id)),
        () -> assertThrows(IllegalArgumentException.class, () -> ColorMapCompiler.sample(map, 1)));
  }
}
