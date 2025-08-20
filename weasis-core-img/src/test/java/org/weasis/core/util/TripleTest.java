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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayNameGeneration(ReplaceUnderscores.class)
class TripleTest {

  // Test data constants - using real data structures
  private static final String SAMPLE_STRING = "Hello World";
  private static final Integer SAMPLE_INTEGER = 42;
  private static final Double SAMPLE_DOUBLE = 3.14159;
  private static final Boolean SAMPLE_BOOLEAN = true;
  private static final LocalDate SAMPLE_DATE = LocalDate.of(2024, 1, 15);
  private static final List<String> SAMPLE_LIST = List.of("apple", "banana", "cherry");
  private static final Map<String, Integer> SAMPLE_MAP = Map.of("key1", 100, "key2", 200);
  private static final Set<Integer> SAMPLE_SET = Set.of(1, 2, 3, 4, 5);
  private static final BigDecimal SAMPLE_BIG_DECIMAL = new BigDecimal("123.456");

  @Nested
  class Basic_construction_and_access {

    @Test
    void should_store_and_retrieve_all_three_values_correctly() {
      var triple = new Triple<>(SAMPLE_STRING, SAMPLE_INTEGER, SAMPLE_DOUBLE);

      assertEquals(SAMPLE_STRING, triple.first());
      assertEquals(SAMPLE_INTEGER, triple.second());
      assertEquals(SAMPLE_DOUBLE, triple.third());
    }

    @Test
    void should_handle_different_types_correctly() {
      var triple = new Triple<>(SAMPLE_DATE, SAMPLE_LIST, SAMPLE_BOOLEAN);

      assertEquals(SAMPLE_DATE, triple.first());
      assertEquals(SAMPLE_LIST, triple.second());
      assertEquals(SAMPLE_BOOLEAN, triple.third());
    }

    @Test
    void should_handle_mixed_null_and_non_null_values() {
      var partialNull1 = new Triple<>(SAMPLE_STRING, null, SAMPLE_DOUBLE);
      assertEquals(SAMPLE_STRING, partialNull1.first());
      assertNull(partialNull1.second());
      assertEquals(SAMPLE_DOUBLE, partialNull1.third());

      var partialNull2 = new Triple<>(null, SAMPLE_INTEGER, null);
      assertNull(partialNull2.first());
      assertEquals(SAMPLE_INTEGER, partialNull2.second());
      assertNull(partialNull2.third());
    }

    @Test
    void should_handle_all_null_values() {
      var allNull = new Triple<String, Integer, Double>(null, null, null);

      assertNull(allNull.first());
      assertNull(allNull.second());
      assertNull(allNull.third());
    }

    @Test
    void should_handle_complex_data_structures() {
      var complexTriple = new Triple<>(SAMPLE_MAP, SAMPLE_SET, SAMPLE_BIG_DECIMAL);

      assertEquals(SAMPLE_MAP, complexTriple.first());
      assertEquals(SAMPLE_SET, complexTriple.second());
      assertEquals(SAMPLE_BIG_DECIMAL, complexTriple.third());
    }
  }

  @Nested
  class Factory_methods {

    @Test
    void should_create_triple_using_of_factory_method() {
      var triple = Triple.of(SAMPLE_STRING, SAMPLE_INTEGER, SAMPLE_BOOLEAN);

      assertEquals(SAMPLE_STRING, triple.first());
      assertEquals(SAMPLE_INTEGER, triple.second());
      assertEquals(SAMPLE_BOOLEAN, triple.third());
    }

    @Test
    void should_create_empty_triple_with_all_null_values() {
      var empty = Triple.<String, Integer, Boolean>empty();

      assertNull(empty.first());
      assertNull(empty.second());
      assertNull(empty.third());
    }

    @Test
    void should_create_triple_with_null_values_using_of() {
      var nullTriple = Triple.<String, Integer, Boolean>of(null, null, null);

      assertNull(nullTriple.first());
      assertNull(nullTriple.second());
      assertNull(nullTriple.third());
    }

    @Test
    void should_create_triple_with_collections() {
      var collectionsTriple = Triple.of(SAMPLE_LIST, SAMPLE_MAP, SAMPLE_SET);

      assertEquals(SAMPLE_LIST, collectionsTriple.first());
      assertEquals(SAMPLE_MAP, collectionsTriple.second());
      assertEquals(SAMPLE_SET, collectionsTriple.third());
    }
  }

