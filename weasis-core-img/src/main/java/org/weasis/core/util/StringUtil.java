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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Collator;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for string manipulation, parsing, and formatting operations. Provides common string
 * operations with null-safe implementations.
 */
public final class StringUtil {
  private static final Logger LOGGER = LoggerFactory.getLogger(StringUtil.class);

  public static final String EMPTY_STRING = "";
  public static final String SPACE = " ";
  public static final String COLON = ":";
  public static final String COLON_AND_SPACE = ": ";

  private static final String[] EMPTY_STRING_ARRAY = new String[0];
  private static final int[] EMPTY_INT_ARRAY = new int[0];
  private static final Pattern DIACRITICAL_MARKS_PATTERN =
      Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
  private static final Pattern CAMEL_CASE_PATTERN =
      Pattern.compile("(?<!(^|[A-Z]))(?=[A-Z])|(?<!^)(?=[A-Z][a-z])");
  private static final HexFormat HEX_FORMAT = HexFormat.of().withUpperCase();

  public static final Collator collator = createCollator();

  private static Collator createCollator() {
    var collator = Collator.getInstance(Locale.getDefault());
    collator.setStrength(Collator.PRIMARY); // Case and accent insensitive
    return collator;
  }

  /** Suffix options for string truncation operations. */
  public enum Suffix {
    NO(""),

    UNDERSCORE("_"),

    ONE_PTS("."),

    THREE_PTS("...");

    private final String value;

    Suffix(String suffix) {
      this.value = suffix;
    }

    public String getValue() {
      return value;
    }

    public int getLength() {
      return value.length();
    }

    @Override
    public String toString() {
      return value;
    }
  }

  private StringUtil() {}

  /**
   * Truncates a string to the specified limit, optionally adding a suffix.
   *
   * @param name the string to truncate
   * @param limit the maximum length of the result
   * @param suffix the suffix to append if truncation occurs
   * @return the truncated string, or the original string if no truncation is needed
   */
  public static String getTruncatedString(String name, int limit, Suffix suffix) {
    if (name == null || limit < 0 || name.length() <= limit) {
      return name;
    }

    var actualSuffix = Objects.requireNonNullElse(suffix, Suffix.NO);
    int truncateLength = limit - actualSuffix.getLength();

    return truncateLength <= 0 ? name : name.substring(0, truncateLength) + actualSuffix.getValue();
  }

  /**
   * Gets the first character of a string.
   *
   * @param val the input string
   * @return the first character, or null if the string is null or empty
   */
  public static Character getFirstCharacter(String val) {
    return hasText(val) ? val.charAt(0) : null;
  }

  /**
   * Splits a string into an array using the specified delimiter.
   *
   * @param val the string to split
   * @param delimiter the delimiter to use for splitting
   * @return an array of strings, or empty array if input is invalid
   */
  public static String[] getStringArray(String val, String delimiter) {
    if (delimiter == null || !hasText(val)) {
      return EMPTY_STRING_ARRAY;
    }
    return val.split(Pattern.quote(delimiter));
  }

  /**
   * Parses a delimited string into an array of integers.
   *
   * @param val the string to parse
   * @param delimiter the delimiter separating the integers
   * @return an array of integers, or empty array if parsing fails
   */
  public static int[] getIntegerArray(String val, String delimiter) {
    if (delimiter == null || !hasText(val)) {
      return EMPTY_INT_ARRAY;
    }

    return Arrays.stream(val.split(Pattern.quote(delimiter)))
        .mapToInt(StringUtil::getInt)
        .toArray();
  }

  /**
   * Parses a string to an Integer, returning null if parsing fails.
   *
   * @param val the string to parse
   * @return the parsed Integer, or null if parsing fails
   */
  public static Integer getInteger(String val) {
    if (!hasText(val)) {
      return null;
    }
    return parseInteger(val.trim(), null);
  }

  public static int getInt(String val) {
    return getInt(val, 0);
  }

