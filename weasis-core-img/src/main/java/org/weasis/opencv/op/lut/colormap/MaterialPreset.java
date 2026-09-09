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

/** Named Blinn-Phong settings so a user picks a look instead of three coefficients. */
public enum MaterialPreset {
  MATTE(new Material(0.25f, 0.95f, 0.0f), 10f),
  SOFT_TISSUE(new Material(0.2f, 0.9f, 0.1f), 10f),
  SKIN(new Material(0.2f, 0.85f, 0.25f), 15f),
  VESSEL(new Material(0.15f, 0.8f, 0.6f), 30f),
  BONE(new Material(0.3f, 0.9f, 0.5f), 20f),
  GLASS(new Material(0.1f, 0.3f, 0.9f), 60f),
  METAL(new Material(0.35f, 0.6f, 0.95f), 80f);

  private final Material material;
  private final float specularPower;

  MaterialPreset(Material material, float specularPower) {
    this.material = material;
    this.specularPower = specularPower;
  }

  public Material material() {
    return material;
  }

  /** Highlight tightness that goes with the material; a map-wide setting in {@link Lighting}. */
  public float specularPower() {
    return specularPower;
  }
}
