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

/** A color with straight (non-premultiplied) alpha, each component in {@code [0, 1]}. */
public record Rgba(float red, float green, float blue, float alpha) {

  public static final Rgba TRANSPARENT = new Rgba(0f, 0f, 0f, 0f);
  public static final Rgba BLACK = new Rgba(0f, 0f, 0f, 1f);
  public static final Rgba WHITE = new Rgba(1f, 1f, 1f, 1f);

  public Rgba {
    red = clamp(red);
    green = clamp(green);
    blue = clamp(blue);
    alpha = clamp(alpha);
  }

  public static Rgba of(Color color) {
    return new Rgba(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, 1f);
  }

  public Rgba withAlpha(float newAlpha) {
    return new Rgba(red, green, blue, newAlpha);
  }

  public Color toColor() {
    return new Color(red, green, blue, alpha);
  }

  public int red8() {
    return to8(red);
  }

  public int green8() {
    return to8(green);
  }

  public int blue8() {
    return to8(blue);
  }

  public int alpha8() {
    return to8(alpha);
  }

  static int to8(float component) {
    return Math.round(clamp(component) * 255f);
  }

  static float clamp(float value) {
    return Float.isNaN(value) ? 0f : Math.max(0f, Math.min(1f, value));
  }
}
