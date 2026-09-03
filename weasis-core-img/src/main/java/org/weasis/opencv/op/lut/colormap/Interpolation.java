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

/** How values between two stops are produced. */
public enum Interpolation {
  /** Straight blend between neighbouring stops. */
  LINEAR,
  /** Each stop holds until the next one: qualitative maps and isodose bands. */
  STEP,
  /** Stops are samples of a table, blended linearly; editors show them read-only. */
  SAMPLED;

  public boolean isStep() {
    return this == STEP;
  }
}
