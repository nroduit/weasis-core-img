/*
 * Copyright (c) 2009-2020 Weasis Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.weasis.opencv.op.lut;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.Arrays;
import java.util.Objects;
import javax.swing.Icon;

/**
 * A record representing a Byte Lookup Table (LUT) with a name and a 2D byte array for color
 * mapping. The LUT typically contains 3 channels (Red, Green, Blue) with 256 values per channel
 * used for color transformation.
 */
public record ByteLut(String name, byte[][] lutTable) {

  /** Default gray LUT used as fallback when lutTable is null */
  private static final byte[][] DEFAULT_GRAY_LUT;

  static {
    DEFAULT_GRAY_LUT = new byte[3][256];
    for (int i = 0; i < 256; i++) {
      byte value = (byte) i;
      DEFAULT_GRAY_LUT[0][i] = value; // Blue
      DEFAULT_GRAY_LUT[1][i] = value; // Green
      DEFAULT_GRAY_LUT[2][i] = value; // Red
    }
  }

  public ByteLut {
    Objects.requireNonNull(name, "Name cannot be null");
    validateLutTable(lutTable);
  }

  /**
   * Validates the LUT table structure
   *
   * @param lutTable the table to validate
   * @throws IllegalArgumentException if the table has invalid dimensions
   */
  private static void validateLutTable(byte[][] lutTable) {
    if (lutTable == null) return;

    if (lutTable.length != 3) {
      throw new IllegalArgumentException("LUT must have exactly 3 channels (RGB)");
    }

    for (int i = 0; i < 3; i++) {
      if (lutTable[i] == null || lutTable[i].length != 256) {
        throw new IllegalArgumentException("Each LUT channel must have exactly 256 values");
      }
    }
  }

  @Override
  public String toString() {
    return name;
  }

  @Override
  public boolean equals(Object o) {
    return this == o
        || (o instanceof ByteLut other
            && Objects.equals(name, other.name)
            && Arrays.deepEquals(lutTable, other.lutTable));
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, Arrays.deepHashCode(lutTable));
  }

  /**
   * Creates an icon with default width (256) and specified height
   *
   * @param height the height of the icon
   * @return the generated icon
   */
  public Icon getIcon(int height) {
    return getIcon(256, height);
  }

  /**
   * Creates an icon with specified dimensions
   *
   * @param width the width of the icon (must be positive)
   * @param height the height of the icon (must be positive)
   * @return the generated icon
   * @throws IllegalArgumentException if width or height are not positive
   */
  public Icon getIcon(int width, int height) {
    if (width < 1 || height < 1) {
      throw new IllegalArgumentException("Width and height must be positive values");
    }

    return new LutIcon(width, height);
  }

  /**
   * Gets the color at a specific position for the given width
   *
   * @param position the position (0 to width-1)
   * @param width the total width for scaling
   * @return the color at that position
   */
  Color getColor(int position, int width) {
    byte[][] lut = lutTable != null ? lutTable : DEFAULT_GRAY_LUT;

    int index;
    // Handle edge case to avoid division by zero
    if (width <= 1) {
      index = 255;
    } else {
      // Normal case: scale position to LUT index range [0, 255]
      index = Math.min(255, (position * 255) / (width - 1));
    }

    int red = lut[2][index] & 0xFF;
    int green = lut[1][index] & 0xFF;
    int blue = lut[0][index] & 0xFF;

    return new Color(red, green, blue);
  }

  /** Inner class for the Icon implementation to improve encapsulation */
  private class LutIcon implements Icon {
    private static final int BORDER = 2;
    private static final float STROKE_WIDTH = 1.2f;

    private final int width;
    private final int height;

    LutIcon(int width, int height) {
      this.width = width;
      this.height = height;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
      setupGraphics(g);

      int lutHeight = height - 2 * BORDER;
      int startX = x + BORDER;
      int startY = y + BORDER;
      for (int k = 0; k < width; k++) {
        g.setColor(getColor(k, width));
        g.drawLine(startX + k, startY, startX + k, startY + lutHeight);
      }
    }

    private void setupGraphics(Graphics g) {
      if (g instanceof Graphics2D g2d) {
        g2d.setStroke(new BasicStroke(STROKE_WIDTH));
      }
    }

    @Override
    public int getIconWidth() {
      return width + 2 * BORDER;
    }

    @Override
    public int getIconHeight() {
      return height;
    }
  }
}
