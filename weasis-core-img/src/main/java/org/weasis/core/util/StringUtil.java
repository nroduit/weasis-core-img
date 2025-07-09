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
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

  public static final Collator collator = initializeCollator();

  private static Collator initializeCollator() {
    Collator collator = Collator.getInstance(Locale.getDefault());
    collator.setStrength(Collator.PRIMARY); // Case and accent insensitive
    return collator;
  }

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

  public static String getTruncatedString(String name, int limit, Suffix suffix) {
    if (name == null || limit < 0 || name.length() <= limit) {
      return name;
    }
    Suffix actualSuffix = Objects.requireNonNullElse(suffix, Suffix.NO);
    int suffixLength = actualSuffix.getLength();
    int end = limit - suffixLength;

    if (end <= 0) {
      return name;
    }

    return name.substring(0, end) + actualSuffix.getValue();
  }

  public static Character getFirstCharacter(String val) {
    return hasText(val) ? val.charAt(0) : null;
  }

  public static String[] getStringArray(String val, String delimiter) {
    if (delimiter == null || !hasText(val)) {
      return EMPTY_STRING_ARRAY;
    }
    return val.split(Pattern.quote(delimiter));
  }

  public static int[] getIntegerArray(String val, String delimiter) {
    if (delimiter == null || !hasText(val)) {
      return EMPTY_INT_ARRAY;
    }

    String[] parts = val.split(Pattern.quote(delimiter));
    return Arrays.stream(parts).mapToInt(StringUtil::getInt).toArray();
  }

  public static Integer getInteger(String val) {
    if (!hasText(val)) {
      return null;
    }
    try {
      return Integer.valueOf(val.trim());
    } catch (NumberFormatException e) {
      LOGGER.warn("Cannot parse {} to Integer", val);
      return null;
    }
  }

  public static int getInt(String val) {
    return getInt(val, 0);
  }

  public static int getInt(String value, int defaultValue) {
    if (!hasText(value)) {
      return defaultValue;
    }
    try {
      return Integer.parseInt(value.trim());
    } catch (NumberFormatException e) {
      LOGGER.warn("Cannot parse {} to int", value);
      return defaultValue;
    }
  }

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

  public static String splitCamelCaseString(String s) {
    if (s == null) {
      return null;
    }
    return String.join(SPACE, CAMEL_CASE_PATTERN.split(s));
  }

  public static boolean hasLength(CharSequence str) {
    return str != null && !str.isEmpty();
  }

  public static boolean hasText(CharSequence str) {
    return hasLength(str) && str.chars().anyMatch(c -> !Character.isWhitespace(c));
  }

  /**
   * Removing diacritical marks aka accents
   *
   * @param str the input string
   * @return the input string without accents
   */
  public static String deAccent(String str) {
    if (str == null) {
      return null;
    }

    String normalized = Normalizer.normalize(str, Normalizer.Form.NFD);
    return DIACRITICAL_MARKS_PATTERN.matcher(normalized).replaceAll("");
  }

  public static String bytesToHex(byte[] bytes) {
    if (bytes == null) {
      return null;
    }

    StringBuilder hexString = new StringBuilder(bytes.length * 2);
    for (byte b : bytes) {
      hexString.append(String.format("%02X", b));
    }
    return hexString.toString();
  }

  public static String integerToHex(int val) {
    return Integer.toHexString(val).toUpperCase(Locale.ROOT);
  }

  public static String bytesToMD5(byte[] val) throws NoSuchAlgorithmException {
    if (val == null) {
      return null;
    }
    MessageDigest md = MessageDigest.getInstance("MD5"); // NOSONAR not a security issue
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