  @Nested
  class Equality_and_hash_code {

    @Test
    void should_be_equal_when_all_elements_are_equal() {
      var triple1 = new Triple<>(SAMPLE_STRING, SAMPLE_INTEGER, SAMPLE_DOUBLE);
      var triple2 = new Triple<>(SAMPLE_STRING, SAMPLE_INTEGER, SAMPLE_DOUBLE);

      assertEquals(triple1, triple2);
      assertEquals(triple1.hashCode(), triple2.hashCode());
    }

    @Test
    void should_not_be_equal_when_elements_differ() {
      var original = new Triple<>(SAMPLE_STRING, SAMPLE_INTEGER, SAMPLE_DOUBLE);

      var differentFirst = new Triple<>("Different", SAMPLE_INTEGER, SAMPLE_DOUBLE);
      var differentSecond = new Triple<>(SAMPLE_STRING, 999, SAMPLE_DOUBLE);
      var differentThird = new Triple<>(SAMPLE_STRING, SAMPLE_INTEGER, 9.99);

      assertNotEquals(original, differentFirst);
      assertNotEquals(original, differentSecond);
      assertNotEquals(original, differentThird);
    }

    @Test
    void should_handle_null_equality_correctly() {
      var triple1 = new Triple<>(null, SAMPLE_INTEGER, null);
      var triple2 = new Triple<>(null, SAMPLE_INTEGER, null);

      assertEquals(triple1, triple2);
      assertEquals(triple1.hashCode(), triple2.hashCode());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"not a triple", "42"})
    void should_not_be_equal_to_null_or_other_types(Object other) {
      var triple = new Triple<>(SAMPLE_STRING, SAMPLE_INTEGER, SAMPLE_DOUBLE);

      assertNotEquals(triple, other);
    }

