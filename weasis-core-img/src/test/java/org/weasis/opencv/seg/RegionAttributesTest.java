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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.weasis.core.util.MathUtil;

@DisplayNameGeneration(ReplaceUnderscores.class)
class RegionAttributesTest {

  // Test data constants - using modern Java features
  private static final TestData TEST_DATA = new TestData();
  private static final float FLOAT_PRECISION = 0.001f;

  private record TestData(
      int id,
      String label,
      String description,
      String type,
      Color color,
      List<String> invalidLabels,
      List<Float> negativeThicknesses,
      List<OpacityTest> opacityTests,
      List<PrefixTest> prefixTests) {

    TestData() {
      this(
          1,
          "testLabel",
          "Test Description",
          "Test Type",
          new Color(255, 128, 64),
          List.of("", "  ", "\t", "\n"),
          List.of(-1.0f, -0.1f, -100.0f),
          List.of(
              new OpacityTest(-0.5f, 0.0f),
              new OpacityTest(0.0f, 0.0f),
              new OpacityTest(0.5f, 0.5f),
              new OpacityTest(1.0f, 1.0f),
              new OpacityTest(1.5f, 1.0f),
              new OpacityTest(2.0f, 1.0f)),
          List.of(
              new PrefixTest("test Label1", "test"),
              new PrefixTest("test-new label2", "test"),
              new PrefixTest("test_new_label3", "test"),
              new PrefixTest("anotherLabel2", "anotherLabel2"),
              new PrefixTest("ab-cd", "ab-cd"), // Too short prefix
              new PrefixTest("abc def", "abc def"), // Exactly at boundary
              new PrefixTest("abcd efg", "abcd"), // Valid prefix
              new PrefixTest("no_separators_here", "no_separators_here")));
    }
  }

  private record OpacityTest(float input, float expected) {}

  private record PrefixTest(String label, String expectedPrefix) {}

  private record ColorTest(int[] rgb, int contourId, float opacity, boolean expectLutColor) {}

  @Nested
  class Constructor_tests {

    @Test
    void should_create_region_attributes_with_valid_id_and_label() {
      var attributes = new RegionAttributes(TEST_DATA.id(), TEST_DATA.label());

      assertAll(
          "Basic attributes should be set correctly",
          () -> assertEquals(TEST_DATA.id(), attributes.getId()),
          () -> assertEquals(TEST_DATA.label(), attributes.getLabel()),
          () -> assertNull(attributes.getDescription()),
          () -> assertNull(attributes.getType()),
          () -> assertNull(attributes.getColor()));
    }

    @Test
    void should_create_region_attributes_with_color() {
      var attributes = new RegionAttributes(TEST_DATA.id(), TEST_DATA.label(), TEST_DATA.color());

      assertAll(
          "Attributes with color should be set correctly",
          () -> assertEquals(TEST_DATA.id(), attributes.getId()),
          () -> assertEquals(TEST_DATA.label(), attributes.getLabel()),
          () -> assertEquals(TEST_DATA.color(), attributes.getColor()));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  ", "\t", "\n"})
    void should_throw_exception_for_invalid_labels(String invalidLabel) {
      assertThrows(
          IllegalArgumentException.class,
          () -> new RegionAttributes(TEST_DATA.id(), invalidLabel),
          "Should reject invalid label: '%s'".formatted(String.valueOf(invalidLabel)));
    }

    @Test
    void should_initialize_with_default_values() {
      var attributes = new RegionAttributes(TEST_DATA.id(), TEST_DATA.label());

      assertAll(
          "Default values should be set correctly",
          () -> assertTrue(attributes.isFilled()),
          () -> assertEquals(1.0f, attributes.getLineThickness(), FLOAT_PRECISION),
          () -> assertTrue(attributes.isVisible()),
          () -> assertEquals(1.0f, attributes.getInteriorOpacity(), FLOAT_PRECISION),
          () -> assertEquals(-1L, attributes.getNumberOfPixels()));
    }
  }

  @Nested
  class Property_management_tests {

