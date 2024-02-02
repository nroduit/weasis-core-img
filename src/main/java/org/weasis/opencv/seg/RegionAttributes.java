/*
 * Copyright (c) 2024 Weasis Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.weasis.opencv.seg;

import java.awt.Color;
import org.weasis.core.util.StringUtil;
import org.weasis.opencv.op.lut.ColorLut;

public class RegionAttributes {
  private final int id;
  private String label;
  private String description;
  private String type;
  private Color color;
  private boolean filled;
  private float lineThickness;
  private boolean visible;
  private float interiorOpacity;
  protected int numberOfPixels;

  public RegionAttributes(int id, String label) {
    this(id, label, null);
  }

  public RegionAttributes(int id, String label, Color color) {
    if (StringUtil.hasText(label)) {
      this.label = label;
    } else {
      throw new IllegalArgumentException("Label cannot be null or empty");
    }
    this.id = id;
    this.description = null;
    this.type = null;
    this.color = color;
    this.filled = true;
    this.lineThickness = 1.0f;
    this.visible = true;
    this.interiorOpacity = 1.0f;
    this.numberOfPixels = -1;
  }

  public String type() {
    return type;
  }

  public int getId() {
    return id;
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
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

  public int getNumberOfPixels() {
    return numberOfPixels;
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
