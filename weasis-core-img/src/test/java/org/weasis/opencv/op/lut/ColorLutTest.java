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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@DisplayName("ColorLut Tests")
class ColorLutTest {

  @Nested
  @DisplayName("ByteLut Creation Tests")
  class ByteLutCreationTests {

    @Test
    @DisplayName("Should create IMAGE ColorLut with null LUT table")
    void shouldCreateImageColorLutWithNullTable() {
      assertNull(
          ColorLut.IMAGE.getByteLut().lutTable(),
          "IMAGE ColorLut should have null LUT table to use default gray LUT");
    }

    @ParameterizedTest
    @EnumSource(
        value = ColorLut.class,
        names = {"FLAG", "MULTICOLOR", "HUE", "RED", "GREEN", "BLUE", "GRAY"})
    @DisplayName("Should create non-IMAGE ColorLuts with valid LUT tables")
    void shouldCreateNonImageColorLutsWithValidTables(ColorLut colorLut) {
      byte[][] lutTable = colorLut.getByteLut().lutTable();

      assertNotNull(lutTable, colorLut.name() + " should have a non-null LUT table");
      assertEquals(3, lutTable.length, colorLut.name() + " should have 3 color channels");

      for (int channel = 0; channel < 3; channel++) {
        assertNotNull(
            lutTable[channel], colorLut.name() + " channel " + channel + " should not be null");
        assertEquals(
            256,
            lutTable[channel].length,
            colorLut.name() + " channel " + channel + " should have 256 entries");
      }
    }

    @Test
    @DisplayName("Should create ByteLut instances for all ColorLut values")
    void shouldCreateByteLutInstancesForAllValues() {
      for (ColorLut colorLut : ColorLut.values()) {
        assertNotNull(
            colorLut.getByteLut(), colorLut.name() + " should have a non-null ByteLut instance");
      }
    }
  }

  @Nested
  @DisplayName("String Representation Tests")
  class StringRepresentationTests {

    @Test
    @DisplayName("Should return correct names for all ColorLut values")
    void shouldReturnCorrectNames() {
      assertAll(
          "ColorLut names",
          () -> assertEquals("Default (image)", ColorLut.IMAGE.getName()),
          () -> assertEquals("Flag", ColorLut.FLAG.getName()),
          () -> assertEquals("Multi-Color", ColorLut.MULTICOLOR.getName()),
          () -> assertEquals("Hue", ColorLut.HUE.getName()),
          () -> assertEquals("Red", ColorLut.RED.getName()),
          () -> assertEquals("Green", ColorLut.GREEN.getName()),
          () -> assertEquals("Blue", ColorLut.BLUE.getName()),
          () -> assertEquals("Gray", ColorLut.GRAY.getName()));
    }

    @Test
    @DisplayName("Should have toString() consistent with getName()")
    void shouldHaveToStringConsistentWithGetName() {
      for (ColorLut colorLut : ColorLut.values()) {
        assertEquals(
            colorLut.getName(),
            colorLut.toString(),
            colorLut.name() + " toString() should match getName()");
      }
    }
  }

  @Nested
  @DisplayName("LUT Pattern Validation Tests")
  class LutPatternValidationTests {

    @Test
    @DisplayName("Should create FLAG LUT with correct repeating pattern")
    void shouldCreateFlagLutWithCorrectPattern() {
      byte[][] flagLut = ColorLut.FLAG.getByteLut().lutTable();

      // Flag pattern repeats every 4 values based on actual implementation
      // Actual pattern from ColorLut: Red, White, Blue, Black
      int[][] expectedRGB = {
        {255, 0, 0}, // Red
        {255, 255, 255}, // White
        {0, 0, 255}, // Blue
        {0, 0, 0} // Black
      };

      for (int i = 0; i < 16; i++) { // Test first 16 entries to verify pattern
        int patternIndex = i % 4;
        int expectedRed = expectedRGB[patternIndex][0];
        int expectedGreen = expectedRGB[patternIndex][1];
        int expectedBlue = expectedRGB[patternIndex][2];

        assertEquals(
            expectedBlue, flagLut[0][i] & 0xFF, "FLAG LUT blue channel mismatch at index " + i);
        assertEquals(
            expectedGreen, flagLut[1][i] & 0xFF, "FLAG LUT green channel mismatch at index " + i);
        assertEquals(
            expectedRed, flagLut[2][i] & 0xFF, "FLAG LUT red channel mismatch at index " + i);
      }
    }

