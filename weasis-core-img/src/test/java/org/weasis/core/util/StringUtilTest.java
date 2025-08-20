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
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayNameGeneration(ReplaceUnderscores.class)
class StringUtilTest {

  @Nested
  class String_Truncation_Tests {

    @Test
    void should_return_null_when_input_is_null() {
      assertNull(StringUtil.getTruncatedString(null, 5, StringUtil.Suffix.NO));
    }

    @Test
    void should_return_original_string_when_length_is_within_limit() {
      assertEquals("Name", StringUtil.getTruncatedString("Name", 10, StringUtil.Suffix.NO));
      assertEquals("Name", StringUtil.getTruncatedString("Name", 4, StringUtil.Suffix.NO));
    }

    @Test
    void should_handle_zero_or_negative_limits() {
      assertEquals("Name", StringUtil.getTruncatedString("Name", -1, StringUtil.Suffix.NO));
      assertEquals("Name", StringUtil.getTruncatedString("Name", 0, StringUtil.Suffix.NO));
    }

    @Test
    void should_handle_null_suffix_as_NO_suffix() {
      assertEquals("N", StringUtil.getTruncatedString("Name", 1, null));
    }

    static Stream<Arguments> truncation_test_cases() {
      return Stream.of(
          Arguments.of("Name", 3, StringUtil.Suffix.UNDERSCORE, "Na_"),
          Arguments.of("Name", 3, StringUtil.Suffix.ONE_PTS, "Na."),
          Arguments.of("Longer phrase", 11, StringUtil.Suffix.THREE_PTS, "Longer p..."),
          Arguments.of("Short", 10, StringUtil.Suffix.THREE_PTS, "Short"),
          Arguments.of("VeryLongString", 5, StringUtil.Suffix.NO, "VeryL"),
          Arguments.of("Test", 4, StringUtil.Suffix.UNDERSCORE, "Test"));
    }

    @ParameterizedTest
    @MethodSource("truncation_test_cases")
    void should_truncate_with_different_suffixes(
        String input, int limit, StringUtil.Suffix suffix, String expected) {
      assertEquals(expected, StringUtil.getTruncatedString(input, limit, suffix));
    }

    @Test
    void should_handle_empty_string() {
      assertEquals("", StringUtil.getTruncatedString("", 5, StringUtil.Suffix.NO));
    }

    @Test
    void should_handle_case_where_suffix_is_longer_than_limit() {
      assertEquals("Test", StringUtil.getTruncatedString("Test", 2, StringUtil.Suffix.THREE_PTS));
    }
  }

  @Nested
  class Character_Extraction_Tests {

    static Stream<Arguments> valid_first_character_cases() {
      return Stream.of(
          Arguments.of("Val", 'V'),
          Arguments.of("A", 'A'),
          Arguments.of(" test", ' '),
          Arguments.of("123", '1'),
          Arguments.of("αβγ", 'α'));
    }

    @ParameterizedTest
    @MethodSource("valid_first_character_cases")
    void should_return_first_character_of_non_empty_string(String input, char expected) {
      assertEquals(expected, StringUtil.getFirstCharacter(input));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t\n", "\r\n\t "})
    void should_return_null_for_invalid_strings(String input) {
      assertNull(StringUtil.getFirstCharacter(input));
    }
  }

  @Nested
  class String_Array_Parsing_Tests {

    @Test
    void should_return_empty_array_for_null_delimiter() {
      assertArrayEquals(new String[0], StringUtil.getStringArray("Val", null));
    }

    @Test
    void should_return_empty_array_for_empty_string() {
      assertArrayEquals(new String[0], StringUtil.getStringArray("", ","));
    }

    static Stream<Arguments> string_array_test_cases() {
      return Stream.of(
          Arguments.of("Val,v2,v3", ",", new String[] {"Val", "v2", "v3"}),
          Arguments.of("a.b.c", ".", new String[] {"a", "b", "c"}),
          Arguments.of("a|b|c", "|", new String[] {"a", "b", "c"}),
          Arguments.of("single", ",", new String[] {"single"}),
          Arguments.of("a::b::c", "::", new String[] {"a", "b", "c"}));
    }

    @ParameterizedTest
    @MethodSource("string_array_test_cases")
    void should_split_string_correctly(String input, String delimiter, String[] expected) {
      assertArrayEquals(expected, StringUtil.getStringArray(input, delimiter));
    }

