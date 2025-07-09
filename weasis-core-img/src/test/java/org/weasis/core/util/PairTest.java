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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Pair Tests")
class PairTest {

  private static final String FIRST_VALUE = "first";
  private static final String SECOND_VALUE = "second";
  private static final Integer FIRST_NUMBER = 1;
  private static final Integer SECOND_NUMBER = 2;

  @Nested
  @DisplayName("Basic Construction and Access")
  class BasicConstructionTests {

    @Test
    @DisplayName("Should store and retrieve first and second values correctly")
    void shouldStoreAndRetrieveValues() {
      Pair<String, Integer> pair = new Pair<>(FIRST_VALUE, FIRST_NUMBER);
      assertEquals(FIRST_VALUE, pair.first());
      assertEquals(FIRST_NUMBER, pair.second());
    }

    @Test
    @DisplayName("Should handle different types correctly")
    void shouldHandleDifferentTypes() {
      Pair<Integer, String> pair = new Pair<>(FIRST_NUMBER, SECOND_VALUE);
      assertEquals(FIRST_NUMBER, pair.first());
      assertEquals(SECOND_VALUE, pair.second());
    }

    @Test
    @DisplayName("Should handle mixed null and non-null values")
    void shouldHandleMixedNullAndNonNullValues() {
      Pair<String, Integer> pair = new Pair<>(FIRST_VALUE, null);
      assertEquals(FIRST_VALUE, pair.first());
      assertNull(pair.second());

      pair = new Pair<>(null, FIRST_NUMBER);
      assertNull(pair.first());
      assertEquals(FIRST_NUMBER, pair.second());
    }

    @Test
    @DisplayName("Should handle all null values")
    void shouldHandleAllNullValues() {
      Pair<String, Integer> pair = new Pair<>(null, null);
      assertNull(pair.first());
      assertNull(pair.second());
    }
  }

  @Nested
  @DisplayName("Factory Methods")
  class FactoryMethodTests {

    @Test
    @DisplayName("Should create pair using of() factory method")
    void shouldCreatePairUsingOfFactory() {
      Pair<String, Integer> pair = Pair.of(FIRST_VALUE, FIRST_NUMBER);
      assertEquals(FIRST_VALUE, pair.first());
      assertEquals(FIRST_NUMBER, pair.second());
    }

    @Test
    @DisplayName("Should create empty pair with all null values")
    void shouldCreateEmptyPair() {
      Pair<String, Integer> pair = Pair.empty();
      assertNull(pair.first());
      assertNull(pair.second());
    }

    @Test
    @DisplayName("Should create pair with same values using same() factory")
    void shouldCreatePairWithSameValues() {
      Pair<String, String> pair = Pair.same(FIRST_VALUE);
      assertEquals(FIRST_VALUE, pair.first());
      assertEquals(FIRST_VALUE, pair.second());
    }

    @Test
    @DisplayName("Should create pair with null values using same() factory")
    void shouldCreatePairWithNullValueUsingSameFactory() {
      Pair<String, String> pair = Pair.same(null);
      assertNull(pair.first());
      assertNull(pair.second());
    }
  }

  @Nested
  @DisplayName("Equality and HashCode")
  class EqualityTests {

    @Test
    @DisplayName("Should be equal when all elements are equal")
    void shouldBeEqualWhenAllElementsAreEqual() {
      Pair<String, Integer> pair1 = new Pair<>(FIRST_VALUE, FIRST_NUMBER);
      Pair<String, Integer> pair2 = new Pair<>(FIRST_VALUE, FIRST_NUMBER);
      assertEquals(pair1, pair2);
      assertEquals(pair1.hashCode(), pair2.hashCode());
    }

    @Test
    @DisplayName("Should not be equal when elements differ")
    void shouldNotBeEqualWhenElementsDiffer() {
      Pair<String, Integer> pair1 = new Pair<>(FIRST_VALUE, FIRST_NUMBER);
      Pair<String, Integer> pair2 = new Pair<>(SECOND_VALUE, FIRST_NUMBER);
      assertNotEquals(pair1, pair2);

      pair2 = new Pair<>(FIRST_VALUE, SECOND_NUMBER);
      assertNotEquals(pair1, pair2);
    }

    @Test
    @DisplayName("Should handle null equality correctly")
    void shouldHandleNullEqualityCorrectly() {
      Pair<String, Integer> pair1 = new Pair<>(null, null);
      Pair<String, Integer> pair2 = new Pair<>(null, null);
      assertEquals(pair1, pair2);

      pair1 = new Pair<>(FIRST_VALUE, null);
      pair2 = new Pair<>(FIRST_VALUE, null);
      assertEquals(pair1, pair2);
    }
  }

  @Nested
  @DisplayName("Swapping Operations")
  class SwappingTests {

    @Test
    @DisplayName("Should swap first and second values")
    void shouldSwapFirstAndSecondValues() {
      Pair<String, Integer> original = new Pair<>(FIRST_VALUE, FIRST_NUMBER);
      Pair<Integer, String> swapped = original.swap();

      assertEquals(FIRST_NUMBER, swapped.first());
      assertEquals(FIRST_VALUE, swapped.second());
    }

