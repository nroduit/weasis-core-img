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

import java.io.*;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("Triple Tests")
class TripleTest {

  @Nested
  @DisplayName("Basic Construction and Access")
  class BasicConstructionTests {

    @Test
    @DisplayName("Should store and retrieve all three values correctly")
    void testTripleStoresFirstSecondAndThirdValues() {
      Triple<String, Integer, Double> triple = new Triple<>("first", 1, 2.0);
      assertEquals("first", triple.first());
      assertEquals(1, triple.second());
      assertEquals(2.0, triple.third());
    }

    @Test
    @DisplayName("Should handle different types correctly")
    void testTripleWithDifferentTypes() {
      Triple<Integer, String, Boolean> triple = new Triple<>(1, "second", true);
      assertEquals(1, triple.first());
      assertEquals("second", triple.second());
      assertTrue(triple.third());
    }

    @Test
    @DisplayName("Should handle mixed null and non-null values")
    void testTripleHandlesMixedNullAndNonNullValues() {
      Triple<String, Integer, Double> triple = new Triple<>("first", null, 2.0);
      assertEquals("first", triple.first());
      assertNull(triple.second());
      assertEquals(2.0, triple.third());

      triple = new Triple<>(null, 1, null);
      assertNull(triple.first());
      assertEquals(1, triple.second());
      assertNull(triple.third());
    }

    @Test
    @DisplayName("Should handle all null values")
    void testTripleWithAllNullValues() {
      Triple<String, Integer, Double> triple = new Triple<>(null, null, null);
      assertNull(triple.first());
      assertNull(triple.second());
      assertNull(triple.third());
    }
  }

  @Nested
  @DisplayName("Factory Methods")
  class FactoryMethodTests {

    @Test
    @DisplayName("Should create triple using of() factory method")
    void testFactoryMethodOf() {
      Triple<String, Integer, Boolean> triple = Triple.of("hello", 42, true);
      assertEquals("hello", triple.first());
      assertEquals(42, triple.second());
      assertTrue(triple.third());
    }

    @Test
    @DisplayName("Should create empty triple with all null values")
    void testFactoryMethodEmpty() {
      Triple<String, Integer, Boolean> triple = Triple.empty();
      assertNull(triple.first());
      assertNull(triple.second());
      assertNull(triple.third());
    }

    @Test
    @DisplayName("Should create triple with null values using of()")
    void testFactoryMethodOfWithNulls() {
      Triple<String, Integer, Boolean> triple = Triple.of(null, null, null);
      assertNull(triple.first());
      assertNull(triple.second());
      assertNull(triple.third());
    }
  }

  @Nested
  @DisplayName("Equality and HashCode")
  class EqualityTests {

    @Test
    @DisplayName("Should be equal when all elements are equal")
    void testTripleEquality() {
      Triple<String, Integer, Double> triple1 = new Triple<>("first", 1, 2.0);
      Triple<String, Integer, Double> triple2 = new Triple<>("first", 1, 2.0);
      assertEquals(triple1, triple2);
      assertEquals(triple1.hashCode(), triple2.hashCode());
    }

    @Test
    @DisplayName("Should not be equal when elements differ")
    void testTripleInequality() {
      Triple<String, Integer, Double> triple1 = new Triple<>("first", 1, 2.0);

      // Different first element
      Triple<String, Integer, Double> triple2 = new Triple<>("second", 1, 2.0);
      assertNotEquals(triple1, triple2);

      // Different second element
      triple2 = new Triple<>("first", 2, 2.0);
      assertNotEquals(triple1, triple2);

      // Different third element
      triple2 = new Triple<>("first", 1, 3.0);
      assertNotEquals(triple1, triple2);
    }

    @Test
    @DisplayName("Should handle null equality correctly")
    void testTripleEqualityWithNulls() {
      Triple<String, Integer, Double> triple1 = new Triple<>(null, 1, null);
      Triple<String, Integer, Double> triple2 = new Triple<>(null, 1, null);
      assertEquals(triple1, triple2);
      assertEquals(triple1.hashCode(), triple2.hashCode());
    }

    @Test
    @DisplayName("Should not be equal to null or other types")
    void testTripleNotEqualToNullOrOtherTypes() {
      Triple<String, Integer, Double> triple = new Triple<>("first", 1, 2.0);
      assertNotEquals(null, triple);
      assertNotEquals("not a triple", triple);
      assertNotEquals(42, triple);
    }
  }

  @Nested
  @DisplayName("Immutable Updates")
  class ImmutableUpdateTests {