    @Test
    void should_handle_empty_segments() {
      var result = StringUtil.getStringArray(",a,,b,c,,", ",");
      assertArrayEquals(new String[] {"", "a", "", "b", "c"}, result);
    }
  }

  @Nested
  class Integer_Array_Parsing_Tests {

    @Test
    void should_return_empty_array_for_null_delimiter() {
      assertArrayEquals(new int[0], StringUtil.getIntegerArray("3", null));
    }

    @Test
    void should_return_empty_array_for_empty_string() {
      assertArrayEquals(new int[0], StringUtil.getIntegerArray("", " "));
    }

    static Stream<Arguments> integer_array_test_cases() {
      return Stream.of(
          Arguments.of("1,2,3", ",", new int[] {1, 2, 3}),
          Arguments.of("5 " + Integer.MIN_VALUE, " ", new int[] {5, Integer.MIN_VALUE}),
          Arguments.of("-5,-10,15", ",", new int[] {-5, -10, 15}),
          Arguments.of("0", ",", new int[] {0}),
          Arguments.of("1,abc,3", ",", new int[] {1, 0, 3}));
    }

    @ParameterizedTest
    @MethodSource("integer_array_test_cases")
    void should_parse_integer_arrays(String input, String delimiter, int[] expected) {
      assertArrayEquals(expected, StringUtil.getIntegerArray(input, delimiter));
    }
  }

  @Nested
  class Number_Parsing_Tests {

    @Nested
    class Integer_Parsing_Tests {

      static Stream<String> valid_integer_strings() {
        return Stream.of(
            "42",
            "  42  ",
            "0",
            "-42",
            String.valueOf(Integer.MAX_VALUE),
            String.valueOf(Integer.MIN_VALUE),
            "\t123\n");
      }

      @ParameterizedTest
      @MethodSource("valid_integer_strings")
      void should_parse_valid_integers(String input) {
        assertNotNull(StringUtil.getInteger(input));
      }

      static Stream<String> invalid_integer_strings() {
        return Stream.of("42a", "abc", "  ", "42.5", "42.0", "1.23e10");
      }

      @ParameterizedTest
      @NullAndEmptySource
      @MethodSource("invalid_integer_strings")
      void should_return_null_for_invalid_integers(String input) {
        assertNull(StringUtil.getInteger(input));
      }

      static Stream<Arguments> integer_parsing_test_cases() {
        return Stream.of(
            Arguments.of("42", 42),
            Arguments.of("-42", -42),
            Arguments.of("0", 0),
            Arguments.of("  123  ", 123));
      }

      @ParameterizedTest
      @MethodSource("integer_parsing_test_cases")
      void should_parse_valid_integers_to_int(String input, int expected) {
        assertEquals(expected, StringUtil.getInt(input));
      }

      @ParameterizedTest
      @NullAndEmptySource
      @ValueSource(strings = {"42a", "abc", "  "})
      void should_return_default_for_invalid_int_parsing(String input) {
        assertEquals(0, StringUtil.getInt(input));
      }

      static Stream<Arguments> int_with_default_test_cases() {
        return Stream.of(
            Arguments.of("42", 10, 42),
            Arguments.of(null, 10, 10),
            Arguments.of("foo", 35, 35),
            Arguments.of("", 99, 99),
            Arguments.of("123", -1, 123));
      }

      @ParameterizedTest
      @MethodSource("int_with_default_test_cases")
      void should_return_correct_value_with_default(String input, int defaultValue, int expected) {
        assertEquals(expected, StringUtil.getInt(input, defaultValue));
      }
    }

    @Nested
    class Double_Parsing_Tests {

      static Stream<String> valid_double_strings() {
        return Stream.of("42.0", "  42.5  ", "0.0", "-42.7", "42", "3.14159", "1e10", "-1.23E-4");
      }

      @ParameterizedTest
      @MethodSource("valid_double_strings")
      void should_parse_valid_doubles(String input) {
        assertNotNull(StringUtil.getDouble(input));
      }

      static Stream<String> invalid_double_strings() {
        return Stream.of("abc", "  ", "42.5a", "42.5.5", "42,5", "N", "Inf");
      }

      @ParameterizedTest
      @NullAndEmptySource
      @MethodSource("invalid_double_strings")
      void should_return_null_for_invalid_doubles(String input) {
        assertNull(StringUtil.getDouble(input));
      }

      static Stream<Double> specific_double_values() {
        return Stream.of(42.0, -42.7, 0.0, Math.PI, Double.MAX_VALUE, Double.MIN_VALUE);
      }

