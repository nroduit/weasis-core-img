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

/** Blinn-Phong coefficients attached to a stop, each in {@code [0, 1]}. */
public record Material(float ambient, float diffuse, float specular) {

  public static final Material DEFAULT = new Material(0.2f, 0.9f, 0.2f);

  public Material {
    validate(ambient, "ambient");
    validate(diffuse, "diffuse");
    validate(specular, "specular");
  }

  private static void validate(float value, String label) {
    if (Float.isNaN(value) || value < 0f || value > 1f) {
      throw new IllegalArgumentException(label + " must be in [0, 1]: " + value);
    }
  }

  public Material mix(Material other, double t) {
    float f = (float) t;
    return new Material(
        ambient + (other.ambient - ambient) * f,
        diffuse + (other.diffuse - diffuse) * f,
        specular + (other.specular - specular) * f);
  }
}
