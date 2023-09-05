/*
 * Copyright (c) 2023 Weasis Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.weasis.core.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.weasis.core.util.StringUtil.Suffix;

class StringUtilTest {

  /** Method under test: {@link StringUtil#getTruncatedString(String, int, StringUtil.Suffix)} */
  @Test
  void testGetTruncatedString() {
    assertEquals("N", StringUtil.getTruncatedString("Name", 1, StringUtil.Suffix.NO));
    assertEquals("N", StringUtil.getTruncatedString("Name", 1, null));
    assertNull(StringUtil.getTruncatedString(null, 1, StringUtil.Suffix.NO));
    assertEquals(
        StringUtil.EMPTY_STRING,
        StringUtil.getTruncatedString(StringUtil.EMPTY_STRING, 1, StringUtil.Suffix.NO));
    assertEquals("Name", StringUtil.getTruncatedString("Name", 0, StringUtil.Suffix.NO));
    StringUtil.getTruncatedString("Name", Integer.MIN_VALUE, StringUtil.Suffix.UNDERSCORE);
    assertEquals("Na_", StringUtil.getTruncatedString("Name", 3, StringUtil.Suffix.UNDERSCORE));
    assertEquals("Na.", StringUtil.getTruncatedString("Name", 3, StringUtil.Suffix.ONE_PTS));
    assertEquals(
        "Longer p...",
        StringUtil.getTruncatedString("Longer phrase2", 11, StringUtil.Suffix.THREE_PTS));
  }

  /** Method under test: {@link StringUtil#getFirstCharacter(String)} */
  @Test
  void testGetFirstCharacter() {
    assertEquals('V', Objects.requireNonNull(StringUtil.getFirstCharacter("Val")).charValue());
    assertNull(StringUtil.getFirstCharacter(null));
    assertNull(StringUtil.getFirstCharacter(StringUtil.EMPTY_STRING));
  }

  /** Method under test: {@link StringUtil#getStringArray(String, String)} */
  @Test
  void testGetStringArray() {
    assertEquals(0, StringUtil.getStringArray("Val", null).length);
    String[] actualStringArray = StringUtil.getStringArray("Val,v2,v3,", ",");
    assertEquals(3, actualStringArray.length);
    assertEquals("Val", actualStringArray[0]);
    assertEquals("v2", actualStringArray[1]);
    assertEquals("v3", actualStringArray[2]);
  }

  /** Method under test: {@link StringUtil#getIntegerArray(String, String)} */
  @Test
  void testGetIntegerArray() {
    assertEquals(0, StringUtil.getIntegerArray("3", null).length);
    assertEquals(0, StringUtil.getIntegerArray("foo", null).length);
    assertEquals(0, StringUtil.getIntegerArray(StringUtil.EMPTY_STRING, " ").length);

    int[] actualIntegerArray = StringUtil.getIntegerArray("5 " + Integer.MIN_VALUE, " ");
    assertEquals(2, actualIntegerArray.length);
    assertEquals(5, actualIntegerArray[0]);
    assertEquals(Integer.MIN_VALUE, actualIntegerArray[1]);
  }

  /** Method under test: {@link StringUtil#getInteger(String)} */
  @Test
  void testGetInteger() {
    assertNull(StringUtil.getInteger("42a"));
    assertNull(StringUtil.getInteger(null));
    assertNull(StringUtil.getInteger(StringUtil.EMPTY_STRING));
    assertEquals(42, Objects.requireNonNull(StringUtil.getInteger("42")).intValue());
  }

  /** Method under test: {@link StringUtil#getInt(String)} */
  @Test
  void testGetInt() {
    assertEquals(0, StringUtil.getInt("42a"));
    assertEquals(0, StringUtil.getInt(null));
    assertEquals(0, StringUtil.getInt(StringUtil.EMPTY_STRING));
    assertEquals(42, StringUtil.getInt("42"));

    assertEquals(42, StringUtil.getInt("42", 4));
    assertEquals(42, StringUtil.getInt(null, 42));
    assertEquals(35, StringUtil.getInt("foo", 35));
  }

  /** Method under test: {@link StringUtil#getDouble(String)} */
  @Test
  void testGetDouble() {
    assertNull(StringUtil.getDouble("Val"));
    assertNull(StringUtil.getDouble(null));
    assertNull(StringUtil.getDouble(StringUtil.EMPTY_STRING));
    assertEquals(42.0d, Objects.requireNonNull(StringUtil.getDouble("42")).doubleValue());
  }

  /** Method under test: {@link StringUtil#splitCamelCaseString(String)} */
  @Test
  void testSplitCamelCaseString() {
    assertEquals("Camél123 Cáse Phrásé", StringUtil.splitCamelCaseString("Camél123CásePhrásé"));
    // Do not support diacritical marks on upper case characters
    assertEquals("Camél Cásé PhráseÉté", StringUtil.splitCamelCaseString("CamélCáséPhráseÉté"));
    assertEquals("CAMEL Split CASE123", StringUtil.splitCamelCaseString("CAMELSplitCASE123"));
  }

  /** Method under test: {@link StringUtil#hasLength(CharSequence)} */
  @Test
  void testHasLength() {
    assertFalse(StringUtil.hasLength(new StringBuilder(1)));
    assertFalse(StringUtil.hasLength(null));
    assertFalse(StringUtil.hasLength(StringUtil.EMPTY_STRING));
    assertTrue(StringUtil.hasLength("Str"));
  }

  /** Method under test: {@link StringUtil#hasText(CharSequence)} */
  @Test
  void testHasText() {
    assertFalse(StringUtil.hasText(new StringBuilder(1)));
    assertFalse(StringUtil.hasText(null));
    assertFalse(StringUtil.hasText(StringUtil.EMPTY_STRING));
    assertFalse(StringUtil.hasText("     "));
    assertTrue(StringUtil.hasText("Str"));
  }

  /** Method under test: {@link StringUtil#deAccent(String)} */
  @Test
  void testDeAccent() {
    assertEquals("A E I O U", StringUtil.deAccent("Á É Í Ó Ú"));
  }

  /** Method under test: {@link StringUtil#bytesToHex(byte[])} */
  @Test
  void testBytesToHex() throws UnsupportedEncodingException {
    assertEquals("415820615C78303139", StringUtil.bytesToHex("AX a\\x019".getBytes("UTF-8")));
  }

  /** Method under test: {@link StringUtil#integerToHex(int)} */
  @Test
  void testIntegerToHex() {
    assertEquals("A45F", StringUtil.integerToHex(42079));
  }

  /** Method under test: {@link StringUtil#bytesToMD5(byte[])} */
  @Test
  void testBytesToMD5() throws UnsupportedEncodingException, NoSuchAlgorithmException {
    assertEquals(
        "0F719D7161F722CE7B8E7F79629F0A2B", StringUtil.bytesToMD5("AX a\\x019".getBytes("UTF-8")));
  }

  /** Method under test: {@link StringUtil#getNullIfNull(Object)} */
  @Test
  void testGetNullIfNull() {
    assertNull(StringUtil.getNullIfNull(null));
    assertEquals("Object", StringUtil.getNullIfNull("Object"));
  }

  /** Method under test: {@link StringUtil#getEmptyStringIfNull(Object)} */
  @Test
  void testGetEmptyStringIfNull() {
    assertEquals(StringUtil.EMPTY_STRING, StringUtil.getEmptyStringIfNull(null));
    assertEquals("Object", StringUtil.getEmptyStringIfNull("Object"));
  }

  /** Method under test: {@link StringUtil#getEmptyStringIfNullEnum(Enum)} */
  @Test
  void testGetEmptyStringIfNullEnum() {
    assertEquals(StringUtil.EMPTY_STRING, StringUtil.getEmptyStringIfNullEnum(null));
    assertEquals("THREE_PTS", StringUtil.getEmptyStringIfNullEnum(Suffix.THREE_PTS));
  }

  /**
   * Methods under test:
   *
   * <ul>
   *   <li>{@link StringUtil.Suffix#getValue()}
   *   <li>{@link StringUtil.Suffix#getLength()}
   *   <li>{@link StringUtil.Suffix#toString()}
   * </ul>
   */
  @Test
  void testSuffixGetValue() {
    assertEquals(3, StringUtil.Suffix.THREE_PTS.getLength());

    StringUtil.Suffix valueOfResult = StringUtil.Suffix.valueOf("UNDERSCORE");
    String actualValue = valueOfResult.getValue();
    assertEquals("_", actualValue);
    assertEquals("_", valueOfResult.toString());
  }
}
