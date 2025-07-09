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

import java.util.Objects;
import java.util.function.Function;

/**
 * A generic record Pair that holds two values: first and second. This class is designed to
 * encapsulate two objects of potentially different types, providing a simple tuple-like structure.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * Pair<String, Integer> pair = Pair.of("hello", 42);
 * String greeting = pair.first();
 * Integer number = pair.second();
 *
 * // Transform values
 * Pair<String, String> transformed = pair.mapSecond(String::valueOf);
 *
 * // Swap elements
 * Pair<Integer, String> swapped = pair.swap();
 * }</pre>
 *
 * @param <K> the type of the first element in the pair
 * @param <V> the type of the second element in the pair
 * @param first the first value in the pair
 * @param second the second value in the pair
 */
public record Pair<K, V>(K first, V second) {

  /**
   * Creates a new Pair with the specified first and second values.
   *
   * @param <K> the type of the first element
   * @param <V> the type of the second element
   * @param first the first value
   * @param second the second value
   * @return a new Pair containing the specified values
   */
  public static <K, V> Pair<K, V> of(K first, V second) {
    return new Pair<>(first, second);
  }

  /**
   * Creates a new Pair with both values set to null.
   *
   * @param <K> the type of the first element
   * @param <V> the type of the second element
   * @return a new Pair with both values null
   */
  public static <K, V> Pair<K, V> empty() {
    return new Pair<>(null, null);
  }

  /**
   * Creates a new Pair with the same value for both first and second elements.
   *
   * @param <T> the type of both elements
   * @param value the value to use for both elements
   * @return a new Pair with the same value in both positions
   */
  public static <T> Pair<T, T> same(T value) {
    return new Pair<>(value, value);
  }

  /**
   * Creates a new Pair with the first and second values swapped.
   *
   * @return a new Pair with swapped values
   */
  public Pair<V, K> swap() {
    return new Pair<>(second, first);
  }

  /**
   * Creates a new Pair with the first value replaced by the specified value.
   *
   * @param <T> the type of the new first value
   * @param newFirst the new first value
   * @return a new Pair with the updated first value
   */
  public <T> Pair<T, V> withFirst(T newFirst) {
    return new Pair<>(newFirst, second);
  }

  /**
   * Creates a new Pair with the second value replaced by the specified value.
   *
   * @param <T> the type of the new second value
   * @param newSecond the new second value
   * @return a new Pair with the updated second value
   */
  public <T> Pair<K, T> withSecond(T newSecond) {
    return new Pair<>(first, newSecond);
  }

  /**
   * Creates a new Pair with the first value transformed by the given function.
   *
   * @param <T> the type of the transformed first value
   * @param mapper the function to transform the first value
   * @return a new Pair with the transformed first value
   * @throws NullPointerException if mapper is null
   */
  public <T> Pair<T, V> mapFirst(Function<? super K, ? extends T> mapper) {
    Objects.requireNonNull(mapper, "Mapper function cannot be null");
    return new Pair<>(mapper.apply(first), second);
  }

  /**
   * Creates a new Pair with the second value transformed by the given function.
   *
   * @param <T> the type of the transformed second value
   * @param mapper the function to transform the second value
   * @return a new Pair with the transformed second value
   * @throws NullPointerException if mapper is null
   */
  public <T> Pair<K, T> mapSecond(Function<? super V, ? extends T> mapper) {
    Objects.requireNonNull(mapper, "Mapper function cannot be null");
    return new Pair<>(first, mapper.apply(second));
  }

  /**
   * Creates a new Pair with both values transformed by the given functions.
   *
   * @param <T> the type of the transformed first value
   * @param <U> the type of the transformed second value
   * @param firstMapper the function to transform the first value
   * @param secondMapper the function to transform the second value
   * @return a new Pair with both values transformed
   * @throws NullPointerException if either mapper is null
   */
  public <T, U> Pair<T, U> mapBoth(
      Function<? super K, ? extends T> firstMapper, Function<? super V, ? extends U> secondMapper) {
    Objects.requireNonNull(firstMapper, "First mapper function cannot be null");
    Objects.requireNonNull(secondMapper, "Second mapper function cannot be null");
    return new Pair<>(firstMapper.apply(first), secondMapper.apply(second));
  }

  /**
   * Checks if either the first or second value is null.
   *
   * @return true if either value is null, false otherwise
   */
  public boolean hasNull() {
    return first == null || second == null;
  }

  /**
   * Checks if both the first and second values are null.
   *
   * @return true if both values are null, false otherwise
   */
  public boolean isAllNull() {
    return first == null && second == null;
  }

  /**
   * Checks if both the first and second values are non-null.
   *
   * @return true if both values are non-null, false otherwise
   */
  public boolean isAllNonNull() {
    return first != null && second != null;
  }

  /**
   * Converts this Pair to an array containing both values.
   *
   * @return an array containing the first and second values
   */
  public Object[] toArray() {
    return new Object[] {first, second};
  }

  /**
   * Returns a string representation of this Pair in the format "(first, second)".
   *
   * @return a string representation of this Pair
   */
  @Override
  public String toString() {
    return "(" + first + ", " + second + ")";
  }
}