    @Test
    @DisplayName("Should create new triple with different first element")
    void testWithFirst() {
      Triple<String, Integer, Double> original = Triple.of("first", 1, 2.0);
      Triple<Integer, Integer, Double> updated = original.withFirst(42);

      assertEquals(42, updated.first());
      assertEquals(1, updated.second());
      assertEquals(2.0, updated.third());

      // Original should be unchanged
      assertEquals("first", original.first());
    }

    @Test
    @DisplayName("Should create new triple with different second element")
    void testWithSecond() {
      Triple<String, Integer, Double> original = Triple.of("first", 1, 2.0);
      Triple<String, String, Double> updated = original.withSecond("new");

      assertEquals("first", updated.first());
      assertEquals("new", updated.second());
      assertEquals(2.0, updated.third());

      // Original should be unchanged
      assertEquals(1, original.second());
    }

    @Test
    @DisplayName("Should create new triple with different third element")
    void testWithThird() {
      Triple<String, Integer, Double> original = Triple.of("first", 1, 2.0);
      Triple<String, Integer, Boolean> updated = original.withThird(true);

      assertEquals("first", updated.first());
      assertEquals(1, updated.second());
      assertTrue(updated.third());

      // Original should be unchanged
      assertEquals(2.0, original.third());
    }

    @Test
    @DisplayName("Should handle null values in with methods")
    void testWithMethodsWithNulls() {
      Triple<String, Integer, Double> original = Triple.of("first", 1, 2.0);

      Triple<String, Integer, Double> withNullFirst = original.withFirst(null);
      assertNull(withNullFirst.first());

      Triple<String, Integer, Double> withNullSecond = original.withSecond(null);
      assertNull(withNullSecond.second());

      Triple<String, Integer, Double> withNullThird = original.withThird(null);
      assertNull(withNullThird.third());
    }
  }

  @Nested
  @DisplayName("Transformation Methods")
  class TransformationTests {

    @Test
    @DisplayName("Should transform first element using mapFirst")
    void testMapFirst() {
      Triple<String, Integer, Double> original = Triple.of("hello", 1, 2.0);
      Triple<Integer, Integer, Double> transformed = original.mapFirst(String::length);

      assertEquals(5, transformed.first());
      assertEquals(1, transformed.second());
      assertEquals(2.0, transformed.third());
    }

    @Test
    @DisplayName("Should transform second element using mapSecond")
    void testMapSecond() {
      Triple<String, Integer, Double> original = Triple.of("hello", 1, 2.0);
      Triple<String, Integer, Double> transformed = original.mapSecond(x -> x * 2);

      assertEquals("hello", transformed.first());
      assertEquals(2, transformed.second());
      assertEquals(2.0, transformed.third());
    }

    @Test
    @DisplayName("Should transform third element using mapThird")
    void testMapThird() {
      Triple<String, Integer, Double> original = Triple.of("hello", 1, 2.0);
      Triple<String, Integer, String> transformed = original.mapThird(Object::toString);

      assertEquals("hello", transformed.first());
      assertEquals(1, transformed.second());
      assertEquals("2.0", transformed.third());
    }

    @Test
    @DisplayName("Should transform all elements using mapAll")
    void testMapAll() {
      Triple<String, Integer, Double> original = Triple.of("hello", 1, 2.0);
      Triple<Integer, String, Boolean> transformed =
          original.mapAll(String::length, Object::toString, x -> x > 1.0);

      assertEquals(5, transformed.first());
      assertEquals("1", transformed.second());
      assertTrue(transformed.third());
    }

    @Test
    @DisplayName("Should handle null values in transformations")
    void testTransformationsWithNulls() {
      Triple<String, Integer, Double> original = Triple.of(null, 1, null);

      Triple<Integer, Integer, Double> transformed =
          original.mapFirst(x -> x == null ? 0 : x.length());
      assertEquals(0, transformed.first());

      Triple<String, Integer, String> transformedThird =
          original.mapThird(x -> x == null ? "null" : x.toString());
      assertEquals("null", transformedThird.third());
    }

    @Test
    @DisplayName("Should throw NullPointerException for null mappers")
    void testNullMapperThrowsException() {
      Triple<String, Integer, Double> triple = Triple.of("hello", 1, 2.0);

      assertThrows(NullPointerException.class, () -> triple.mapFirst(null));
      assertThrows(NullPointerException.class, () -> triple.mapSecond(null));
      assertThrows(NullPointerException.class, () -> triple.mapThird(null));
      assertThrows(NullPointerException.class, () -> triple.mapAll(null, x -> x, x -> x));
      assertThrows(NullPointerException.class, () -> triple.mapAll(x -> x, null, x -> x));
      assertThrows(NullPointerException.class, () -> triple.mapAll(x -> x, x -> x, null));
    }
  }

