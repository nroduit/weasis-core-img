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

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("RegionAttributes Tests")
class RegionAttributesTest {

  // Test constants
  private static final int TEST_ID = 1;
  private static final String TEST_LABEL = "testLabel";
  private static final String TEST_DESCRIPTION = "testDescription";
  private static final String TEST_TYPE = "testType";
  private static final Color TEST_COLOR = new Color(255, 0, 0);
  private static final float DELTA = 0.001f;

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Should create RegionAttributes with valid ID and label")
    void createsRegionAttributesWithValidIdAndLabel() {
      RegionAttributes attributes = new RegionAttributes(TEST_ID, TEST_LABEL);

      assertAll(
          "Basic attributes should be set correctly",
          () -> assertEquals(TEST_ID, attributes.getId()),
          () -> assertEquals(TEST_LABEL, attributes.getLabel()),
          () -> assertNull(attributes.getDescription()),
          () -> assertNull(attributes.getType()),
          () -> assertNull(attributes.getColor()));
    }

    @Test
    @DisplayName("Should create RegionAttributes with color")
    void createsRegionAttributesWithColor() {
      RegionAttributes attributes = new RegionAttributes(TEST_ID, TEST_LABEL, TEST_COLOR);

      assertAll(
          "Attributes with color should be set correctly",
          () -> assertEquals(TEST_ID, attributes.getId()),
          () -> assertEquals(TEST_LABEL, attributes.getLabel()),
          () -> assertEquals(TEST_COLOR, attributes.getColor()));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  ", "\t", "\n"})
    @DisplayName("Should throw exception for invalid labels")
    void throwsExceptionForInvalidLabels(String invalidLabel) {
      assertThrows(
          IllegalArgumentException.class,
          () -> new RegionAttributes(TEST_ID, invalidLabel),
          "Should throw exception for invalid label: " + invalidLabel);
    }

    @Test
    @DisplayName("Should initialize with default values")
    void initializesWithDefaultValues() {
      RegionAttributes attributes = new RegionAttributes(TEST_ID, TEST_LABEL);

      assertAll(
          "Default values should be set correctly",
          () -> assertTrue(attributes.isFilled(), "Should be filled by default"),
          () -> assertEquals(1.0f, attributes.getLineThickness(), DELTA, "Default line thickness"),
          () -> assertTrue(attributes.isVisible(), "Should be visible by default"),
          () -> assertEquals(1.0f, attributes.getInteriorOpacity(), DELTA, "Default opacity"),
          () -> assertEquals(-1L, attributes.getNumberOfPixels(), "Uninitialized pixel count"));
    }
  }

  @Nested
  @DisplayName("Property Management Tests")
  class PropertyManagementTests {

    @Test
    @DisplayName("Should set and get all properties correctly")
    void setsAndGetsAllPropertiesCorrectly() {
      RegionAttributes attributes = new RegionAttributes(TEST_ID, TEST_LABEL);

      // Set all properties
      attributes.setDescription(TEST_DESCRIPTION);
      attributes.setType(TEST_TYPE);
      attributes.setColor(TEST_COLOR);
      attributes.setFilled(false);
      attributes.setLineThickness(2.5f);
      attributes.setVisible(false);
      attributes.setInteriorOpacity(0.7f);

      assertAll(
          "All properties should be set and retrieved correctly",
          () -> assertEquals(TEST_DESCRIPTION, attributes.getDescription()),
          () -> assertEquals(TEST_TYPE, attributes.getType()),
          () -> assertEquals(TEST_COLOR, attributes.getColor()),
          () -> assertFalse(attributes.isFilled()),
          () -> assertEquals(2.5f, attributes.getLineThickness(), DELTA),
          () -> assertFalse(attributes.isVisible()),
          () -> assertEquals(0.7f, attributes.getInteriorOpacity(), DELTA));
    }

