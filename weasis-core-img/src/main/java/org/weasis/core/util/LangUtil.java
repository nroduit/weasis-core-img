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
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.function.Supplier;
import org.weasis.core.util.annotations.Generated;

/**
 * Utility class providing common language-level operations and conversions.
 *
 * @author Nicolas Roduit
 */
public final class LangUtil {

  private LangUtil() {}

  /**
   * Returns an empty iterable if the input is null, otherwise returns the input unchanged.
   *
   * @param <T> the type of elements in the iterable
   * @param iterable the input iterable
   * @return an empty list if the input is null, otherwise the input
   */
  public static <T> Iterable<T> emptyIfNull(Iterable<T> iterable) {
    return iterable != null ? iterable : Collections.emptyList();
  }

  /**
   * Creates a memoized supplier that lazily initializes and caches the result of the original
   * supplier. The computation is performed only once, and subsequent calls return the cached
   * result. Thread-safe implementation using double-checked locking pattern.
   *
   * @param <T> the type of the value supplied
   * @param original the original supplier whose result is to be memoized
   * @return a memoized supplier that computes and caches the result of the original supplier
   * @throws NullPointerException if the original supplier is null
   */
  public static <T> Supplier<T> memoize(Supplier<T> original) {
    Objects.requireNonNull(original, "Supplier cannot be null");
    return new Supplier<T>() {
      private T cachedValue;
      private volatile boolean computed = false;

      @Override
      public T get() {
        if (!computed) {
          synchronized (this) {
            if (!computed) {
              cachedValue = original.get();
              computed = true;
            }
          }
        }
        return cachedValue;
      }
    };
  }

  /**
   * Converts a Boolean object to a primitive boolean, returning false if null.
   *
   * @param value the Boolean object to convert, may be null
   * @return false if the value is null, otherwise the boolean value
   */
  public static boolean nullToFalse(Boolean value) {
    return Boolean.TRUE.equals(value);
  }

  /**
   * Converts a Boolean object to a primitive boolean, returning true if null.
   *
   * @param value the Boolean object to convert, may be null
   * @return true if the value is null, otherwise the boolean value
   */
  public static boolean nullToTrue(Boolean value) {
    return !Boolean.FALSE.equals(value);
  }

  /**
   * Converts a string to a boolean, returning false if the string is null or empty.
   *
   * @param value the input string, may be null or empty
   * @return true if the string equals "true" (case-insensitive), false otherwise
   */
  public static boolean emptyToFalse(String value) {
    return StringUtil.hasText(value) && Boolean.parseBoolean(value);
  }

  /**
   * Converts a string to a boolean, returning true if the string is null or empty.
   *
   * @param value the input string, may be null or empty
   * @return false if the string equals "false" (case-insensitive), true otherwise
   */
  public static boolean emptyToTrue(String value) {
    return !StringUtil.hasText(value) || Boolean.parseBoolean(value);
  }

  /**
   * Converts a Double object to an OptionalDouble.
   *
   * @param value the Double object to convert, may be null
   * @return an OptionalDouble containing the value if not null, otherwise empty
   */
  public static OptionalDouble toOptional(Double value) {
    return value != null ? OptionalDouble.of(value) : OptionalDouble.empty();
  }

  /**
   * Converts an Integer object to an OptionalInt.
   *
   * @param value the Integer object to convert, may be null
   * @return an OptionalInt containing the value if not null, otherwise empty
   */
  public static OptionalInt toOptional(Integer value) {
    return value != null ? OptionalInt.of(value) : OptionalInt.empty();
  }

  /**
   * Converts any object to an Optional.
   *
   * @param <T> the type of the value
   * @param value the object to convert, may be null
   * @return an Optional containing the value if not null, otherwise empty
   */
  public static <T> Optional<T> toOptional(T value) {
    return Optional.ofNullable(value);
  }

  /**
   * Returns the first non-null value from the provided arguments.
   *
   * @param <T> the type of the values
   * @param values the values to check, in order of preference
   * @return the first non-null value, or null if all are null
   */
  @SafeVarargs
  public static <T> T firstNonNull(T... values) {
    if (values != null) {
      for (T value : values) {
        if (value != null) {
          return value;
        }
      }
    }
    return null;
  }

  /**
   * Returns the given value if it's not null, otherwise returns the default value.
   *
   * @param <T> the type of the values
   * @param value the value to check
   * @param defaultValue the default value to return if the first is null
   * @return the value if not null, otherwise the default value
   */
  public static <T> T defaultIfNull(T value, T defaultValue) {
    return value != null ? value : defaultValue;
  }

  // ============================== DEPRECATED FILE-BASED METHODS ==============================

  /**
   * @deprecated since 4.12, for removal in a future version. Use {@link #nullToFalse(Boolean)}
   *     instead.
   * @param val the {@link Boolean} object to be converted, which may be {@code null}
   * @return {@code false} if {@code val} is {@code null}, otherwise the value of the {@code
   *     Boolean}
   */
  @Deprecated(since = "4.12", forRemoval = true)
  @Generated
  public static boolean getNULLtoFalse(Boolean val) {
    return nullToFalse(val);
  }

  /**
   * @deprecated since 4.12, for removal in a future version. Use {@link #nullToTrue(Boolean)}
   *     instead.
   * @param val the {@link Boolean} object to be converted, which may be {@code null}
   * @return {@code true} if {@code val} is {@code null}, otherwise the value of the {@code Boolean}
   */
  @Deprecated(since = "4.12", forRemoval = true)
  @Generated
  public static boolean getNULLtoTrue(Boolean val) {
    return nullToTrue(val);
  }

  /**
   * @deprecated since 4.12, for removal in a future version. Use {@link #emptyToFalse(String)}
   *     instead.
   * @param val the input string, which may be null or blank
   * @return true if the input string has text and can be converted to a boolean, otherwise false
   */
  @Deprecated(since = "4.12", forRemoval = true)
  @Generated
  public static boolean getEmptytoFalse(String val) {
    return emptyToFalse(val);
  }

  /**
   * @deprecated since 4.12, for removal in a future version. Use {@link #emptyToTrue(String)}
   *     instead.
   * @param val the input string, which may be null or empty
   * @return true if the input string is null or empty, otherwise the boolean representation of the
   *     input string
   */
  @Deprecated(since = "4.12", forRemoval = true)
  @Generated
  public static boolean geEmptytoTrue(String val) {
    return emptyToTrue(val);
  }

  /**
   * @deprecated since 4.12, for removal in a future version. Use {@link #toOptional(Double)}
   *     instead.
   * @param val the {@link Double} object to be converted, which may be {@code null}
   * @return an {@link OptionalDouble} containing the value if the input is not {@code null},
   *     otherwise an empty {@link OptionalDouble}
   */
  @Deprecated(since = "4.12", forRemoval = true)
  @Generated
  public static OptionalDouble getOptionalDouble(Double val) {
    return toOptional(val);
  }

  /**
   * @deprecated since 4.12, for removal in a future version. Use {@link #toOptional(Integer)}
   *     instead.
   * @param val the {@link Integer} object to be converted, which may be {@code null}
   * @return an {@link OptionalInt} containing the value if the input is not {@code null}, otherwise
   *     an empty {@link OptionalInt}
   */
  @Deprecated(since = "4.12", forRemoval = true)
  @Generated
  public static OptionalInt getOptionalInteger(Integer val) {
    return toOptional(val);
  }
}