  @Nested
  @DisplayName("Null Checking Methods")
  class NullCheckingTests {

    @Test
    @DisplayName("Should correctly identify when any element is null")
    void testHasNull() {
      assertTrue(Triple.of(null, 1, 2.0).hasNull());
      assertTrue(Triple.of("hello", null, 2.0).hasNull());
      assertTrue(Triple.of("hello", 1, null).hasNull());
      assertTrue(Triple.of(null, null, null).hasNull());
      assertFalse(Triple.of("hello", 1, 2.0).hasNull());
    }

    @Test
    @DisplayName("Should correctly identify when all elements are null")
    void testIsAllNull() {
      assertTrue(Triple.of(null, null, null).isAllNull());
      assertTrue(Triple.empty().isAllNull());
      assertFalse(Triple.of("hello", null, null).isAllNull());
      assertFalse(Triple.of(null, 1, null).isAllNull());
      assertFalse(Triple.of(null, null, 2.0).isAllNull());
      assertFalse(Triple.of("hello", 1, 2.0).isAllNull());
    }

    @Test
    @DisplayName("Should correctly identify when all elements are non-null")
    void testIsAllNonNull() {
      assertTrue(Triple.of("hello", 1, 2.0).isAllNonNull());
      assertFalse(Triple.of(null, 1, 2.0).isAllNonNull());
      assertFalse(Triple.of("hello", null, 2.0).isAllNonNull());
      assertFalse(Triple.of("hello", 1, null).isAllNonNull());
      assertFalse(Triple.of(null, null, null).isAllNonNull());
    }
  }

  @Nested
  @DisplayName("Swapping Methods")
  class SwappingTests {

    @Test
    @DisplayName("Should swap first and second elements")
    void testSwapFirstSecond() {
      Triple<String, Integer, Double> original = Triple.of("hello", 42, 3.14);
      Triple<Integer, String, Double> swapped = original.swapFirstSecond();

      assertEquals(42, swapped.first());
      assertEquals("hello", swapped.second());
      assertEquals(3.14, swapped.third());
    }

    @Test
    @DisplayName("Should swap first and third elements")
    void testSwapFirstThird() {
      Triple<String, Integer, Double> original = Triple.of("hello", 42, 3.14);
      Triple<Double, Integer, String> swapped = original.swapFirstThird();

      assertEquals(3.14, swapped.first());
      assertEquals(42, swapped.second());
      assertEquals("hello", swapped.third());
    }

    @Test
    @DisplayName("Should swap second and third elements")
    void testSwapSecondThird() {
      Triple<String, Integer, Double> original = Triple.of("hello", 42, 3.14);
      Triple<String, Double, Integer> swapped = original.swapSecondThird();

      assertEquals("hello", swapped.first());
      assertEquals(3.14, swapped.second());
      assertEquals(42, swapped.third());
    }

    @Test
    @DisplayName("Should handle null values in swapping")
    void testSwapWithNulls() {
      Triple<String, Integer, Double> original = Triple.of(null, 42, null);

      Triple<Integer, String, Double> swapped = original.swapFirstSecond();
      assertEquals(42, swapped.first());
      assertNull(swapped.second());
      assertNull(swapped.third());
    }
  }

  @Nested
  @DisplayName("Array Conversion")
  class ArrayConversionTests {

    @Test
    @DisplayName("Should convert triple to array")
    void testToArray() {
      Triple<String, Integer, Double> triple = Triple.of("hello", 42, 3.14);
      Object[] array = triple.toArray();

      assertEquals(3, array.length);
      assertEquals("hello", array[0]);
      assertEquals(42, array[1]);
      assertEquals(3.14, array[2]);
    }

    @Test
    @DisplayName("Should handle null values in array conversion")
    void testToArrayWithNulls() {
      Triple<String, Integer, Double> triple = Triple.of(null, 42, null);
      Object[] array = triple.toArray();

      assertEquals(3, array.length);
      assertNull(array[0]);
      assertEquals(42, array[1]);
      assertNull(array[2]);
    }
  }

  @Nested
  @DisplayName("String Representation")
  class StringRepresentationTests {

