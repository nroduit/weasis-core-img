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

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayNameGeneration(ReplaceUnderscores.class)
class EscapeCharsTest {

  // Test data structures for different escaping scenarios
  private static final Map<String, String> HTML_BASIC_ESCAPES =
      Map.of(
          "<html>",
          "&lt;html&gt;",
          "\"test\"",
          "&quot;test&quot;",
          "'test'",
          "&#39;test&#39;",
          "test & test",
          "test &amp; test",
          EscapeChars.AMPERSAND,
          "&amp;amp;");

  private static final Map<String, String> HTML_COMPLEX_ESCAPES =
      Map.of(
          "test & test & test", "test &amp; test &amp; test",
          "test << 1", "test &lt;&lt; 1",
          "a\"b<c>d&", "a&quot;b&lt;c&gt;d&amp;",
          "foo&&bar", "foo&amp;&amp;bar");

  private static final Map<String, String> HTML_ACCENTED_CHARS =
      Map.of(
          "éàèç%¬°", "&eacute;&agrave;&egrave;&ccedil;%&not;&deg;",
          "Äöüß", "&Auml;&ouml;&uuml;&szlig;");

  private static final Map<String, String> HTML_UNICODE_QUOTES =
      Map.of(
          "\u2018Hello\u2019", "&lsquo;Hello&rsquo;",
          "'Hello'", "&#39;Hello&#39;",
          "\u201cWorld\u201d", "&ldquo;World&rdquo;",
          "‚test„", "&sbquo;test&bdquo;");

  private static final Map<String, String> HTML_MATH_SYMBOLS =
      Map.of(
          "α + β = γ", "&alpha; + &beta; = &gamma;",
          "∑ ∞ √", "&sum; &infin; &radic;");

  private static final Map<String, String> XML_BASIC_ESCAPES =
      Map.of(
          "<xml>",
          "&lt;xml&gt;",
          "\"A Text\" 'test'",
          "&quot;A Text&quot; &apos;test&apos;",
          EscapeChars.AMPERSAND,
          "&amp;amp;");

  private static final List<String> SAFE_CHARACTERS =
      List.of(
          "!@#$%^*()_+=-/?\\|]}[{,.;:",
          "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ", "1234567890");

  private static final Map<String, String> URL_AMPERSAND_ESCAPES =
      Map.of(
          "https://example.org/example&param=1", "https://example.org/example&amp;param=1",
          "url?a=1&b=2&c=3", "url?a=1&amp;b=2&amp;c=3");

  private static final Map<String, String> TAG_DISABLE_ESCAPES =
      Map.of(
          "<html>", "&lt;html&gt;",
          "<div class=\"test\">content</div>", "&lt;div class=\"test\"&gt;content&lt;/div&gt;",
          "<script>alert('xss')</script>", "&lt;script&gt;alert('xss')&lt;/script&gt;");

  @Nested
  class Html_Escaping_Tests {

    @ParameterizedTest
    @NullAndEmptySource
    void should_handle_null_and_empty_strings(String input) {
      assertEquals(StringUtil.EMPTY_STRING, EscapeChars.forHTML(input));
    }

    @ParameterizedTest
    @MethodSource("org.weasis.core.util.EscapeCharsTest#htmlBasicEscapeData")
    void should_escape_basic_html_characters(String input, String expected) {
      assertEquals(expected, EscapeChars.forHTML(input));
    }

    @ParameterizedTest
    @MethodSource("org.weasis.core.util.EscapeCharsTest#htmlComplexEscapeData")
    void should_handle_complex_html_escaping_scenarios(String input, String expected) {
      assertEquals(expected, EscapeChars.forHTML(input));
    }

    @ParameterizedTest
    @MethodSource("org.weasis.core.util.EscapeCharsTest#htmlAccentedCharData")
    void should_escape_accented_and_special_characters(String input, String expected) {
      assertEquals(expected, EscapeChars.forHTML(input));
    }

