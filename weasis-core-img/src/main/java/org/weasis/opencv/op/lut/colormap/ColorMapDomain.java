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

import java.util.Objects;

/**
 * Value axis of a color map: what stop positions mean and the default range they cover.
 *
 * @param unit physical unit for {@link DomainKind#ABSOLUTE} and {@link DomainKind#FIXED}, else null
 * @param reference what 100 percent means for {@link DomainKind#PERCENT}, else null
 */
public record ColorMapDomain(
    DomainKind kind, String unit, double min, double max, String reference) {

  public static final ColorMapDomain RELATIVE =
      new ColorMapDomain(DomainKind.RELATIVE, null, 0.0, 1.0, null);

  public static final String REFERENCE_MAX = "max";
  public static final String REFERENCE_WINDOW = "window";
  public static final String REFERENCE_PRESCRIPTION = "prescription";

  public ColorMapDomain {
    Objects.requireNonNull(kind, "Domain kind cannot be null");
    if (!Double.isFinite(min) || !Double.isFinite(max) || min >= max) {
      throw new IllegalArgumentException("Domain range must be finite with min < max");
    }
    if (kind == DomainKind.RELATIVE && (min != 0.0 || max != 1.0)) {
      throw new IllegalArgumentException("A relative domain spans [0, 1]");
    }
    if (kind == DomainKind.PERCENT && reference == null) {
      throw new IllegalArgumentException("A percent domain needs a reference");
    }
  }

  public static ColorMapDomain absolute(String unit, double min, double max) {
    return new ColorMapDomain(DomainKind.ABSOLUTE, unit, min, max, null);
  }

  public static ColorMapDomain fixed(String unit, double min, double max) {
    return new ColorMapDomain(DomainKind.FIXED, unit, min, max, null);
  }

  public static ColorMapDomain percent(String reference, double min, double max) {
    return new ColorMapDomain(DomainKind.PERCENT, null, min, max, reference);
  }

  public double span() {
    return max - min;
  }

  /** Fraction of the range at {@code value}, not clamped. */
  public double normalize(double value) {
    return (value - min) / span();
  }

  public double denormalize(double fraction) {
    return min + fraction * span();
  }

  public boolean isRelative() {
    return kind == DomainKind.RELATIVE;
  }
}