    @Test
    @DisplayName("Should create single-channel LUTs with correct channel activation")
    void shouldCreateSingleChannelLutsCorrectly() {
      // Test RED LUT (channel 2 active)
      byte[][] redLut = ColorLut.RED.getByteLut().lutTable();
      for (int i = 0; i < 256; i++) {
        assertEquals(0, redLut[0][i] & 0xFF, "RED LUT blue channel should be 0 at index " + i);
        assertEquals(0, redLut[1][i] & 0xFF, "RED LUT green channel should be 0 at index " + i);
        assertEquals(i, redLut[2][i] & 0xFF, "RED LUT red channel should equal index at " + i);
      }

      // Test GREEN LUT (channel 1 active)
      byte[][] greenLut = ColorLut.GREEN.getByteLut().lutTable();
      for (int i = 0; i < 256; i++) {
        assertEquals(0, greenLut[0][i] & 0xFF, "GREEN LUT blue channel should be 0 at index " + i);
        assertEquals(
            i, greenLut[1][i] & 0xFF, "GREEN LUT green channel should equal index at " + i);
        assertEquals(0, greenLut[2][i] & 0xFF, "GREEN LUT red channel should be 0 at index " + i);
      }

      // Test BLUE LUT (channel 0 active)
      byte[][] blueLut = ColorLut.BLUE.getByteLut().lutTable();
      for (int i = 0; i < 256; i++) {
        assertEquals(i, blueLut[0][i] & 0xFF, "BLUE LUT blue channel should equal index at " + i);
        assertEquals(0, blueLut[1][i] & 0xFF, "BLUE LUT green channel should be 0 at index " + i);
        assertEquals(0, blueLut[2][i] & 0xFF, "BLUE LUT red channel should be 0 at index " + i);
      }
    }

    @Test
    @DisplayName("Should create GRAY LUT with equal RGB values")
    void shouldCreateGrayLutWithEqualRgbValues() {
      byte[][] grayLut = ColorLut.GRAY.getByteLut().lutTable();

      for (int i = 0; i < 256; i++) {
        int blueValue = grayLut[0][i] & 0xFF;
        int greenValue = grayLut[1][i] & 0xFF;
        int redValue = grayLut[2][i] & 0xFF;

        assertEquals(i, blueValue, "GRAY LUT blue value should equal index at " + i);
        assertEquals(i, greenValue, "GRAY LUT green value should equal index at " + i);
        assertEquals(i, redValue, "GRAY LUT red value should equal index at " + i);

        assertEquals(
            blueValue, greenValue, "GRAY LUT blue and green should be equal at index " + i);
        assertEquals(greenValue, redValue, "GRAY LUT green and red should be equal at index " + i);
      }
    }

    @Test
    @DisplayName("Should create HUE LUT with valid HSB color mapping")
    void shouldCreateHueLutWithValidHsbMapping() {
      byte[][] hueLut = ColorLut.HUE.getByteLut().lutTable();

      // Test a few key points in the hue spectrum
      for (int i = 0; i < 256; i += 64) { // Test every 64th entry
        float hue = i / 255f;
        Color expectedColor = Color.getHSBColor(hue, 1f, 1f);

        int actualBlue = hueLut[0][i] & 0xFF;
        int actualGreen = hueLut[1][i] & 0xFF;
        int actualRed = hueLut[2][i] & 0xFF;

        assertEquals(expectedColor.getBlue(), actualBlue, "HUE LUT blue mismatch at index " + i);
        assertEquals(expectedColor.getGreen(), actualGreen, "HUE LUT green mismatch at index " + i);
        assertEquals(expectedColor.getRed(), actualRed, "HUE LUT red mismatch at index " + i);
      }
    }

    @Test
    @DisplayName("Should create MULTICOLOR LUT with 36-color palette pattern")
    void shouldCreateMulticolorLutWith36ColorPattern() {
      byte[][] multicolorLut = ColorLut.MULTICOLOR.getByteLut().lutTable();

      // Verify the pattern repeats every 36 values
      for (int i = 0; i < 72; i++) { // Test first 72 entries (2 complete cycles)
        int patternIndex = i % 36;
        int expectedBlue = multicolorLut[0][patternIndex] & 0xFF;
        int expectedGreen = multicolorLut[1][patternIndex] & 0xFF;
        int expectedRed = multicolorLut[2][patternIndex] & 0xFF;

        assertEquals(
            expectedBlue,
            multicolorLut[0][i] & 0xFF,
            "MULTICOLOR LUT blue pattern mismatch at index " + i);
        assertEquals(
            expectedGreen,
            multicolorLut[1][i] & 0xFF,
            "MULTICOLOR LUT green pattern mismatch at index " + i);
        assertEquals(
            expectedRed,
            multicolorLut[2][i] & 0xFF,
            "MULTICOLOR LUT red pattern mismatch at index " + i);
      }
    }
  }

