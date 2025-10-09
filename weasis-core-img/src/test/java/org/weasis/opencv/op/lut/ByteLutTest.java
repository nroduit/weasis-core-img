/*
 * Copyright (c) 2023 Weasis Team and other contributors.
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
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.stream.Stream;
import javax.swing.Icon;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayNameGeneration(ReplaceUnderscores.class)
class ByteLutTest {

  private static final String TEST_LUT_NAME = "Test LUT";
  private static final int BORDER_SIZE = 4; // 2 * BORDER from LutIcon

  private ByteLut byteLut;
  private byte[][] validLutTable;

  @BeforeEach
  void setUp() {
    validLutTable = createTestLut();
    byteLut = new ByteLut(TEST_LUT_NAME, validLutTable);
  }

  /** Creates a test LUT with blue=i, green=255-i, red=i */
  private byte[][] createTestLut() {
    var lut = new byte[3][256];
    for (int i = 0; i < 256; i++) {
      lut[0][i] = (byte) i; // Blue channel
      lut[1][i] = (byte) (255 - i); // Green channel (inverted)
      lut[2][i] = (byte) i; // Red channel
    }
    return lut;
  }

  /** Creates a LUT with all channels having the same values */
  private static byte[][] createUniformLut(int value) {
    var lut = new byte[3][256];
    var byteValue = (byte) value;
    for (int ch = 0; ch < 3; ch++) {
      for (int i = 0; i < 256; i++) {
        lut[ch][i] = byteValue;
      }
    }
    return lut;
  }

  /** Creates an inverted LUT where each channel has 255-i */
  private static byte[][] createInvertedLut() {
    var lut = new byte[3][256];
    for (int ch = 0; ch < 3; ch++) {
      for (int i = 0; i < 256; i++) {
        lut[ch][i] = (byte) (255 - i);
      }
    }
    return lut;
  }

  @Nested
  class Constructor_tests {

    @Test
    void should_throw_null_pointer_exception_when_name_is_null() {
      assertThrows(NullPointerException.class, () -> new ByteLut(null, validLutTable));
    }

    @Test
    void should_create_byte_lut_with_null_table() {
      var lut = new ByteLut("Null Table LUT", null);

      assertAll(
          () -> assertNotNull(lut),
          () -> assertEquals("Null Table LUT", lut.name()),
          () -> assertNull(lut.lutTable()));
    }

    @Test
    void should_throw_exception_when_incorrect_channel_count() {
      var invalidLut = new byte[][] {{0, 1, 2}, {3, 4, 5}}; // Only 2 channels

      var exception =
          assertThrows(IllegalArgumentException.class, () -> new ByteLut("Invalid", invalidLut));
      assertEquals("LUT must have exactly 3 channels (RGB)", exception.getMessage());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 4, 5, 255, 257})
    void should_throw_exception_when_incorrect_channel_size(int channelSize) {
      var invalidLut = new byte[3][];
      for (int i = 0; i < 3; i++) {
        invalidLut[i] = new byte[channelSize];
      }

      var exception =
          assertThrows(
              IllegalArgumentException.class, () -> new ByteLut("Invalid Size", invalidLut));
      assertEquals("Each LUT channel must have exactly 256 values", exception.getMessage());
    }

    @Test
    void should_create_valid_byte_lut() {
      assertAll(
          () -> assertDoesNotThrow(() -> new ByteLut("Valid LUT", validLutTable)),
          () -> assertEquals(TEST_LUT_NAME, byteLut.name()),
          () -> assertArrayEquals(validLutTable, byteLut.lutTable()));
    }
  }

  @Nested
  class Object_methods_tests {

    @Test
    void should_return_name_as_string_representation() {
      assertEquals(TEST_LUT_NAME, byteLut.toString());
    }

    @Test
    void should_be_equal_when_same_name_and_table() {
      var identicalLut = new ByteLut(TEST_LUT_NAME, createTestLut());

      assertAll(
          () -> assertEquals(byteLut, identicalLut),
          () -> assertEquals(byteLut.hashCode(), identicalLut.hashCode()));
    }

    @Test
    void should_not_be_equal_when_names_differ() {
      var differentNameLut = new ByteLut("Different LUT", validLutTable);
      assertNotEquals(byteLut, differentNameLut);
    }

    @Test
    void should_not_be_equal_when_tables_differ() {
      var differentTable = createInvertedLut();
      var differentTableLut = new ByteLut(TEST_LUT_NAME, differentTable);

      assertAll(
          () -> assertNotEquals(byteLut, differentTableLut),
          () -> assertNotEquals(byteLut.hashCode(), differentTableLut.hashCode()));
    }

    @ParameterizedTest
    @MethodSource("provideNonByteLutObjects")
    void should_not_be_equal_to_different_objects(Object other) {
      assertNotEquals(byteLut, other);
    }

    static Stream<Arguments> provideNonByteLutObjects() {
      return Stream.of(
          Arguments.of((Object) null),
          Arguments.of("Not a ByteLut"),
          Arguments.of(42),
          Arguments.of(new Object()));
    }
  }

  @Nested
  class Icon_generation_tests {

    @Test
    void should_create_icon_with_default_width() {
      int height = 10;
      Icon icon = byteLut.getIcon(height);

      assertAll(
          () -> assertNotNull(icon),
          () -> assertEquals(height, icon.getIconHeight()),
          () -> assertEquals(256 + BORDER_SIZE, icon.getIconWidth()));
    }

    @ParameterizedTest
    @MethodSource("provideValidDimensions")
    void should_create_icon_with_custom_dimensions(int width, int height) {
      Icon icon = byteLut.getIcon(width, height);

      assertAll(
          () -> assertNotNull(icon),
          () -> assertEquals(height, icon.getIconHeight()),
          () -> assertEquals(width + BORDER_SIZE, icon.getIconWidth()));
    }

    static Stream<Arguments> provideValidDimensions() {
      return Stream.of(
          Arguments.of(1, 1),
          Arguments.of(128, 20),
          Arguments.of(256, 32),
          Arguments.of(512, 64),
          Arguments.of(1000, 100));
    }

    @ParameterizedTest
    @MethodSource("provideInvalidDimensions")
    void should_throw_exception_for_invalid_dimensions(int width, int height) {
      assertThrows(IllegalArgumentException.class, () -> byteLut.getIcon(width, height));
    }

    @ParameterizedTest
    @ValueSource(ints = {-5, 0})
    void should_throw_exception_for_invalid_height_in_single_param_method(int height) {
      assertThrows(IllegalArgumentException.class, () -> byteLut.getIcon(height));
    }

    static Stream<Arguments> provideInvalidDimensions() {
      return Stream.of(
          Arguments.of(-1, 128), Arguments.of(128, 0), Arguments.of(-5, -10), Arguments.of(0, 100));
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 16, 64, 128, 256, 512})
    void should_paint_icon_without_exceptions(int width) {
      int height = 20;
      Icon icon = byteLut.getIcon(width, height);
      var image =
          new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_RGB);
      Graphics2D graphics = image.createGraphics();

      assertDoesNotThrow(() -> icon.paintIcon(null, graphics, 0, 0));
      graphics.dispose();
    }

    @Test
    void should_create_minimum_size_icon() {
      Icon icon = byteLut.getIcon(1, 1);

      assertAll(
          () -> assertNotNull(icon),
          () -> assertEquals(1, icon.getIconHeight()),
          () -> assertEquals(5, icon.getIconWidth()) // 1 + BORDER_SIZE
          );
    }
  }

  @Nested
  class Color_mapping_tests {

    @Test
    void should_map_colors_at_boundaries() {
      // First color (index 0): blue=0, green=255, red=0
      Color firstColor = byteLut.getColor(0, 256);
      assertAll(
          () -> assertEquals(0, firstColor.getBlue()),
          () -> assertEquals(255, firstColor.getGreen()),
          () -> assertEquals(0, firstColor.getRed()));

      // Last color (index 255): blue=255, green=0, red=255
      Color lastColor = byteLut.getColor(255, 256);
      assertAll(
          () -> assertEquals(255, lastColor.getBlue()),
          () -> assertEquals(0, lastColor.getGreen()),
          () -> assertEquals(255, lastColor.getRed()));
    }

    @Test
    void should_handle_null_lut_table() {
      var nullTableLut = new ByteLut("Null Table", null);
      Color grayColor = nullTableLut.getColor(128, 256);

      // Should use default gray LUT
      assertAll(
          () -> assertEquals(128, grayColor.getRed()),
          () -> assertEquals(128, grayColor.getGreen()),
          () -> assertEquals(128, grayColor.getBlue()));
    }

    @ParameterizedTest
    @MethodSource("provideWidthTestCases")
    void should_scale_colors_for_different_widths(int width, int position, Color expected) {
      Color actual = byteLut.getColor(position, width);
      assertAll(
          () -> assertEquals(expected.getRed(), actual.getRed()),
          () -> assertEquals(expected.getGreen(), actual.getGreen()),
          () -> assertEquals(expected.getBlue(), actual.getBlue()));
    }

    static Stream<Arguments> provideWidthTestCases() {
      // For width 1, position 0 should map to LUT index 255
      Color lastIndexColor = new Color(255, 0, 255); // red=255, green=0, blue=255

      return Stream.of(
          Arguments.of(1, 0, lastIndexColor),
          Arguments.of(2, 1, lastIndexColor),
          Arguments.of(256, 255, lastIndexColor),
          Arguments.of(256, 0, new Color(0, 255, 0)), // First index: red=0, green=255, blue=0
          Arguments.of(256, 128, new Color(128, 127, 128)) // Middle: red=128, green=127, blue=128
          );
    }
  }

  @Nested
  class Edge_cases_tests {

    @Test
    void should_handle_very_large_dimensions() {
      assertDoesNotThrow(
          () -> {
            Icon largeIcon = byteLut.getIcon(10000, 100);
            assertAll(
                () -> assertNotNull(largeIcon),
                () -> assertEquals(100, largeIcon.getIconHeight()),
                () -> assertEquals(10004, largeIcon.getIconWidth()));
          });
    }

    @ParameterizedTest
    @MethodSource("provideExtremeLutValues")
    void should_handle_extreme_lut_values(
        byte[][] extremeLut, int expectedRed, int expectedGreen, int expectedBlue) {
      var extremeByteLut = new ByteLut("Extreme LUT", extremeLut);
      Color color = extremeByteLut.getColor(100, 256);

      assertAll(
          () -> assertEquals(expectedBlue, color.getBlue()),
          () -> assertEquals(expectedGreen, color.getGreen()),
          () -> assertEquals(expectedRed, color.getRed()));
    }

    static Stream<Arguments> provideExtremeLutValues() {
      return Stream.of(
          Arguments.of(createUniformLut(255), 255, 255, 255), // All white
          Arguments.of(createUniformLut(0), 0, 0, 0), // All black
          Arguments.of(createUniformLut(127), 127, 127, 127), // Mid gray
          Arguments.of(createInvertedLut(), 155, 155, 155) // Inverted at position 100
          );
    }

    @Test
    void should_handle_color_consistency_across_different_positions() {
      var grayLut = new ByteLut("Gray LUT", null);

      // Test that colors are consistent for the same relative position
      Color color1 = grayLut.getColor(50, 100);
      Color color2 = grayLut.getColor(100, 200);
      Color color3 = grayLut.getColor(25, 50);

      // All should map to the same LUT index (127 or 128)
      assertAll(
          () -> assertTrue(Math.abs(color1.getRed() - color2.getRed()) <= 1),
          () -> assertTrue(Math.abs(color1.getGreen() - color2.getGreen()) <= 1),
          () -> assertTrue(Math.abs(color1.getBlue() - color2.getBlue()) <= 1),
          () -> assertTrue(Math.abs(color1.getRed() - color3.getRed()) <= 2));
    }

    @Test
    void should_handle_single_channel_validation_with_null_channel() {
      var lutWithNullChannel = new byte[3][];
      lutWithNullChannel[0] = new byte[256];
      lutWithNullChannel[1] = null; // Null channel
      lutWithNullChannel[2] = new byte[256];

      var exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> new ByteLut("Null Channel", lutWithNullChannel));
      assertEquals("Each LUT channel must have exactly 256 values", exception.getMessage());
    }
  }
}
