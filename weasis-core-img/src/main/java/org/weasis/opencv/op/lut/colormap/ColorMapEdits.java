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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Whole-map edits an editor offers as single actions; every method returns a new map. */
public final class ColorMapEdits {

  private ColorMapEdits() {}

  /**
   * Moves every stop linearly from the map's default range onto {@code [newMin, newMax]}. A
   * non-relative domain takes the new range; a relative one keeps {@code [0, 1]} and the target is
   * clamped into it.
   */
  public static ColorMap rescaled(ColorMap map, double newMin, double newMax) {
    ColorMapDomain domain = map.domain();
    double lo = domain.isRelative() ? Math.max(0.0, newMin) : newMin;
    double hi = domain.isRelative() ? Math.min(1.0, newMax) : newMax;
    if (!(hi > lo)) {
      throw new IllegalArgumentException("Range must have max > min: [" + lo + ", " + hi + "]");
    }
    double scale = (hi - lo) / domain.span();
    var stops = new ArrayList<ColorStop>(map.stops().size());
    for (ColorStop stop : map.stops()) {
      stops.add(stop.withPosition(lo + (stop.position() - domain.min()) * scale));
    }
    ColorMapDomain target =
        domain.isRelative()
            ? domain
            : new ColorMapDomain(domain.kind(), domain.unit(), lo, hi, domain.reference());
    return map.toBuilder().domain(target).stops(stops).build();
  }

  /**
   * The map spread over the physical range {@code [min, max]} of an absolute domain in {@code
   * unit}; a map already anchored to values (absolute or fixed domain) is returned as is.
   */
  public static ColorMap anchored(ColorMap map, String unit, double min, double max) {
    ColorMapDomain domain = map.domain();
    if (domain.kind() == DomainKind.ABSOLUTE || domain.kind() == DomainKind.FIXED) {
      return map;
    }
    ColorMap absolute =
        map.toBuilder().domain(ColorMapDomain.absolute(unit, domain.min(), domain.max())).build();
    return rescaled(absolute, min, max);
  }

  /** Samples the map at the center of {@code bands} equal bands and keeps one step per band. */
  public static ColorMap discretized(ColorMap map, int bands) {
    if (bands < 2) {
      throw new IllegalArgumentException("At least 2 bands are needed: " + bands);
    }
    ColorMapDomain domain = map.domain();
    ColorMapSampler sampler = map.sampler();
    boolean alpha = map.hasAlpha();
    var stops = new ArrayList<ColorStop>(bands);
    for (int i = 0; i < bands; i++) {
      double start = domain.denormalize((double) i / bands);
      Rgba sample = sampler.sample(domain.denormalize((i + 0.5) / bands));
      stops.add(
          new ColorStop(
              start, sample.withAlpha(1f).toColor(), alpha ? sample.alpha() : null, null, null));
    }
    return map.toBuilder().interpolation(Interpolation.STEP).stops(stops).build();
  }

  /**
   * Replaces the alpha curve by a ramp from fully transparent at {@code transparentBelow} to fully
   * opaque at {@code opaqueFrom}; equal values make a hard edge. Colors are untouched.
   */
  public static ColorMap withThreshold(ColorMap map, double transparentBelow, double opaqueFrom) {
    if (opaqueFrom < transparentBelow) {
      throw new IllegalArgumentException("opaqueFrom must not be below transparentBelow");
    }
    var stops = new ArrayList<ColorStop>();
    for (ColorStop stop : map.stops()) {
      if (stop.hasColor()) {
        stops.add(stop.withAlpha(null));
      }
    }
    stops.add(ColorStop.ofAlpha(transparentBelow, 0f));
    stops.add(ColorStop.ofAlpha(opaqueFrom, 1f));
    return map.toBuilder().type(ColorMapType.TRANSFER).stops(stops).build();
  }

  /**
   * Takes the color curve of {@code palette}, stretched over the map's default range, and keeps the
   * map's alpha curve and metadata.
   */
  public static ColorMap withPalette(ColorMap map, ColorMap palette) {
    Objects.requireNonNull(palette, "Palette cannot be null");
    ColorMapDomain target = map.domain();
    ColorMapDomain source = palette.domain();
    var stops = new ArrayList<ColorStop>();
    for (ColorStop stop : map.stops()) {
      if (stop.hasAlpha()) {
        stops.add(stop.withColor(null).withMaterial(null));
      }
    }
    for (ColorStop stop : palette.stops()) {
      if (stop.hasColor()) {
        double position = target.denormalize(source.normalize(stop.position()));
        stops.add(ColorStop.of(position, stop.color()).withGroup(stop.group()));
      }
    }
    return map.toBuilder()
        .space(palette.space())
        .interpolation(palette.interpolation())
        .stops(stops)
        .build();
  }

  public static ColorMap withStop(ColorMap map, ColorStop stop) {
    var stops = new ArrayList<>(map.stops());
    stops.add(Objects.requireNonNull(stop, "Stop cannot be null"));
    return map.toBuilder().stops(stops).build();
  }

  /** Replaces the stop at {@code index} of the sorted stop list. */
  public static ColorMap withStop(ColorMap map, int index, ColorStop stop) {
    var stops = new ArrayList<>(map.stops());
    stops.set(index, Objects.requireNonNull(stop, "Stop cannot be null"));
    return map.toBuilder().stops(stops).build();
  }

  /**
   * Removes the stop at {@code index} of the sorted stop list.
   *
   * @throws IllegalArgumentException when it is the last stop
   */
  public static ColorMap withoutStop(ColorMap map, int index) {
    if (map.stops().size() == 1) {
      throw new IllegalArgumentException("A color map keeps at least one stop");
    }
    var stops = new ArrayList<>(map.stops());
    stops.remove(index);
    return map.toBuilder().stops(stops).build();
  }

  /** Same stops, evenly spread over the default range; useful after pasting a color list. */
  public static ColorMap evenlySpaced(ColorMap map) {
    List<ColorStop> stops = map.stops();
    int n = stops.size();
    var spread = new ArrayList<ColorStop>(n);
    for (int i = 0; i < n; i++) {
      double f = n == 1 ? 0.0 : (double) i / (n - 1);
      spread.add(stops.get(i).withPosition(map.domain().denormalize(f)));
    }
    return map.toBuilder().stops(spread).build();
  }
}
