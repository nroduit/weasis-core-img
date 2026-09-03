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

/** Volume rendering shading settings of a map; absent on 2D-only maps. */
public record Lighting(boolean shade, float specularPower) {

  public static final Lighting DEFAULT = new Lighting(true, 10f);

  public Lighting {
    if (Float.isNaN(specularPower) || specularPower <= 0f) {
      throw new IllegalArgumentException("Specular power must be positive: " + specularPower);
    }
  }
}
