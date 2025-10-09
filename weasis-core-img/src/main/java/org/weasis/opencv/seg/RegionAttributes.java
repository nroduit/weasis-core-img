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
import org.weasis.core.util.MathUtil;
import org.weasis.core.util.StringUtil;
import org.weasis.opencv.op.lut.ColorLut;

/**
 * Represents the visual and descriptive attributes of a segmentation region. This class
 * encapsulates display properties such as color, opacity, visibility, and metadata like labels and
 * pixel counts for regions in medical imaging.
 */
public class RegionAttributes implements Comparable<RegionAttributes> {

  private static final float DEFAULT_LINE_THICKNESS = 1.0f;
  private static final float DEFAULT_OPACITY = 1.0f;
  private static final float MIN_OPACITY = 0.0f;
  private static final float MAX_OPACITY = 1.0f;
  private static final long UNINITIALIZED_PIXEL_COUNT = -1L;
  private static final String[] LABEL_SEPARATORS = {" ", "_", "-"};
  private static final int MIN_PREFIX_LENGTH = 3;
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

  /**
   * Creates a new RegionAttributes with the specified ID and label.
   *
   * @param id the unique identifier for this region
   * @param label the display label for this region
   * @throws IllegalArgumentException if the label is null or empty
   */
  public RegionAttributes(int id, String label) {
    this(id, label, null);
  }

  /**
   * Creates a new RegionAttributes with the specified ID, label, and color.
   *
   * @param id the unique identifier for this region
   * @param label the display label for this region
   * @param color the color for this region (may be null)
   * @throws IllegalArgumentException if the label is null or empty
   */
  public RegionAttributes(int id, String label, Color color) {
    this.id = id;
    setLabel(label);
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
   * Sets the interior opacity, automatically clamped between 0.0 and 1.0.
   *
   * @param interiorOpacity the opacity value to set
   */
  public void setInteriorOpacity(float interiorOpacity) {
    this.interiorOpacity = clampOpacity(interiorOpacity);
  }

  public long getNumberOfPixels() {
    return numberOfPixels;
  }

  /**
   * Extracts the prefix from the label by finding the first separator after position 3. Returns the
   * full label if no suitable separator is found.
   *
   * @return the label prefix or the full label
   */
  public String getPrefix() {
    if (label == null) {
      return "";
    }

    return findFirstSeparator(label).map(index -> label.substring(0, index)).orElse(label);
  }

  private java.util.Optional<Integer> findFirstSeparator(String text) {
    return java.util.Arrays.stream(LABEL_SEPARATORS)
        .mapToInt(text::indexOf)
        .filter(index -> index > MIN_PREFIX_LENGTH)
        .min()
        .stream()
        .boxed()
        .findFirst();
  }

  /**
   * Adds pixels from a region to this region's pixel count.
   *
   * @param region the region whose pixels to add (null-safe)
   */
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
   * @param <E> the region type extending RegionAttributes
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
      groupedRegions.computeIfAbsent(prefix, k -> new ArrayList<>()).add(region);
    }
    // Sort each group
    groupedRegions.values().forEach(Collections::sort);
    return groupedRegions;
  }

  /**
   * Creates a Color from RGB values and contour ID with default opacity.
   *
   * @param colorRgb RGB color array (may be null or invalid)
   * @param contourID the contour identifier for color generation
   * @return a Color object
   */
  public static Color getColor(int[] colorRgb, int contourID) {
    return getColor(colorRgb, contourID, DEFAULT_OPACITY);
  }

  /**
   * Creates a Color from RGB values and contour ID with specified opacity. Falls back to LUT-based
   * color generation if RGB array is invalid.
   *
   * @param colorRgb RGB color array (may be null or invalid)
   * @param contourID the contour identifier for color generation
   * @param opacity the opacity value (clamped between 0.0 and 1.0)
   * @return a Color object
   */
  public static Color getColor(int[] colorRgb, int contourID, float opacity) {
    int alphaValue = Math.round(clampOpacity(opacity) * 255f);

    if (isValidColorArray(colorRgb)) {
      return new Color(colorRgb[0], colorRgb[1], colorRgb[2], alphaValue);
    }

    return generateColorFromLut(contourID, alphaValue);
  }

  private static float clampOpacity(float opacity) {
    return MathUtil.clamp(opacity, MIN_OPACITY, MAX_OPACITY);
  }

  private static boolean isValidColorArray(int[] colorRgb) {
    return colorRgb != null && colorRgb.length >= 3;
  }

  private static Color generateColorFromLut(int contourID, int alphaValue) {
    byte[][] lut = ColorLut.MULTICOLOR.getByteLut().lutTable();
    int lutIndex = Math.abs(contourID) % lut[0].length;

    return new Color(
        Byte.toUnsignedInt(lut[0][lutIndex]),
        Byte.toUnsignedInt(lut[1][lutIndex]),
        Byte.toUnsignedInt(lut[2][lutIndex]),
        alphaValue);
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
    return this == obj
        || (obj instanceof RegionAttributes other
            && id == other.id
            && Objects.equals(label, other.label));
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, label);
  }

  @Override
  public String toString() {
    return "RegionAttributes{id=%d, label='%s', visible=%s, pixels=%d}"
        .formatted(id, label, visible, numberOfPixels);
  }
}
