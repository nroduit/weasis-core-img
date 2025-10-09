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
 * mapping. The LUT contains 3 channels (Red, Green, Blue) with 256 values per channel used for
 * color transformation.
 */
public record ByteLut(String name, byte[][] lutTable) {

  private static final int CHANNEL_COUNT = 3;
  private static final int CHANNEL_SIZE = 256;
  private static final int DEFAULT_ICON_WIDTH = 256;

  /** Default gray LUT used when lutTable is null */
  private static final byte[][] DEFAULT_GRAY_LUT = createDefaultGrayLut();

  private static byte[][] createDefaultGrayLut() {
    var lut = new byte[CHANNEL_COUNT][CHANNEL_SIZE];
    for (int i = 0; i < CHANNEL_SIZE; i++) {
      byte value = (byte) i;
      lut[0][i] = value; // Blue
      lut[1][i] = value; // Green
      lut[2][i] = value; // Red
    }
    return lut;
  }

  public ByteLut {
    Objects.requireNonNull(name, "Name cannot be null");
    validateLutTable(lutTable);
  }

  private static void validateLutTable(byte[][] lutTable) {
    if (lutTable == null) return;

    if (lutTable.length != CHANNEL_COUNT) {
      throw new IllegalArgumentException(
          "LUT must have exactly %d channels (RGB)".formatted(CHANNEL_COUNT));
    }

    for (int i = 0; i < CHANNEL_COUNT; i++) {
      if (lutTable[i] == null || lutTable[i].length != CHANNEL_SIZE) {
        throw new IllegalArgumentException(
            "Each LUT channel must have exactly %d values".formatted(CHANNEL_SIZE));
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
   * Creates an icon with default width and specified height.
   *
   * @param height the height of the icon (must be positive)
   * @return the generated icon
   * @throws IllegalArgumentException if height is not positive
   */
  public Icon getIcon(int height) {
    return getIcon(DEFAULT_ICON_WIDTH, height);
  }

  /**
   * Creates an icon with specified dimensions.
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
   * Gets the color at a specific position for the given width.
   *
   * @param position the position (0 to width-1)
   * @param width the total width for scaling
   * @return the color at that position
   */
  Color getColor(int position, int width) {
    var lut = lutTable != null ? lutTable : DEFAULT_GRAY_LUT;
    int index = calculateLutIndex(position, width);
    return createColorFromLut(lut, index);
  }

  private int calculateLutIndex(int position, int width) {
    return width <= 1
        ? CHANNEL_SIZE - 1
        : Math.min(CHANNEL_SIZE - 1, (position * (CHANNEL_SIZE - 1)) / (width - 1));
  }

  private Color createColorFromLut(byte[][] lut, int index) {
    int red = Byte.toUnsignedInt(lut[2][index]);
    int green = Byte.toUnsignedInt(lut[1][index]);
    int blue = Byte.toUnsignedInt(lut[0][index]);

    return new Color(red, green, blue);
  }

  /** Icon implementation for LUT visualization */
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
      drawLutBars(g, x, y);
    }

    private void setupGraphics(Graphics g) {
      if (g instanceof Graphics2D g2d) {
        g2d.setStroke(new BasicStroke(STROKE_WIDTH));
      }
    }

    private void drawLutBars(Graphics g, int x, int y) {
      int lutHeight = height - 2 * BORDER;
      int startX = x + BORDER;
      int startY = y + BORDER;
      for (int k = 0; k < width; k++) {
        g.setColor(getColor(k, width));
        g.drawLine(startX + k, startY, startX + k, startY + lutHeight);
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
