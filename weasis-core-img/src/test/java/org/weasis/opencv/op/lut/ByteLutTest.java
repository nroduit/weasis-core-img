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
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.util.List;
import javax.swing.Icon;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

@DisplayName("ByteLut Tests")
class ByteLutTest {

  private ByteLut byteLut;
  private byte[][] validLutTable;

  @BeforeEach
  void setUp() {
    validLutTable = new byte[3][256];
    for (int i = 0; i < 256; i++) {
      validLutTable[0][i] = (byte) i; // Blue channel
      validLutTable[1][i] = (byte) (255 - i); // Green channel (inverted)
      validLutTable[2][i] = (byte) i; // Red channel
    }
    byteLut = new ByteLut("Test LUT", validLutTable);
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Should throw NullPointerException when name is null")
    void shouldThrowExceptionWhenNameIsNull() {
      assertThrows(NullPointerException.class, () -> new ByteLut(null, validLutTable));
    }

    @Test
    @DisplayName("Should create ByteLut with null LUT table using default gray LUT")
    void shouldCreateByteLutWithNullTable() {
      ByteLut lutWithNullTable = new ByteLut("Null Table LUT", null);
      assertNotNull(lutWithNullTable);
      assertEquals("Null Table LUT", lutWithNullTable.name());
      assertNull(lutWithNullTable.lutTable());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when LUT has incorrect number of channels")
    void shouldThrowExceptionWhenIncorrectChannelCount() {
      byte[][] invalidLutTable = new byte[][] {{0, 1, 2}, {3, 4, 5}}; // Only 2 channels

      IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> new ByteLut("Invalid Channel Count", invalidLutTable));
      assertEquals("LUT must have exactly 3 channels (RGB)", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when channel has incorrect size")
    void shouldThrowExceptionWhenIncorrectChannelSize() {
      byte[][] invalidLutTable = new byte[][] {{0, 1, 2}, {3, 4, 5}, {6, 7}}; // Channels too small

      IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> new ByteLut("Invalid Channel Size", invalidLutTable));
      assertEquals("Each LUT channel must have exactly 256 values", exception.getMessage());
    }

    @Test
    @DisplayName("Should create valid ByteLut with proper parameters")
    void shouldCreateValidByteLut() {
      assertDoesNotThrow(() -> new ByteLut("Valid LUT", validLutTable));
      assertEquals("Test LUT", byteLut.name());
      assertArrayEquals(validLutTable, byteLut.lutTable());
    }
  }

  @Nested
  @DisplayName("Object Methods Tests")
  class ObjectMethodsTests {

    @Test
    @DisplayName("Should return name as string representation")
    void shouldReturnNameAsString() {
      assertEquals("Test LUT", byteLut.toString());
    }

    @Test
    @DisplayName("Should be equal when name and LUT table are the same")
    void shouldBeEqualWhenSameNameAndTable() {
      byte[][] sameLutTable = new byte[3][256];
      for (int i = 0; i < 256; i++) {
        sameLutTable[0][i] = (byte) i;
        sameLutTable[1][i] = (byte) (255 - i);
        sameLutTable[2][i] = (byte) i;
      }
      ByteLut identicalByteLut = new ByteLut("Test LUT", sameLutTable);

      assertEquals(byteLut, identicalByteLut);
      assertEquals(byteLut.hashCode(), identicalByteLut.hashCode());
    }

    @Test
    @DisplayName("Should not be equal when names differ")
    void shouldNotBeEqualWhenNamesDiffer() {
      ByteLut differentNameLut = new ByteLut("Different LUT", validLutTable);
      assertNotEquals(byteLut, differentNameLut);
    }

    @Test
    @DisplayName("Should not be equal when LUT tables differ")
    void shouldNotBeEqualWhenTablesDiffer() {
      byte[][] differentTable = new byte[3][256];
      // Fill with different values
      for (int i = 0; i < 256; i++) {
        differentTable[0][i] = (byte) (255 - i);
        differentTable[1][i] = (byte) i;
        differentTable[2][i] = (byte) (255 - i);
      }
      ByteLut differentTableLut = new ByteLut("Test LUT", differentTable);

      assertNotEquals(byteLut, differentTableLut);
      assertNotEquals(byteLut.hashCode(), differentTableLut.hashCode());
    }

    @Test
    @DisplayName("Should not be equal to null or different class")
    void shouldNotBeEqualToNullOrDifferentClass() {
      assertNotEquals(byteLut, null);
      assertNotEquals(byteLut, "Not a ByteLut");
    }
  }

  @Nested
  @DisplayName("Icon Generation Tests")
  class IconGenerationTests {
    @Test
    @DisplayName("Should create icon with default width and specified height")
    void shouldCreateIconWithDefaultWidth() {
      int height = 10;

      Icon icon = byteLut.getIcon(height);
      assertNotNull(icon);
      assertEquals(height, icon.getIconHeight());
      assertEquals(256 + 4, icon.getIconWidth()); // 256 + 2*BORDER
    }

    @Test
    @DisplayName("Should create icon with custom width and height")
    void shouldCreateIconWithCustomDimensions() {
      int width = 128;
      int height = 20;
      Icon icon = byteLut.getIcon(width, height);
      assertNotNull(icon);
      assertEquals(height, icon.getIconHeight());
      assertEquals(width + 4, icon.getIconWidth()); // width + 2*BORDER
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException for invalid dimensions")
    void shouldThrowExceptionForInvalidDimensions() {
      assertAll(
          "Invalid dimensions",
          () -> assertThrows(IllegalArgumentException.class, () -> byteLut.getIcon(-1, 128)),
          () -> assertThrows(IllegalArgumentException.class, () -> byteLut.getIcon(128, 0)),
          () -> assertThrows(IllegalArgumentException.class, () -> byteLut.getIcon(-5)),
          () -> assertThrows(IllegalArgumentException.class, () -> byteLut.getIcon(0)));
    }

    @Test
    @DisplayName("Should paint correct colors for small width icon")
    void shouldPaintCorrectColorsForSmallWidth() {
      int width = 128;
      int height = 1;
      Icon icon = byteLut.getIcon(width, height);
      Graphics mockGraphics = Mockito.mock(Graphics.class);
      Component mockComponent = Mockito.mock(Component.class);
      ArgumentCaptor<Color> colorCaptor = ArgumentCaptor.forClass(Color.class);

      icon.paintIcon(mockComponent, mockGraphics, 0, 0);
      Mockito.verify(mockGraphics, Mockito.times(width)).setColor(colorCaptor.capture());
      List<Color> capturedColors = colorCaptor.getAllValues();
      for (int i = 0; i < width; i++) {
        assertEquals(
            byteLut.getColor(i, width), capturedColors.get(i), "Color mismatch at position " + i);
      }
    }

    @Test
    @DisplayName("Should paint correct colors for large width icon")
    void shouldPaintCorrectColorsForLargeWidth() {
      int width = 767;
      int height = 1;
      Icon icon = byteLut.getIcon(width, height);

      Graphics mockGraphics = Mockito.mock(Graphics.class);
      Component mockComponent = Mockito.mock(Component.class);
      ArgumentCaptor<Color> colorCaptor = ArgumentCaptor.forClass(Color.class);

      icon.paintIcon(mockComponent, mockGraphics, 0, 0);
      Mockito.verify(mockGraphics, Mockito.times(width)).setColor(colorCaptor.capture());
      List<Color> capturedColors = colorCaptor.getAllValues();
      for (int i = 0; i < width; i++) {
        assertEquals(
            byteLut.getColor(i, width), capturedColors.get(i), "Color mismatch at position " + i);
      }
    }

    @Test
    @DisplayName("Should create minimum size icon")
    void shouldCreateMinimumSizeIcon() {
      Icon icon = byteLut.getIcon(1, 1);

      assertNotNull(icon);
      assertEquals(1, icon.getIconHeight());
      assertEquals(5, icon.getIconWidth()); // 1 + 2*BORDER
    }
  }

  @Nested
  @DisplayName("Color Mapping Tests")
  class ColorMappingTests {

    @Test
    @DisplayName("Should map colors correctly at boundaries")
    void shouldMapColorsBoundaries() {
      // Test first color (index 0)
      Color firstColor = byteLut.getColor(0, 256);
      assertEquals(0, firstColor.getBlue()); // Blue channel: byte 0 -> 0
      assertEquals(255, firstColor.getGreen()); // Green channel: byte 255 -> 255
      assertEquals(0, firstColor.getRed()); // Red channel: byte 0 -> 0

      // Test last color (index 255)
      Color lastColor = byteLut.getColor(255, 256);
      assertEquals(255, lastColor.getBlue()); // Blue channel: byte 255 -> 255
      assertEquals(0, lastColor.getGreen()); // Green channel: byte 0 -> 0
      assertEquals(255, lastColor.getRed()); // Red channel: byte 255 -> 255
    }

    @Test
    @DisplayName("Should handle color mapping with null LUT table")
    void shouldHandleNullLutTable() {
      ByteLut nullTableLut = new ByteLut("Null Table", null);
      Color grayColor = nullTableLut.getColor(128, 256);

      // Should use default gray LUT
      assertEquals(128, grayColor.getRed());
      assertEquals(128, grayColor.getGreen());
      assertEquals(128, grayColor.getBlue());
    }

    @Test
    @DisplayName("Should scale colors correctly for different widths")
    void shouldScaleColorsForDifferentWidths() {
      // For width 1, position 0 should map to LUT index 255 (last entry)
      Color color1 = byteLut.getColor(0, 1);
      assertEquals(255, color1.getBlue());
      assertEquals(0, color1.getGreen());
      assertEquals(255, color1.getRed());

      // For width 2, position 1 should also map to LUT index 255
      Color color2 = byteLut.getColor(1, 2);
      assertEquals(255, color2.getBlue());
      assertEquals(0, color2.getGreen());
      assertEquals(255, color2.getRed());

      // For width 256, position 255 should map to LUT index 255
      Color color256 = byteLut.getColor(255, 256);
      assertEquals(255, color256.getBlue());
      assertEquals(0, color256.getGreen());
      assertEquals(255, color256.getRed());
    }
  }

  @Nested
  @DisplayName("Edge Cases Tests")
  class EdgeCasesTests {

    @Test
    @DisplayName("Should handle very large icon dimensions")
    void shouldHandleVeryLargeDimensions() {
      assertDoesNotThrow(
          () -> {
            Icon largeIcon = byteLut.getIcon(10000, 100);
            assertNotNull(largeIcon);
            assertEquals(100, largeIcon.getIconHeight());
            assertEquals(10004, largeIcon.getIconWidth());
          });
    }

    @Test
    @DisplayName("Should handle ByteLut with extreme LUT values")
    void shouldHandleExtremeLutValues() {
      byte[][] extremeLutTable = new byte[3][256];
      for (int i = 0; i < 256; i++) {
        extremeLutTable[0][i] = (byte) 255; // All blue
        extremeLutTable[1][i] = (byte) 0; // No green
        extremeLutTable[2][i] = (byte) 127; // Mid red
      }

      ByteLut extremeLut = new ByteLut("Extreme LUT", extremeLutTable);
      Color color = extremeLut.getColor(100, 256);

      assertEquals(255, color.getBlue());
      assertEquals(0, color.getGreen());
      assertEquals(127, color.getRed());
    }
  }
}