    @Test
    @DisplayName("Should provide correct string representation")
    void testToString() {
      Triple<String, Integer, Double> triple = Triple.of("hello", 42, 3.14);
      assertEquals("(hello, 42, 3.14)", triple.toString());
    }

    @Test
    @DisplayName("Should handle null values in string representation")
    void testToStringWithNulls() {
      Triple<String, Integer, Double> triple = Triple.of(null, 42, null);
      assertEquals("(null, 42, null)", triple.toString());
    }

    @Test
    @DisplayName("Should handle all null values in string representation")
    void testToStringAllNulls() {
      Triple<String, Integer, Double> triple = Triple.empty();
      assertEquals("(null, null, null)", triple.toString());
    }
  }

  @Nested
  @DisplayName("Serialization")
  class SerializationTests {

    @Test
    @DisplayName("Should be serializable and deserializable")
    void testSerialization() throws IOException, ClassNotFoundException {
      Triple<String, Integer, Double> original = Triple.of("hello", 42, 3.14);

      // Serialize
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
        oos.writeObject(original);
      }

      // Deserialize
      ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
      Triple<String, Integer, Double> deserialized;
      try (ObjectInputStream ois = new ObjectInputStream(bais)) {
        deserialized = (Triple<String, Integer, Double>) ois.readObject();
      }

      assertEquals(original, deserialized);
      assertEquals(original.first(), deserialized.first());
      assertEquals(original.second(), deserialized.second());
      assertEquals(original.third(), deserialized.third());
    }

    @Test
    @DisplayName("Should serialize and deserialize null values")
    void testSerializationWithNulls() throws IOException, ClassNotFoundException {
      Triple<String, Integer, Double> original = Triple.of(null, 42, null);

      // Serialize
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
        oos.writeObject(original);
      }

      // Deserialize
      ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
      Triple<String, Integer, Double> deserialized;
      try (ObjectInputStream ois = new ObjectInputStream(bais)) {
        deserialized = (Triple<String, Integer, Double>) ois.readObject();
      }

      assertEquals(original, deserialized);
      assertNull(deserialized.first());
      assertEquals(42, deserialized.second());
      assertNull(deserialized.third());
    }
  }

  @Nested
  @DisplayName("Edge Cases and Integration")
  class EdgeCasesTests {

    @Test
    @DisplayName("Should handle method chaining")
    void testMethodChaining() {
      Triple<String, Integer, Double> result =
          Triple.of("hello", 1, 2.0)
              .mapFirst(String::toUpperCase)
              .mapSecond(x -> x * 10)
              .withThird(100.0);

      assertEquals("HELLO", result.first());
      assertEquals(10, result.second());
      assertEquals(100.0, result.third());
    }

    @Test
    @DisplayName("Should handle complex transformations")
    void testComplexTransformations() {
      Triple<String, String, String> triple = Triple.of("hello", "world", "java");

      Triple<Integer, Integer, Integer> lengths =
          triple.mapAll(String::length, String::length, String::length);

      assertEquals(5, lengths.first());
      assertEquals(5, lengths.second());
      assertEquals(4, lengths.third());
    }

    @ParameterizedTest
    @MethodSource("provideTripleTestData")
    @DisplayName("Should handle various data types")
    void testVariousDataTypes(Object first, Object second, Object third) {
      Triple<Object, Object, Object> triple = Triple.of(first, second, third);
      assertEquals(first, triple.first());
      assertEquals(second, triple.second());
      assertEquals(third, triple.third());
    }

    static Stream<Arguments> provideTripleTestData() {
      return Stream.of(
          Arguments.of("string", 42, 3.14),
          Arguments.of(true, false, null),
          Arguments.of(new Object(), "test", 100L),
          Arguments.of(null, null, null),
          Arguments.of(new int[] {1, 2, 3}, "array", 'c'));
    }

    @Test
    @DisplayName("Should maintain immutability contract")
    void testImmutabilityContract() {
      Triple<String, Integer, Double> original = Triple.of("hello", 42, 3.14);

      // All transformation methods should return new instances
      Triple<String, Integer, Double> withFirst = original.withFirst("world");
      Triple<String, Integer, Double> withSecond = original.withSecond(100);
      Triple<String, Integer, Double> withThird = original.withThird(2.71);

      assertNotSame(original, withFirst);
      assertNotSame(original, withSecond);
      assertNotSame(original, withThird);

      // Original should be unchanged
      assertEquals("hello", original.first());
      assertEquals(42, original.second());
      assertEquals(3.14, original.third());
    }
  }
}