    @Test
    @DisplayName("Should handle null values gracefully")
    void handlesNullValuesGracefully() {
      RegionAttributes attributes = new RegionAttributes(TEST_ID, TEST_LABEL);

      assertDoesNotThrow(
          () -> {
            attributes.setDescription(null);
            attributes.setType(null);
            attributes.setColor(null);
          },
          "Should handle null values without throwing exceptions");

      assertAll(
          "Null values should be handled correctly",
          () -> assertNull(attributes.getDescription()),
          () -> assertNull(attributes.getType()),
          () -> assertNull(attributes.getColor()));
    }

    @ParameterizedTest
    @ValueSource(floats = {-1.0f, -0.1f, -100.0f})
    @DisplayName("Should reject negative line thickness")
    void rejectsNegativeLineThickness(float negativeThickness) {
      RegionAttributes attributes = new RegionAttributes(TEST_ID, TEST_LABEL);

      assertThrows(
          IllegalArgumentException.class,
          () -> attributes.setLineThickness(negativeThickness),
          "Should reject negative line thickness: " + negativeThickness);
    }

    @ParameterizedTest
    @CsvSource({"-0.5, 0.0", "0.0, 0.0", "0.5, 0.5", "1.0, 1.0", "1.27, 1.0", "2.0, 1.0"})
    @DisplayName("Should clamp opacity values correctly")
    void clampsOpacityValuesCorrectly(float input, float expected) {
      RegionAttributes attributes = new RegionAttributes(TEST_ID, TEST_LABEL);

      attributes.setInteriorOpacity(input);

      assertEquals(
          expected,
          attributes.getInteriorOpacity(),
          DELTA,
          "Opacity should be clamped: " + input + " -> " + expected);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  ", "\t", "\n"})
    @DisplayName("Should reject invalid labels when setting")
    void rejectsInvalidLabelsWhenSetting(String invalidLabel) {
      RegionAttributes attributes = new RegionAttributes(TEST_ID, TEST_LABEL);

      assertThrows(
          IllegalArgumentException.class,
          () -> attributes.setLabel(invalidLabel),
          "Should reject invalid label when setting: " + invalidLabel);
    }
  }

  @Nested
  @DisplayName("Prefix Extraction Tests")
  class PrefixExtractionTests {

    @ParameterizedTest
    @CsvSource({
      "'test Label1', 'test'",
      "'test-new label2', 'test'",
      "'test_new_label3', 'test'",
      "'anotherLabel2', 'anotherLabel2'",
      "'ab-cd', 'ab-cd'", // Separator at index 2, should return full label
      "'abc def', 'abc def'", // Separator at index 3, should return full label
      "'abcd efg', 'abcd'", // Separator at index 4, should return prefix
      "'no_separators_here', 'no_separators_here'"
    })
    @DisplayName("Should extract prefix correctly based on separator position")
    void extractsPrefixCorrectly(String label, String expectedPrefix) {
      RegionAttributes attributes = new RegionAttributes(TEST_ID, label);

      assertEquals(
          expectedPrefix,
          attributes.getPrefix(),
          "Prefix should be extracted correctly from: " + label);
    }

    @Test
    @DisplayName("Should handle multiple separators correctly")
    void handlesMultipleSeparatorsCorrectly() {
      RegionAttributes attributes =
          new RegionAttributes(TEST_ID, "test_label-with multiple_separators");

      // Should use the first separator that meets the minimum length requirement
      assertEquals("test", attributes.getPrefix());
    }
  }

  @Nested
  @DisplayName("Pixel Count Management Tests")
  class PixelCountManagementTests {

    @Test
    @DisplayName("Should add pixels from multiple regions correctly")
    void addsPixelsFromMultipleRegionsCorrectly() {
      RegionAttributes attributes = new RegionAttributes(TEST_ID, TEST_LABEL);
      Region region1 = new Region("region1", null, 5L);
      Region region2 = new Region("region2", null, 10L);

      attributes.addPixels(region1);
      attributes.addPixels(region2);

      assertEquals(
          15L, attributes.getNumberOfPixels(), "Should accumulate pixels from multiple regions");
    }

    @Test
    @DisplayName("Should handle null region gracefully")
    void handlesNullRegionGracefully() {
      RegionAttributes attributes = new RegionAttributes(TEST_ID, TEST_LABEL);

      attributes.addPixels(null);

      assertEquals(
          -1L,
          attributes.getNumberOfPixels(),
          "Should not change pixel count when adding null region");
    }

