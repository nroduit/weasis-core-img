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

/** Taxonomy of a color map (matplotlib, ColorBrewer); drives editor behaviour and menu grouping. */
public enum ColorMapType {
  /** Ordered ramp from low to high. */
  SEQUENTIAL,
  /** Two ramps meeting at a neutral center. */
  DIVERGING,
  /** Ends meet: positions wrap around the domain instead of clamping. */
  CYCLIC,
  /** Distinct colors for categories, one step per stop. */
  QUALITATIVE,
  /** Color plus opacity, for overlays and volume rendering. */
  TRANSFER
}
