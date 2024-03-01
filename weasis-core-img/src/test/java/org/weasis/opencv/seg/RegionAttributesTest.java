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

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class RegionAttributesTest {

  @Test
  void createsRegionAttributesWithGivenIdAndLabel() {
    RegionAttributes regionAttributes = new RegionAttributes(1, "testLabel");
    regionAttributes.setDescription("testDescription");
    regionAttributes.setType("testType");
    Color color = new Color(255, 0, 0);
    regionAttributes.setColor(color);
    regionAttributes.setFilled(false);
    regionAttributes.setLineThickness(2.0f);
    regionAttributes.setVisible(false);
    regionAttributes.setInteriorOpacity(1.27f);

    assertEquals(1, regionAttributes.getId());
    assertEquals("testLabel", regionAttributes.getLabel());
    assertEquals("testDescription", regionAttributes.getDescription());
    assertEquals("testType", regionAttributes.getType());
    assertEquals(color, regionAttributes.getColor());
    assertFalse(regionAttributes.isFilled());
    assertEquals(2.0f, regionAttributes.getLineThickness());
    assertFalse(regionAttributes.isVisible());
    assertEquals(1.0f, regionAttributes.getInteriorOpacity()); // 1.27f is out of range
  }

  @Test
  void regionAttributesCreationWithBlankLabel() {
    Assertions.assertThrows(IllegalArgumentException.class, () -> new RegionAttributes(1, null));
    Assertions.assertThrows(IllegalArgumentException.class, () -> new RegionAttributes(1, " "));
  }

  @Test
  void addPixelsWithMultipleRegionsIncreasesNumberOfPixels() {
    RegionAttributes regionAttributes = new RegionAttributes(1, "validLabel");
    Region region1 = new Region("testRegion1", null, 5);
    Region region2 = new Region("testRegion2");
    region2.setSegmentList(null, 10);
    regionAttributes.addPixels(region1);
    regionAttributes.addPixels(region2);
    assertEquals(15, regionAttributes.getNumberOfPixels());
  }

  @Test
  void addPixelsDoesNotChangeNumberOfPixelsWhenRegionHasNoPixels() {
    RegionAttributes regionAttributes = new RegionAttributes(1, "validLabel");
    Region region = new Region("testRegion");
    regionAttributes.addPixels(null);
    assertEquals(-1, regionAttributes.getNumberOfPixels());
    regionAttributes.addPixels(region);
    assertEquals(-1, regionAttributes.getNumberOfPixels());
  }

  @Test
  void groupRegionsWithEmptyCollection() {
    Collection<RegionAttributes> regions = new ArrayList<>();
    Map<String, List<RegionAttributes>> groupedRegions = RegionAttributes.groupRegions(regions);
    Assertions.assertTrue(groupedRegions.isEmpty());
  }

  @Test
  void groupRegionsWithMultipleRegionsDifferentPrefixes() {
    Collection<RegionAttributes> regions = new ArrayList<>();
    regions.add(new RegionAttributes(1, "test Label1"));
    regions.add(new RegionAttributes(2, "test new label1"));
    regions.add(new RegionAttributes(3, "test-new label2"));
    regions.add(new RegionAttributes(4, "test_new_label3"));
    regions.add(new RegionAttributes(5, "anotherLabel2"));
    Map<String, List<RegionAttributes>> groupedRegions = RegionAttributes.groupRegions(regions);
    Assertions.assertEquals(2, groupedRegions.size());
    Assertions.assertEquals(4, groupedRegions.get("test").size());
    Assertions.assertEquals(1, groupedRegions.get("anotherLabel2").size());
  }

  @Test
  void getColorReturnsExpectedColor() {
    int[] colorRgb = {255, 0, 0};
    Color expectedColor = new Color(255, 0, 0, 127);

    Color resultColor = RegionAttributes.getColor(colorRgb, 1, 0.5f);

    Assertions.assertEquals(expectedColor, resultColor);
    assertEquals(RegionAttributes.getColor(null, 3), RegionAttributes.getColor(null, 3));
    assertNotEquals(RegionAttributes.getColor(null, 3), RegionAttributes.getColor(null, 5));
  }
}