    @Test
    @DisplayName("Should handle null values in swapping")
    void shouldHandleNullValuesInSwapping() {
      Pair<String, Integer> original = new Pair<>(FIRST_VALUE, null);
      Pair<Integer, String> swapped = original.swap();

      assertNull(swapped.first());
      assertEquals(FIRST_VALUE, swapped.second());
    }
  }

  @Nested
  @DisplayName("Immutable Updates")
  class ImmutableUpdateTests {

    @Test
    @DisplayName("Should create new pair with different first element")
    void shouldCreateNewPairWithDifferentFirstElement() {
      Pair<String, Integer> original = new Pair<>(FIRST_VALUE, FIRST_NUMBER);
      Pair<String, Integer> updated = original.withFirst(SECOND_VALUE);

      assertEquals(SECOND_VALUE, updated.first());
      assertEquals(FIRST_NUMBER, updated.second());
      // Original should remain unchanged
      assertEquals(FIRST_VALUE, original.first());
    }

    @Test
    @DisplayName("Should create new pair with different second element")
    void shouldCreateNewPairWithDifferentSecondElement() {
      Pair<String, Integer> original = new Pair<>(FIRST_VALUE, FIRST_NUMBER);
      Pair<String, Integer> updated = original.withSecond(SECOND_NUMBER);

      assertEquals(FIRST_VALUE, updated.first());
      assertEquals(SECOND_NUMBER, updated.second());
      // Original should remain unchanged
      assertEquals(FIRST_NUMBER, original.second());
    }

    @Test
    @DisplayName("Should handle null values in with methods")
    void shouldHandleNullValuesInWithMethods() {
      Pair<String, Integer> original = new Pair<>(FIRST_VALUE, FIRST_NUMBER);
      Pair<String, Integer> updated = original.withFirst(null);

      assertNull(updated.first());
      assertEquals(FIRST_NUMBER, updated.second());
    }
  }

  @Nested
  @DisplayName("Transformation Methods")
  class TransformationTests {

    @Test
    @DisplayName("Should transform first element using mapFirst")
    void shouldTransformFirstElementUsingMapFirst() {
      Pair<String, Integer> original = new Pair<>(FIRST_VALUE, FIRST_NUMBER);
      Pair<String, Integer> transformed = original.mapFirst(String::toUpperCase);

      assertEquals(FIRST_VALUE.toUpperCase(), transformed.first());
      assertEquals(FIRST_NUMBER, transformed.second());
    }

    @Test
    @DisplayName("Should transform second element using mapSecond")
    void shouldTransformSecondElementUsingMapSecond() {
      Pair<String, Integer> original = new Pair<>(FIRST_VALUE, FIRST_NUMBER);
      Pair<String, String> transformed = original.mapSecond(String::valueOf);

      assertEquals(FIRST_VALUE, transformed.first());
      assertEquals(FIRST_NUMBER.toString(), transformed.second());
    }

    @Test
    @DisplayName("Should transform both elements using mapBoth")
    void shouldTransformBothElementsUsingMapBoth() {
      Pair<String, Integer> original = new Pair<>(FIRST_VALUE, FIRST_NUMBER);
      Pair<String, String> transformed = original.mapBoth(String::toUpperCase, String::valueOf);

      assertEquals(FIRST_VALUE.toUpperCase(), transformed.first());
      assertEquals(FIRST_NUMBER.toString(), transformed.second());
    }

    @Test
    @DisplayName("Should handle null values in transformations")
    void shouldHandleNullValuesInTransformations() {
      Pair<String, Integer> original = new Pair<>(null, FIRST_NUMBER);
      Pair<String, Integer> transformed =
          original.mapFirst(value -> value == null ? "NULL" : value);

      assertEquals("NULL", transformed.first());
      assertEquals(FIRST_NUMBER, transformed.second());
    }

    @Test
    @DisplayName("Should throw NullPointerException for null mappers")
    void shouldThrowNullPointerExceptionForNullMappers() {
      Pair<String, Integer> pair = new Pair<>(FIRST_VALUE, FIRST_NUMBER);

      assertThrows(NullPointerException.class, () -> pair.mapFirst(null));
      assertThrows(NullPointerException.class, () -> pair.mapSecond(null));
      assertThrows(NullPointerException.class, () -> pair.mapBoth(null, String::valueOf));
      assertThrows(NullPointerException.class, () -> pair.mapBoth(String::toUpperCase, null));
    }
  }

  @Nested
  @DisplayName("Null Checking Methods")
  class NullCheckingTests {

    @Test
    @DisplayName("Should correctly identify when any element is null")
    void shouldCorrectlyIdentifyWhenAnyElementIsNull() {
      Pair<String, Integer> pair = new Pair<>(null, FIRST_NUMBER);
      assertTrue(pair.hasNull());

      pair = new Pair<>(FIRST_VALUE, null);
      assertTrue(pair.hasNull());

      pair = new Pair<>(null, null);
      assertTrue(pair.hasNull());

      pair = new Pair<>(FIRST_VALUE, FIRST_NUMBER);
      assertFalse(pair.hasNull());
    }

