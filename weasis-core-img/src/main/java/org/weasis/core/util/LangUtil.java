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

import java.util.*;
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
   * Returns an empty collection if the input is null, otherwise returns the input unchanged.
   *
   * @param <T> the type of elements in the collection
   * @param collection the input collection
   * @return an empty list if the input is null, otherwise the input
   */
  public static <T> Collection<T> emptyIfNull(Collection<T> collection) {
    return collection == null ? Collections.emptyList() : collection;
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
    return new MemoizedSupplier<>(original);
  }

  private static final class MemoizedSupplier<T> implements Supplier<T> {
    private final Supplier<T> original;
    private volatile boolean computed = false;
    private T cachedValue;

    MemoizedSupplier(Supplier<T> original) {
      this.original = original;
    }

    @Override
    public T get() {
      if (!computed) {
        synchronized (this) {
          if (!computed) {
            try {
              cachedValue = original.get();
            } finally {
              computed = true;
            }
          }
        }
      }
      return cachedValue;
    }
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
    return value == null ? OptionalDouble.empty() : OptionalDouble.of(value);
  }

  /**
   * Converts an Integer object to an OptionalInt.
   *
   * @param value the Integer object to convert, may be null
   * @return an OptionalInt containing the value if not null, otherwise empty
   */
  public static OptionalInt toOptional(Integer value) {
    return value == null ? OptionalInt.empty() : OptionalInt.of(value);
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
    if (values == null) return null;
    for (T value : values) {
      if (value != null) {
        return value;
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
    return value == null ? defaultValue : value;
  }

  // ============================== DEPRECATED METHODS ==============================

  /**
   * @deprecated since 4.12, for removal in a future version. Use {@link #nullToFalse(Boolean)}
   *     instead.
   */
  @Deprecated(since = "4.12", forRemoval = true)
  @Generated
  public static boolean getNULLtoFalse(Boolean val) {
    return nullToFalse(val);
  }

  /**
   * @deprecated since 4.12, for removal in a future version. Use {@link #nullToTrue(Boolean)}
   *     instead.
   */
  @Deprecated(since = "4.12", forRemoval = true)
  @Generated
  public static boolean getNULLtoTrue(Boolean val) {
    return nullToTrue(val);
  }

  /**
   * @deprecated since 4.12, for removal in a future version. Use {@link #emptyToFalse(String)}
   *     instead.
   */
  @Deprecated(since = "4.12", forRemoval = true)
  @Generated
  public static boolean getEmptytoFalse(String val) {
    return emptyToFalse(val);
  }

  /**
   * @deprecated since 4.12, for removal in a future version. Use {@link #emptyToTrue(String)}
   *     instead.
   */
  @Deprecated(since = "4.12", forRemoval = true)
  @Generated
  public static boolean geEmptytoTrue(String val) {
    return emptyToTrue(val);
  }

  /**
   * @deprecated since 4.12, for removal in a future version. Use {@link #toOptional(Double)}
   *     instead.
   */
  @Deprecated(since = "4.12", forRemoval = true)
  @Generated
  public static OptionalDouble getOptionalDouble(Double val) {
    return toOptional(val);
  }

  /**
   * @deprecated since 4.12, for removal in a future version. Use {@link #toOptional(Integer)}
   *     instead.
   */
  @Deprecated(since = "4.12", forRemoval = true)
  @Generated
  public static OptionalInt getOptionalInteger(Integer val) {
    return toOptional(val);
  }
}
