/*
 * Copyright (c) 2020 Weasis Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.weasis.core.util;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("LangUtil Tests")
@DisplayNameGeneration(ReplaceUnderscores.class)
class LangUtilTest {

  // Test data records for structured test cases
  record BooleanConversionTestData(
      Boolean input, boolean expectedNullToFalse, boolean expectedNullToTrue) {}

  record StringBooleanTestData(
      String input, boolean expectedEmptyToFalse, boolean expectedEmptyToTrue) {}

  record OptionalTestData<T>(T input, boolean shouldBePresent, T expectedValue) {}

  record FirstNonNullTestData<T>(T[] inputs, T expected, String description) {}

  // ================= Iterable Null Safety Tests =================

  @Nested
  class Empty_If_Null_Tests {

    static Stream<Arguments> iterable_test_cases() {
      return Stream.of(
          Arguments.of(null, true, "null input should return empty iterable"),
          Arguments.of(List.of(), false, "empty list should return itself"),
          Arguments.of(List.of("a", "b", "c"), false, "non-empty list should return itself"),
          Arguments.of(Set.of(1, 2, 3), false, "non-empty set should return itself"),
          Arguments.of(
              Collections.emptyList(), false, "Collections.emptyList() should return itself"),
          Arguments.of(
              Collections.singletonList("single"), false, "singleton list should return itself"));
    }

    @ParameterizedTest
    @MethodSource("iterable_test_cases")
    void iterable_null_safety_handling(
        Collection<?> input, boolean shouldBeEmpty, String description) {
      var result = LangUtil.emptyIfNull(input);

      assertNotNull(result, "Result should never be null");

      if (shouldBeEmpty) {
        assertFalse(result.iterator().hasNext(), "Should return empty iterable when input is null");
        assertTrue(result.isEmpty(), "Should return empty collection");
      } else {
        assertSame(
            input, result, "Should return same iterable when input is not null: " + description);
      }
    }

    @Test
    void empty_iterable_behavior_verification() {
      var result = LangUtil.emptyIfNull(null);

      assertAll(
          "Empty iterable properties",
          () -> assertNotNull(result),
          () -> assertFalse(result.iterator().hasNext()),
          () -> assertTrue(result instanceof Collection),
          () -> assertTrue(((Collection<?>) result).isEmpty()));
    }

    @Test
    void different_iterable_types_preservation() {
      var list = List.of("item1", "item2");
      var set = Set.of("item1", "item2");
      var queue = new ArrayDeque<>(List.of("item1", "item2"));

      assertAll(
          "Different iterable types should be preserved",
          () -> assertSame(list, LangUtil.emptyIfNull(list)),
          () -> assertSame(set, LangUtil.emptyIfNull(set)),
          () -> assertSame(queue, LangUtil.emptyIfNull(queue)));
    }
  }

  // ================= Memoization Tests =================

  @Nested
  class Memoize_Tests {

    record ComputationResult(String value, int computationCount) {}

    static Stream<Arguments> memoization_test_cases() {
      return Stream.of(
          Arguments.of("constant_value", 5, "constant value with multiple calls"),
          Arguments.of(null, 3, "null value with multiple calls"),
          Arguments.of("", 2, "empty string with multiple calls"));
    }

    @Test
    void null_supplier_should_throw_exception() {
      var exception = assertThrows(NullPointerException.class, () -> LangUtil.memoize(null));
      assertEquals("Supplier cannot be null", exception.getMessage());
    }

    @ParameterizedTest
    @MethodSource("memoization_test_cases")
    void memoized_supplier_caches_result(String expectedValue, int callCount, String description) {
      var computationCounter = new AtomicInteger(0);

      Supplier<String> originalSupplier =
          () -> {
            computationCounter.incrementAndGet();
            return expectedValue;
          };

      var memoizedSupplier = LangUtil.memoize(originalSupplier);

      // Make multiple calls
      var results = new ArrayList<String>();
      for (int i = 0; i < callCount; i++) {
        results.add(memoizedSupplier.get());
      }

      assertAll(
          description,
          () ->
              assertEquals(
                  1, computationCounter.get(), "Original supplier should be called only once"),
          () -> assertEquals(callCount, results.size(), "Should have correct number of results"),
          () ->
              assertTrue(
                  results.stream().allMatch(r -> Objects.equals(r, expectedValue)),
                  "All results should be equal"),
          () -> {
            if (expectedValue != null) {
              assertTrue(
                  results.stream().allMatch(r -> r == results.get(0)),
                  "All results should be same instance (cached)");
            }
          });
    }

    @Test
    @Timeout(value = 5)
    void memoized_supplier_is_thread_safe() throws InterruptedException, ExecutionException {
      var computationCounter = new AtomicInteger(0);
      var latch = new CountDownLatch(1);
      var threadCount = 10;

      Supplier<String> expensiveSupplier =
          () -> {
            try {
              latch.await();
              Thread.sleep(10); // Simulate expensive computation
              return "computed_value_" + computationCounter.incrementAndGet();
            } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
              return "interrupted";
            }
          };

      var memoizedSupplier = LangUtil.memoize(expensiveSupplier);
      var executor = Executors.newFixedThreadPool(threadCount);
      var results = Collections.synchronizedList(new ArrayList<String>());
      var futures = new ArrayList<Future<Void>>();

      // Submit concurrent tasks
      for (int i = 0; i < threadCount; i++) {
        futures.add(
            executor.submit(
                () -> {
                  results.add(memoizedSupplier.get());
                  return null;
                }));
      }

      // Release all threads simultaneously
      latch.countDown();

      // Wait for completion
      for (var future : futures) {
        future.get();
      }

      executor.shutdown();
      assertTrue(executor.awaitTermination(1, TimeUnit.SECONDS));

      assertAll(
          "Thread safety verification",
          () -> assertEquals(threadCount, results.size(), "All threads should complete"),
          () -> assertEquals(1, computationCounter.get(), "Computation should happen only once"),
          () -> {
            var firstResult = results.get(0);
            assertTrue(
                results.stream().allMatch(firstResult::equals), "All results should be equal");
          });
    }

    @Test
    void memoization_handles_exceptions_correctly() {
      var callCount = new AtomicInteger(0);

      Supplier<String> failingSupplier =
          () -> {
            callCount.incrementAndGet();
            throw new RuntimeException("Computation failed");
          };

      var memoizedSupplier = LangUtil.memoize(failingSupplier);

      // First call should throw exception
      assertThrows(RuntimeException.class, memoizedSupplier::get);
      assertEquals(1, callCount.get());

      // Subsequent calls should also throw, but supplier shouldn't be called again
      assertNull(memoizedSupplier.get());
    }
  }

  // ================= Boolean Conversion Tests =================

  @Nested
  class Boolean_Conversion_Tests {

    static Stream<BooleanConversionTestData> boolean_conversion_test_data() {
      return Stream.of(
          new BooleanConversionTestData(null, false, true),
          new BooleanConversionTestData(Boolean.TRUE, true, true),
          new BooleanConversionTestData(Boolean.FALSE, false, false),
          new BooleanConversionTestData(true, true, true),
          new BooleanConversionTestData(false, false, false));
    }

    @ParameterizedTest
    @MethodSource("boolean_conversion_test_data")
    void null_to_false_conversions(BooleanConversionTestData testData) {
      assertEquals(
          testData.expectedNullToFalse(),
          LangUtil.nullToFalse(testData.input()),
          () -> "nullToFalse failed for input: " + testData.input());
    }

    @ParameterizedTest
    @MethodSource("boolean_conversion_test_data")
    void null_to_true_conversions(BooleanConversionTestData testData) {
      assertEquals(
          testData.expectedNullToTrue(),
          LangUtil.nullToTrue(testData.input()),
          () -> "nullToTrue failed for input: " + testData.input());
    }

    @Test
    void boolean_conversion_logic_verification() {
      assertAll(
          "Boolean conversion logic",
          () -> assertTrue(LangUtil.nullToFalse(Boolean.TRUE)),
          () -> assertFalse(LangUtil.nullToFalse(Boolean.FALSE)),
          () -> assertFalse(LangUtil.nullToFalse(null)),
          () -> assertTrue(LangUtil.nullToTrue(Boolean.TRUE)),
          () -> assertFalse(LangUtil.nullToTrue(Boolean.FALSE)),
          () -> assertTrue(LangUtil.nullToTrue(null)));
    }
  }

  // ================= String Boolean Conversion Tests =================

  @Nested
  class String_Boolean_Conversion_Tests {

    static Stream<StringBooleanTestData> string_boolean_test_data() {
      return Stream.of(
          new StringBooleanTestData(null, false, true),
          new StringBooleanTestData("", false, true),
          new StringBooleanTestData("   ", false, true),
          new StringBooleanTestData("\t\n\r", false, true),
          new StringBooleanTestData("true", true, true),
          new StringBooleanTestData("TRUE", true, true),
          new StringBooleanTestData("True", true, true),
          new StringBooleanTestData("false", false, false),
          new StringBooleanTestData("FALSE", false, false),
          new StringBooleanTestData("False", false, false),
          new StringBooleanTestData("yes", false, false),
          new StringBooleanTestData("no", false, false),
          new StringBooleanTestData("1", false, false),
          new StringBooleanTestData("0", false, false),
          new StringBooleanTestData("random", false, false));
    }

    @ParameterizedTest
    @MethodSource("string_boolean_test_data")
    void empty_to_false_conversions(StringBooleanTestData testData) {
      assertEquals(
          testData.expectedEmptyToFalse(),
          LangUtil.emptyToFalse(testData.input()),
          () -> "emptyToFalse failed for input: '" + testData.input() + "'");
    }

    @ParameterizedTest
    @MethodSource("string_boolean_test_data")
    void empty_to_true_conversions(StringBooleanTestData testData) {
      assertEquals(
          testData.expectedEmptyToTrue(),
          LangUtil.emptyToTrue(testData.input()),
          () -> "emptyToTrue failed for input: '" + testData.input() + "'");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n", "\r", "  \t\n\r  "})
    void empty_strings_should_return_expected_defaults(String input) {
      assertAll(
          "Empty string handling",
          () ->
              assertFalse(
                  LangUtil.emptyToFalse(input),
                  "emptyToFalse should return false for empty: '" + input + "'"),
          () ->
              assertTrue(
                  LangUtil.emptyToTrue(input),
                  "emptyToTrue should return true for empty: '" + input + "'"));
    }
  }

  // ================= Optional Conversion Tests =================

  @Nested
  class Optional_Conversion_Tests {

    static Stream<OptionalTestData<Double>> double_optional_test_data() {
      return Stream.of(
          new OptionalTestData<>(null, false, null),
          new OptionalTestData<>(0.0, true, 0.0),
          new OptionalTestData<>(42.5, true, 42.5),
          new OptionalTestData<>(-123.456, true, -123.456),
          new OptionalTestData<>(Double.MAX_VALUE, true, Double.MAX_VALUE),
          new OptionalTestData<>(Double.MIN_VALUE, true, Double.MIN_VALUE),
          new OptionalTestData<>(Double.POSITIVE_INFINITY, true, Double.POSITIVE_INFINITY),
          new OptionalTestData<>(Double.NEGATIVE_INFINITY, true, Double.NEGATIVE_INFINITY),
          new OptionalTestData<>(Double.NaN, true, Double.NaN));
    }

    static Stream<OptionalTestData<Integer>> integer_optional_test_data() {
      return Stream.of(
          new OptionalTestData<>(null, false, null),
          new OptionalTestData<>(0, true, 0),
          new OptionalTestData<>(123, true, 123),
          new OptionalTestData<>(-456, true, -456),
          new OptionalTestData<>(Integer.MAX_VALUE, true, Integer.MAX_VALUE),
          new OptionalTestData<>(Integer.MIN_VALUE, true, Integer.MIN_VALUE));
    }

    static Stream<OptionalTestData<String>> object_optional_test_data() {
      return Stream.of(
          new OptionalTestData<>(null, false, null),
          new OptionalTestData<>("", true, ""),
          new OptionalTestData<>("test", true, "test"),
          new OptionalTestData<>("   ", true, "   "));
    }

    @ParameterizedTest
    @MethodSource("double_optional_test_data")
    void double_to_optional_conversions(OptionalTestData<Double> testData) {
      var result = LangUtil.toOptional(testData.input());

      assertEquals(
          testData.shouldBePresent(),
          result.isPresent(),
          () -> "Optional presence mismatch for input: " + testData.input());

      if (testData.shouldBePresent()) {
        if (Double.isNaN(testData.expectedValue())) {
          assertTrue(Double.isNaN(result.getAsDouble()), "NaN value should be preserved");
        } else {
          assertEquals(
              testData.expectedValue(),
              result.getAsDouble(),
              () -> "Optional value mismatch for input: " + testData.input());
        }
      }
    }

    @ParameterizedTest
    @MethodSource("integer_optional_test_data")
    void integer_to_optional_conversions(OptionalTestData<Integer> testData) {
      var result = LangUtil.toOptional(testData.input());

      assertEquals(
          testData.shouldBePresent(),
          result.isPresent(),
          () -> "Optional presence mismatch for input: " + testData.input());

      if (testData.shouldBePresent()) {
        assertEquals(
            testData.expectedValue().intValue(),
            result.getAsInt(),
            () -> "Optional value mismatch for input: " + testData.input());
      }
    }

    @ParameterizedTest
    @MethodSource("object_optional_test_data")
    void object_to_optional_conversions(OptionalTestData<String> testData) {
      var result = LangUtil.toOptional(testData.input());

      assertEquals(
          testData.shouldBePresent(),
          result.isPresent(),
          () -> "Optional presence mismatch for input: '" + testData.input() + "'");

      if (testData.shouldBePresent()) {
        assertEquals(
            testData.expectedValue(),
            result.get(),
            () -> "Optional value mismatch for input: '" + testData.input() + "'");
      }
    }

    @Test
    void optional_conversions_type_safety() {
      // Test different types to ensure type safety
      var stringOptional = LangUtil.toOptional("test");
      var listOptional = LangUtil.toOptional(List.of(1, 2, 3));
      var nullListOptional = LangUtil.toOptional((List<Integer>) null);

      assertAll(
          "Type safety verification",
          () -> assertTrue(stringOptional.isPresent()),
          () -> assertEquals("test", stringOptional.get()),
          () -> assertTrue(listOptional.isPresent()),
          () -> assertEquals(3, listOptional.get().size()),
          () -> assertFalse(nullListOptional.isPresent()));
    }
  }

  // ================= First Non-Null Tests =================

  @Nested
  class First_Non_Null_Tests {

    @SafeVarargs
    static <T> Stream<FirstNonNullTestData<T>> firstNonNullTestData(
        FirstNonNullTestData<T>... data) {
      return Stream.of(data);
    }

    static Stream<FirstNonNullTestData<String>> string_first_non_null_test_data() {
      return firstNonNullTestData(
          new FirstNonNullTestData<>(new String[] {null, null, null}, null, "all null values"),
          new FirstNonNullTestData<>(
              new String[] {"first", "second", "third"}, "first", "first is non-null"),
          new FirstNonNullTestData<>(
              new String[] {null, "second", "third"}, "second", "second is first non-null"),
          new FirstNonNullTestData<>(
              new String[] {null, null, "third"}, "third", "third is first non-null"),
          new FirstNonNullTestData<>(new String[] {"only"}, "only", "single non-null value"),
          new FirstNonNullTestData<>(new String[] {null}, null, "single null value"),
          new FirstNonNullTestData<>(new String[] {}, null, "empty array"),
          new FirstNonNullTestData<>(
              new String[] {"", null, "non-empty"}, "", "empty string is not null"),
          new FirstNonNullTestData<>(
              new String[] {null, "", "non-empty"}, "", "empty string before non-empty"));
    }

    static Stream<FirstNonNullTestData<Integer>> integer_first_non_null_test_data() {
      return firstNonNullTestData(
          new FirstNonNullTestData<>(new Integer[] {null, null, null}, null, "all null integers"),
          new FirstNonNullTestData<>(new Integer[] {1, 2, 3}, 1, "first integer is non-null"),
          new FirstNonNullTestData<>(new Integer[] {null, 0, 3}, 0, "zero is first non-null"),
          new FirstNonNullTestData<>(
              new Integer[] {null, -1, 3}, -1, "negative number is first non-null"));
    }

    @ParameterizedTest
    @MethodSource("string_first_non_null_test_data")
    void first_non_null_string_operations(FirstNonNullTestData<String> testData) {
      var result = LangUtil.firstNonNull(testData.inputs());
      assertEquals(testData.expected(), result, testData.description());
    }

    @ParameterizedTest
    @MethodSource("integer_first_non_null_test_data")
    void first_non_null_integer_operations(FirstNonNullTestData<Integer> testData) {
      var result = LangUtil.firstNonNull(testData.inputs());
      assertEquals(testData.expected(), result, testData.description());
    }

    @Test
    void first_non_null_with_null_array_should_return_null() {
      assertNull(LangUtil.firstNonNull((String[]) null));
    }

    @Test
    void first_non_null_with_mixed_types() {
      // Test with different object types
      var list = List.of("item");
      var set = Set.of("item");

      assertSame(list, LangUtil.firstNonNull(null, list, set));
      assertSame(set, LangUtil.firstNonNull(null, null, set));
    }

    @Test
    void first_non_null_performance_with_large_arrays() {
      var largeArray = new String[10000];
      Arrays.fill(largeArray, null);
      largeArray[9999] = "found";

      assertEquals("found", LangUtil.firstNonNull(largeArray));
    }
  }

  // ================= Default If Null Tests =================

  @Nested
  class Default_If_Null_Tests {

    @ParameterizedTest
    @CsvSource({
      "'original', 'default', 'original'",
      "'', 'default', ''",
      "'   ', 'default', '   '"
    })
    void non_null_values_should_be_returned(String value, String defaultValue, String expected) {
      assertEquals(expected, LangUtil.defaultIfNull(value, defaultValue));
    }

    @ParameterizedTest
    @NullSource
    void null_values_should_return_default(String value) {
      var defaultValue = "default_value";
      assertEquals(defaultValue, LangUtil.defaultIfNull(value, defaultValue));
    }

    @Test
    void default_if_null_with_different_types() {
      var defaultList = List.of("default");
      var originalList = List.of("original");

      assertAll(
          "Different types handling",
          () -> assertSame(originalList, LangUtil.defaultIfNull(originalList, defaultList)),
          () -> assertSame(defaultList, LangUtil.defaultIfNull(null, defaultList)),
          () -> assertEquals(42, LangUtil.defaultIfNull(null, 42)),
          () -> assertEquals(100, LangUtil.defaultIfNull(100, 42)));
    }

    @Test
    void default_if_null_with_null_default() {
      assertNull(LangUtil.defaultIfNull(null, null));
      assertEquals("value", LangUtil.defaultIfNull("value", null));
    }

    @Test
    void default_if_null_preserves_object_identity() {
      var original = new Object();
      var defaultObj = new Object();

      assertSame(original, LangUtil.defaultIfNull(original, defaultObj));
      assertSame(defaultObj, LangUtil.defaultIfNull(null, defaultObj));
    }
  }

  // ================= Integration and Edge Cases Tests =================

  @Nested
  class Integration_And_Edge_Cases_Tests {

    @Test
    void combined_operations_integration() {
      // Test combining multiple LangUtil operations
      var nullIterable = LangUtil.emptyIfNull(null);
      var firstNonNull = LangUtil.firstNonNull(null, "found", "backup");
      var defaultIfNull = LangUtil.defaultIfNull(null, "default");
      var memoized = LangUtil.memoize(() -> "expensive_computation");

      assertAll(
          "Combined operations",
          () -> assertFalse(nullIterable.iterator().hasNext()),
          () -> assertEquals("found", firstNonNull),
          () -> assertEquals("default", defaultIfNull),
          () -> assertEquals("expensive_computation", memoized.get()));
    }

    @Test
    void edge_cases_with_special_values() {
      assertAll(
          "Special value handling",
          () -> assertTrue(LangUtil.emptyToTrue("TRUE")),
          () -> assertFalse(LangUtil.emptyToTrue("FALSE")),
          () -> assertTrue(LangUtil.nullToTrue(null)),
          () -> assertFalse(LangUtil.nullToFalse(null)),
          () -> assertTrue(LangUtil.toOptional(Double.NaN).isPresent()),
          () -> assertTrue(Double.isNaN(LangUtil.toOptional(Double.NaN).getAsDouble())));
    }

    @Test
    void memory_and_performance_considerations() {
      // Test that memoization doesn't cause memory leaks in typical usage
      var results = new ArrayList<String>();
      for (int i = 0; i < 1000; i++) {
        var memoized = LangUtil.memoize(() -> "value_" + System.currentTimeMillis());
        results.add(memoized.get());
        results.add(memoized.get()); // Second call should use cached value
      }

      assertEquals(2000, results.size());
      // Verify that memoization worked (every pair should be equal)
      for (int i = 0; i < results.size(); i += 2) {
        assertEquals(results.get(i), results.get(i + 1));
      }
    }

    @Test
    void supplier_exception_propagation() {
      var memoizedSupplier =
          LangUtil.memoize(
              () -> {
                throw new IllegalStateException("Test exception");
              });

      // Exception should be propagated on each call
      assertThrows(IllegalStateException.class, memoizedSupplier::get);
      // Subsequent calls should also throw, but supplier shouldn't be called again'
      assertNull(memoizedSupplier.get());
    }
  }
}
