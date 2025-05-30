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
import java.util.Locale;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StringUtil {
  private static final Logger LOGGER = LoggerFactory.getLogger(StringUtil.class);

  public static final String EMPTY_STRING = "";
  public static final String SPACE = " ";
  public static final String COLON = ":";
  public static final String COLON_AND_SPACE = ": ";

  private static final String[] EMPTY_STRING_ARRAY = new String[0];
  private static final int[] EMPTY_INT_ARRAY = new int[0];

  public static final Collator collator = Collator.getInstance(Locale.getDefault());

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
    if (name != null && limit >= 0 && name.length() > limit) {
      Suffix s = suffix == null ? Suffix.NO : suffix;
      int sLength = s.getLength();
      int end = limit - sLength;
      if (end > 0 && end + sLength < name.length()) {
        return name.substring(0, end).concat(s.getValue());
      }
    }
    return name;
  }

  public static Character getFirstCharacter(String val) {
    if (StringUtil.hasText(val)) {
      return val.charAt(0);
    }
    return null;
  }

  public static String[] getStringArray(String val, String delimiter) {
    if (delimiter != null && StringUtil.hasText(val)) {
      return val.split(Pattern.quote(delimiter));
    }
    return EMPTY_STRING_ARRAY;
  }

  public static int[] getIntegerArray(String val, String delimiter) {
    if (delimiter != null && StringUtil.hasText(val)) {
      String[] vl = val.split(Pattern.quote(delimiter));
      int[] res = new int[vl.length];
      for (int i = 0; i < res.length; i++) {
        res[i] = getInt(vl[i]);
      }
      return res;
    }
    return EMPTY_INT_ARRAY;
  }

  public static Integer getInteger(String val) {
    if (StringUtil.hasText(val)) {
      try {
        return Integer.parseInt(val.trim());
      } catch (NumberFormatException e) {
        LOGGER.warn("Cannot parse {} to Integer", val);
      }
    }
    return null;
  }

  public static int getInt(String val) {
    if (StringUtil.hasText(val)) {
      try {
        return Integer.parseInt(val.trim());
      } catch (NumberFormatException e) {
        LOGGER.warn("Cannot parse {} to int", val);
      }
    }
    return 0;
  }

  public static int getInt(String value, int defaultValue) {
    if (value != null) {
      try {
        return Integer.parseInt(value.trim());
      } catch (NumberFormatException e) {
        LOGGER.warn("Cannot parse {} to int", value);
      }
    }
    return defaultValue;
  }

  public static Double getDouble(String val) {
    if (StringUtil.hasText(val)) {
      try {
        return Double.parseDouble(val.trim());
      } catch (NumberFormatException e) {
        LOGGER.warn("Cannot parse {} to Double", val);
      }
    }
    return null;
  }

  public static String splitCamelCaseString(String s) {
    StringBuilder builder = new StringBuilder();
    for (String w : s.split("(?<!(^|[A-Z]))(?=[A-Z])|(?<!^)(?=[A-Z][a-z])")) {
      builder.append(w);
      builder.append(' ');
    }
    return builder.toString().trim();
  }

  public static boolean hasLength(CharSequence str) {
    return str != null && !str.isEmpty();
  }

  public static boolean hasText(CharSequence str) {
    if (!hasLength(str)) {
      return false;
    }
    for (int i = 0; i < str.length(); i++) {
      if (!Character.isWhitespace(str.charAt(i))) {
        return true;
      }
    }
    return false;
  }

  /**
   * Removing diacritical marks aka accents
   *
   * @param str the input string
   * @return the input string without accents
   */
  public static String deAccent(String str) {
    String nfdNormalizedString = Normalizer.normalize(str, Normalizer.Form.NFD);
    Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
    return pattern.matcher(nfdNormalizedString).replaceAll("");
  }

  public static String bytesToHex(byte[] bytes) {
    StringBuilder hexString = new StringBuilder();
    for (byte b : bytes) {
      hexString.append(String.format("%02X", b));
    }
    return hexString.toString();
  }

  public static String integerToHex(int val) {
    return Integer.toHexString(val).toUpperCase();
  }

  public static String bytesToMD5(byte[] val) throws NoSuchAlgorithmException {
    MessageDigest md = MessageDigest.getInstance("MD5"); // NOSONAR not a security issue
    return bytesToHex(md.digest(val));
  }

  public static String getNullIfNull(Object object) {
    if (object == null) {
      return null;
    }
    return object.toString();
  }

  public static String getEmptyStringIfNull(Object object) {
    if (object == null) {
      return EMPTY_STRING;
    }
    return object.toString();
  }

  public static String getEmptyStringIfNullEnum(Enum<?> object) {
    if (object == null) {
      return EMPTY_STRING;
    }
    return object.name();
  }
}
