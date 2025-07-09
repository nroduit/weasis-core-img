/*
 * Copyright (c) 2024 Weasis Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.weasis.opencv.seg;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.weasis.core.util.StringUtil;
import org.weasis.opencv.op.lut.ColorLut;

public class RegionAttributes implements Comparable<RegionAttributes> {

  private static final float DEFAULT_LINE_THICKNESS = 1.0f;
  private static final float DEFAULT_OPACITY = 1.0f;
  private static final float MIN_OPACITY = 0.0f;
  private static final float MAX_OPACITY = 1.0f;
  private static final long UNINITIALIZED_PIXEL_COUNT = -1L;
  private static final String[] LABEL_SEPARATORS = {" ", "_", "-"};
  private final int id;
  private String label;
  private String description;
  private String type;
  private Color color;
  private boolean filled = true;
  private float lineThickness = DEFAULT_LINE_THICKNESS;
  private boolean visible = true;
  private float interiorOpacity = DEFAULT_OPACITY;
  protected long numberOfPixels = UNINITIALIZED_PIXEL_COUNT;

  public RegionAttributes(int id, String label) {
    this(id, label, null);
  }

  public RegionAttributes(int id, String label, Color color) {
    this.id = id;
    setLabel(label); // Use setter for validation
    this.color = color;
  }

  public int getId() {
    return id;
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    if (!StringUtil.hasText(label)) {
      throw new IllegalArgumentException("Label cannot be null or empty");
    }
    this.label = label;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public Color getColor() {
    return color;
  }

  public void setColor(Color color) {
    this.color = color;
  }

  public boolean isFilled() {
    return filled;
  }

  public void setFilled(boolean filled) {
    this.filled = filled;
  }

  public float getLineThickness() {
    return lineThickness;
  }

  public void setLineThickness(float lineThickness) {
    if (lineThickness < 0) {
      throw new IllegalArgumentException("Line thickness cannot be negative");
    }
    this.lineThickness = lineThickness;
  }

  public boolean isVisible() {
    return visible;
  }

  public void setVisible(boolean visible) {
    this.visible = visible;
  }

  public float getInteriorOpacity() {
    return interiorOpacity;
  }

  /**
   * Sets the opacity of the interior of the contour.
   *
   * @param interiorOpacity the opacity value, automatically clamped between 0.0 and 1.0
   */
  public void setInteriorOpacity(float interiorOpacity) {
    this.interiorOpacity = clampOpacity(interiorOpacity);
  }

  private static float clampOpacity(float opacity) {
    return Math.max(MIN_OPACITY, Math.min(opacity, MAX_OPACITY));
  }

  public long getNumberOfPixels() {
    return numberOfPixels;
  }

  public String getPrefix() {
    if (label == null) {
      return "";
    }

    int firstSeparatorIndex = label.length();
    for (String separator : LABEL_SEPARATORS) {
      int index = label.indexOf(separator);
      if (index > 3 && index < firstSeparatorIndex) {
        firstSeparatorIndex = index;
      }
    }

    return firstSeparatorIndex == label.length() ? label : label.substring(0, firstSeparatorIndex);
  }

  public void addPixels(Region region) {
    if (region == null) {
      return;
    }

    long regionPixels = region.getNumberOfPixels();
    if (regionPixels < 0) {
      return;
    }
    if (isPixelCountUninitialized()) {
      resetPixelCount();
    }
    this.numberOfPixels += regionPixels;
  }

  public void resetPixelCount() {
    this.numberOfPixels = 0;
  }

  private boolean isPixelCountUninitialized() {
    return numberOfPixels < 0;
  }

  /**
   * Groups regions by their label prefix, maintaining sorted order within each group.
   *
   * @param regions the collection of regions to group
   * @return a map where keys are prefixes and values are sorted lists of regions
   */
  public static <E extends RegionAttributes> Map<String, List<E>> groupRegions(
      Collection<E> regions) {
    if (regions == null || regions.isEmpty()) {
      return Collections.emptyMap();
    }

    Map<String, List<E>> groupedRegions = new HashMap<>();
    for (E region : regions) {
      String prefix = region.getPrefix();
      List<E> regionList = groupedRegions.computeIfAbsent(prefix, k -> new ArrayList<>());
      insertSorted(regionList, region);
    }
    return groupedRegions;
  }

  /** Inserts a region into a sorted list maintaining the sort order. */
  private static <E extends RegionAttributes> void insertSorted(List<E> sortedList, E region) {
    int insertionPoint = Collections.binarySearch(sortedList, region);
    int index = insertionPoint < 0 ? -(insertionPoint + 1) : insertionPoint;
    sortedList.add(index, region);
  }

  public static Color getColor(int[] colorRgb, int contourID) {
    return getColor(colorRgb, contourID, DEFAULT_OPACITY);
  }

  public static Color getColor(int[] colorRgb, int contourID, float opacity) {
    float clampedOpacity = clampOpacity(opacity);
    int alphaValue = Math.round(clampedOpacity * 255f);

    if (isValidColorArray(colorRgb)) {
      return new Color(colorRgb[0], colorRgb[1], colorRgb[2], alphaValue);
    }

    return generateColorFromLut(contourID, alphaValue);
  }

  private static boolean isValidColorArray(int[] colorRgb) {
    return colorRgb != null && colorRgb.length >= 3;
  }

  private static Color generateColorFromLut(int contourID, int alphaValue) {
    byte[][] lut = ColorLut.MULTICOLOR.getByteLut().lutTable();
    int lutIndex = Math.abs(contourID) % 256;

    return new Color(
        lut[0][lutIndex] & 0xFF, lut[1][lutIndex] & 0xFF, lut[2][lutIndex] & 0xFF, alphaValue);
  }

  @Override
  public int compareTo(RegionAttributes other) {
    if (other == null) {
      return 1;
    }
    return StringUtil.collator.compare(this.label, other.label);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!(obj instanceof RegionAttributes other)) return false;

    return id == other.id && Objects.equals(label, other.label);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, label);
  }

  @Override
  public String toString() {
    return String.format(
        "RegionAttributes{id=%d, label='%s', visible=%s, pixels=%d}",
        id, label, visible, numberOfPixels);
  }
}
