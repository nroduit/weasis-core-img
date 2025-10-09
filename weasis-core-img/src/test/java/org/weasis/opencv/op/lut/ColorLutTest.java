/*
 * Copyright (c) 2025 Weasis Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.weasis.opencv.op.lut;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Color;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayNameGeneration(ReplaceUnderscores.class)
class ColorLutTest {

  private static final int LUT_SIZE = 256;
  private static final int CHANNEL_COUNT = 3;
  private static final List<ColorLut> NON_IMAGE_LUTS =
      List.of(
          ColorLut.FLAG,
          ColorLut.MULTICOLOR,
          ColorLut.HUE,
          ColorLut.RED,
          ColorLut.GREEN,
          ColorLut.BLUE,
          ColorLut.GRAY);

  @Nested
  class Lut_Creation_Tests {

    @Test
    void image_lut_should_have_null_table() {
      assertNull(ColorLut.IMAGE.getByteLut().lutTable());
    }

    @ParameterizedTest
    @EnumSource(
        value = ColorLut.class,
        names = {"FLAG", "MULTICOLOR", "HUE", "RED", "GREEN", "BLUE", "GRAY"})
    void non_image_luts_should_have_valid_structure(ColorLut colorLut) {
      var lutTable = colorLut.getByteLut().lutTable();

      assertAll(
          () -> assertNotNull(lutTable),
          () -> assertEquals(CHANNEL_COUNT, lutTable.length),
          () ->
              Arrays.stream(lutTable)
                  .forEach(
                      channel -> {
                        assertNotNull(channel);
                        assertEquals(LUT_SIZE, channel.length);
                      }));
    }

    @Test
    void all_color_luts_should_have_byte_lut_instances() {
      Arrays.stream(ColorLut.values()).forEach(colorLut -> assertNotNull(colorLut.getByteLut()));
    }
  }

  @Nested
  class String_Representation_Tests {

    private static final List<LutNamePair> EXPECTED_NAMES =
        List.of(
            new LutNamePair(ColorLut.IMAGE, "Default (image)"),
            new LutNamePair(ColorLut.FLAG, "Flag"),
            new LutNamePair(ColorLut.MULTICOLOR, "Multi-Color"),
            new LutNamePair(ColorLut.HUE, "Hue"),
            new LutNamePair(ColorLut.RED, "Red"),
            new LutNamePair(ColorLut.GREEN, "Green"),
            new LutNamePair(ColorLut.BLUE, "Blue"),
            new LutNamePair(ColorLut.GRAY, "Gray"));

    @Test
    void should_return_correct_names() {
      EXPECTED_NAMES.forEach(pair -> assertEquals(pair.expectedName(), pair.colorLut().getName()));
    }

    @Test
    void to_string_should_match_get_name() {
      Arrays.stream(ColorLut.values())
          .forEach(colorLut -> assertEquals(colorLut.getName(), colorLut.toString()));
    }

    private record LutNamePair(ColorLut colorLut, String expectedName) {}
  }

  @Nested
  class Pattern_Validation_Tests {

    @Test
    void flag_lut_should_have_correct_pattern() {
      var flagLut = ColorLut.FLAG.getByteLut().lutTable();
      var expectedPattern = createFlagPattern();

      IntStream.range(0, 16)
          .forEach(
              i -> {
                var patternIndex = i % expectedPattern.length;
                var expected = expectedPattern[patternIndex];

                assertAll(
                    () -> assertEquals(expected.blue(), Byte.toUnsignedInt(flagLut[0][i])),
                    () -> assertEquals(expected.green(), Byte.toUnsignedInt(flagLut[1][i])),
                    () -> assertEquals(expected.red(), Byte.toUnsignedInt(flagLut[2][i])));
              });
    }

    @Test
    void single_channel_luts_should_activate_correct_channels() {
      var singleChannelLuts =
          List.of(
              new ChannelTestData(ColorLut.RED, 2),
              new ChannelTestData(ColorLut.GREEN, 1),
              new ChannelTestData(ColorLut.BLUE, 0));

      singleChannelLuts.forEach(this::verifySingleChannelLut);
    }

    @Test
    void gray_lut_should_have_equal_rgb_values() {
      var grayLut = ColorLut.GRAY.getByteLut().lutTable();

      IntStream.range(0, LUT_SIZE)
          .forEach(
              i -> {
                var blue = Byte.toUnsignedInt(grayLut[0][i]);
                var green = Byte.toUnsignedInt(grayLut[1][i]);
                var red = Byte.toUnsignedInt(grayLut[2][i]);

                assertAll(
                    () -> assertEquals(i, blue),
                    () -> assertEquals(i, green),
                    () -> assertEquals(i, red));
              });
    }

    @Test
    void hue_lut_should_match_hsb_color_mapping() {
      var hueLut = ColorLut.HUE.getByteLut().lutTable();

      IntStream.iterate(0, i -> i < LUT_SIZE, i -> i + 32)
          .forEach(
              i -> {
                var expectedColor = Color.getHSBColor(i / 255f, 1f, 1f);

                assertAll(
                    () -> assertEquals(expectedColor.getBlue(), Byte.toUnsignedInt(hueLut[0][i])),
                    () -> assertEquals(expectedColor.getGreen(), Byte.toUnsignedInt(hueLut[1][i])),
                    () -> assertEquals(expectedColor.getRed(), Byte.toUnsignedInt(hueLut[2][i])));
              });
    }

    @Test
    void multicolor_lut_should_repeat_36_color_pattern() {
      var multicolorLut = ColorLut.MULTICOLOR.getByteLut().lutTable();
      var patternSize = 36;

      IntStream.range(0, 72)
          .forEach(
              i -> {
                var patternIndex = i % patternSize;

                assertAll(
                    () ->
                        assertEquals(
                            Byte.toUnsignedInt(multicolorLut[0][patternIndex]),
                            Byte.toUnsignedInt(multicolorLut[0][i])),
                    () ->
                        assertEquals(
                            Byte.toUnsignedInt(multicolorLut[1][patternIndex]),
                            Byte.toUnsignedInt(multicolorLut[1][i])),
                    () ->
                        assertEquals(
                            Byte.toUnsignedInt(multicolorLut[2][patternIndex]),
                            Byte.toUnsignedInt(multicolorLut[2][i])));
              });
    }

    private ColorRGB[] createFlagPattern() {
      return new ColorRGB[] {
        new ColorRGB(0, 0, 255), // Blue
        new ColorRGB(255, 255, 255), // White
        new ColorRGB(255, 0, 255), // Magenta
        new ColorRGB(0, 0, 0) // Black
      };
    }

    private void verifySingleChannelLut(ChannelTestData testData) {
      var lut = testData.colorLut().getByteLut().lutTable();

      IntStream.range(0, LUT_SIZE)
          .forEach(
              i -> {
                for (int channel = 0; channel < CHANNEL_COUNT; channel++) {
                  var expected = channel == testData.activeChannel() ? i : 0;
                  assertEquals(expected, Byte.toUnsignedInt(lut[channel][i]));
                }
              });
    }

    private record ColorRGB(int red, int green, int blue) {}

    private record ChannelTestData(ColorLut colorLut, int activeChannel) {}
  }

  @Nested
  class Boundary_Tests {

    @ParameterizedTest
    @EnumSource(value = ColorLut.class, names = "IMAGE", mode = EnumSource.Mode.EXCLUDE)
    void lut_values_should_be_valid_at_boundaries(ColorLut colorLut) {
      var lutTable = colorLut.getByteLut().lutTable();
      var boundaryIndices = List.of(0, LUT_SIZE - 1);

      boundaryIndices.forEach(
          index ->
              IntStream.range(0, CHANNEL_COUNT)
                  .forEach(
                      channel -> {
                        var value = Byte.toUnsignedInt(lutTable[channel][index]);
                        assertTrue(value >= 0 && value <= 255);
                      }));
    }

    @Test
    void should_have_expected_enum_values() {
      var expectedLuts =
          Set.of(
              ColorLut.IMAGE,
              ColorLut.FLAG,
              ColorLut.MULTICOLOR,
              ColorLut.HUE,
              ColorLut.RED,
              ColorLut.GREEN,
              ColorLut.BLUE,
              ColorLut.GRAY);

      var actualLuts = Set.of(ColorLut.values());

      assertEquals(expectedLuts, actualLuts);
    }
  }

  @Nested
  class Color_Generation_Tests {

    @ParameterizedTest
    @ValueSource(ints = {0, 64, 128, 192, 255})
    void single_channel_luts_should_generate_distinct_colors(int testIndex) {
      var colorData =
          List.of(
              new ColorExpectation(ColorLut.RED, testIndex, testIndex, 0, 0),
              new ColorExpectation(ColorLut.GREEN, testIndex, 0, testIndex, 0),
              new ColorExpectation(ColorLut.BLUE, testIndex, 0, 0, testIndex),
              new ColorExpectation(ColorLut.GRAY, testIndex, testIndex, testIndex, testIndex));

      colorData.forEach(this::verifyColorExpectation);
    }

    @Test
    void different_luts_should_produce_different_colors() {
      var testIndex = 128;

      var colors =
          NON_IMAGE_LUTS.stream()
              .collect(
                  java.util.stream.Collectors.toMap(
                      lut -> lut, lut -> extractColorFromLut(lut, testIndex)));

      // Verify that single-channel LUTs produce expected distinct colors
      var redColor = colors.get(ColorLut.RED);
      var greenColor = colors.get(ColorLut.GREEN);
      var blueColor = colors.get(ColorLut.BLUE);

      assertAll(
          () -> assertNotEquals(redColor, greenColor),
          () -> assertNotEquals(greenColor, blueColor),
          () -> assertNotEquals(blueColor, redColor));
    }

    private void verifyColorExpectation(ColorExpectation expectation) {
      var color = extractColorFromLut(expectation.colorLut(), expectation.index());

      assertAll(
          () -> assertEquals(expectation.expectedRed(), color.getRed()),
          () -> assertEquals(expectation.expectedGreen(), color.getGreen()),
          () -> assertEquals(expectation.expectedBlue(), color.getBlue()));
    }

    private Color extractColorFromLut(ColorLut colorLut, int index) {
      var lutTable = colorLut.getByteLut().lutTable();
      return new Color(
          Byte.toUnsignedInt(lutTable[2][index]), // Red
          Byte.toUnsignedInt(lutTable[1][index]), // Green
          Byte.toUnsignedInt(lutTable[0][index]) // Blue
          );
    }

    private record ColorExpectation(
        ColorLut colorLut, int index, int expectedRed, int expectedGreen, int expectedBlue) {}
  }
}
