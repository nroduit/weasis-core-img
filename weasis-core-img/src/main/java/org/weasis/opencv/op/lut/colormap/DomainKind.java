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

/** How stop positions relate to pixel values. */
public enum DomainKind {
  /** Positions in {@code [0, 1]} over the current window; the legacy behaviour. */
  RELATIVE,
  /** Positions in a physical unit; the range is the default window and window/level rescales it. */
  ABSOLUTE,
  /** Positions in a physical unit; window/level never remaps the colors. */
  FIXED,
  /**
   * Positions in percent of a reference supplied at compile time (image maximum, prescribed dose).
   */
  PERCENT
}