    @Test
    void should_handle_complex_objects_equality() {
      var complex1 = new Triple<>(SAMPLE_LIST, SAMPLE_MAP, LocalDateTime.now());
      var complex2 = new Triple<>(SAMPLE_LIST, SAMPLE_MAP, complex1.third());

      assertEquals(complex1, complex2);
    }
  }

  @Nested
  class Immutable_updates {

    @Test
    void should_create_new_triple_with_different_first_element() {
      var original = Triple.of(SAMPLE_STRING, SAMPLE_INTEGER, SAMPLE_DOUBLE);
      var updated = original.withFirst(SAMPLE_DATE);

      assertEquals(SAMPLE_DATE, updated.first());
      assertEquals(SAMPLE_INTEGER, updated.second());
      assertEquals(SAMPLE_DOUBLE, updated.third());

      // Original unchanged
      assertEquals(SAMPLE_STRING, original.first());
    }

    @Test
    void should_create_new_triple_with_different_second_element() {
      var original = Triple.of(SAMPLE_STRING, SAMPLE_INTEGER, SAMPLE_DOUBLE);
      var updated = original.withSecond(SAMPLE_LIST);

      assertEquals(SAMPLE_STRING, updated.first());
      assertEquals(SAMPLE_LIST, updated.second());
      assertEquals(SAMPLE_DOUBLE, updated.third());

      // Original unchanged
      assertEquals(SAMPLE_INTEGER, original.second());
    }

    @Test
    void should_create_new_triple_with_different_third_element() {
      var original = Triple.of(SAMPLE_STRING, SAMPLE_INTEGER, SAMPLE_DOUBLE);
      var updated = original.withThird(SAMPLE_BOOLEAN);

      assertEquals(SAMPLE_STRING, updated.first());
      assertEquals(SAMPLE_INTEGER, updated.second());
      assertEquals(SAMPLE_BOOLEAN, updated.third());

      // Original unchanged
      assertEquals(SAMPLE_DOUBLE, original.third());
    }

    @Test
    void should_handle_null_values_in_with_methods() {
      var original = Triple.of(SAMPLE_STRING, SAMPLE_INTEGER, SAMPLE_DOUBLE);

      var withNullFirst = original.withFirst(null);
      var withNullSecond = original.withSecond(null);
      var withNullThird = original.withThird(null);

      assertNull(withNullFirst.first());
      assertNull(withNullSecond.second());
      assertNull(withNullThird.third());
    }

    @Test
    void should_maintain_immutability_with_complex_objects() {
      var original = Triple.of(SAMPLE_LIST, SAMPLE_MAP, SAMPLE_SET);
      var modified = original.withSecond(Map.of("new", 999));

      assertEquals(SAMPLE_MAP, original.second());
      assertEquals(Map.of("new", 999), modified.second());
    }
  }

  @Nested
  class Transformation_methods {

    @Test
    void should_transform_first_element_using_map_first() {
      var original = Triple.of(SAMPLE_STRING, SAMPLE_INTEGER, SAMPLE_DOUBLE);
      var transformed = original.mapFirst(String::length);

      assertEquals(SAMPLE_STRING.length(), transformed.first());
      assertEquals(SAMPLE_INTEGER, transformed.second());
      assertEquals(SAMPLE_DOUBLE, transformed.third());
    }

    @Test
    void should_transform_second_element_using_map_second() {
      var original = Triple.of(SAMPLE_STRING, SAMPLE_INTEGER, SAMPLE_DOUBLE);
      var transformed = original.mapSecond(x -> x * 2);

      assertEquals(SAMPLE_STRING, transformed.first());
      assertEquals(SAMPLE_INTEGER * 2, transformed.second());
      assertEquals(SAMPLE_DOUBLE, transformed.third());
    }

    @Test
    void should_transform_third_element_using_map_third() {
      var original = Triple.of(SAMPLE_STRING, SAMPLE_INTEGER, SAMPLE_DOUBLE);
      var transformed = original.mapThird(Object::toString);

      assertEquals(SAMPLE_STRING, transformed.first());
      assertEquals(SAMPLE_INTEGER, transformed.second());
      assertEquals(SAMPLE_DOUBLE.toString(), transformed.third());
    }

    @Test
    void should_transform_all_elements_using_map_all() {
      var original = Triple.of(SAMPLE_STRING, SAMPLE_INTEGER, SAMPLE_DOUBLE);
      var transformed = original.mapAll(String::length, x -> x * 10, x -> x > 3.0);

      assertEquals(SAMPLE_STRING.length(), transformed.first());
      assertEquals(SAMPLE_INTEGER * 10, transformed.second());
      assertTrue(transformed.third());
    }

    @Test
    void should_handle_null_values_in_transformations() {
      var original = Triple.of((String) null, SAMPLE_INTEGER, (Double) null);

      var transformed = original.mapFirst(x -> x == null ? "NULL" : x.toUpperCase());
      assertEquals("NULL", transformed.first());

      var transformedThird = original.mapThird(x -> x == null ? -1.0 : x * 2);
      assertEquals(-1.0, transformedThird.third());
    }

    @Test
    void should_transform_complex_objects() {
      var original = Triple.of(SAMPLE_LIST, SAMPLE_MAP, SAMPLE_SET);
      var transformed =
          original.mapAll(
              List::size, Map::keySet, set -> set.stream().mapToInt(Integer::intValue).sum());

      assertEquals(SAMPLE_LIST.size(), transformed.first());
      assertEquals(SAMPLE_MAP.keySet(), transformed.second());
      assertEquals(15, transformed.third()); // Sum of 1+2+3+4+5
    }

    @ParameterizedTest
    @MethodSource("nullMapperTestCases")
    void should_throw_null_pointer_exception_for_null_mappers(Runnable nullMapperTest) {
      assertThrows(NullPointerException.class, nullMapperTest::run);
    }

    static Stream<Arguments> nullMapperTestCases() {
      var triple = Triple.of(SAMPLE_STRING, SAMPLE_INTEGER, SAMPLE_DOUBLE);
      return Stream.of(
          Arguments.of((Runnable) () -> triple.mapFirst(null)),
          Arguments.of((Runnable) () -> triple.mapSecond(null)),
          Arguments.of((Runnable) () -> triple.mapThird(null)),
          Arguments.of((Runnable) () -> triple.mapAll(null, x -> x, x -> x)),
          Arguments.of((Runnable) () -> triple.mapAll(x -> x, null, x -> x)),
          Arguments.of((Runnable) () -> triple.mapAll(x -> x, x -> x, null)));
    }
  }

  @Nested
  class Null_checking_methods {

    @ParameterizedTest
    @MethodSource("hasNullTestCases")
    void should_correctly_identify_when_any_element_is_null(
        Triple<?, ?, ?> triple, boolean expected) {
      assertEquals(expected, triple.hasNull());
    }

    static Stream<Arguments> hasNullTestCases() {
      return Stream.of(
          Arguments.of(Triple.of(null, SAMPLE_INTEGER, SAMPLE_DOUBLE), true),
          Arguments.of(Triple.of(SAMPLE_STRING, null, SAMPLE_DOUBLE), true),
          Arguments.of(Triple.of(SAMPLE_STRING, SAMPLE_INTEGER, null), true),
          Arguments.of(Triple.of(null, null, null), true),
          Arguments.of(Triple.of(SAMPLE_STRING, SAMPLE_INTEGER, SAMPLE_DOUBLE), false));
    }

    @ParameterizedTest
    @MethodSource("isAllNullTestCases")
    void should_correctly_identify_when_all_elements_are_null(
        Triple<?, ?, ?> triple, boolean expected) {
      assertEquals(expected, triple.isAllNull());
    }

    static Stream<Arguments> isAllNullTestCases() {
      return Stream.of(
          Arguments.of(Triple.of(null, null, null), true),
          Arguments.of(Triple.empty(), true),
          Arguments.of(Triple.of(SAMPLE_STRING, null, null), false),
          Arguments.of(Triple.of(null, SAMPLE_INTEGER, null), false),
          Arguments.of(Triple.of(null, null, SAMPLE_DOUBLE), false),
          Arguments.of(Triple.of(SAMPLE_STRING, SAMPLE_INTEGER, SAMPLE_DOUBLE), false));
    }

    @ParameterizedTest
    @MethodSource("isAllNonNullTestCases")
    void should_correctly_identify_when_all_elements_are_non_null(
        Triple<?, ?, ?> triple, boolean expected) {
      assertEquals(expected, triple.isAllNonNull());
    }

    static Stream<Arguments> isAllNonNullTestCases() {
      return Stream.of(
          Arguments.of(Triple.of(SAMPLE_STRING, SAMPLE_INTEGER, SAMPLE_DOUBLE), true),
          Arguments.of(Triple.of(SAMPLE_LIST, SAMPLE_MAP, SAMPLE_SET), true),
          Arguments.of(Triple.of(null, SAMPLE_INTEGER, SAMPLE_DOUBLE), false),
          Arguments.of(Triple.of(SAMPLE_STRING, null, SAMPLE_DOUBLE), false),
          Arguments.of(Triple.of(SAMPLE_STRING, SAMPLE_INTEGER, null), false),
          Arguments.of(Triple.of(null, null, null), false));
    }
  }

  @Nested
  class Swapping_methods {

    @Test
    void should_swap_first_and_second_elements() {
      var original = Triple.of(SAMPLE_STRING, SAMPLE_INTEGER, SAMPLE_DOUBLE);
      var swapped = original.swapFirstSecond();

      assertEquals(SAMPLE_INTEGER, swapped.first());
      assertEquals(SAMPLE_STRING, swapped.second());
      assertEquals(SAMPLE_DOUBLE, swapped.third());
    }

    @Test
    void should_swap_first_and_third_elements() {
      var original = Triple.of(SAMPLE_STRING, SAMPLE_INTEGER, SAMPLE_DOUBLE);
      var swapped = original.swapFirstThird();

      assertEquals(SAMPLE_DOUBLE, swapped.first());
      assertEquals(SAMPLE_INTEGER, swapped.second());
      assertEquals(SAMPLE_STRING, swapped.third());
    }

    @Test
    void should_swap_second_and_third_elements() {
      var original = Triple.of(SAMPLE_STRING, SAMPLE_INTEGER, SAMPLE_DOUBLE);
      var swapped = original.swapSecondThird();

      assertEquals(SAMPLE_STRING, swapped.first());
      assertEquals(SAMPLE_DOUBLE, swapped.second());
      assertEquals(SAMPLE_INTEGER, swapped.third());
    }

    @Test
    void should_handle_null_values_in_swapping() {
      var original = Triple.of((String) null, SAMPLE_INTEGER, (Double) null);

      var swapped = original.swapFirstSecond();
      assertEquals(SAMPLE_INTEGER, swapped.first());
      assertNull(swapped.second());
      assertNull(swapped.third());
    }

    @Test
    void should_swap_complex_objects() {
      var original = Triple.of(SAMPLE_LIST, SAMPLE_MAP, SAMPLE_SET);
      var swapped = original.swapFirstThird();

      assertEquals(SAMPLE_SET, swapped.first());
      assertEquals(SAMPLE_MAP, swapped.second());
      assertEquals(SAMPLE_LIST, swapped.third());
    }
  }

  @Nested
  class Array_conversion {

    @Test
    void should_convert_triple_to_array() {
      var triple = Triple.of(SAMPLE_STRING, SAMPLE_INTEGER, SAMPLE_DOUBLE);
      var array = triple.toArray();

      assertEquals(3, array.length);
      assertEquals(SAMPLE_STRING, array[0]);
      assertEquals(SAMPLE_INTEGER, array[1]);
      assertEquals(SAMPLE_DOUBLE, array[2]);
    }

    @Test
    void should_handle_null_values_in_array_conversion() {
      var triple = Triple.of((String) null, SAMPLE_INTEGER, (Double) null);
      var array = triple.toArray();

      assertEquals(3, array.length);
      assertNull(array[0]);
      assertEquals(SAMPLE_INTEGER, array[1]);
      assertNull(array[2]);
    }

    @Test
    void should_convert_complex_objects_to_array() {
      var triple = Triple.of(SAMPLE_LIST, SAMPLE_MAP, SAMPLE_SET);
      var array = triple.toArray();

      assertEquals(3, array.length);
      assertEquals(SAMPLE_LIST, array[0]);
      assertEquals(SAMPLE_MAP, array[1]);
      assertEquals(SAMPLE_SET, array[2]);
    }
  }

  @Nested
  class String_representation {

    @Test
    void should_provide_correct_string_representation() {
      var triple = Triple.of(SAMPLE_STRING, SAMPLE_INTEGER, SAMPLE_DOUBLE);
      var expected = "(%s, %s, %s)".formatted(SAMPLE_STRING, SAMPLE_INTEGER, SAMPLE_DOUBLE);

      assertEquals(expected, triple.toString());
    }

    @Test
    void should_handle_null_values_in_string_representation() {
      var triple = Triple.of((String) null, SAMPLE_INTEGER, (Double) null);
      var expected = "(null, %s, null)".formatted(SAMPLE_INTEGER);

      assertEquals(expected, triple.toString());
    }

    @Test
    void should_handle_all_null_values_in_string_representation() {
      var empty = Triple.<String, Integer, Double>empty();

      assertEquals("(null, null, null)", empty.toString());
    }

    @Test
    void should_format_complex_objects_correctly() {
      var triple = Triple.of(SAMPLE_LIST, SAMPLE_MAP, SAMPLE_DATE);
      var result = triple.toString();

      assertTrue(result.startsWith("("));
      assertTrue(result.endsWith(")"));
      assertTrue(result.contains(SAMPLE_LIST.toString()));
      assertTrue(result.contains(SAMPLE_MAP.toString()));
      assertTrue(result.contains(SAMPLE_DATE.toString()));
    }
  }

  @Nested
  class Serialization {

    @Test
    void should_be_serializable_and_deserializable() throws IOException, ClassNotFoundException {
      var original = Triple.of(SAMPLE_STRING, SAMPLE_INTEGER, SAMPLE_DOUBLE);

      var serialized = serializeObject(original);
      var deserialized = deserializeObject(serialized, Triple.class);

      assertEquals(original, deserialized);
      assertEquals(original.first(), deserialized.first());
      assertEquals(original.second(), deserialized.second());
      assertEquals(original.third(), deserialized.third());
    }

    @Test
    void should_serialize_and_deserialize_null_values() throws IOException, ClassNotFoundException {
      var original = Triple.of((String) null, SAMPLE_INTEGER, (Double) null);

      var serialized = serializeObject(original);
      var deserialized = deserializeObject(serialized, Triple.class);

      assertEquals(original, deserialized);
      assertNull(deserialized.first());
      assertEquals(SAMPLE_INTEGER, deserialized.second());
      assertNull(deserialized.third());
    }

    @Test
    void should_serialize_complex_objects() throws IOException, ClassNotFoundException {
      var original = Triple.of(SAMPLE_LIST, SAMPLE_DATE, SAMPLE_BIG_DECIMAL);

      var serialized = serializeObject(original);
      var deserialized = deserializeObject(serialized, Triple.class);

      assertEquals(original, deserialized);
    }

    @SuppressWarnings("unchecked")
    private <T> T deserializeObject(byte[] data, Class<T> type)
        throws IOException, ClassNotFoundException {
      try (var bais = new ByteArrayInputStream(data);
          var ois = new ObjectInputStream(bais)) {
        return (T) ois.readObject();
      }
    }

    private byte[] serializeObject(Object obj) throws IOException {
      try (var baos = new ByteArrayOutputStream();
          var oos = new ObjectOutputStream(baos)) {
        oos.writeObject(obj);
        return baos.toByteArray();
      }
    }
  }

  @Nested
  class Edge_cases_and_integration {

    @Test
    void should_handle_method_chaining() {
      var result =
          Triple.of(SAMPLE_STRING, 1, 2.0)
              .mapFirst(String::toUpperCase)
              .mapSecond(x -> x * 10)
              .withThird(100.0);

      assertEquals(SAMPLE_STRING.toUpperCase(), result.first());
      assertEquals(10, result.second());
      assertEquals(100.0, result.third());
    }

    @Test
    void should_handle_complex_transformations() {
      var triple = Triple.of("hello", "world", "java");
      var lengths = triple.mapAll(String::length, String::length, String::length);

      assertEquals(5, lengths.first());
      assertEquals(5, lengths.second());
      assertEquals(4, lengths.third());
    }

    @ParameterizedTest
    @MethodSource("provideRealWorldTestData")
    void should_handle_various_real_world_data_types(Object first, Object second, Object third) {
      var triple = Triple.of(first, second, third);

      assertEquals(first, triple.first());
      assertEquals(second, triple.second());
      assertEquals(third, triple.third());
    }

    static Stream<Arguments> provideRealWorldTestData() {
      return Stream.of(
          Arguments.of(SAMPLE_STRING, SAMPLE_INTEGER, SAMPLE_DOUBLE),
          Arguments.of(SAMPLE_BOOLEAN, SAMPLE_DATE, null),
          Arguments.of(SAMPLE_LIST, SAMPLE_MAP, SAMPLE_SET),
          Arguments.of(null, null, null),
          Arguments.of(SAMPLE_BIG_DECIMAL, LocalDateTime.now(), "test"),
          Arguments.of(new int[] {1, 2, 3}, "array", 'c'),
          Arguments.of(Map.of("nested", List.of(1, 2, 3)), SAMPLE_SET, SAMPLE_DOUBLE));
    }

    @Test
    void should_maintain_immutability_contract() {
      var original = Triple.of(SAMPLE_STRING, SAMPLE_INTEGER, SAMPLE_DOUBLE);

      var withFirst = original.withFirst("world");
      var withSecond = original.withSecond(100);
      var withThird = original.withThird(2.71);

      assertNotSame(original, withFirst);
      assertNotSame(original, withSecond);
      assertNotSame(original, withThird);

      // Original unchanged
      assertEquals(SAMPLE_STRING, original.first());
      assertEquals(SAMPLE_INTEGER, original.second());
      assertEquals(SAMPLE_DOUBLE, original.third());
    }

    @Test
    void should_work_with_different_generic_combinations() {
      var stringIntBool = Triple.of("test", 42, true);
      var listMapSet = Triple.of(SAMPLE_LIST, SAMPLE_MAP, SAMPLE_SET);
      var dateTimeNumber = Triple.of(SAMPLE_DATE, LocalDateTime.now(), SAMPLE_BIG_DECIMAL);

      assertAll(
          () -> assertEquals("test", stringIntBool.first()),
          () -> assertEquals(42, stringIntBool.second()),
          () -> assertTrue(stringIntBool.third()),
          () -> assertEquals(SAMPLE_LIST, listMapSet.first()),
          () -> assertNotNull(dateTimeNumber.second()));
    }
  }
}