      @ParameterizedTest
      @MethodSource("specific_double_values")
      void should_parse_specific_double_values_correctly(double value) {
        var stringValue = String.valueOf(value);
        assertEquals(value, StringUtil.getDouble(stringValue), 0.001);
      }
    }
  }

  @Nested
  class Text_Manipulation_Tests {

    @Test
    void should_return_null_for_null_input_in_camelCase_splitting() {
      assertNull(StringUtil.splitCamelCaseString(null));
    }

    static Stream<Arguments> camel_case_test_cases() {
      return Stream.of(
          Arguments.of("camelCase", "camel Case"),
          Arguments.of("CamelCase", "Camel Case"),
          Arguments.of("XMLParser", "XML Parser"),
          Arguments.of("simpleTest", "simple Test"),
          Arguments.of("HTTPSConnection", "HTTPS Connection"),
          Arguments.of("word", "word"),
          Arguments.of("WORD", "WORD"),
          Arguments.of("", ""),
          Arguments.of("iPhone", "i Phone"),
          Arguments.of("HTML5Parser", "HTML5 Parser"));
    }

    @ParameterizedTest
    @MethodSource("camel_case_test_cases")
    void should_split_camelCase_strings_correctly(String input, String expected) {
      assertEquals(expected, StringUtil.splitCamelCaseString(input));
    }
  }

  @Nested
  class Text_Validation_Tests {

    static Stream<String> strings_with_length() {
      return Stream.of("Str", "   ", "a", "\t", "\n", "0");
    }

    @ParameterizedTest
    @MethodSource("strings_with_length")
    void should_return_true_for_strings_with_length(String input) {
      assertTrue(StringUtil.hasLength(input));
    }

    static Stream<CharSequence> sequences_without_length() {
      return Stream.of(null, "", new StringBuilder());
    }

    @ParameterizedTest
    @MethodSource("sequences_without_length")
    void should_return_false_for_sequences_without_length(CharSequence input) {
      assertFalse(StringUtil.hasLength(input));
    }

    static Stream<String> strings_with_text() {
      return Stream.of("Str", "  a  ", "a", "\ta\n", "0", "Hello World");
    }

    @ParameterizedTest
    @MethodSource("strings_with_text")
    void should_return_true_for_strings_with_text(String input) {
      assertTrue(StringUtil.hasText(input));
    }

    static Stream<CharSequence> sequences_without_text() {
      return Stream.of(null, "", "     ", "\t\n\r", "\u001F\u001C", new StringBuilder());
    }

    @ParameterizedTest
    @MethodSource("sequences_without_text")
    void should_return_false_for_sequences_without_text(CharSequence input) {
      assertFalse(StringUtil.hasText(input));
    }
  }

  @Nested
  class Text_Processing_Tests {

    @Test
    void should_return_null_for_null_input_in_deAccent() {
      assertNull(StringUtil.deAccent(null));
    }

    static Stream<Arguments> accent_removal_test_cases() {
      return Stream.of(
          Arguments.of("Á É Í Ó Ú", "A E I O U"),
          Arguments.of("café", "cafe"),
          Arguments.of("naïve", "naive"),
          Arguments.of("résumé", "resume"),
          Arguments.of("piñata", "pinata"),
          Arguments.of("Zürich", "Zurich"),
          Arguments.of("hello world", "hello world"),
          Arguments.of("", ""),
          Arguments.of("Ñoño", "Nono"),
          Arguments.of("São Paulo", "Sao Paulo"));
    }

    @ParameterizedTest
    @MethodSource("accent_removal_test_cases")
    void should_remove_accents_correctly(String input, String expected) {
      assertEquals(expected, StringUtil.deAccent(input));
    }
  }

  @Nested
  class Encoding_Tests {

    @Test
    void should_return_null_for_null_byte_array_in_bytesToHex() {
      assertNull(StringUtil.bytesToHex(null));
    }

    static Stream<Arguments> bytes_to_hex_test_cases() {
      return Stream.of(
          Arguments.of(new byte[0], ""),
          Arguments.of(new byte[] {0, -1}, "00FF"),
          Arguments.of(new byte[] {127, -128}, "7F80"),
          Arguments.of("Hello".getBytes(StandardCharsets.UTF_8), "48656C6C6F"));
    }

    @ParameterizedTest
    @MethodSource("bytes_to_hex_test_cases")
    void should_convert_bytes_to_hex_correctly(byte[] input, String expected) {
      assertEquals(expected, StringUtil.bytesToHex(input));
    }