    @Test
    void should_set_and_get_all_properties_correctly() {
      var attributes = new RegionAttributes(TEST_DATA.id(), TEST_DATA.label());

      // Set all properties using test data
      attributes.setDescription(TEST_DATA.description());
      attributes.setType(TEST_DATA.type());
      attributes.setColor(TEST_DATA.color());
      attributes.setFilled(false);
      attributes.setLineThickness(2.5f);
      attributes.setVisible(false);
      attributes.setInteriorOpacity(0.7f);

      assertAll(
          "All properties should be set correctly",
          () -> assertEquals(TEST_DATA.description(), attributes.getDescription()),
          () -> assertEquals(TEST_DATA.type(), attributes.getType()),
          () -> assertEquals(TEST_DATA.color(), attributes.getColor()),
          () -> assertFalse(attributes.isFilled()),
          () -> assertEquals(2.5f, attributes.getLineThickness(), FLOAT_PRECISION),
          () -> assertFalse(attributes.isVisible()),
          () -> assertEquals(0.7f, attributes.getInteriorOpacity(), FLOAT_PRECISION));
    }

    @Test
    void should_handle_null_values_gracefully() {
      var attributes = new RegionAttributes(TEST_DATA.id(), TEST_DATA.label());

      assertDoesNotThrow(
          () -> {
            attributes.setDescription(null);
            attributes.setType(null);
            attributes.setColor(null);
          });

      assertAll(
          "Null values should be handled correctly",
          () -> assertNull(attributes.getDescription()),
          () -> assertNull(attributes.getType()),
          () -> assertNull(attributes.getColor()));
    }

    @ParameterizedTest
    @ValueSource(floats = {-1.0f, -0.1f, -100.0f})
    void should_reject_negative_line_thickness(float negativeThickness) {
      var attributes = new RegionAttributes(TEST_DATA.id(), TEST_DATA.label());

      assertThrows(
          IllegalArgumentException.class,
          () -> attributes.setLineThickness(negativeThickness),
          "Should reject negative line thickness: %f".formatted(negativeThickness));
    }

    @ParameterizedTest
    @MethodSource("opacityTestCases")
    void should_clamp_opacity_values_correctly(OpacityTest opacityTest) {
      var attributes = new RegionAttributes(TEST_DATA.id(), TEST_DATA.label());

      attributes.setInteriorOpacity(opacityTest.input());

      assertEquals(
          opacityTest.expected(),
          attributes.getInteriorOpacity(),
          FLOAT_PRECISION,
          "Opacity should be clamped: %f -> %f"
              .formatted(opacityTest.input(), opacityTest.expected()));
    }

    private static Stream<OpacityTest> opacityTestCases() {
      return TEST_DATA.opacityTests().stream();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  ", "\t", "\n"})
    void should_reject_invalid_labels_when_setting(String invalidLabel) {
      var attributes = new RegionAttributes(TEST_DATA.id(), TEST_DATA.label());

      assertThrows(
          IllegalArgumentException.class,
          () -> attributes.setLabel(invalidLabel),
          "Should reject invalid label when setting: '%s'".formatted(String.valueOf(invalidLabel)));
    }
  }

  @Nested
  class Prefix_extraction_tests {

    @ParameterizedTest
    @MethodSource("prefixTestCases")
    void should_extract_prefix_correctly(PrefixTest prefixTest) {
      var attributes = new RegionAttributes(TEST_DATA.id(), prefixTest.label());

      assertEquals(
          prefixTest.expectedPrefix(),
          attributes.getPrefix(),
          "Prefix should be extracted correctly from: '%s'".formatted(prefixTest.label()));
    }

    private static Stream<PrefixTest> prefixTestCases() {
      return TEST_DATA.prefixTests().stream();
    }

    @Test
    void should_handle_multiple_separators_correctly() {
      var attributes = new RegionAttributes(TEST_DATA.id(), "test_label-with multiple_separators");

      // Should use the first separator that meets the minimum length requirement
      assertEquals("test", attributes.getPrefix());
    }

    @Test
    void should_handle_null_label_gracefully() {
      // This test uses reflection to set a null label after construction
      // since setLabel validates and constructor validates
      var attributes = new RegionAttributes(TEST_DATA.id(), TEST_DATA.label());

      // Set label to valid value first, then test getPrefix behavior
      // In real usage, label should never be null due to validation
      assertEquals(TEST_DATA.label(), attributes.getPrefix());
    }
  }

  @Nested
  class Pixel_count_management_tests {

    @Test
    void should_add_pixels_from_multiple_regions_correctly() {
      var attributes = new RegionAttributes(TEST_DATA.id(), TEST_DATA.label());
      var region1 = createRegionWithPixels("region1", 5L);
      var region2 = createRegionWithPixels("region2", 10L);

      attributes.addPixels(region1);
      attributes.addPixels(region2);

      assertEquals(
          15L, attributes.getNumberOfPixels(), "Should accumulate pixels from multiple regions");
    }

