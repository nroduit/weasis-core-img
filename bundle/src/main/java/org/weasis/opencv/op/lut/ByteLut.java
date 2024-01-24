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

public record ByteLut(String name, byte[][] lutTable) {

  public ByteLut {
    Objects.requireNonNull(name);
    if (lutTable != null && (lutTable.length != 3 || lutTable[0].length != 256)) {
      throw new IllegalArgumentException("LUT must have 3 channels and 256 values per channel");
    }
  }

  @Override
  public String toString() {
    return name;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ByteLut byteLut = (ByteLut) o;
    return Objects.equals(name, byteLut.name) && Arrays.deepEquals(lutTable, byteLut.lutTable);
  }

  @Override
  public int hashCode() {
    int result = Objects.hash(name);
    result = 31 * result + Arrays.deepHashCode(lutTable);
    return result;
  }

  public Icon getIcon(int height) {
    return getIcon(256, height);
  }

  public Icon getIcon(int width, int height) {
    if (width < 1 || height < 1) {
      throw new IllegalArgumentException("Width and height are not valid");
    }
    int border = 2;
    return new Icon() {
      @Override
      public void paintIcon(Component c, Graphics g, int x, int y) {
        if (g instanceof Graphics2D g2d) {
          g2d.setStroke(new BasicStroke(1.2f));
        }
        int lutHeight = height - 2 * border;
        int sx = x + border;
        int sy = y + border;
        for (int k = 0; k < width; k++) {
          g.setColor(getColor(k, width));
          g.drawLine(sx + k, sy, sx + k, sy + lutHeight);
        }
      }

      @Override
      public int getIconWidth() {
        return width + 2 * border;
      }

      @Override
      public int getIconHeight() {
        return height;
      }
    };
  }

  Color getColor(int position, int width) {
    byte[][] lut = lutTable == null ? ColorLut.GRAY.getByteLut().lutTable() : lutTable;
    int i = (position * 255) / width;
    return new Color(lut[2][i] & 0xFF, lut[1][i] & 0xFF, lut[0][i] & 0xFF);
  }
}
