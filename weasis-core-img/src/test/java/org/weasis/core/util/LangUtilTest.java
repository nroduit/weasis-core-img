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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class LangUtilTest {

  @Nested
  @DisplayName("emptyIfNull tests")
  class EmptyIfNullTests {

    @Test
    @DisplayName("Should return empty list when input is null")
    void testEmptyIfNull_withNull() {
      Iterable<Object> result = LangUtil.emptyIfNull(null);
      assertNotNull(result);
      assertTrue(((Collection<Object>) result).isEmpty());
      assertFalse(result.iterator().hasNext());
    }

    @Test
    @DisplayName("Should return same iterable when input is not null")
    void testEmptyIfNull_withValidIterable() {
      List<String> original = Arrays.asList("a", "b", "c");
      Iterable<String> result = LangUtil.emptyIfNull(original);
      assertSame(original, result);
    }

    @Test
    @DisplayName("Should return empty list when input is empty list")
    void testEmptyIfNull_withEmptyList() {
      List<String> emptyList = Collections.emptyList();
      Iterable<String> result = LangUtil.emptyIfNull(emptyList);
      assertSame(emptyList, result);
    }
  }

  @Nested
  @DisplayName("memoize tests")
  class MemoizeTests {

    @Test
    @DisplayName("Should throw NullPointerException for null supplier")
    void testMemoize_withNullSupplier() {
      assertThrows(NullPointerException.class, () -> LangUtil.memoize(null));
    }

    @Test
    @DisplayName("Should cache result after first call")
    void testMemoize_cachesResult() {
      AtomicInteger callCount = new AtomicInteger(0);
      String expectedValue = "cached_value";

      Supplier<String> original =
          () -> {
            callCount.incrementAndGet();
            return expectedValue;
          };

      Supplier<String> memoized = LangUtil.memoize(original);

      // First call should invoke original supplier
      String result1 = memoized.get();
      assertEquals(expectedValue, result1);
      assertEquals(1, callCount.get());

      // Second call should return cached result
      String result2 = memoized.get();
      assertEquals(expectedValue, result2);
      assertEquals(1, callCount.get()); // Should not increment

      assertSame(result1, result2);
    }

    @Test
    @DisplayName("Should handle null return value from supplier")
    void testMemoize_withNullReturnValue() {
      Supplier<String> nullSupplier = () -> null;
      Supplier<String> memoized = LangUtil.memoize(nullSupplier);

      assertNull(memoized.get());
      assertNull(memoized.get()); // Second call should also return null
    }

    @Test
    @DisplayName("Should be thread-safe")
    void testMemoize_threadSafety() throws InterruptedException {
      AtomicInteger callCount = new AtomicInteger(0);
      CountDownLatch latch = new CountDownLatch(1);

      Supplier<String> original =
          () -> {
            try {
              latch.await(); // Wait for all threads to be ready
            } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
            }
            return "value_" + callCount.incrementAndGet();
          };

      Supplier<String> memoized = LangUtil.memoize(original);
      ExecutorService executor = Executors.newFixedThreadPool(10);
      List<String> results = Collections.synchronizedList(new ArrayList<>());

      // Submit 10 concurrent tasks
      for (int i = 0; i < 10; i++) {
        executor.submit(() -> results.add(memoized.get()));
      }

      latch.countDown(); // Release all threads
      executor.shutdown();
      assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));

      // All results should be the same and supplier should be called only once
      assertEquals(10, results.size());
      String firstResult = results.get(0);
      assertTrue(results.stream().allMatch(r -> r.equals(firstResult)));
      assertEquals(1, callCount.get());
    }
  }

  @Nested
  @DisplayName("nullToFalse tests")
  class NullToFalseTests {

    @ParameterizedTest
    @CsvSource({"true, true", "false, false"})
    @DisplayName("Should return boolean value when not null")
    void testNullToFalse_withValidBoolean(Boolean input, boolean expected) {
      assertEquals(expected, LangUtil.nullToFalse(input));
    }

    @Test
    @DisplayName("Should return false when input is null")
    void testNullToFalse_withNull() {
      assertFalse(LangUtil.nullToFalse(null));
    }
  }

  @Nested
  @DisplayName("nullToTrue tests")
  class NullToTrueTests {

    @Test
    @DisplayName("Should return true when input is null")
    void testNullToTrue_withNull() {
      assertTrue(LangUtil.nullToTrue(null));
    }

    @Test
    @DisplayName("Should return true when input is true")
    void testNullToTrue_withTrue() {
      assertTrue(LangUtil.nullToTrue(true));
    }

    @Test
    @DisplayName("Should return false when input is false")
    void testNullToTrue_withFalse() {
      assertFalse(LangUtil.nullToTrue(false));
    }
  }

  @Nested
  @DisplayName("emptyToFalse tests")
  class EmptyToFalseTests {

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    @DisplayName("Should return false for null, empty or whitespace strings")
    void testEmptyToFalse_withEmptyStrings(String input) {
      assertFalse(LangUtil.emptyToFalse(input));
    }

    @ParameterizedTest
    @CsvSource({
      "'true', true",
      "'TRUE', true",
      "'True', true",
      "'false', false",
      "'FALSE', false",
      "'False', false",
      "'yes', false",
      "'no', false",
      "'1', false",
      "'0', false"
    })
    @DisplayName("Should parse boolean values correctly")
    void testEmptyToFalse_withValidStrings(String input, boolean expected) {
      assertEquals(expected, LangUtil.emptyToFalse(input));
    }
  }

  @Nested
  @DisplayName("emptyToTrue tests")
  class EmptyToTrueTests {

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    @DisplayName("Should return true for null, empty or whitespace strings")
    void testEmptyToTrue_withEmptyStrings(String input) {
      assertTrue(LangUtil.emptyToTrue(input));
    }

    @ParameterizedTest
    @CsvSource({
      "'true', true",
      "'TRUE', true",
      "'True', true",
      "'false', false",
      "'FALSE', false",
      "'False', false"
    })
    @DisplayName("Should parse boolean values correctly")
    void testEmptyToTrue_withValidStrings(String input, boolean expected) {
      assertEquals(expected, LangUtil.emptyToTrue(input));
    }
  }

  @Nested
  @DisplayName("toOptional tests")
  class ToOptionalTests {

    @Test
    @DisplayName("Should return empty OptionalDouble for null Double")
    void testToOptional_withNullDouble() {
      OptionalDouble result = LangUtil.toOptional((Double) null);
      assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("Should return populated OptionalDouble for valid Double")
    void testToOptional_withValidDouble() {
      double value = 42.5;
      OptionalDouble result = LangUtil.toOptional(value);
      assertTrue(result.isPresent());
      assertEquals(value, result.getAsDouble());
    }

    @Test
    @DisplayName("Should return empty OptionalInt for null Integer")
    void testToOptional_withNullInteger() {
      OptionalInt result = LangUtil.toOptional((Integer) null);
      assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("Should return populated OptionalInt for valid Integer")
    void testToOptional_withValidInteger() {
      int value = 123;
      OptionalInt result = LangUtil.toOptional(value);
      assertTrue(result.isPresent());
      assertEquals(value, result.getAsInt());
    }

    @Test
    @DisplayName("Should return empty Optional for null Object")
    void testToOptional_withNullObject() {
      Optional<String> result = LangUtil.toOptional((String) null);
      assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("Should return populated Optional for valid Object")
    void testToOptional_withValidObject() {
      String value = "test";
      Optional<String> result = LangUtil.toOptional(value);
      assertTrue(result.isPresent());
      assertEquals(value, result.get());
    }
  }

  @Nested
  @DisplayName("firstNonNull tests")
  class FirstNonNullTests {

    @Test
    @DisplayName("Should return null when all arguments are null")
    void testFirstNonNull_allNull() {
      String result = LangUtil.firstNonNull(null, null, null);
      assertNull(result);
    }

    @Test
    @DisplayName("Should return null when no arguments provided")
    void testFirstNonNull_noArguments() {
      String result = LangUtil.firstNonNull();
      assertNull(result);
    }

    @Test
    @DisplayName("Should return first non-null value")
    void testFirstNonNull_mixedValues() {
      String result = LangUtil.firstNonNull(null, "first", "second", null);
      assertEquals("first", result);
    }

    @Test
    @DisplayName("Should return first argument when it's not null")
    void testFirstNonNull_firstNotNull() {
      String result = LangUtil.firstNonNull("first", "second");
      assertEquals("first", result);
    }

    @Test
    @DisplayName("Should handle different types")
    void testFirstNonNull_differentTypes() {
      Integer result = LangUtil.firstNonNull(null, 42, 24);
      assertEquals(Integer.valueOf(42), result);
    }
  }

  @Nested
  @DisplayName("defaultIfNull tests")
  class DefaultIfNullTests {

    @Test
    @DisplayName("Should return default when value is null")
    void testDefaultIfNull_withNullValue() {
      String defaultValue = "default";
      String result = LangUtil.defaultIfNull(null, defaultValue);
      assertEquals(defaultValue, result);
    }

    @Test
    @DisplayName("Should return original value when not null")
    void testDefaultIfNull_withValidValue() {
      String originalValue = "original";
      String defaultValue = "default";
      String result = LangUtil.defaultIfNull(originalValue, defaultValue);
      assertEquals(originalValue, result);
    }

    @Test
    @DisplayName("Should handle null default value")
    void testDefaultIfNull_withNullDefault() {
      String result = LangUtil.defaultIfNull(null, null);
      assertNull(result);
    }

    @Test
    @DisplayName("Should work with different types")
    void testDefaultIfNull_withDifferentTypes() {
      Integer result = LangUtil.defaultIfNull(null, 42);
      assertEquals(Integer.valueOf(42), result);

      List<String> list = Arrays.asList("a", "b");
      List<String> result2 = LangUtil.defaultIfNull(list, Collections.emptyList());
      assertSame(list, result2);
    }
  }

  @Nested
  @DisplayName("Legacy compatibility tests")
  class LegacyCompatibilityTests {

    @Test
    @DisplayName("Legacy behavior equivalence for boolean conversions")
    void testLegacyBehaviorEquivalence() {
      // Test that new methods behave the same as old ones
      assertTrue(LangUtil.nullToFalse(Boolean.TRUE));
      assertFalse(LangUtil.nullToFalse(Boolean.FALSE));
      assertFalse(LangUtil.nullToFalse(null));

      assertTrue(LangUtil.nullToTrue(Boolean.TRUE));
      assertFalse(LangUtil.nullToTrue(Boolean.FALSE));
      assertTrue(LangUtil.nullToTrue(null));

      assertTrue(LangUtil.emptyToFalse("true"));
      assertFalse(LangUtil.emptyToFalse("false"));
      assertFalse(LangUtil.emptyToFalse(null));
      assertFalse(LangUtil.emptyToFalse(""));

      assertTrue(LangUtil.emptyToTrue("true"));
      assertFalse(LangUtil.emptyToTrue("false"));
      assertTrue(LangUtil.emptyToTrue(null));
      assertTrue(LangUtil.emptyToTrue(""));
    }
  }

  @Nested
  @DisplayName("Edge cases and performance")
  class EdgeCasesAndPerformanceTests {

    @Test
    @DisplayName("Should handle edge cases gracefully")
    void testEdgeCases() {
      // Test with extreme values
      OptionalDouble maxDouble = LangUtil.toOptional(Double.MAX_VALUE);
      assertTrue(maxDouble.isPresent());
      assertEquals(Double.MAX_VALUE, maxDouble.getAsDouble());

      OptionalInt minInt = LangUtil.toOptional(Integer.MIN_VALUE);
      assertTrue(minInt.isPresent());
      assertEquals(Integer.MIN_VALUE, minInt.getAsInt());

      // Test with special double values
      OptionalDouble nanDouble = LangUtil.toOptional(Double.NaN);
      assertTrue(nanDouble.isPresent());
      assertTrue(Double.isNaN(nanDouble.getAsDouble()));

      OptionalDouble infDouble = LangUtil.toOptional(Double.POSITIVE_INFINITY);
      assertTrue(infDouble.isPresent());
      assertTrue(Double.isInfinite(infDouble.getAsDouble()));
    }

    @Test
    @DisplayName("Memoization should work with expensive computations")
    void testMemoizeWithExpensiveComputation() {
      AtomicInteger computationCount = new AtomicInteger(0);

      Supplier<Integer> expensiveComputation =
          () -> {
            computationCount.incrementAndGet();
            try {
              Thread.sleep(10); // Simulate expensive computation
            } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
            }
            return 42;
          };

      Supplier<Integer> memoized = LangUtil.memoize(expensiveComputation);

      long startTime = System.currentTimeMillis();
      Integer result1 = memoized.get();
      long firstCallTime = System.currentTimeMillis() - startTime;

      startTime = System.currentTimeMillis();
      Integer result2 = memoized.get();
      long secondCallTime = System.currentTimeMillis() - startTime;

      assertEquals(result1, result2);
      assertEquals(1, computationCount.get());
      assertTrue(secondCallTime < firstCallTime); // Second call should be faster
    }
  }
}