    static Stream<Arguments> integer_to_hex_test_cases() {
      return Stream.of(
          Arguments.of(42079, "A45F"),
          Arguments.of(0, "0"),
          Arguments.of(255, "FF"),
          Arguments.of(-1, "FFFFFFFF"),
          Arguments.of(Integer.MAX_VALUE, "7FFFFFFF"),
          Arguments.of(Integer.MIN_VALUE, "80000000"));
    }

    @ParameterizedTest
    @MethodSource("integer_to_hex_test_cases")
    void should_convert_integer_to_hex(int input, String expected) {
      assertEquals(expected, StringUtil.integerToHex(input));
    }

    @Test
    void should_return_null_for_null_input_in_bytesToMD5() throws NoSuchAlgorithmException {
      assertNull(StringUtil.bytesToMD5(null));
    }

    @Test
    void should_compute_MD5_hash_correctly() throws NoSuchAlgorithmException {
      var input = "Hello World".getBytes(StandardCharsets.UTF_8);
      var expected = "B10A8DB164E0754105B7A99BE72E3FE5";
      assertEquals(expected, StringUtil.bytesToMD5(input));
    }

    @Test
    void should_handle_empty_byte_array_for_MD5() throws NoSuchAlgorithmException {
      assertEquals("D41D8CD98F00B204E9800998ECF8427E", StringUtil.bytesToMD5(new byte[0]));
    }
  }

  @Nested
  class Null_Handling_Tests {

    static Stream<Arguments> null_handling_test_cases() {
      return Stream.of(
          Arguments.of(null, null),
          Arguments.of("Object", "Object"),
          Arguments.of(42, "42"),
          Arguments.of(true, "true"),
          Arguments.of(List.of(1, 2, 3), "[1, 2, 3]"));
    }

    @ParameterizedTest
    @MethodSource("null_handling_test_cases")
    void should_handle_null_objects_correctly(Object input, String expected) {
      assertEquals(expected, StringUtil.getNullIfNull(input));
    }

    static Stream<Arguments> empty_string_for_null_test_cases() {
      return Stream.of(
          Arguments.of(null, ""),
          Arguments.of("Object", "Object"),
          Arguments.of(42, "42"),
          Arguments.of(false, "false"));
    }

    @ParameterizedTest
    @MethodSource("empty_string_for_null_test_cases")
    void should_return_empty_string_for_null_objects(Object input, String expected) {
      assertEquals(expected, StringUtil.getEmptyStringIfNull(input));
    }

    @Test
    void should_handle_null_enum_correctly() {
      assertEquals("", StringUtil.getEmptyStringIfNullEnum(null));
    }

    @ParameterizedTest
    @EnumSource(StringUtil.Suffix.class)
    void should_handle_enum_values_correctly(StringUtil.Suffix suffix) {
      assertEquals(suffix.name(), StringUtil.getEmptyStringIfNullEnum(suffix));
    }
  }

  @Nested
  class Suffix_Enum_Tests {

    static Stream<Arguments> suffix_value_test_cases() {
      return Stream.of(
          Arguments.of(StringUtil.Suffix.NO, ""),
          Arguments.of(StringUtil.Suffix.UNDERSCORE, "_"),
          Arguments.of(StringUtil.Suffix.ONE_PTS, "."),
          Arguments.of(StringUtil.Suffix.THREE_PTS, "..."));
    }

    @ParameterizedTest
    @MethodSource("suffix_value_test_cases")
    void should_return_correct_values_for_suffix_enum(StringUtil.Suffix suffix, String expected) {
      assertEquals(expected, suffix.getValue());
      assertEquals(expected, suffix.toString());
    }

    static Stream<Arguments> suffix_length_test_cases() {
      return Stream.of(
          Arguments.of(StringUtil.Suffix.NO, 0),
          Arguments.of(StringUtil.Suffix.UNDERSCORE, 1),
          Arguments.of(StringUtil.Suffix.ONE_PTS, 1),
          Arguments.of(StringUtil.Suffix.THREE_PTS, 3));
    }

    @ParameterizedTest
    @MethodSource("suffix_length_test_cases")
    void should_return_correct_lengths_for_suffix_enum(StringUtil.Suffix suffix, int expected) {
      assertEquals(expected, suffix.getLength());
    }

    @ParameterizedTest
    @ValueSource(strings = {"NO", "UNDERSCORE", "ONE_PTS", "THREE_PTS"})
    void should_handle_valueOf_correctly(String enumName) {
      assertDoesNotThrow(() -> StringUtil.Suffix.valueOf(enumName));
    }
  }

