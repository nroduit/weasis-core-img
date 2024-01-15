/*
 * Copyright (c) 2023 Weasis Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.weasis.opencv.seg;

import java.awt.Color;
import org.weasis.opencv.op.lut.ColorLut;

public final class SegmentAttributes {

  private Color color;
  private boolean filled;
  private float lineThickness;
  private boolean visible;
  private float interiorOpacity;

  public SegmentAttributes(Color color, boolean filled, float lineThickness) {
    this.color = color;
    this.filled = filled;
    this.lineThickness = lineThickness;
    this.visible = true;
    this.interiorOpacity = 1.0f;
  }

  public Color getColor() {
    return color;
  }

  public void setColor(Color color) {
    this.color = color;
  }

  public boolean isFilled() {
    return filled;
  }

  public void setFilled(boolean filled) {
    this.filled = filled;
  }

  public float getLineThickness() {
    return lineThickness;
  }

  public void setLineThickness(float lineThickness) {
    this.lineThickness = lineThickness;
  }

  public boolean isVisible() {
    return visible;
  }

  public void setVisible(boolean visible) {
    this.visible = visible;
  }

  public float getInteriorOpacity() {
    return interiorOpacity;
  }

  /**
   * @param interiorOpacity the opacity of the interior of the contour. The value is between 0.0 and
   *     1.0.
   */
  public void setInteriorOpacity(float interiorOpacity) {
    this.interiorOpacity = Math.max(0.0f, Math.min(interiorOpacity, 1.0f));
  }

  public static Color getColor(int[] colorRgb, int nb) {
    return getColor(colorRgb, nb, 1.0f);
  }

  public static Color getColor(int[] colorRgb, int nb, float opacity) {
    int opacityInt = (int) (opacity * 255f);
    Color rgbColor;
    if (colorRgb == null || colorRgb.length < 3) {
      byte[][] lut = ColorLut.MULTICOLOR.getByteLut().lutTable();
      int lutIndex = nb % 256;
      rgbColor =
          new Color(
              lut[0][lutIndex] & 0xFF,
              lut[1][lutIndex] & 0xFF,
              lut[2][lutIndex] & 0xFF,
              opacityInt);
    } else {
      rgbColor = new Color(colorRgb[0], colorRgb[1], colorRgb[2], opacityInt);
    }
    return rgbColor;
  }
}