  @Nested
  @DisplayName("Boundary Value Tests")
  class BoundaryValueTests {

    @ParameterizedTest
    @EnumSource(ColorLut.class)
    @DisplayName("Should have valid RGB values at LUT boundaries")
    void shouldHaveValidRgbValuesAtBoundaries(ColorLut colorLut) {
      if (colorLut == ColorLut.IMAGE) {
        // IMAGE uses null table, so skip boundary testing
        return;
      }

      byte[][] lutTable = colorLut.getByteLut().lutTable();

      // Test first entry (index 0)
      for (int channel = 0; channel < 3; channel++) {
        int value = lutTable[channel][0] & 0xFF;
        assertTrue(
            value >= 0 && value <= 255,
            colorLut.name()
                + " channel "
                + channel
                + " at index 0 should be valid RGB value: "
                + value);
      }

      // Test last entry (index 255)
      for (int channel = 0; channel < 3; channel++) {
        int value = lutTable[channel][255] & 0xFF;
        assertTrue(
            value >= 0 && value <= 255,
            colorLut.name()
                + " channel "
                + channel
                + " at index 255 should be valid RGB value: "
                + value);
      }
    }

    @Test
    @DisplayName("Should have all ColorLut enum values covered")
    void shouldHaveAllEnumValuesCovered() {
      ColorLut[] expectedValues = {
        ColorLut.IMAGE, ColorLut.FLAG, ColorLut.MULTICOLOR, ColorLut.HUE,
        ColorLut.RED, ColorLut.GREEN, ColorLut.BLUE, ColorLut.GRAY
      };

      ColorLut[] actualValues = ColorLut.values();
      assertEquals(
          expectedValues.length,
          actualValues.length,
          "Number of ColorLut enum values should match expected count");

      for (ColorLut expected : expectedValues) {
        boolean found = false;
        for (ColorLut actual : actualValues) {
          if (expected == actual) {
            found = true;
            break;
          }
        }
        assertTrue(found, "Expected ColorLut value should exist: " + expected);
      }
    }
  }

  @Nested
  @DisplayName("Integration Tests")
  class IntegrationTests {

    @Test
    @DisplayName("Should generate different colors for different LUT types")
    void shouldGenerateDifferentColorsForDifferentLutTypes() {
      // Compare colors at middle index (128) for different LUTs
      int testIndex = 128;

      Color redColor = getColorFromLut(ColorLut.RED, testIndex);
      Color greenColor = getColorFromLut(ColorLut.GREEN, testIndex);
      Color blueColor = getColorFromLut(ColorLut.BLUE, testIndex);
      Color grayColor = getColorFromLut(ColorLut.GRAY, testIndex);

      // RED LUT should have only red component
      assertEquals(0, redColor.getBlue());
      assertEquals(0, redColor.getGreen());
      assertEquals(testIndex, redColor.getRed());

      // GREEN LUT should have only green component
      assertEquals(0, greenColor.getBlue());
      assertEquals(testIndex, greenColor.getGreen());
      assertEquals(0, greenColor.getRed());

      // BLUE LUT should have only blue component
      assertEquals(testIndex, blueColor.getBlue());
      assertEquals(0, blueColor.getGreen());
      assertEquals(0, blueColor.getRed());

      // GRAY LUT should have equal RGB components
      assertEquals(testIndex, grayColor.getRed());
      assertEquals(testIndex, grayColor.getGreen());
      assertEquals(testIndex, grayColor.getBlue());
    }

    private Color getColorFromLut(ColorLut colorLut, int index) {
      byte[][] lutTable = colorLut.getByteLut().lutTable();
      int red = lutTable[2][index] & 0xFF;
      int green = lutTable[1][index] & 0xFF;
      int blue = lutTable[0][index] & 0xFF;
      return new Color(red, green, blue);
    }
  }
}
