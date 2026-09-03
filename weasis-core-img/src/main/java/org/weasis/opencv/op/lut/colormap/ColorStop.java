/*
 * Copyright (c) 2026 Weasis Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.weasis.opencv.op.lut.colormap;

import java.awt.Color;
import java.util.Objects;

/**
 * A control point of a color map. Color and alpha are two independent curves sharing the position
 * axis: a stop may carry either or both. The alpha component of {@code color} is ignored.
 *
 * @param alpha opacity in {@code [0, 1]}, or null when the stop does not shape the alpha curve
 * @param material shading coefficients for volume rendering, or null
 * @param group optional editing label, ignored when compiling
 */
public record ColorStop(
    double position, Color color, Float alpha, Material material, String group) {

  public ColorStop {
    if (!Double.isFinite(position)) {
      throw new IllegalArgumentException("Position must be finite: " + position);
    }
    if (color == null && alpha == null) {
      throw new IllegalArgumentException("A stop needs a color or an alpha");
    }
    if (alpha != null && (alpha.isNaN() || alpha < 0f || alpha > 1f)) {
      throw new IllegalArgumentException("Alpha must be in [0, 1]: " + alpha);
    }
  }

  public static ColorStop of(double position, Color color) {
    return new ColorStop(position, Objects.requireNonNull(color), null, null, null);
  }

  public static ColorStop of(double position, Color color, float alpha) {
    return new ColorStop(position, Objects.requireNonNull(color), alpha, null, null);
  }

  public static ColorStop ofAlpha(double position, float alpha) {
    return new ColorStop(position, null, alpha, null, null);
  }

  public boolean hasColor() {
    return color != null;
  }

  public boolean hasAlpha() {
    return alpha != null;
  }

  public boolean hasMaterial() {
    return material != null;
  }

  public ColorStop withPosition(double newPosition) {
    return new ColorStop(newPosition, color, alpha, material, group);
  }

  public ColorStop withColor(Color newColor) {
    return new ColorStop(position, newColor, alpha, material, group);
  }

  public ColorStop withAlpha(Float newAlpha) {
    return new ColorStop(position, color, newAlpha, material, group);
  }

  public ColorStop withMaterial(Material newMaterial) {
    return new ColorStop(position, color, alpha, newMaterial, group);
  }

  public ColorStop withGroup(String newGroup) {
    return new ColorStop(position, color, alpha, material, newGroup);
  }
}
