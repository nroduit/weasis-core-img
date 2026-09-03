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

/**
 * Colors for values outside the stops. A null entry clamps to the nearest stop; a null {@code nan}
 * is transparent.
 */
public record OutsideColors(Rgba low, Rgba high, Rgba nan) {

  public static final OutsideColors CLAMP = new OutsideColors(null, null, null);
  public static final OutsideColors TRANSPARENT =
      new OutsideColors(Rgba.TRANSPARENT, Rgba.TRANSPARENT, Rgba.TRANSPARENT);
}