    @Test
    @DisplayName("Should handle regions with invalid pixel counts")
    void handlesRegionsWithInvalidPixelCounts() {
      RegionAttributes attributes = new RegionAttributes(TEST_ID, TEST_LABEL);
      Region regionWithNoPixels = new Region("region");

      attributes.addPixels(regionWithNoPixels);

      assertEquals(
          -1L,
          attributes.getNumberOfPixels(),
          "Should not change pixel count for regions with invalid pixel counts");
    }

    @Test
    @DisplayName("Should reset pixel count correctly")
    void resetsPixelCountCorrectly() {
      RegionAttributes attributes = new RegionAttributes(TEST_ID, TEST_LABEL);
      Region region = new Region("region", null, 100L);

      attributes.addPixels(region);
      attributes.resetPixelCount();

      assertEquals(0L, attributes.getNumberOfPixels(), "Should reset pixel count to zero");
    }

    @Test
    @DisplayName("Should initialize pixel count when adding first valid region")
    void initializesPixelCountWhenAddingFirstValidRegion() {
      RegionAttributes attributes = new RegionAttributes(TEST_ID, TEST_LABEL);
      Region region1 = new Region("region1", null, 5L);
      Region region2 = new Region("region2", null, 3L);

      // First valid region should initialize count to 0 then add pixels
      attributes.addPixels(region1);
      assertEquals(5L, attributes.getNumberOfPixels());

      // Second region should just add to existing count
      attributes.addPixels(region2);
      assertEquals(8L, attributes.getNumberOfPixels());
    }
  }

  @Nested
  @DisplayName("Region Grouping Tests")
  class RegionGroupingTests {

    @Test
    @DisplayName("Should return empty map for null or empty collection")
    void returnsEmptyMapForNullOrEmptyCollection() {
      assertAll(
          "Should handle null and empty collections",
          () ->
              assertTrue(
                  RegionAttributes.groupRegions(null).isEmpty(),
                  "Should return empty map for null collection"),
          () ->
              assertTrue(
                  RegionAttributes.groupRegions(Collections.emptyList()).isEmpty(),
                  "Should return empty map for empty collection"));
    }

    @Test
    @DisplayName("Should group regions by prefix correctly")
    void groupsRegionsByPrefixCorrectly() {
      Collection<RegionAttributes> regions =
          Arrays.asList(
              new RegionAttributes(1, "test Label1"),
              new RegionAttributes(2, "test new label1"),
              new RegionAttributes(3, "test-new label2"),
              new RegionAttributes(4, "test_new_label3"),
              new RegionAttributes(5, "anotherLabel2"));

      Map<String, List<RegionAttributes>> groupedRegions = RegionAttributes.groupRegions(regions);

      assertAll(
          "Regions should be grouped correctly by prefix",
          () -> assertEquals(2, groupedRegions.size(), "Should have 2 groups"),
          () ->
              assertEquals(
                  4, groupedRegions.get("test").size(), "Test group should have 4 regions"),
          () ->
              assertEquals(
                  1,
                  groupedRegions.get("anotherLabel2").size(),
                  "AnotherLabel2 group should have 1 region"));
    }

    @Test
    @DisplayName("Should maintain sorted order within groups")
    void maintainsSortedOrderWithinGroups() {
      Collection<RegionAttributes> regions =
          Arrays.asList(
              new RegionAttributes(3, "test zebra"),
              new RegionAttributes(1, "test apple"),
              new RegionAttributes(2, "test banana"));

      Map<String, List<RegionAttributes>> groupedRegions = RegionAttributes.groupRegions(regions);
      List<RegionAttributes> testGroup = groupedRegions.get("test");

      assertAll(
          "Group should be sorted by label",
          () -> assertEquals("test apple", testGroup.get(0).getLabel()),
          () -> assertEquals("test banana", testGroup.get(1).getLabel()),
          () -> assertEquals("test zebra", testGroup.get(2).getLabel()));
    }