    @ParameterizedTest
    @MethodSource("org.weasis.core.util.EscapeCharsTest#htmlUnicodeQuoteData")
    void should_handle_unicode_quotation_marks(String input, String expected) {
      assertEquals(expected, EscapeChars.forHTML(input));
    }

    @ParameterizedTest
    @MethodSource("org.weasis.core.util.EscapeCharsTest#htmlMathSymbolData")
    void should_handle_mathematical_and_greek_symbols(String input, String expected) {
      assertEquals(expected, EscapeChars.forHTML(input));
    }

    @ParameterizedTest
    @MethodSource("org.weasis.core.util.EscapeCharsTest#safeCharacterData")
    void should_preserve_non_escaped_characters(String safeChars) {
      assertSame(safeChars, EscapeChars.forHTML(safeChars));
    }

    @Test
    void should_handle_mixed_escaped_and_non_escaped_characters() {
      var input = "\" \t ! # $ % ' (*) + , ; - . / : = ? @ [\\] ^ _ ` { | } ~";
      var expected = "&quot; \t ! # $ % &#39; (*) + , ; - . / : = ? @ [\\] ^ _ ` { | } ~";
      assertEquals(expected, EscapeChars.forHTML(input));
    }

    @ParameterizedTest
    @CsvSource({
      "'<script>alert(\"XSS\")</script>', '&lt;script&gt;alert(&quot;XSS&quot;)&lt;/script&gt;'",
      "'<img src=\"x\" onerror=\"alert(1)\">', '&lt;img src=&quot;x&quot; onerror=&quot;alert(1)&quot;&gt;'",
      "'</body>', '&lt;/body&gt;'"
    })
    void should_prevent_xss_attacks(String input, String expected) {
      assertEquals(expected, EscapeChars.forHTML(input));
    }
  }

  @Nested
  class Xml_Escaping_Tests {

    @ParameterizedTest
    @NullAndEmptySource
    void should_handle_null_and_empty_strings(String input) {
      assertEquals(StringUtil.EMPTY_STRING, EscapeChars.forXML(input));
    }

    @ParameterizedTest
    @MethodSource("org.weasis.core.util.EscapeCharsTest#xmlBasicEscapeData")
    void should_escape_basic_xml_characters(String input, String expected) {
      assertEquals(expected, EscapeChars.forXML(input));
    }

    @Test
    void should_filter_invalid_xml_characters() {
      // Invalid XML characters should be removed
      assertEquals("A Text", EscapeChars.forXML("A Text" + (char) 0xFFFE));
      assertEquals("&lt;xml&gt;", EscapeChars.forXML("<\u0000\uD800x\u0018\u0019ml\uDC00>"));
    }

    @Test
    void should_preserve_valid_xml_characters() {
      var validXml = "A Text\t\n\r with valid chars";
      var result = EscapeChars.forXML(validXml);
      assertAll(
          () -> assertTrue(result.contains("A Text")),
          () -> assertTrue(result.contains("valid chars")));
    }

    @ParameterizedTest
    @ValueSource(chars = {0x9, 0xA, 0xD, 0x20, 0x7F, 0xD7FF, 0xE000, 0xFFFD})
    void should_preserve_valid_xml_control_characters(char c) {
      var input = "test" + c + "text";
      var result = EscapeChars.forXML(input);
      assertAll(
          () -> assertTrue(result.contains("test")), () -> assertTrue(result.contains("text")));
    }
  }

  @Nested
  class Url_Ampersand_Tests {

    @ParameterizedTest
    @MethodSource("org.weasis.core.util.EscapeCharsTest#urlAmpersandEscapeData")
    void should_escape_ampersands_in_urls(String input, String expected) {
      assertEquals(expected, EscapeChars.forUrlAmpersand(input));
    }