    @Test
    @DisplayName("Should correctly identify when all elements are null")
    void shouldCorrectlyIdentifyWhenAllElementsAreNull() {
      Pair<String, Integer> pair = new Pair<>(null, null);
      assertTrue(pair.isAllNull());

      pair = new Pair<>(null, FIRST_NUMBER);
      assertFalse(pair.isAllNull());

      pair = new Pair<>(FIRST_VALUE, null);
      assertFalse(pair.isAllNull());

      pair = new Pair<>(FIRST_VALUE, FIRST_NUMBER);
      assertFalse(pair.isAllNull());
    }

    @Test
    @DisplayName("Should correctly identify when all elements are non-null")
    void shouldCorrectlyIdentifyWhenAllElementsAreNonNull() {
      Pair<String, Integer> pair = new Pair<>(FIRST_VALUE, FIRST_NUMBER);
      assertTrue(pair.isAllNonNull());

      pair = new Pair<>(null, FIRST_NUMBER);
      assertFalse(pair.isAllNonNull());

      pair = new Pair<>(FIRST_VALUE, null);
      assertFalse(pair.isAllNonNull());

      pair = new Pair<>(null, null);
      assertFalse(pair.isAllNonNull());
    }
  }

  @Nested
  @DisplayName("Array Conversion")
  class ArrayConversionTests {

    @Test
    @DisplayName("Should convert pair to array")
    void shouldConvertPairToArray() {
      Pair<String, Integer> pair = new Pair<>(FIRST_VALUE, FIRST_NUMBER);
      Object[] array = pair.toArray();

      assertEquals(2, array.length);
      assertEquals(FIRST_VALUE, array[0]);
      assertEquals(FIRST_NUMBER, array[1]);
    }

    @Test
    @DisplayName("Should handle null values in array conversion")
    void shouldHandleNullValuesInArrayConversion() {
      Pair<String, Integer> pair = new Pair<>(null, FIRST_NUMBER);
      Object[] array = pair.toArray();

      assertEquals(2, array.length);
      assertNull(array[0]);
      assertEquals(FIRST_NUMBER, array[1]);
    }
  }

  @Nested
  @DisplayName("String Representation")
  class StringRepresentationTests {

    @Test
    @DisplayName("Should provide correct string representation")
    void shouldProvideCorrectStringRepresentation() {
      Pair<String, Integer> pair = new Pair<>(FIRST_VALUE, FIRST_NUMBER);
      String expected = "(" + FIRST_VALUE + ", " + FIRST_NUMBER + ")";
      assertEquals(expected, pair.toString());
    }

    @Test
    @DisplayName("Should handle null values in string representation")
    void shouldHandleNullValuesInStringRepresentation() {
      Pair<String, Integer> pair = new Pair<>(null, FIRST_NUMBER);
      String expected = "(null, " + FIRST_NUMBER + ")";
      assertEquals(expected, pair.toString());
    }

    @Test
    @DisplayName("Should handle all null values in string representation")
    void shouldHandleAllNullValuesInStringRepresentation() {
      Pair<String, Integer> pair = new Pair<>(null, null);
      assertEquals("(null, null)", pair.toString());
    }
  }

  @Nested
  @DisplayName("Edge Cases and Integration")
  class EdgeCasesTests {

    @Test
    @DisplayName("Should handle method chaining")
    void shouldHandleMethodChaining() {
      Pair<Integer, String> result =
          new Pair<>(FIRST_VALUE, FIRST_NUMBER)
              .withFirst(SECOND_VALUE)
              .mapSecond(n -> n * 2)
              .swap()
              .withSecond("transformed");

      assertEquals(SECOND_NUMBER, result.first());
      assertEquals("transformed", result.second());
    }

    @Test
    @DisplayName("Should handle complex transformations")
    void shouldHandleComplexTransformations() {
      Pair<String, Integer> original = new Pair<>("hello", 42);
      Pair<Integer, String> transformed = original.mapBoth(String::length, n -> "Number: " + n);
      Pair<String, Integer> result = transformed.swap();

      assertEquals("Number: 42", result.first());
      assertEquals(5, result.second());
    }

    @Test
    @DisplayName("Should maintain immutability contract")
    void shouldMaintainImmutabilityContract() {
      Pair<String, Integer> original = new Pair<>(FIRST_VALUE, FIRST_NUMBER);

      // All operations should return new instances
      assertNotSame(original, original.withFirst(SECOND_VALUE));
      assertNotSame(original, original.withSecond(SECOND_NUMBER));
      assertNotSame(original, original.mapFirst(String::toUpperCase));
      assertNotSame(original, original.mapSecond(n -> n * 2));
      assertNotSame(original, original.swap());

      // Original should remain unchanged
      assertEquals(FIRST_VALUE, original.first());
      assertEquals(FIRST_NUMBER, original.second());
    }
  }
}