  @Nested
  class Edge_Cases_and_Integration_Tests {

    @Test
    void should_handle_unicode_characters_correctly() {
      // Use Latin-1 compatible Unicode characters
      assertEquals("cafe", StringUtil.deAccent("café")); // Already without accents
      assertTrue(StringUtil.hasText("café"));
      assertEquals('c', StringUtil.getFirstCharacter("café"));

      // Test with accented characters that will be de-accented
      assertEquals("naive", StringUtil.deAccent("naïve"));
      assertTrue(StringUtil.hasText("naïve"));
      assertEquals('n', StringUtil.getFirstCharacter("naïve"));

      // Test with Latin-1 extended characters
      assertEquals("Zurich", StringUtil.deAccent("Zürich"));
      assertTrue(StringUtil.hasText("Zürich"));
      assertEquals('Z', StringUtil.getFirstCharacter("Zürich"));
    }

    @Test
    void should_handle_very_long_strings() {
      var longString = "a".repeat(10000);
      assertTrue(StringUtil.hasText(longString));
      assertEquals(
          longString.length(),
          StringUtil.getTruncatedString(longString, 10000, StringUtil.Suffix.NO).length());
    }

    @Test
    void should_handle_special_characters_in_splitting() {
      assertArrayEquals(new String[] {"a", "b", "c"}, StringUtil.getStringArray("a|b|c", "|"));
      assertArrayEquals(new String[] {"a", "b", "c"}, StringUtil.getStringArray("a*b*c", "*"));
      assertArrayEquals(
          new String[] {"a", "b", "c"}, StringUtil.getStringArray("a[SPLIT]b[SPLIT]c", "[SPLIT]"));
    }

    @Test
    void should_handle_extreme_integer_values() {
      assertEquals(Integer.MAX_VALUE, StringUtil.getInt(String.valueOf(Integer.MAX_VALUE)));
      assertEquals(Integer.MIN_VALUE, StringUtil.getInt(String.valueOf(Integer.MIN_VALUE)));
    }

    @Test
    void should_handle_collator_initialization() {
      assertNotNull(StringUtil.collator);
      assertEquals(java.text.Collator.PRIMARY, StringUtil.collator.getStrength());
    }

    @Test
    void should_handle_whitespace_edge_cases() {
      // Test various types of whitespace - all should return false for hasText()
      // because Character.isWhitespace() correctly identifies them as whitespace

      assertFalse(StringUtil.hasText("\u2000")); // En quad
      assertFalse(StringUtil.hasText("\u2001")); // Em quad
      assertFalse(StringUtil.hasText("\u2002")); // En space
      assertFalse(StringUtil.hasText("\u2003")); // Em space
      assertFalse(StringUtil.hasText("\u2009")); // Thin space

      // But they should all have length
      assertTrue(StringUtil.hasLength("\u00A0"));
      assertTrue(StringUtil.hasLength("\u2000"));
      assertTrue(StringUtil.hasLength("\u2001"));

      // But strings with actual content mixed with Unicode whitespace should return true
      assertTrue(StringUtil.hasText("a\u00A0b"));
      assertTrue(StringUtil.hasText("\u2000text\u2001"));

      // Test regular ASCII whitespace
      assertFalse(StringUtil.hasText(" \t\n\r\f"));
    }

    static Stream<Arguments> comprehensive_integration_test_cases() {
      return Stream.of(
          Arguments.of(
              "Test123,456,invalid,789",
              ",",
              6,
              StringUtil.Suffix.THREE_PTS,
              new int[] {0, 456, 0, 789},
              "Tes..."),
          Arguments.of("A,B,C", ",", 10, StringUtil.Suffix.NO, new int[] {0, 0, 0}, "A,B,C"));
    }

    @ParameterizedTest
    @MethodSource("comprehensive_integration_test_cases")
    void should_handle_comprehensive_integration_scenarios(
        String input,
        String delimiter,
        int truncateLimit,
        StringUtil.Suffix suffix,
        int[] expectedInts,
        String expectedTruncated) {

      assertArrayEquals(expectedInts, StringUtil.getIntegerArray(input, delimiter));
      assertEquals(expectedTruncated, StringUtil.getTruncatedString(input, truncateLimit, suffix));
      assertTrue(StringUtil.hasText(input));
      assertNotNull(StringUtil.getFirstCharacter(input));
    }
  }
}
