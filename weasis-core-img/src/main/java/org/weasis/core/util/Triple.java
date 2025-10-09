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

import java.io.Serializable;
import java.util.Objects;
import java.util.function.Function;

/**
 * A generic record Triple that holds three values: first, second, and third. This immutable class
 * is designed to encapsulate three objects of potentially different types, providing a simple
 * tuple-like structure.
 *
 * <p>This record provides:
 *
 * <ul>
 *   <li>Immutable storage of three values
 *   <li>Null-safe operations
 *   <li>Functional transformation methods
 *   <li>Convenient factory methods
 *   <li>String representation for debugging
 * </ul>
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * Triple<String, Integer, Boolean> triple = Triple.of("hello", 42, true);
 * Triple<String, Integer, Boolean> transformed = triple.mapSecond(x -> x * 2);
 * }</pre>
 *
 * @param <K> the type of the first element in the triple
 * @param <V> the type of the second element in the triple
 * @param <T> the type of the third element in the triple
 * @param first the first value in the triple
 * @param second the second value in the triple
 * @param third the third value in the triple
 */
public record Triple<K, V, T>(K first, V second, T third) implements Serializable {

  /**
   * Creates a new Triple with the specified values.
   *
   * @param <K> the type of the first element
   * @param <V> the type of the second element
   * @param <T> the type of the third element
   * @param first the first value
   * @param second the second value
   * @param third the third value
   * @return a new Triple instance
   */
  public static <K, V, T> Triple<K, V, T> of(K first, V second, T third) {
    return new Triple<>(first, second, third);
  }

  /**
   * Creates a new Triple with all null values.
   *
   * @param <K> the type of the first element
   * @param <V> the type of the second element
   * @param <T> the type of the third element
   * @return a new Triple instance with all null values
   */
  public static <K, V, T> Triple<K, V, T> empty() {
    return new Triple<>(null, null, null);
  }

  /**
   * Creates a new Triple with the same first and second values, but a different third value.
   *
   * @param <U> the type of the new third element
   * @param newThird the new third value
   * @return a new Triple instance
   */
  public <U> Triple<K, V, U> withThird(U newThird) {
    return new Triple<>(first, second, newThird);
  }

  /**
   * Creates a new Triple with the same first and third values, but a different second value.
   *
   * @param <U> the type of the new second element
   * @param newSecond the new second value
   * @return a new Triple instance
   */
  public <U> Triple<K, U, T> withSecond(U newSecond) {
    return new Triple<>(first, newSecond, third);
  }

  /**
   * Creates a new Triple with the same second and third values, but a different first value.
   *
   * @param <U> the type of the new first element
   * @param newFirst the new first value
   * @return a new Triple instance
   */
  public <U> Triple<U, V, T> withFirst(U newFirst) {
    return new Triple<>(newFirst, second, third);
  }

  /**
   * Transforms the first element using the provided function.
   *
   * @param <U> the type of the transformed first element
   * @param mapper the function to transform the first element
   * @return a new Triple instance with the transformed first element
   * @throws NullPointerException if mapper is null
   */
  public <U> Triple<U, V, T> mapFirst(Function<? super K, ? extends U> mapper) {
    Objects.requireNonNull(mapper);
    return new Triple<>(mapper.apply(first), second, third);
  }

  /**
   * Transforms the second element using the provided function.
   *
   * @param <U> the type of the transformed second element
   * @param mapper the function to transform the second element
   * @return a new Triple instance with the transformed second element
   * @throws NullPointerException if mapper is null
   */
  public <U> Triple<K, U, T> mapSecond(Function<? super V, ? extends U> mapper) {
    Objects.requireNonNull(mapper);
    return new Triple<>(first, mapper.apply(second), third);
  }

  /**
   * Transforms the third element using the provided function.
   *
   * @param <U> the type of the transformed third element
   * @param mapper the function to transform the third element
   * @return a new Triple instance with the transformed third element
   * @throws NullPointerException if mapper is null
   */
  public <U> Triple<K, V, U> mapThird(Function<? super T, ? extends U> mapper) {
    Objects.requireNonNull(mapper);
    return new Triple<>(first, second, mapper.apply(third));
  }

  /**
   * Transforms all elements using the provided functions.
   *
   * @param <U> the type of the transformed first element
   * @param <W> the type of the transformed second element
   * @param <X> the type of the transformed third element
   * @param firstMapper the function to transform the first element
   * @param secondMapper the function to transform the second element
   * @param thirdMapper the function to transform the third element
   * @return a new Triple instance with all transformed elements
   * @throws NullPointerException if any mapper is null
   */
  public <U, W, X> Triple<U, W, X> mapAll(
      Function<? super K, ? extends U> firstMapper,
      Function<? super V, ? extends W> secondMapper,
      Function<? super T, ? extends X> thirdMapper) {
    return new Triple<>(
        Objects.requireNonNull(firstMapper).apply(first),
        Objects.requireNonNull(secondMapper).apply(second),
        Objects.requireNonNull(thirdMapper).apply(third));
  }

  /**
   * Checks if any of the elements is null.
   *
   * @return true if any element is null, false otherwise
   */
  public boolean hasNull() {
    return first == null || second == null || third == null;
  }

  /**
   * Checks if all elements are null.
   *
   * @return true if all elements are null, false otherwise
   */
  public boolean isAllNull() {
    return first == null && second == null && third == null;
  }

  /**
   * Checks if all elements are non-null.
   *
   * @return true if all elements are non-null, false otherwise
   */
  public boolean isAllNonNull() {
    return first != null && second != null && third != null;
  }

  /**
   * Swaps the first and second elements.
   *
   * @return a new Triple instance with first and second elements swapped
   */
  public Triple<V, K, T> swapFirstSecond() {
    return new Triple<>(second, first, third);
  }

  /**
   * Swaps the first and third elements.
   *
   * @return a new Triple instance with first and third elements swapped
   */
  public Triple<T, V, K> swapFirstThird() {
    return new Triple<>(third, second, first);
  }

  /**
   * Swaps the second and third elements.
   *
   * @return a new Triple instance with second and third elements swapped
   */
  public Triple<K, T, V> swapSecondThird() {
    return new Triple<>(first, third, second);
  }

  /**
   * Converts this Triple to an array containing all three elements. Note: The returned array has
   * Object type due to type erasure.
   *
   * @return an array containing [first, second, third]
   */
  public Object[] toArray() {
    return new Object[] {first, second, third};
  }

  @Override
  public String toString() {
    return "(%s, %s, %s)".formatted(first, second, third);
  }
}