    @Test
    void should_handle_multiple_ampersands() {
      assertEquals("url?a=1&amp;b=2&amp;c=3", EscapeChars.forUrlAmpersand("url?a=1&b=2&c=3"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"https://example.com", "ftp://test.org"})
    void should_handle_urls_without_ampersands(String url) {
      assertEquals(url, EscapeChars.forUrlAmpersand(url));
    }

    @ParameterizedTest
    @NullAndEmptySource
    void should_handle_null_and_empty_urls(String input) {
      assertEquals(StringUtil.EMPTY_STRING, EscapeChars.forUrlAmpersand(input));
    }
  }

  @Nested
  class Tag_Disabling_Tests {

    @ParameterizedTest
    @NullAndEmptySource
    void should_handle_null_and_empty_strings(String input) {
      var result = EscapeChars.toDisableTags(input);
      assertTrue(result.isEmpty());
    }

    @ParameterizedTest
    @MethodSource("org.weasis.core.util.EscapeCharsTest#tagDisableEscapeData")
    void should_disable_html_xml_tags(String input, String expected) {
      assertEquals(expected, EscapeChars.toDisableTags(input));
    }

    @Test
    void should_preserve_other_characters() {
      var input = "This is plain text with & symbols but no tags";
      assertEquals(input, EscapeChars.toDisableTags(input));
    }

    @Test
    void should_handle_nested_tags() {
      var input = "<div><span>nested</span></div>";
      var expected = "&lt;div&gt;&lt;span&gt;nested&lt;/span&gt;&lt;/div&gt;";
      assertEquals(expected, EscapeChars.toDisableTags(input));
    }
  }

  @Nested
  class Line_Conversion_Tests {

    @Test
    void should_handle_null_input() {
      assertArrayEquals(new String[0], EscapeChars.convertToLines(null));
    }

    @Test
    void should_handle_single_line_without_line_breaks() {
      var input = "single line";
      assertArrayEquals(new String[] {input}, EscapeChars.convertToLines(input));
    }

    @ParameterizedTest
    @CsvSource({
      "'line1\nline2', 2, \\n",
      "'line1\r\nline2', 2, \\r\\n",
      "'line1\rline2', 2, \\r",
      "'line1\n\rline2', 2, \\n\\r"
    })
    void should_handle_different_line_break_types(
        String input, int expectedLines, String delimiter) {
      var result = EscapeChars.convertToLines(input);
      assertEquals(expectedLines, result.length);
      assertEquals(
          input.split(delimiter.equals("\\n\\r") ? "\n\r" : delimiter, -1).length, result.length);
    }

    @Test
    void should_handle_mixed_line_break_types() {
      var input = "line1\r\nline2\nline3\rline4";
      var result = EscapeChars.convertToLines(input);
      assertTrue(result.length >= 4);
    }

    @Test
    void should_preserve_empty_lines() {
      var input = "line1\n\nline3";
      var result = EscapeChars.convertToLines(input);
      assertAll(
          () -> assertEquals(3, result.length),
          () -> assertEquals("line1", result[0]),
          () -> assertEquals("", result[1]),
          () -> assertEquals("line3", result[2]));
    }

    @Test
    void should_handle_trailing_line_breaks() {
      var input = "line1\nline2\n";
      var result = EscapeChars.convertToLines(input);
      assertAll(
          () -> assertEquals(3, result.length),
          () -> assertEquals("line1", result[0]),
          () -> assertEquals("line2", result[1]),
          () -> assertEquals("", result[2]));
    }

    @ParameterizedTest
    @CsvSource({
      "'', 1",
      "'single', 1",
      "'line1\nline2', 2",
      "'line1\r\nline2\r\nline3', 3",
      "'\n\n\n', 4"
    })
    void should_return_correct_number_of_lines(String input, int expectedCount) {
      assertEquals(expectedCount, EscapeChars.convertToLines(input).length);
    }
  }

  @Nested
  class Performance_And_Edge_Tests {

