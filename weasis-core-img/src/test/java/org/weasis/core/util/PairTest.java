/*
 * Copyright (c) 2024 Weasis Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.weasis.core.util;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(ReplaceUnderscores.class)
class PairTest {

  @Nested
  class Basic_Construction_And_Access {

    @Test
    void should_store_and_retrieve_values_correctly() {
      var pair = new Pair<>("Alice", 25);
      assertEquals("Alice", pair.first());
      assertEquals(25, pair.second());
    }

    @Test
    void should_handle_different_types() {
      var pair = new Pair<>(LocalDate.of(2024, 1, 1), "New Year");
      assertEquals(LocalDate.of(2024, 1, 1), pair.first());
      assertEquals("New Year", pair.second());
    }

    @Test
    void should_handle_mixed_null_and_non_null_values() {
      var pair = new Pair<String, Integer>("test", null);
      assertEquals("test", pair.first());
      assertNull(pair.second());

      pair = new Pair<>(null, 42);
      assertNull(pair.first());
      assertEquals(42, pair.second());
    }

    @Test
    void should_handle_all_null_values() {
      var pair = new Pair<String, Integer>(null, null);
      assertNull(pair.first());
      assertNull(pair.second());
    }
  }

  @Nested
  class Factory_Methods {

    @Test
    void should_create_pair_using_of_factory_method() {
      var pair = Pair.of("username", "password");
      assertEquals("username", pair.first());
      assertEquals("password", pair.second());
    }

    @Test
    void should_create_empty_pair() {
      Pair<String, Integer> pair = Pair.empty();
      assertNull(pair.first());
      assertNull(pair.second());
    }

    @Test
    void should_create_pair_with_same_values() {
      var pair = Pair.same("duplicate");
      assertEquals("duplicate", pair.first());
      assertEquals("duplicate", pair.second());
    }

    @Test
    void should_create_pair_with_null_using_same_factory() {
      Pair<String, String> pair = Pair.same(null);
      assertNull(pair.first());
      assertNull(pair.second());
    }
  }

  @Nested
  class Equality_And_HashCode {

    @Test
    void should_be_equal_when_all_elements_are_equal() {
      var pair1 = new Pair<>("key", 123);
      var pair2 = new Pair<>("key", 123);
      assertEquals(pair1, pair2);
      assertEquals(pair1.hashCode(), pair2.hashCode());
    }

    @Test
    void should_not_be_equal_when_elements_differ() {
      var pair1 = new Pair<>("key1", 123);
      var pair2 = new Pair<>("key2", 123);
      assertNotEquals(pair1, pair2);

      pair2 = new Pair<>("key1", 456);
      assertNotEquals(pair1, pair2);
    }

    @Test
    void should_handle_null_equality_correctly() {
      var pair1 = new Pair<String, Integer>(null, null);
      var pair2 = new Pair<String, Integer>(null, null);
      assertEquals(pair1, pair2);

      pair1 = new Pair<>("test", null);
      pair2 = new Pair<>("test", null);
      assertEquals(pair1, pair2);
    }
  }

  @Nested
  class Swapping_Operations {

    @Test
    void should_swap_values() {
      var original = new Pair<>("name", 42);
      var swapped = original.swap();

      assertEquals(42, swapped.first());
      assertEquals("name", swapped.second());
    }

    @Test
    void should_handle_null_values_in_swapping() {
      var original = new Pair<String, Integer>("value", null);
      var swapped = original.swap();

      assertNull(swapped.first());
      assertEquals("value", swapped.second());
    }
  }

  @Nested
  class Immutable_Updates {

    @Test
    void should_create_new_pair_with_different_first_element() {
      var original = new Pair<>("original", 100);
      var updated = original.withFirst("updated");

      assertEquals("updated", updated.first());
      assertEquals(100, updated.second());
      assertEquals("original", original.first()); // Verify immutability
    }

    @Test
    void should_create_new_pair_with_different_second_element() {
      var original = new Pair<>("key", 100);
      var updated = original.withSecond(200);

      assertEquals("key", updated.first());
      assertEquals(200, updated.second());
      assertEquals(100, original.second()); // Verify immutability
    }

    @Test
    void should_handle_null_values_in_with_methods() {
      var original = new Pair<>("key", 100);
      var updated = original.withFirst(null);

      assertNull(updated.first());
      assertEquals(100, updated.second());
    }
  }

  @Nested
  class Transformation_Methods {

    @Test
    void should_transform_first_element() {
      var original = new Pair<>("hello", 5);
      var transformed = original.mapFirst(String::toUpperCase);

      assertEquals("HELLO", transformed.first());
      assertEquals(5, transformed.second());
    }

    @Test
    void should_transform_second_element() {
      var original = new Pair<>("count", 42);
      var transformed = original.mapSecond(n -> n * 2);

      assertEquals("count", transformed.first());
      assertEquals(84, transformed.second());
    }

    @Test
    void should_transform_both_elements() {
      var original = new Pair<>("test", 10);
      var transformed = original.mapBoth(String::length, String::valueOf);

      assertEquals(4, transformed.first()); // "test".length()
      assertEquals("10", transformed.second());
    }

    @Test
    void should_handle_null_values_in_transformations() {
      var original = new Pair<String, Integer>(null, 42);
      var transformed = original.mapFirst(value -> value == null ? "NULL" : value);

      assertEquals("NULL", transformed.first());
      assertEquals(42, transformed.second());
    }

    @Test
    void should_throw_exception_for_null_mappers() {
      var pair = new Pair<>("test", 100);

      assertAll(
          () -> assertThrows(NullPointerException.class, () -> pair.mapFirst(null)),
          () -> assertThrows(NullPointerException.class, () -> pair.mapSecond(null)),
          () -> assertThrows(NullPointerException.class, () -> pair.mapBoth(null, String::valueOf)),
          () ->
              assertThrows(
                  NullPointerException.class, () -> pair.mapBoth(String::toUpperCase, null)));
    }
  }

  @Nested
  class Null_Checking_Methods {

    @Test
    void should_identify_when_any_element_is_null() {
      assertAll(
          () -> assertTrue(new Pair<String, Integer>(null, 42).hasNull()),
          () -> assertTrue(new Pair<>("test", null).hasNull()),
          () -> assertTrue(new Pair<String, Integer>(null, null).hasNull()),
          () -> assertFalse(new Pair<>("test", 42).hasNull()));
    }

    @Test
    void should_identify_when_all_elements_are_null() {
      assertAll(
          () -> assertTrue(new Pair<String, Integer>(null, null).isAllNull()),
          () -> assertFalse(new Pair<String, Integer>(null, 42).isAllNull()),
          () -> assertFalse(new Pair<>("test", null).isAllNull()),
          () -> assertFalse(new Pair<>("test", 42).isAllNull()));
    }

    @Test
    void should_identify_when_all_elements_are_non_null() {
      assertAll(
          () -> assertTrue(new Pair<>("test", 42).isAllNonNull()),
          () -> assertFalse(new Pair<String, Integer>(null, 42).isAllNonNull()),
          () -> assertFalse(new Pair<>("test", null).isAllNonNull()),
          () -> assertFalse(new Pair<String, Integer>(null, null).isAllNonNull()));
    }
  }

  @Nested
  class Array_Conversion {

    @Test
    void should_convert_pair_to_array() {
      var pair = new Pair<>("data", 123);
      Object[] array = pair.toArray();

      assertEquals(2, array.length);
      assertEquals("data", array[0]);
      assertEquals(123, array[1]);
    }

    @Test
    void should_handle_null_values_in_array_conversion() {
      var pair = new Pair<String, Integer>(null, 456);
      Object[] array = pair.toArray();

      assertEquals(2, array.length);
      assertNull(array[0]);
      assertEquals(456, array[1]);
    }
  }

  @Nested
  class String_Representation {

    @Test
    void should_provide_correct_string_representation() {
      var pair = new Pair<>("key", 789);
      assertEquals("(key, 789)", pair.toString());
    }

    @Test
    void should_handle_null_values_in_string_representation() {
      var pair = new Pair<String, Integer>(null, 123);
      assertEquals("(null, 123)", pair.toString());
    }

    @Test
    void should_handle_all_null_values_in_string_representation() {
      var pair = new Pair<String, Integer>(null, null);
      assertEquals("(null, null)", pair.toString());
    }
  }

  @Nested
  class Complex_Scenarios {

    @Test
    void should_handle_method_chaining() {
      var result =
          new Pair<>("initial", 10)
              .withFirst("changed")
              .mapSecond(n -> n * 2)
              .swap()
              .withSecond("final");

      assertEquals(20, result.first());
      assertEquals("final", result.second());
    }

    @Test
    void should_handle_complex_transformations_with_realistic_data() {
      var userProfile = new Pair<>("john_doe", List.of("admin", "user"));
      var transformed =
          userProfile.mapBoth(username -> username.replace("_", " ").toUpperCase(), List::size);
      var final_result = transformed.swap();

      assertEquals(2, final_result.first()); // Number of roles
      assertEquals("JOHN DOE", final_result.second()); // Formatted username
    }

    @Test
    void should_maintain_immutability_contract() {
      var original = new Pair<>("immutable", 999);

      // All operations should return new instances
      assertAll(
          () -> assertNotSame(original, original.withFirst("different")),
          () -> assertNotSame(original, original.withSecond(111)),
          () -> assertNotSame(original, original.mapFirst(String::toUpperCase)),
          () -> assertNotSame(original, original.mapSecond(n -> n + 1)),
          () -> assertNotSame(original, original.swap()));

      // Original should remain unchanged
      assertEquals("immutable", original.first());
      assertEquals(999, original.second());
    }

    @Test
    void should_work_with_complex_data_types() {
      var dateRange = new Pair<>(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31));

      var daysBetween = dateRange.mapBoth(LocalDate::getDayOfYear, LocalDate::getDayOfYear);

      assertTrue(daysBetween.first() < daysBetween.second());
      assertEquals(1, daysBetween.first());
      assertEquals(366, daysBetween.second()); // 2024 is a leap year
    }
  }
}
