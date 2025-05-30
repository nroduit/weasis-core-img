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
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class ByteLutTest {

  private ByteLut byteLut;

  @BeforeEach
  void setUp() {
    byte[][] lutTable = new byte[3][256];
    for (int i = 0; i < 256; i++) {
      lutTable[0][i] = (byte) i;
      lutTable[1][i] = (byte) (255 - i);
      lutTable[2][i] = (byte) i;
    }
    byteLut = new ByteLut("Test LUT", lutTable);
  }

  @Test
  void testConstructor() {
    // Test invalid LUT with null name
    assertThrows(NullPointerException.class, () -> new ByteLut(null, byteLut.lutTable()));
    assertNotNull(new ByteLut("Invalid LUT", null));

    // Test invalid LUT with incorrect number of channels
    byte[][] invalidLutTable1 = new byte[][] {{0, 1, 2}, {3, 4, 5}};
    assertThrows(
        IllegalArgumentException.class, () -> new ByteLut("Invalid LUT", invalidLutTable1));

    // Test invalid LUT with incorrect number of values per channel
    byte[][] invalidLutTable2 = new byte[][] {{0, 1, 2}, {3, 4, 5}, {6, 7}};
    assertThrows(
        IllegalArgumentException.class, () -> new ByteLut("Invalid LUT", invalidLutTable2));
  }

  @Test
  void testToString() {
    assertEquals("Test LUT", byteLut.toString());
  }

  @Test
  void testEquals() {
    byte[][] lutTable = new byte[3][256];
    for (int i = 0; i < 256; i++) {
      lutTable[0][i] = (byte) i;
      lutTable[1][i] = (byte) (255 - i);
      lutTable[2][i] = (byte) i;
    }
    ByteLut byteLut2 = new ByteLut("Test LUT", lutTable);
    assertEquals(byteLut2, byteLut);

    // test HashCode
    assertEquals(byteLut2.hashCode(), byteLut.hashCode());
  }

  @Test
  void testGetIcon() {
    int height = 10;

    Icon icon = byteLut.getIcon(height);
    assertNotNull(icon);
    assertEquals(height, icon.getIconHeight());
    assertEquals(256 + 4, icon.getIconWidth());

    height = 1;
    int width = 128;
    icon = byteLut.getIcon(width, height);
    assertNotNull(icon);
    assertEquals(height, icon.getIconHeight());
    assertEquals(width + 4, icon.getIconWidth());

    // Create a mock Graphics object using Mockito
    Graphics mockGraphics = Mockito.mock(Graphics.class);
    Component mockComponent = Mockito.mock(Component.class);
    ArgumentCaptor<Color> colorCaptor = ArgumentCaptor.forClass(Color.class);

    icon.paintIcon(mockComponent, mockGraphics, 0, 0);
    Mockito.verify(mockGraphics, Mockito.times(width)).setColor(colorCaptor.capture());
    List<Color> capturedColors = colorCaptor.getAllValues();
    for (int i = 0; i < width; i++) {
      assertEquals(byteLut.getColor(i, width), capturedColors.get(i));
    }

    Mockito.reset(mockGraphics, mockComponent);
    colorCaptor = ArgumentCaptor.forClass(Color.class);
    width = 767;
    icon = byteLut.getIcon(width, height);
    assertNotNull(icon);
    assertEquals(height, icon.getIconHeight());
    assertEquals(width + 4, icon.getIconWidth());

    icon.paintIcon(mockComponent, mockGraphics, 0, 0);
    Mockito.verify(mockGraphics, Mockito.times(width)).setColor(colorCaptor.capture());
    capturedColors = colorCaptor.getAllValues();
    for (int i = 0; i < width; i++) {
      assertEquals(byteLut.getColor(i, width), capturedColors.get(i));
    }
  }

  @Test
  void testGetIconWithInvalidHeight() {
    assertThrows(IllegalArgumentException.class, () -> byteLut.getIcon(-1, 128));
    assertThrows(IllegalArgumentException.class, () -> byteLut.getIcon(128, 0));
  }
}