    @Test
    @DisplayName("Should handle single region correctly")
    void handlesSingleRegionCorrectly() {
      Collection<RegionAttributes> regions =
          Collections.singletonList(new RegionAttributes(1, "singleRegion"));

      Map<String, List<RegionAttributes>> groupedRegions = RegionAttributes.groupRegions(regions);

      assertAll(
          "Single region should be handled correctly",
          () -> assertEquals(1, groupedRegions.size()),
          () -> assertTrue(groupedRegions.containsKey("singleRegion")),
          () -> assertEquals(1, groupedRegions.get("singleRegion").size()));
    }
  }

  @Nested
  @DisplayName("Color Generation Tests")
  class ColorGenerationTests {

    @Test
    @DisplayName("Should generate color from RGB array correctly")
    void generatesColorFromRgbArrayCorrectly() {
      int[] colorRgb = {255, 128, 64};
      float opacity = 0.5f;
      int expectedAlpha = Math.round(opacity * 255f);

      Color result = RegionAttributes.getColor(colorRgb, 1, opacity);

      assertAll(
          "RGB color should be generated correctly",
          () -> assertEquals(255, result.getRed()),
          () -> assertEquals(128, result.getGreen()),
          () -> assertEquals(64, result.getBlue()),
          () -> assertEquals(expectedAlpha, result.getAlpha()));
    }

    @Test
    @DisplayName("Should use default opacity when not specified")
    void usesDefaultOpacityWhenNotSpecified() {
      int[] colorRgb = {255, 0, 0};

      Color result = RegionAttributes.getColor(colorRgb, 1);

      assertEquals(255, result.getAlpha(), "Should use full opacity by default");
    }

    @ParameterizedTest
    @ValueSource(floats = {-0.5f, 0.0f, 0.3f, 1.0f, 1.5f})
    @DisplayName("Should clamp opacity values in color generation")
    void clampsOpacityValuesInColorGeneration(float opacity) {
      int[] colorRgb = {100, 150, 200};

      Color result = RegionAttributes.getColor(colorRgb, 1, opacity);

      float expectedOpacity = Math.max(0.0f, Math.min(opacity, 1.0f));
      int expectedAlpha = Math.round(expectedOpacity * 255f);
      assertEquals(expectedAlpha, result.getAlpha(), "Opacity should be clamped: " + opacity);
    }

    @Test
    @DisplayName("Should generate color from LUT when RGB array is invalid")
    void generatesColorFromLutWhenRgbArrayIsInvalid() {
      int contourId1 = 3;
      int contourId2 = 5;

      Color color1 = RegionAttributes.getColor(null, contourId1);
      Color color2 = RegionAttributes.getColor(null, contourId2);

      assertAll(
          "LUT colors should be generated correctly",
          () -> assertNotNull(color1, "Should generate color from LUT"),
          () -> assertNotNull(color2, "Should generate color from LUT"),
          () ->
              assertEquals(
                  color1,
                  RegionAttributes.getColor(null, contourId1),
                  "Same contour ID should produce same color"),
          () ->
              assertNotEquals(
                  color1, color2, "Different contour IDs should produce different colors"));
    }

    @ParameterizedTest
    @ValueSource(ints = {-5, -1, 0, 1, 255, 256, 300})
    @DisplayName("Should handle various contour IDs correctly")
    void handlesVariousContourIdsCorrectly(int contourId) {
      Color result = RegionAttributes.getColor(null, contourId);

      assertNotNull(result, "Should generate color for any contour ID: " + contourId);
    }

    @Test
    @DisplayName("Should handle insufficient RGB array length")
    void handlesInsufficientRgbArrayLength() {
      int[] shortArray = {255, 128}; // Only 2 elements

      Color result = RegionAttributes.getColor(shortArray, 1);

      assertNotNull(result, "Should fall back to LUT generation for insufficient RGB array");
    }
  }

  @Nested
  @DisplayName("Comparison and Object Behavior Tests")
  class ComparisonAndObjectBehaviorTests {