  /**
   * Parses a string to an int, returning a default value if parsing fails.
   *
   * @param value the string to parse
   * @param defaultValue the value to return if parsing fails
   * @return the parsed int, or the default value
   */
  public static int getInt(String value, int defaultValue) {
    if (!hasText(value)) {
      return defaultValue;
    }
    var result = parseInteger(value.trim(), null);
    return result != null ? result : defaultValue;
  }

  private static Integer parseInteger(String value, Integer defaultValue) {
    try {
      return Integer.valueOf(value);
    } catch (NumberFormatException e) {
      LOGGER.warn("Cannot parse {} to Integer", value);
      return defaultValue;
    }
  }

  /**
   * Parses a string to a Double, returning null if parsing fails.
   *
   * @param val the string to parse
   * @return the parsed Double, or null if parsing fails
   */
  public static Double getDouble(String val) {
    if (!hasText(val)) {
      return null;
    }
    try {
      return Double.valueOf(val.trim());
    } catch (NumberFormatException e) {
      LOGGER.warn("Cannot parse {} to Double", val);
      return null;
    }
  }

  /**
   * Splits a camelCase string into words separated by spaces.
   *
   * @param s the camelCase string
   * @return the split string, or null if input is null
   */
  public static String splitCamelCaseString(String s) {
    return s == null ? null : String.join(SPACE, CAMEL_CASE_PATTERN.split(s));
  }

  /**
   * Checks if a CharSequence has a non-zero length.
   *
   * @param str the CharSequence to check
   * @return true if the string is not null and not empty
   */
  public static boolean hasLength(CharSequence str) {
    return str != null && !str.isEmpty();
  }

  /**
   * Checks if a CharSequence contains actual text (non-whitespace characters).
   *
   * @param str the CharSequence to check
   * @return true if the string has length and contains non-whitespace characters
   */
  public static boolean hasText(CharSequence str) {
    if (!hasLength(str)) {
      return false;
    }

    // Optimized check for short strings
    int length = str.length();
    if (length <= 16) {
      for (int i = 0; i < length; i++) {
        if (!Character.isWhitespace(str.charAt(i))) {
          return true;
        }
      }
      return false;
    }
    // Use streams for longer strings
    return str.chars().anyMatch(c -> !Character.isWhitespace(c));
  }

  /**
   * Removes diacritical marks (accents) from a string.
   *
   * @param str the input string
   * @return the string without accents, or null if input is null
   */
  public static String deAccent(String str) {
    if (!hasText(str)) {
      return str; // Return as-is for null or empty
    }

    var normalized = Normalizer.normalize(str, Normalizer.Form.NFD);
    return DIACRITICAL_MARKS_PATTERN.matcher(normalized).replaceAll("");
  }

  /**
   * Converts byte array to uppercase hexadecimal string.
   *
   * @param bytes the byte array
   * @return hexadecimal string representation, or null if input is null
   */
  public static String bytesToHex(byte[] bytes) {
    return bytes == null ? null : HEX_FORMAT.formatHex(bytes);
  }

  /**
   * Converts integer to uppercase hexadecimal string.
   *
   * @param val the integer value
   * @return hexadecimal string representation
   */
  public static String integerToHex(int val) {
    return Integer.toHexString(val).toUpperCase(Locale.ROOT);
  }

  /**
   * Computes MD5 hash of byte array and returns as hexadecimal string.
   *
   * @param val the byte array
   * @return MD5 hash as hexadecimal string, or null if input is null
   * @throws NoSuchAlgorithmException if MD5 algorithm is not available
   */
  public static String bytesToMD5(byte[] val) throws NoSuchAlgorithmException {
    if (val == null) {
      return null;
    }
    var md = MessageDigest.getInstance("MD5"); // NOSONAR not a security issue here
    return bytesToHex(md.digest(val));
  }

  public static String getNullIfNull(Object object) {
    return object == null ? null : object.toString();
  }

  public static String getEmptyStringIfNull(Object object) {
    return object == null ? EMPTY_STRING : object.toString();
  }

  public static String getEmptyStringIfNullEnum(Enum<?> object) {
    return object == null ? EMPTY_STRING : object.name();
  }
}