    @Test
    void should_handle_very_long_strings_efficiently() {
      var largeString = "a".repeat(10000);
      var start = System.nanoTime();
      var result = EscapeChars.forHTML(largeString);
      var duration = System.nanoTime() - start;

      assertAll(
          () ->
              assertSame(largeString, result, "Should return same instance for unchanged content"),
          () -> assertTrue(duration < 200_000_000, "Should complete within 200ms"));
    }

    @Test
    void should_handle_strings_with_only_special_characters() {
      var specialChars = "<>&\"'";
      var result = EscapeChars.forHTML(specialChars);
      assertEquals("&lt;&gt;&amp;&quot;&#39;", result);
    }

    @Test
    void should_maintain_string_identity_for_unchanged_content() {
      var unchangedString = "This string has no special characters to escape";

      assertAll(
          () -> assertSame(unchangedString, EscapeChars.forHTML(unchangedString)),
          () -> assertSame(unchangedString, EscapeChars.forXML(unchangedString)),
          () -> assertSame(unchangedString, EscapeChars.toDisableTags(unchangedString)));
    }

    @Test
    void should_handle_unicode_edge_cases() {
      // High surrogate pairs and other Unicode edge cases
      var unicodeString = "𝓗𝓮𝓵𝓵𝓸 🌍";
      var result = EscapeChars.forHTML(unicodeString);

      assertNotNull(result);
      assertTrue(result.length() >= unicodeString.length());
    }

    @Test
    void should_handle_mixed_content_types() {
      var mixedContent = "Normal text <script> & special chars: αβγ ∞ 'quotes' \"double\" ©®™";
      var htmlResult = EscapeChars.forHTML(mixedContent);
      var xmlResult = EscapeChars.forXML(mixedContent);

      assertAll(
          () -> assertNotEquals(mixedContent, htmlResult),
          () -> assertNotEquals(mixedContent, xmlResult),
          () -> assertTrue(htmlResult.contains("&lt;script&gt;")),
          () -> assertTrue(xmlResult.contains("&lt;script&gt;")));
    }
  }

  // ======== Data provider methods ========
  static Stream<Arguments> htmlBasicEscapeData() {
    return HTML_BASIC_ESCAPES.entrySet().stream()
        .map(entry -> Arguments.of(entry.getKey(), entry.getValue()));
  }

  static Stream<Arguments> htmlComplexEscapeData() {
    return HTML_COMPLEX_ESCAPES.entrySet().stream()
        .map(entry -> Arguments.of(entry.getKey(), entry.getValue()));
  }

  static Stream<Arguments> htmlAccentedCharData() {
    return HTML_ACCENTED_CHARS.entrySet().stream()
        .map(entry -> Arguments.of(entry.getKey(), entry.getValue()));
  }

  static Stream<Arguments> htmlUnicodeQuoteData() {
    return HTML_UNICODE_QUOTES.entrySet().stream()
        .map(entry -> Arguments.of(entry.getKey(), entry.getValue()));
  }

  static Stream<Arguments> htmlMathSymbolData() {
    return HTML_MATH_SYMBOLS.entrySet().stream()
        .map(entry -> Arguments.of(entry.getKey(), entry.getValue()));
  }

  static Stream<Arguments> xmlBasicEscapeData() {
    return XML_BASIC_ESCAPES.entrySet().stream()
        .map(entry -> Arguments.of(entry.getKey(), entry.getValue()));
  }

  static Stream<Arguments> urlAmpersandEscapeData() {
    return URL_AMPERSAND_ESCAPES.entrySet().stream()
        .map(entry -> Arguments.of(entry.getKey(), entry.getValue()));
  }

  static Stream<Arguments> tagDisableEscapeData() {
    return TAG_DISABLE_ESCAPES.entrySet().stream()
        .map(entry -> Arguments.of(entry.getKey(), entry.getValue()));
  }

  static Stream<String> safeCharacterData() {
    return SAFE_CHARACTERS.stream();
  }
}