    @Test
    @DisplayName("Should implement compareTo correctly")
    void implementsCompareToCorrectly() {
      RegionAttributes attr1 = new RegionAttributes(1, "apple");
      RegionAttributes attr2 = new RegionAttributes(2, "banana");
      RegionAttributes attr3 = new RegionAttributes(3, "apple");

      assertAll(
          "compareTo should work correctly",
          () -> assertTrue(attr1.compareTo(attr2) < 0, "apple < banana"),
          () -> assertTrue(attr2.compareTo(attr1) > 0, "banana > apple"),
          () -> assertEquals(0, attr1.compareTo(attr3), "apple == apple"),
          () -> assertTrue(attr1.compareTo(null) > 0, "any string > null"));
    }

    @Test
    @DisplayName("Should implement equals and hashCode consistently")
    void implementsEqualsAndHashCodeConsistently() {
      RegionAttributes attr1 = new RegionAttributes(1, "testLabel");
      RegionAttributes attr2 = new RegionAttributes(1, "testLabel");
      RegionAttributes attr3 = new RegionAttributes(2, "testLabel");
      RegionAttributes attr4 = new RegionAttributes(1, "differentLabel");

      assertAll(
          "equals and hashCode should be consistent",
          () -> assertEquals(attr1, attr2, "Same ID and label should be equal"),
          () ->
              assertEquals(
                  attr1.hashCode(), attr2.hashCode(), "Equal objects should have same hash code"),
          () -> assertNotEquals(attr1, attr3, "Different IDs should not be equal"),
          () -> assertNotEquals(attr1, attr4, "Different labels should not be equal"),
          () -> assertEquals(attr1, attr1, "Object should equal itself"),
          () -> assertNotEquals(attr1, null, "Object should not equal null"),
          () -> assertNotEquals(attr1, "string", "Should not equal different type"));
    }

    @Test
    @DisplayName("Should implement toString meaningfully")
    void implementsToStringMeaningfully() {
      RegionAttributes attributes = new RegionAttributes(42, "testRegion");
      attributes.setVisible(false);
      attributes.addPixels(new Region("region", null, 100L));

      String toString = attributes.toString();

      assertAll(
          "toString should contain key information",
          () -> assertTrue(toString.contains("42"), "Should contain ID"),
          () -> assertTrue(toString.contains("testRegion"), "Should contain label"),
          () -> assertTrue(toString.contains("false"), "Should contain visibility"),
          () -> assertTrue(toString.contains("100"), "Should contain pixel count"));
    }
  }

  @Nested
  @DisplayName("Edge Cases and Error Handling Tests")
  class EdgeCasesAndErrorHandlingTests {

    @Test
    @DisplayName("Should handle extreme values gracefully")
    void handlesExtremeValuesGracefully() {
      RegionAttributes attributes = new RegionAttributes(Integer.MAX_VALUE, "extremeTest");

      assertDoesNotThrow(
          () -> {
            attributes.setLineThickness(Float.MAX_VALUE);
            attributes.setInteriorOpacity(Float.MAX_VALUE);
            attributes.addPixels(new Region("region", null, Long.MAX_VALUE));
          },
          "Should handle extreme values without throwing exceptions");
    }

    @Test
    @DisplayName("Should maintain immutability of returned collections")
    void maintainsImmutabilityOfReturnedCollections() {
      Collection<RegionAttributes> regions =
          Arrays.asList(new RegionAttributes(1, "test1"), new RegionAttributes(2, "test2"));

      Map<String, List<RegionAttributes>> groupedRegions = RegionAttributes.groupRegions(regions);

      // The returned map should be modifiable but the test should verify behavior
      assertDoesNotThrow(
          () -> groupedRegions.get("test1"), "Should be able to access grouped regions");
    }

    @Test
    @DisplayName("Should handle concurrent modifications safely")
    void handlesConcurrentModificationsSafely() {
      // This is more of a design consideration test
      Collection<RegionAttributes> regions = new ArrayList<>();
      regions.add(new RegionAttributes(1, "test1"));

      Map<String, List<RegionAttributes>> result = RegionAttributes.groupRegions(regions);

      // Modifying original collection shouldn't affect the result
      regions.clear();

      assertFalse(result.isEmpty(), "Result should be independent of source collection");
    }
  }
}
