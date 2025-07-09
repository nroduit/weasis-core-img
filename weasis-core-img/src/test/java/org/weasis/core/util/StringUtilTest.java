/*
 * Copyright (c) 2010-2023 Weasis Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.weasis.core.util;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("StringUtil Tests")
class StringUtilTest {

  @Nested
  @DisplayName("String Truncation Tests")
  class TruncationTests {

    @Test
    @DisplayName("Should return null when input is null")
    void testTruncateNullInput() {
      assertNull(StringUtil.getTruncatedString(null, 5, StringUtil.Suffix.NO));
    }

    @Test
    @DisplayName("Should return original string when length is within limit")
    void testTruncateWithinLimit() {
      assertEquals("Name", StringUtil.getTruncatedString("Name", 10, StringUtil.Suffix.NO));
      assertEquals("Name", StringUtil.getTruncatedString("Name", 4, StringUtil.Suffix.NO));
    }

    @Test
    @DisplayName("Should handle zero or negative limits")
    void testTruncateNegativeLimit() {
      assertEquals("Name", StringUtil.getTruncatedString("Name", -1, StringUtil.Suffix.NO));
      assertEquals("Name", StringUtil.getTruncatedString("Name", 0, StringUtil.Suffix.NO));
    }

    @Test
    @DisplayName("Should handle null suffix as NO suffix")
    void testTruncateNullSuffix() {
      assertEquals("N", StringUtil.getTruncatedString("Name", 1, null));
    }

    @ParameterizedTest
    @DisplayName("Should truncate with different suffixes")
    @CsvSource({
      "Name, 3, UNDERSCORE, Na_",
      "Name, 3, ONE_PTS, Na.",
      "Longer phrase2, 11, THREE_PTS, Longer p...",
      "Short, 10, THREE_PTS, Short"
    })
    void testTruncateWithSuffixes(
        String input, int limit, StringUtil.Suffix suffix, String expected) {
      assertEquals(expected, StringUtil.getTruncatedString(input, limit, suffix));
    }

    @Test
    @DisplayName("Should handle empty string")
    void testTruncateEmptyString() {
      assertEquals("", StringUtil.getTruncatedString("", 5, StringUtil.Suffix.NO));
    }

    @Test
    @DisplayName("Should handle case where suffix is longer than limit")
    void testTruncateSuffixLongerThanLimit() {
      assertEquals("Test", StringUtil.getTruncatedString("Test", 2, StringUtil.Suffix.THREE_PTS));
    }
  }

  @Nested
  @DisplayName("Character Extraction Tests")
  class CharacterExtractionTests {

    @Test
    @DisplayName("Should return first character of non-empty string")
    void testGetFirstCharacterValid() {
      assertEquals('V', StringUtil.getFirstCharacter("Val"));
      assertEquals('A', StringUtil.getFirstCharacter("A"));
      assertEquals(' ', StringUtil.getFirstCharacter(" test"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t\n"})
    @DisplayName("Should return null for null, empty, or whitespace-only strings")
    void testGetFirstCharacterInvalid(String input) {
      assertNull(StringUtil.getFirstCharacter(input));
    }
  }

  @Nested
  @DisplayName("String Array Parsing Tests")
  class StringArrayTests {

    @Test
    @DisplayName("Should return empty array for null delimiter")
    void testGetStringArrayNullDelimiter() {
      assertArrayEquals(new String[0], StringUtil.getStringArray("Val", null));
    }

    @Test
    @DisplayName("Should return empty array for empty string")
    void testGetStringArrayEmptyString() {
      assertArrayEquals(new String[0], StringUtil.getStringArray("", ","));
    }

    @Test
    @DisplayName("Should split string correctly")
    void testGetStringArrayValid() {
      String[] result = StringUtil.getStringArray("Val,v2,v3", ",");
      assertArrayEquals(new String[] {"Val", "v2", "v3"}, result);
    }

    @Test
    @DisplayName("Should handle special delimiter characters")
    void testGetStringArraySpecialDelimiter() {
      String[] result = StringUtil.getStringArray("a.b.c", ".");
      assertArrayEquals(new String[] {"a", "b", "c"}, result);
    }

    @Test
    @DisplayName("Should handle additional delimiters")
    void testGetStringArrayTrailingDelimiter() {
      // Test with additional delimiters
      String[] result = StringUtil.getStringArray(",a,,b,c,,", ",");
      // Only trailing empty strings are ignored
      assertArrayEquals(new String[] {"", "a", "", "b", "c"}, result);
    }
  }

  @Nested
  @DisplayName("Integer Array Parsing Tests")
  class IntegerArrayTests {

    @Test
    @DisplayName("Should return empty array for null delimiter")
    void testGetIntegerArrayNullDelimiter() {
      assertArrayEquals(new int[0], StringUtil.getIntegerArray("3", null));
    }

    @Test
    @DisplayName("Should return empty array for empty string")
    void testGetIntegerArrayEmptyString() {
      assertArrayEquals(new int[0], StringUtil.getIntegerArray("", " "));
    }

    @Test
    @DisplayName("Should parse valid integers")
    void testGetIntegerArrayValid() {
      int[] result = StringUtil.getIntegerArray("5 " + Integer.MIN_VALUE, " ");
      assertArrayEquals(new int[] {5, Integer.MIN_VALUE}, result);
    }

    @Test
    @DisplayName("Should handle invalid integers with default value")
    void testGetIntegerArrayWithInvalidNumbers() {
      int[] result = StringUtil.getIntegerArray("1,abc,3", ",");
      assertArrayEquals(new int[] {1, 0, 3}, result);
    }

    @Test
    @DisplayName("Should handle negative numbers")
    void testGetIntegerArrayNegativeNumbers() {
      int[] result = StringUtil.getIntegerArray("-5,-10,15", ",");
      assertArrayEquals(new int[] {-5, -10, 15}, result);
    }
  }

  @Nested
  @DisplayName("Number Parsing Tests")
  class NumberParsingTests {

    @Nested
    @DisplayName("Integer Parsing")
    class IntegerParsingTests {

      static Stream<String> validIntegerStrings() {
        return Stream.of(
            "42",
            "  42  ",
            "0",
            "-42",
            String.valueOf(Integer.MAX_VALUE),
            String.valueOf(Integer.MIN_VALUE));
      }

      @ParameterizedTest
      @MethodSource("validIntegerStrings")
      @DisplayName("Should parse valid integers")
      void testGetIntegerValid(String input) {
        assertNotNull(StringUtil.getInteger(input));
      }

      @ParameterizedTest
      @NullAndEmptySource
      @ValueSource(strings = {"42a", "abc", "  ", "42.5"})
      @DisplayName("Should return null for invalid integers")
      void testGetIntegerInvalid(String input) {
        assertNull(StringUtil.getInteger(input));
      }

      @Test
      @DisplayName("Should parse valid integers to int")
      void testGetIntValid() {
        assertEquals(42, StringUtil.getInt("42"));
        assertEquals(-42, StringUtil.getInt("-42"));
        assertEquals(0, StringUtil.getInt("0"));
      }

      @ParameterizedTest
      @NullAndEmptySource
      @ValueSource(strings = {"42a", "abc", "  "})
      @DisplayName("Should return 0 for invalid int parsing")
      void testGetIntInvalid(String input) {
        assertEquals(0, StringUtil.getInt(input));
      }

      @Test
      @DisplayName("Should return default value for invalid int parsing")
      void testGetIntWithDefaultValue() {
        assertEquals(42, StringUtil.getInt("42", 10));
        assertEquals(10, StringUtil.getInt(null, 10));
        assertEquals(35, StringUtil.getInt("foo", 35));
      }
    }

    @Nested
    @DisplayName("Double Parsing")
    class DoubleParsingTests {

      @ParameterizedTest
      @ValueSource(strings = {"42.0", "  42.5  ", "0.0", "-42.7", "42"})
      @DisplayName("Should parse valid doubles")
      void testGetDoubleValid(String input) {
        assertNotNull(StringUtil.getDouble(input));
      }

      @ParameterizedTest
      @NullAndEmptySource
      @ValueSource(strings = {"abc", "  ", "42.5a", "42.5.5", "42,5"})
      @DisplayName("Should return null for invalid doubles")
      void testGetDoubleInvalid(String input) {
        assertNull(StringUtil.getDouble(input));
      }

      @ParameterizedTest
      @ValueSource(doubles = {42.0, -42.7, 0.0, Math.PI, Double.MAX_VALUE, Double.MIN_VALUE})
      @DisplayName("Should parse specific double values correctly")
      void testGetDoubleSpecificValues(double value) {
        String stringValue = String.valueOf(value);
        assertEquals(value, StringUtil.getDouble(stringValue), 0.001);
      }
    }
  }

  @Nested
  @DisplayName("Text Manipulation Tests")
  class TextManipulationTests {

    @Test
    @DisplayName("Should return null for null input in camelCase splitting")
    void testSplitCamelCaseStringNull() {
      assertNull(StringUtil.splitCamelCaseString(null));
    }

    @ParameterizedTest
    @CsvSource({
      "camelCase, camel Case",
      "CamelCase, Camel Case",
      "XMLParser, XML Parser",
      "simpleTest, simple Test",
      "HTTPSConnection, HTTPS Connection",
      "Camél123CásePhrásé, Camél123 Cáse Phrásé",
      "CAMELSplitCASE123, CAMEL Split CASE123"
    })
    @DisplayName("Should split camelCase strings correctly")
    void testSplitCamelCaseString(String input, String expected) {
      assertEquals(expected, StringUtil.splitCamelCaseString(input));
    }

    @Test
    @DisplayName("Should handle single word")
    void testSplitCamelCaseSingleWord() {
      assertEquals("word", StringUtil.splitCamelCaseString("word"));
      assertEquals("WORD", StringUtil.splitCamelCaseString("WORD"));
    }

    @Test
    @DisplayName("Should handle empty string")
    void testSplitCamelCaseEmptyString() {
      assertEquals("", StringUtil.splitCamelCaseString(""));
    }
  }

  @Nested
  @DisplayName("Text Validation Tests")
  class TextValidationTests {

    @Nested
    @DisplayName("Has Length Tests")
    class HasLengthTests {

      @Test
      @DisplayName("Should return true for non-empty strings")
      void testHasLengthTrue() {
        assertTrue(StringUtil.hasLength("Str"));
        assertTrue(StringUtil.hasLength("   "));
        assertTrue(StringUtil.hasLength("a"));
      }

      @Test
      @DisplayName("Should return false for null and empty")
      void testHasLengthFalse() {
        assertFalse(StringUtil.hasLength(null));
        assertFalse(StringUtil.hasLength(""));
        assertFalse(StringUtil.hasLength(new StringBuilder()));
      }
    }

    @Nested
    @DisplayName("Has Text Tests")
    class HasTextTests {

      @Test
      @DisplayName("Should return true for strings with non-whitespace content")
      void testHasTextTrue() {
        assertTrue(StringUtil.hasText("Str"));
        assertTrue(StringUtil.hasText("  a  "));
        assertTrue(StringUtil.hasText("a"));
      }

      @Test
      @DisplayName("Should return false for null, empty, or whitespace-only")
      void testHasTextFalse() {
        assertFalse(StringUtil.hasText(null));
        assertFalse(StringUtil.hasText(""));
        assertFalse(StringUtil.hasText("     "));
        assertFalse(StringUtil.hasText("\t\n\r"));
        assertFalse(StringUtil.hasText(new StringBuilder()));
      }
    }
  }

  @Nested
  @DisplayName("Text Processing Tests")
  class TextProcessingTests {

    @Test
    @DisplayName("Should return null for null input in deAccent")
    void testDeAccentNull() {
      assertNull(StringUtil.deAccent(null));
    }

    @ParameterizedTest
    @CsvSource({
      "Á É Í Ó Ú, A E I O U",
      "café, cafe",
      "naïve, naive",
      "résumé, resume",
      "piñata, pinata",
      "Zürich, Zurich"
    })
    @DisplayName("Should remove accents correctly")
    void testDeAccent(String input, String expected) {
      assertEquals(expected, StringUtil.deAccent(input));
    }

    @Test
    @DisplayName("Should handle strings without accents")
    void testDeAccentNoAccents() {
      assertEquals("hello world", StringUtil.deAccent("hello world"));
      assertEquals("", StringUtil.deAccent(""));
    }
  }

  @Nested
  @DisplayName("Encoding Tests")
  class EncodingTests {

    @Test
    @DisplayName("Should return null for null byte array in bytesToHex")
    void testBytesToHexNull() {
      assertNull(StringUtil.bytesToHex(null));
    }

    @Test
    @DisplayName("Should convert bytes to hex correctly")
    void testBytesToHex() {
      assertEquals(
          "415820615C78303139",
          StringUtil.bytesToHex("AX a\\x019".getBytes(StandardCharsets.UTF_8)));
      assertEquals("", StringUtil.bytesToHex(new byte[0]));
    }

    @Test
    @DisplayName("Should handle various byte values")
    void testBytesToHexVariousValues() {
      assertEquals("00FF", StringUtil.bytesToHex(new byte[] {0, -1}));
      assertEquals("7F80", StringUtil.bytesToHex(new byte[] {127, -128}));
    }

    @Test
    @DisplayName("Should convert integer to hex")
    void testIntegerToHex() {
      assertEquals("A45F", StringUtil.integerToHex(42079));
      assertEquals("0", StringUtil.integerToHex(0));
      assertEquals("FF", StringUtil.integerToHex(255));
      assertEquals("FFFFFFFF", StringUtil.integerToHex(-1));
    }

    @Test
    @DisplayName("Should return null for null input in bytesToMD5")
    void testBytesToMD5Null() throws NoSuchAlgorithmException {
      assertNull(StringUtil.bytesToMD5(null));
    }

    @Test
    @DisplayName("Should compute MD5 hash correctly")
    void testBytesToMD5() throws NoSuchAlgorithmException {
      assertEquals(
          "0F719D7161F722CE7B8E7F79629F0A2B",
          StringUtil.bytesToMD5("AX a\\x019".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    @DisplayName("Should handle empty byte array for MD5")
    void testBytesToMD5EmptyArray() throws NoSuchAlgorithmException {
      assertEquals("D41D8CD98F00B204E9800998ECF8427E", StringUtil.bytesToMD5(new byte[0]));
    }
  }

  @Nested
  @DisplayName("Null Handling Tests")
  class NullHandlingTests {

    @Test
    @DisplayName("Should handle null objects correctly")
    void testNullHandling() {
      assertNull(StringUtil.getNullIfNull(null));
      assertEquals("Object", StringUtil.getNullIfNull("Object"));
      assertEquals("42", StringUtil.getNullIfNull(42));
    }

    @Test
    @DisplayName("Should return empty string for null objects")
    void testEmptyStringForNull() {
      assertEquals("", StringUtil.getEmptyStringIfNull(null));
      assertEquals("Object", StringUtil.getEmptyStringIfNull("Object"));
      assertEquals("42", StringUtil.getEmptyStringIfNull(42));
    }

    @Test
    @DisplayName("Should handle null enum correctly")
    void testNullEnumHandling() {
      assertEquals("", StringUtil.getEmptyStringIfNullEnum(null));
      assertEquals("THREE_PTS", StringUtil.getEmptyStringIfNullEnum(StringUtil.Suffix.THREE_PTS));
    }
  }

  @Nested
  @DisplayName("Suffix Enum Tests")
  class SuffixEnumTests {

    @Test
    @DisplayName("Should return correct values for suffix enum")
    void testSuffixValues() {
      assertEquals("", StringUtil.Suffix.NO.getValue());
      assertEquals("_", StringUtil.Suffix.UNDERSCORE.getValue());
      assertEquals(".", StringUtil.Suffix.ONE_PTS.getValue());
      assertEquals("...", StringUtil.Suffix.THREE_PTS.getValue());
    }

    @Test
    @DisplayName("Should return correct lengths for suffix enum")
    void testSuffixLengths() {
      assertEquals(0, StringUtil.Suffix.NO.getLength());
      assertEquals(1, StringUtil.Suffix.UNDERSCORE.getLength());
      assertEquals(1, StringUtil.Suffix.ONE_PTS.getLength());
      assertEquals(3, StringUtil.Suffix.THREE_PTS.getLength());
    }

    @Test
    @DisplayName("Should return correct string representation")
    void testSuffixToString() {
      assertEquals("", StringUtil.Suffix.NO.toString());
      assertEquals("_", StringUtil.Suffix.UNDERSCORE.toString());
      assertEquals(".", StringUtil.Suffix.ONE_PTS.toString());
      assertEquals("...", StringUtil.Suffix.THREE_PTS.toString());
    }

    @Test
    @DisplayName("Should handle valueOf correctly")
    void testSuffixValueOf() {
      assertEquals(StringUtil.Suffix.UNDERSCORE, StringUtil.Suffix.valueOf("UNDERSCORE"));
      assertEquals(StringUtil.Suffix.THREE_PTS, StringUtil.Suffix.valueOf("THREE_PTS"));
    }
  }

  @Nested
  @DisplayName("Edge Cases and Integration Tests")
  class EdgeCaseTests {

    @Test
    @DisplayName("Should handle unicode characters correctly")
    void testUnicodeHandling() {
      assertEquals("𝓗𝓮𝓵𝓵𝓸", StringUtil.deAccent("𝓗𝓮𝓵𝓵𝓸"));
      assertTrue(StringUtil.hasText("𝓗𝓮𝓵𝓵𝓸"));
    }

    @Test
    @DisplayName("Should handle very long strings")
    void testVeryLongStrings() {
      String longString = "a".repeat(10000);
      assertTrue(StringUtil.hasText(longString));
      assertEquals(
          longString.length(),
          StringUtil.getTruncatedString(longString, 10000, StringUtil.Suffix.NO).length());
    }

    @Test
    @DisplayName("Should handle special characters in splitting")
    void testSpecialCharacterSplitting() {
      assertArrayEquals(new String[] {"a", "b", "c"}, StringUtil.getStringArray("a|b|c", "|"));
      assertArrayEquals(new String[] {"a", "b", "c"}, StringUtil.getStringArray("a*b*c", "*"));
    }

    @Test
    @DisplayName("Should handle extreme integer values")
    void testExtremeIntegerValues() {
      assertEquals(Integer.MAX_VALUE, StringUtil.getInt(String.valueOf(Integer.MAX_VALUE)));
      assertEquals(Integer.MIN_VALUE, StringUtil.getInt(String.valueOf(Integer.MIN_VALUE)));
    }
  }
}
