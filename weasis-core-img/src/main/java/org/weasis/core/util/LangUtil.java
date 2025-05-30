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

import java.util.Collections;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.function.Supplier;

/**
 * @author Nicolas Roduit
 */
public class LangUtil {

  private LangUtil() {}

  /**
   * @param iterable the input iterable
   * @return an empty list if the input is null, otherwise the input
   */
  public static <T> Iterable<T> emptyIfNull(Iterable<T> iterable) {
    return iterable == null ? Collections.emptyList() : iterable;
  }

  /**
   * Lazily initializes and caches the result of a supplied computation. This ensures that the
   * computation is performed only once, and later calls return the cached result.
   *
   * @param <T> the type of the value supplied
   * @param original the original supplier whose result is to be memoized
   * @return a memoized supplier that computes and caches the result of the original supplier
   */
  public static <T> Supplier<T> memoize(Supplier<T> original) {
    return new Supplier<T>() {
      Supplier<T> delegate = this::firstTime;
      boolean initialized;

      public T get() {
        return delegate.get();
      }

      private synchronized T firstTime() {
        if (!initialized) {
          T value = original.get();
          delegate = () -> value;
          initialized = true;
        }
        return delegate.get();
      }
    };
  }

  /**
   * Converts a {@link Boolean} value to a primitive boolean value. If the input is {@code null},
   * the method returns {@code false}. Otherwise, it returns the value of the {@code Boolean}.
   *
   * @param val the {@link Boolean} object to be converted, which may be {@code null}
   * @return {@code false} if {@code val} is {@code null}, otherwise the value of the {@code
   *     Boolean}
   */
  public static boolean getNULLtoFalse(Boolean val) {
    if (val != null) {
      return val;
    }
    return false;
  }

  /**
   * Converts a {@link Boolean} value to a primitive boolean value. If the input is {@code null},
   * the method returns {@code true}. Otherwise, it returns the value of the {@code Boolean}.
   *
   * @param val the {@link Boolean} object to be converted, which may be {@code null}
   * @return {@code true} if {@code val} is {@code null}, otherwise the value of the {@code Boolean}
   */
  public static boolean getNULLtoTrue(Boolean val) {
    if (val != null) {
      return val;
    }
    return true;
  }

  /**
   * Converts a given string to a boolean based on its content, returning false if the string is
   * null or empty.
   *
   * @param val the input string, which may be null or blank
   * @return true if the input string has text and can be converted to a boolean, otherwise false
   */
  public static boolean getEmptytoFalse(String val) {
    if (StringUtil.hasText(val)) {
      return getBoolean(val);
    }
    return false;
  }

  /**
   * Converts a given string to a boolean value, defaulting to true if the string is null or empty.
   *
   * @param val the input string, which may be null or empty
   * @return true if the input string is null or empty, otherwise the boolean representation of the
   *     input string
   */
  public static boolean geEmptytoTrue(String val) {
    if (StringUtil.hasText(val)) {
      return getBoolean(val);
    }
    return true;
  }

  private static boolean getBoolean(String val) {
    return Boolean.TRUE.toString().equalsIgnoreCase(val);
  }

  /**
   * Converts a {@link Double} object to an {@link OptionalDouble}. If the input value is {@code
   * null}, an empty {@link OptionalDouble} is returned. Otherwise, an {@link OptionalDouble}
   * containing the value is returned.
   *
   * @param val the {@link Double} object to be converted, which may be {@code null}
   * @return an {@link OptionalDouble} containing the value if the input is not {@code null},
   *     otherwise an empty {@link OptionalDouble}
   */
  public static OptionalDouble getOptionalDouble(Double val) {
    return val == null ? OptionalDouble.empty() : OptionalDouble.of(val);
  }

  /**
   * Converts an {@link Integer} object to an {@link OptionalInt}. If the input value is {@code
   * null}, an empty {@link OptionalInt} is returned. Otherwise, an {@link OptionalInt} containing
   * the value is returned.
   *
   * @param val the {@link Integer} object to be converted, which may be {@code null}
   * @return an {@link OptionalInt} containing the value if the input is not {@code null}, otherwise
   *     an empty {@link OptionalInt}
   */
  public static OptionalInt getOptionalInteger(Integer val) {
    return val == null ? OptionalInt.empty() : OptionalInt.of(val);
  }
}