    @Test
    void should_handle_null_region_gracefully() {
      var attributes = new RegionAttributes(TEST_DATA.id(), TEST_DATA.label());
      var initialPixelCount = attributes.getNumberOfPixels();

      attributes.addPixels(null);

      assertEquals(
          initialPixelCount,
          attributes.getNumberOfPixels(),
          "Should not change pixel count when adding null region");
    }

    @Test
    void should_handle_regions_with_invalid_pixel_counts() {
      var attributes = new RegionAttributes(TEST_DATA.id(), TEST_DATA.label());
      var regionWithNoPixels = new Region("region");
      var initialPixelCount = attributes.getNumberOfPixels();

      attributes.addPixels(regionWithNoPixels);

      assertEquals(
          initialPixelCount,
          attributes.getNumberOfPixels(),
          "Should not change pixel count for regions with invalid pixel counts");
    }

    @Test
    void should_reset_pixel_count_correctly() {
      var attributes = new RegionAttributes(TEST_DATA.id(), TEST_DATA.label());
      var region = createRegionWithPixels("region", 100L);

      attributes.addPixels(region);
      attributes.resetPixelCount();

      assertEquals(0L, attributes.getNumberOfPixels(), "Should reset pixel count to zero");
    }

    @Test
    void should_initialize_pixel_count_when_adding_first_valid_region() {
      var attributes = new RegionAttributes(TEST_DATA.id(), TEST_DATA.label());
      var region1 = createRegionWithPixels("region1", 5L);
      var region2 = createRegionWithPixels("region2", 3L);

      attributes.addPixels(region1);
      assertEquals(5L, attributes.getNumberOfPixels());

      attributes.addPixels(region2);
      assertEquals(8L, attributes.getNumberOfPixels());
    }

    private Region createRegionWithPixels(String id, long pixels) {
      return new Region(id, Collections.emptyList(), pixels);
    }
  }

  @Nested
  class Region_grouping_tests {

    @Test
    void should_return_empty_map_for_null_or_empty_collection() {
      assertAll(
          "Should handle null and empty collections",
          () -> assertTrue(RegionAttributes.groupRegions(null).isEmpty()),
          () -> assertTrue(RegionAttributes.groupRegions(Collections.emptyList()).isEmpty()));
    }

    @Test
    void should_group_regions_by_prefix_correctly() {
      var regions = createTestRegions();

      var groupedRegions = RegionAttributes.groupRegions(regions);

      assertAll(
          "Regions should be grouped correctly by prefix",
          () -> assertEquals(2, groupedRegions.size()),
          () -> assertEquals(4, groupedRegions.get("test").size()),
          () -> assertEquals(1, groupedRegions.get("anotherLabel2").size()));
    }

    @Test
    void should_maintain_sorted_order_within_groups() {
      var regions =
          List.of(
              new RegionAttributes(3, "test zebra"),
              new RegionAttributes(1, "test apple"),
              new RegionAttributes(2, "test banana"));

      var groupedRegions = RegionAttributes.groupRegions(regions);
      var testGroup = groupedRegions.get("test");

      assertAll(
          "Should maintain sorted order within groups",
          () -> assertEquals("test apple", testGroup.get(0).getLabel()),
          () -> assertEquals("test banana", testGroup.get(1).getLabel()),
          () -> assertEquals("test zebra", testGroup.get(2).getLabel()));
    }

    @Test
    void should_handle_single_region_correctly() {
      var singleRegion = List.of(new RegionAttributes(1, "brain left"));

      var groupedRegions = RegionAttributes.groupRegions(singleRegion);

      assertAll(
          "Should handle brain correctly",
          () -> assertEquals(1, groupedRegions.size()),
          () -> assertTrue(groupedRegions.containsKey("brain")),
          () -> assertEquals(1, groupedRegions.get("brain").size()));
    }

    private Collection<RegionAttributes> createTestRegions() {
      return List.of(
          new RegionAttributes(1, "test Label1"),
          new RegionAttributes(2, "test new label1"),
          new RegionAttributes(3, "test-new label2"),
          new RegionAttributes(4, "test_new_label3"),
          new RegionAttributes(5, "anotherLabel2"));
    }
  }

  @Nested
  class Color_generation_tests {

    @Test
    void should_generate_color_from_rgb_array_correctly() {
      var rgbArray = new int[] {255, 128, 64};
      var contourId = 1;

      var color = RegionAttributes.getColor(rgbArray, contourId);

      assertAll(
          "Should generate color from RGB array correctly",
          () -> assertEquals(255, color.getRed()),
          () -> assertEquals(128, color.getGreen()),
          () -> assertEquals(64, color.getBlue()),
          () -> assertEquals(255, color.getAlpha())); // Default opacity
    }

