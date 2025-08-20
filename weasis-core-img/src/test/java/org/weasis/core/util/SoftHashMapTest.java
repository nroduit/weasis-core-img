/*
 * Copyright (c) 2023 Weasis Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.weasis.core.util;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayNameGeneration(ReplaceUnderscores.class)
class SoftHashMapTest {

  private static final List<String> TEST_KEYS = List.of("key1", "key2", "key3", "key4", "key5");
  private static final List<String> TEST_VALUES =
      List.of("value1", "value2", "value3", "value4", "value5");
  private static final Map<String, String> SAMPLE_DATA =
      Map.of(
          "name", "John",
          "surname", "Doe",
          "city", "Paris",
          "country", "France");

  private SoftHashMap<String, String> softHashMap;

  @BeforeEach
  void setUp() {
    softHashMap = new SoftHashMap<>();
  }

  @Nested
  class Basic_Operations {

    @Test
    void should_return_null_for_non_existent_key() {
      assertNull(softHashMap.get("non-existent"));
    }

    @Test
    void should_store_and_retrieve_values_correctly() {
      var testData =
          Map.of(
              "spring", "framework",
              "java", "language",
              "test", "unit");

      softHashMap.putAll(testData);

      assertAll(
          () -> assertEquals("framework", softHashMap.get("spring")),
          () -> assertEquals("language", softHashMap.get("java")),
          () -> assertEquals("unit", softHashMap.get("test")));
    }

    @Test
    void should_reject_null_keys() {
      assertThrows(NullPointerException.class, () -> softHashMap.put(null, "value"));
    }

    @Test
    void should_handle_null_key_operations_safely() {
      assertAll(
          () -> assertNull(softHashMap.get(null)),
          () -> assertFalse(softHashMap.containsKey(null)),
          () -> assertNull(softHashMap.remove(null)));
    }

    @Test
    void should_remove_entry_when_putting_null_value() {
      softHashMap.put("key", "value");
      var result = softHashMap.put("key", null);

      assertAll(
          () -> assertEquals("value", result),
          () -> assertNull(softHashMap.get("key")),
          () -> assertEquals(0, softHashMap.size()));
    }

    @Test
    void should_return_previous_value_when_overwriting() {
      var oldValue = softHashMap.put("key", "oldValue");
      var overwrittenValue = softHashMap.put("key", "newValue");

      assertAll(
          () -> assertNull(oldValue),
          () -> assertEquals("oldValue", overwrittenValue),
          () -> assertEquals("newValue", softHashMap.get("key")),
          () -> assertEquals(1, softHashMap.size()));
    }

    @ParameterizedTest
    @MethodSource("org.weasis.core.util.SoftHashMapTest#sampleDataProvider")
    void should_handle_multiple_key_value_pairs(String key, String value) {
      softHashMap.put(key, value);

      assertAll(
          () -> assertTrue(softHashMap.containsKey(key)),
          () -> assertTrue(softHashMap.containsValue(value)),
          () -> assertEquals(value, softHashMap.get(key)));
    }
  }

  @Nested
  class Size_And_Empty_Operations {

    @Test
    void should_be_empty_when_newly_created() {
      assertAll(() -> assertTrue(softHashMap.isEmpty()), () -> assertEquals(0, softHashMap.size()));
    }

    @Test
    void should_track_size_correctly() {
      IntStream.range(0, TEST_KEYS.size())
          .forEach(i -> softHashMap.put(TEST_KEYS.get(i), TEST_VALUES.get(i)));

      assertAll(
          () -> assertFalse(softHashMap.isEmpty()),
          () -> assertEquals(TEST_KEYS.size(), softHashMap.size()));
    }

    @Test
    void should_update_size_when_removing_entries() {
      softHashMap.putAll(SAMPLE_DATA);
      var initialSize = softHashMap.size();

      softHashMap.remove("name");
      softHashMap.remove("city");

      assertEquals(initialSize - 2, softHashMap.size());
    }
  }

  @Nested
  class Clear_Operations {

    @Test
    void should_clear_all_entries() {
      softHashMap.putAll(SAMPLE_DATA);
      softHashMap.clear();

      assertAll(
          () -> assertEquals(0, softHashMap.size()),
          () -> assertTrue(softHashMap.isEmpty()),
          () -> assertNull(softHashMap.get("name")));
    }

    @Test
    void should_handle_clear_on_empty_map() {
      softHashMap.clear();

      assertAll(() -> assertEquals(0, softHashMap.size()), () -> assertTrue(softHashMap.isEmpty()));
    }

    @Test
    void should_maintain_consistency_after_gc_simulation() throws InterruptedException {
      softHashMap.putAll(SAMPLE_DATA);
      var initialSize = softHashMap.size();

      // Simulate garbage collection pressure
      IntStream.range(0, 10)
          .forEach(
              i -> {
                System.gc();
                System.runFinalization();
                softHashMap.size(); // Trigger expunge
              });

      TimeUnit.MILLISECONDS.sleep(50);

      assertAll(
          () -> assertTrue(softHashMap.size() <= initialSize),
          () -> assertEquals(softHashMap.size(), softHashMap.size()),
          () -> assertEquals(softHashMap.isEmpty(), softHashMap.isEmpty()));
    }

    @Test
    void should_expunge_stale_entries_using_reflection() throws Exception {
      softHashMap.put("key1", "value1");
      softHashMap.put("key2", "value2");

      var primaryMapField = SoftHashMap.class.getDeclaredField("primaryMap");
      primaryMapField.setAccessible(true);

      @SuppressWarnings("unchecked")
      var primaryMap = (Map<String, SoftReference<String>>) primaryMapField.get(softHashMap);
      var softRef = primaryMap.get("key1");

      // Simulate GC clearing the reference
      softRef.clear();
      softRef.enqueue();

      softHashMap.size(); // Trigger expunge

      assertAll(
          () -> assertEquals(1, softHashMap.size()),
          () -> assertNull(softHashMap.get("key1")),
          () -> assertEquals("value2", softHashMap.get("key2")));
    }
  }

  @Nested
  class Contains_Operations {

    @Test
    void should_handle_contains_key_operations() {
      softHashMap.put("existing", "value");

      assertAll(
          () -> assertTrue(softHashMap.containsKey("existing")),
          () -> assertFalse(softHashMap.containsKey("non-existing")),
          () -> assertFalse(softHashMap.containsKey(null)));
    }

    @Test
    void should_handle_contains_value_operations() {
      softHashMap.put("key1", "shared-value");
      softHashMap.put("key2", "shared-value");
      softHashMap.put("key3", "unique-value");

      assertAll(
          () -> assertTrue(softHashMap.containsValue("shared-value")),
          () -> assertTrue(softHashMap.containsValue("unique-value")),
          () -> assertFalse(softHashMap.containsValue("non-existing")),
          () -> assertFalse(softHashMap.containsValue(null)));
    }

    @Test
    void should_maintain_contains_value_after_partial_removal() {
      var duplicateValue = "duplicate";
      softHashMap.put("key1", duplicateValue);
      softHashMap.put("key2", duplicateValue);
      softHashMap.put("key3", "unique");

      softHashMap.remove("key1");

      assertAll(
          () -> assertTrue(softHashMap.containsValue(duplicateValue)),
          () -> assertTrue(softHashMap.containsValue("unique")));

      softHashMap.remove("key2");

      assertAll(
          () -> assertFalse(softHashMap.containsValue(duplicateValue)),
          () -> assertTrue(softHashMap.containsValue("unique")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "test-value", "special@chars#123"})
    void should_handle_various_value_types_in_contains(String value) {
      var key = "test-key";
      softHashMap.put(key, value);

      assertAll(
          () -> assertTrue(softHashMap.containsValue(value)),
          () -> assertEquals(value, softHashMap.get(key)));
    }
  }

  @Nested
  class Entry_Set_Operations {

    @Test
    void should_return_empty_entry_set_for_empty_map() {
      var entrySet = softHashMap.entrySet();

      assertAll(() -> assertTrue(entrySet.isEmpty()), () -> assertEquals(0, entrySet.size()));
    }

    @Test
    void should_return_correct_entry_set() {
      SAMPLE_DATA.forEach(softHashMap::put);
      var entrySet = softHashMap.entrySet();

      assertAll(
          () -> assertEquals(SAMPLE_DATA.size(), entrySet.size()),
          () ->
              assertTrue(
                  entrySet.stream()
                      .allMatch(
                          entry -> SAMPLE_DATA.get(entry.getKey()).equals(entry.getValue()))));
    }

    @Test
    void should_allow_value_modification_through_entry() {
      softHashMap.put("modifiable", "original");
      var entry = softHashMap.entrySet().iterator().next();

      var oldValue = entry.setValue("modified");

      assertAll(
          () -> assertEquals("original", oldValue),
          () -> assertEquals("modified", softHashMap.get("modifiable")));
    }

    @Test
    void should_create_independent_entry_set_snapshots() {
      SAMPLE_DATA.forEach(softHashMap::put);

      var entrySet1 = softHashMap.entrySet();
      var entrySet2 = softHashMap.entrySet();

      softHashMap.put("new-key", "new-value");

      // Entry sets are snapshots and shouldn't reflect new additions
      assertAll(
          () -> assertEquals(SAMPLE_DATA.size(), entrySet1.size()),
          () -> assertEquals(SAMPLE_DATA.size(), entrySet2.size()),
          () -> assertEquals(SAMPLE_DATA.size() + 1, softHashMap.size()));
    }
  }

  @Nested
  class Equals_And_Hash_Code {

    @Test
    void should_be_equal_to_itself() {
      softHashMap.putAll(SAMPLE_DATA);
      assertEquals(softHashMap, softHashMap);
    }

    @Test
    void should_be_equal_to_map_with_same_content() {
      softHashMap.putAll(SAMPLE_DATA);

      var otherMap = new SoftHashMap<String, String>();
      otherMap.putAll(SAMPLE_DATA);

      assertAll(
          () -> assertEquals(softHashMap, otherMap),
          () -> assertEquals(softHashMap.hashCode(), otherMap.hashCode()));
    }

    @Test
    void should_not_be_equal_to_different_types() {
      softHashMap.putAll(SAMPLE_DATA);

      var regularMap = new HashMap<String, String>();
      regularMap.putAll(SAMPLE_DATA);

      assertAll(
          () -> assertNotEquals(softHashMap, regularMap),
          () -> assertNotEquals(softHashMap, null),
          () -> assertNotEquals(softHashMap, "not-a-map"));
    }

    @ParameterizedTest
    @MethodSource("org.weasis.core.util.SoftHashMapTest#inequalMapsProvider")
    void should_not_be_equal_to_different_content_maps(Map<String, String> otherData) {
      SAMPLE_DATA.forEach(softHashMap::put);

      var otherMap = new SoftHashMap<String, String>();
      otherMap.putAll(otherData);

      assertNotEquals(softHashMap, otherMap);
    }

    @Test
    void should_maintain_equals_contract_through_modifications() {
      var key = "changeable";
      softHashMap.put(key, "value1");

      var otherMap = new SoftHashMap<String, String>();
      otherMap.put(key, "value1");

      assertEquals(softHashMap, otherMap);

      // Modify both in same way
      softHashMap.put(key, "value2");
      otherMap.put(key, "value2");

      assertEquals(softHashMap, otherMap);
    }

    @Test
    void should_have_consistent_hash_code() {
      var emptyMap1 = new SoftHashMap<String, String>();
      var emptyMap2 = new SoftHashMap<String, String>();

      assertEquals(emptyMap1.hashCode(), emptyMap2.hashCode());

      emptyMap1.putAll(SAMPLE_DATA);
      emptyMap2.putAll(SAMPLE_DATA);

      assertEquals(emptyMap1.hashCode(), emptyMap2.hashCode());
    }
  }

  @Nested
  class Edge_Cases {

    @Test
    void should_handle_rapid_put_remove_cycles() {
      var key = "cycling-key";

      IntStream.range(0, 100)
          .forEach(
              i -> {
                var value = "value-" + i;
                softHashMap.put(key, value);
                assertEquals(value, softHashMap.get(key));
                assertEquals(value, softHashMap.remove(key));
                assertNull(softHashMap.get(key));
              });

      assertTrue(softHashMap.isEmpty());
    }

    @Test
    void should_handle_operations_after_clear() {
      softHashMap.putAll(SAMPLE_DATA);
      softHashMap.clear();

      // Verify all operations work after clear
      assertAll(
          () -> assertNull(softHashMap.get("name")),
          () -> assertFalse(softHashMap.containsKey("name")),
          () -> assertFalse(softHashMap.containsValue("John")),
          () -> assertTrue(softHashMap.isEmpty()),
          () -> assertEquals(0, softHashMap.size()),
          () -> assertTrue(softHashMap.entrySet().isEmpty()));

      // Should accept new entries after clear
      softHashMap.put("new-key", "new-value");
      assertEquals("new-value", softHashMap.get("new-key"));
    }

    @Test
    void should_maintain_internal_consistency() {
      softHashMap.putAll(SAMPLE_DATA);

      // Trigger multiple expunge operations
      IntStream.range(0, 20)
          .forEach(
              i -> {
                softHashMap.size();
                softHashMap.isEmpty();
                softHashMap.containsValue("John");
                softHashMap.entrySet();
              });

      // Verify consistency between different views
      assertAll(
          () -> assertEquals(softHashMap.size(), softHashMap.entrySet().size()),
          () -> assertEquals(softHashMap.size(), softHashMap.keySet().size()),
          () -> assertEquals(softHashMap.isEmpty(), softHashMap.size() == 0));

      // Verify remaining entries are valid
      softHashMap
          .keySet()
          .forEach(
              key -> {
                var value = softHashMap.get(key);
                assertAll(
                    () -> assertNotNull(value, "Value should not be null for key: " + key),
                    () -> assertTrue(softHashMap.containsKey(key)),
                    () -> assertTrue(softHashMap.containsValue(value)));
              });
    }

    @Test
    void should_handle_large_number_of_entries() {
      var largeDataSet =
          IntStream.range(0, 1000)
              .boxed()
              .collect(
                  HashMap<String, String>::new,
                  (map, i) -> map.put("key-" + i, "value-" + i),
                  HashMap::putAll);

      largeDataSet.forEach(softHashMap::put);

      assertAll(
          () -> assertEquals(1000, softHashMap.size()),
          () -> assertTrue(softHashMap.containsKey("key-500")),
          () -> assertTrue(softHashMap.containsValue("value-750")),
          () -> assertEquals("value-999", softHashMap.get("key-999")));
    }
  }

  // Test data providers
  static Stream<Arguments> sampleDataProvider() {
    return SAMPLE_DATA.entrySet().stream()
        .map(entry -> Arguments.of(entry.getKey(), entry.getValue()));
  }

  static Stream<Map<String, String>> inequalMapsProvider() {
    return Stream.of(
        Map.of(
            "name", "Jane", "surname", "Doe", "city", "Paris", "country",
            "France"), // Different value
        Map.of("name", "John", "surname", "Doe", "city", "Paris"), // Missing entry
        Map.of(
            "name", "John", "surname", "Doe", "city", "Paris", "country", "France", "age",
            "30"), // Extra entry
        Map.of(), // Empty map
        Map.of("different", "content", "entirely", "different"));
  }
}