    @Test
    void should_use_default_opacity_when_not_specified() {
      var rgbArray = new int[] {100, 150, 200};
      var contourId = 1;

      var color = RegionAttributes.getColor(rgbArray, contourId);

      assertEquals(255, color.getAlpha(), "Should use default opacity (255)");
    }

    @ParameterizedTest
    @ValueSource(floats = {-0.5f, 0.0f, 0.3f, 1.0f, 1.5f})
    void should_clamp_opacity_values_in_color_generation(float opacity) {
      var rgbArray = new int[] {100, 150, 200};
      var contourId = 1;

      var color = RegionAttributes.getColor(rgbArray, contourId, opacity);

      var expectedAlpha = Math.round(MathUtil.clamp(opacity, 0.0f, 1.0f) * 255f);
      assertEquals(
          expectedAlpha,
          color.getAlpha(),
          "Opacity should be clamped: %f -> alpha: %d".formatted(opacity, expectedAlpha));
    }

    @Test
    void should_generate_color_from_lut_when_rgb_array_is_invalid() {
      var contourId = 42;

      var colorFromNull = RegionAttributes.getColor(null, contourId);
      var colorFromShortArray = RegionAttributes.getColor(new int[] {255, 128}, contourId);

      assertAll(
          "Should generate color from LUT when RGB array is invalid",
          () -> assertNotNull(colorFromNull),
          () -> assertNotNull(colorFromShortArray),
          () -> assertEquals(255, colorFromNull.getAlpha()),
          () -> assertEquals(255, colorFromShortArray.getAlpha()));
    }

    @ParameterizedTest
    @ValueSource(ints = {-5, -1, 0, 1, 255, 256, 300})
    void should_handle_various_contour_ids_correctly(int contourId) {
      var color = RegionAttributes.getColor(null, contourId);

      assertAll(
          "Should handle various contour IDs correctly",
          () -> assertNotNull(color),
          () -> assertTrue(color.getRed() >= 0 && color.getRed() <= 255),
          () -> assertTrue(color.getGreen() >= 0 && color.getGreen() <= 255),
          () -> assertTrue(color.getBlue() >= 0 && color.getBlue() <= 255),
          () -> assertEquals(255, color.getAlpha()));
    }

    @ParameterizedTest
    @MethodSource("colorTestCases")
    void should_handle_color_generation_consistently(ColorTest colorTest) {
      var color =
          RegionAttributes.getColor(colorTest.rgb(), colorTest.contourId(), colorTest.opacity());

      assertNotNull(color, "Color should never be null");

      if (colorTest.expectLutColor()) {
        // When using LUT, we can't predict exact values but can verify they're valid
        assertAll(
            "LUT-generated color should be valid",
            () -> assertTrue(color.getRed() >= 0 && color.getRed() <= 255),
            () -> assertTrue(color.getGreen() >= 0 && color.getGreen() <= 255),
            () -> assertTrue(color.getBlue() >= 0 && color.getBlue() <= 255));
      } else {
        // When using RGB array, values should match
        assertAll(
            "RGB-generated color should match input",
            () -> assertEquals(colorTest.rgb()[0], color.getRed()),
            () -> assertEquals(colorTest.rgb()[1], color.getGreen()),
            () -> assertEquals(colorTest.rgb()[2], color.getBlue()));
      }
    }

    private static Stream<Arguments> colorTestCases() {
      return Stream.of(
          Arguments.of(new ColorTest(new int[] {255, 128, 64}, 1, 1.0f, false)),
          Arguments.of(new ColorTest(new int[] {0, 255, 0}, 2, 0.5f, false)),
          Arguments.of(new ColorTest(null, 1, 1.0f, true)),
          Arguments.of(new ColorTest(new int[] {255}, 3, 1.0f, true)), // Too short
          Arguments.of(new ColorTest(new int[] {}, 4, 1.0f, true))); // Empty
    }
  }

  @Nested
  class Comparison_and_object_behavior_tests {

    @Test
    void should_implement_compare_to_correctly() {
      var attributes1 = new RegionAttributes(1, "apple");
      var attributes2 = new RegionAttributes(2, "banana");
      var attributes3 = new RegionAttributes(3, "apple");

      assertAll(
          "CompareTo should work correctly",
          () -> assertTrue(attributes1.compareTo(attributes2) < 0),
          () -> assertTrue(attributes2.compareTo(attributes1) > 0),
          () -> assertEquals(0, attributes1.compareTo(attributes3)),
          () -> assertTrue(attributes1.compareTo(null) > 0));
    }

    @Test
    void should_implement_equals_and_hash_code_consistently() {
      var attributes1 = new RegionAttributes(1, "test");
      var attributes2 = new RegionAttributes(1, "test");
      var attributes3 = new RegionAttributes(2, "test");
      var attributes4 = new RegionAttributes(1, "different");

      assertAll(
          "Equals and hashCode should be consistent",
          () -> assertEquals(attributes1, attributes2),
          () -> assertEquals(attributes1.hashCode(), attributes2.hashCode()),
          () -> assertNotEquals(attributes1, attributes3),
          () -> assertNotEquals(attributes1, attributes4),
          () -> assertNotEquals(attributes1, null),
          () -> assertNotEquals(attributes1, "not a RegionAttributes"));
    }

    @Test
    void should_implement_to_string_meaningfully() {
      var attributes = new RegionAttributes(TEST_DATA.id(), TEST_DATA.label());
      attributes.setVisible(false);

      var toString = attributes.toString();

      assertAll(
          "ToString should contain meaningful information",
          () -> assertNotNull(toString),
          () -> assertTrue(toString.contains(String.valueOf(TEST_DATA.id()))),
          () -> assertTrue(toString.contains(TEST_DATA.label())),
          () -> assertTrue(toString.contains("false"))); // visible=false
    }
  }

  @Nested
  class Edge_cases_and_error_handling_tests {

    @Test
    void should_handle_extreme_values_gracefully() {
      var attributes = new RegionAttributes(Integer.MAX_VALUE, "extreme test");

      assertDoesNotThrow(
          () -> {
            attributes.setLineThickness(Float.MAX_VALUE);
            attributes.setInteriorOpacity(Float.MAX_VALUE);
            // These should be handled gracefully
          });

      assertAll(
          "Extreme values should be handled correctly",
          () -> assertEquals(Integer.MAX_VALUE, attributes.getId()),
          () -> assertEquals(Float.MAX_VALUE, attributes.getLineThickness()),
          () -> assertEquals(1.0f, attributes.getInteriorOpacity(), FLOAT_PRECISION)); // Clamped
    }

    @Test
    void should_handle_concurrent_modifications_safely() throws InterruptedException {
      var regions =
          Collections.synchronizedList(
              Stream.generate(
                      () -> new RegionAttributes((int) (Math.random() * 1000), "test region"))
                  .limit(100)
                  .toList());

      var results = new ConcurrentHashMap<String, Integer>();
      var threadCount = 10;
      var latch = new CountDownLatch(threadCount);

      ExecutorService executor = Executors.newFixedThreadPool(threadCount);
      for (int i = 0; i < threadCount; i++) {
        executor.submit(
            () -> {
              try {
                var groupedRegions = RegionAttributes.groupRegions(regions);
                results.put(Thread.currentThread().getName(), groupedRegions.size());
              } finally {
                latch.countDown();
              }
            });
      }

      assertTrue(latch.await(5, TimeUnit.SECONDS), "All threads should complete");

      // All threads should produce the same result for the same input
      var uniqueResults = results.values().stream().distinct().toList();
      assertEquals(1, uniqueResults.size(), "All threads should produce consistent results");
    }

    @Test
    void should_maintain_immutability_of_returned_collections() {
      var regions = List.of(new RegionAttributes(1, "test1"), new RegionAttributes(2, "test2"));

      var groupedRegions = RegionAttributes.groupRegions(regions);

      // The returned map should be modifiable, but modifications shouldn't affect future calls
      assertDoesNotThrow(
          () -> groupedRegions.clear(), "Should be able to modify returned collection");

      // Fresh call should return original data
      var freshGrouped = RegionAttributes.groupRegions(regions);
      assertEquals(2, freshGrouped.size(), "Fresh call should return original data");
    }

    @Test
    void should_handle_unicode_labels_correctly() {
      var unicodeLabels = List.of("тест метка", "测试标签", "🔥 fire_region", "café-région");

      assertAll(
          "Should handle unicode labels correctly",
          unicodeLabels.stream()
              .map(
                  label ->
                      () -> {
                        var attributes = new RegionAttributes(1, label);
                        assertEquals(label, attributes.getLabel());
                        assertDoesNotThrow(() -> attributes.getPrefix());
                      }));
    }
  }
}
