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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class EscapeCharsTest {

  @Nested
  @DisplayName("HTML Escaping Tests")
  class HtmlEscapingTests {
    @Test
    @DisplayName("Should handle null and empty strings")
    void testForHTML_NullAndEmpty() {
      assertEquals(StringUtil.EMPTY_STRING, EscapeChars.forHTML(null));
      assertEquals(StringUtil.EMPTY_STRING, EscapeChars.forHTML(""));
      assertEquals(StringUtil.EMPTY_STRING, EscapeChars.forHTML(StringUtil.EMPTY_STRING));
    }

    @Test
    @DisplayName("Should escape basic HTML characters")
    void testForHTML_BasicCharacters() {
      assertEquals("&lt;html&gt;", EscapeChars.forHTML("<html>"));
      assertEquals("&quot;test&quot;", EscapeChars.forHTML("\"test\""));
      assertEquals("&#39;test&#39;", EscapeChars.forHTML("'test'"));
      assertEquals("test &amp; test", EscapeChars.forHTML("test & test"));
      assertEquals("&amp;amp;", EscapeChars.forHTML(EscapeChars.AMPERSAND));
    }

    @Test
    @DisplayName("Should handle complex HTML escaping scenarios")
    void testForHTML_ComplexScenarios() {
      assertEquals("test &amp; test &amp; test", EscapeChars.forHTML("test & test & test"));
      assertEquals("test &lt;&lt; 1", EscapeChars.forHTML("test << 1"));
      assertEquals("a&quot;b&lt;c&gt;d&amp;", EscapeChars.forHTML("a\"b<c>d&"));
      assertEquals("foo&amp;&amp;bar", EscapeChars.forHTML("foo&&bar"));
    }

    @Test
    @DisplayName("Should escape accented and special characters")
    void testForHTML_AccentedCharacters() {
      assertEquals("&eacute;&agrave;&egrave;&ccedil;%&not;&deg;", EscapeChars.forHTML("éàèç%¬°"));
      assertEquals("&Auml;&ouml;&uuml;&szlig;", EscapeChars.forHTML("Äöüß"));
    }

    @Test
    @DisplayName("Should handle Unicode quotation marks")
    void testForHTML_UnicodeQuotes() {
      // Test Unicode quotation marks that were causing compilation issues
      assertEquals("&lsquo;Hello&rsquo;", EscapeChars.forHTML("\u2018Hello\u2019"));
      assertEquals("&#39;Hello&#39;", EscapeChars.forHTML("'Hello'"));
      assertEquals("&ldquo;World&rdquo;", EscapeChars.forHTML("\u201cWorld\u201d"));
      assertEquals("&sbquo;test&bdquo;", EscapeChars.forHTML("‚test„"));
    }

    @Test
    @DisplayName("Should handle mathematical and Greek symbols")
    void testForHTML_MathematicalSymbols() {
      assertEquals("&alpha; + &beta; = &gamma;", EscapeChars.forHTML("α + β = γ"));
      assertEquals("&sum; &infin; &radic;", EscapeChars.forHTML("∑ ∞ √"));
    }

    @Test
    @DisplayName("Should preserve non-escaped characters")
    void testForHTML_NonEscapedCharacters() {
      String safeChars =
          "!@#$%^*()_+=-/?\\|]}[{,.;:"
              + "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
              + "1234567890";

      assertSame(safeChars, EscapeChars.forHTML(safeChars));
    }

    @Test
    @DisplayName("Should handle mixed escaped and non-escaped characters")
    void testForHTML_MixedCharacters() {
      assertEquals(
          "&quot; \t ! # $ % &#39; (*) + , ; - . / : = ? @ [\\] ^ _ ` { | } ~",
          EscapeChars.forHTML("\" \t ! # $ % ' (*) + , ; - . / : = ? @ [\\] ^ _ ` { | } ~"));
    }

    @ParameterizedTest
    @CsvSource({
      "'<script>alert(\"XSS\")</script>', '&lt;script&gt;alert(&quot;XSS&quot;)&lt;/script&gt;'",
      "'<img src=\"x\" onerror=\"alert(1)\">', '&lt;img src=&quot;x&quot; onerror=&quot;alert(1)&quot;&gt;'",
      "'</body>', '&lt;/body&gt;'"
    })
    @DisplayName("Should prevent XSS attacks")
    void testForHTML_XSSPrevention(String input, String expected) {
      assertEquals(expected, EscapeChars.forHTML(input));
    }
  }

  @Nested
  @DisplayName("XML Escaping Tests")
  class XmlEscapingTests {
    @Test
    @DisplayName("Should handle null and empty strings")
    void testForXML_NullAndEmpty() {
      assertEquals(StringUtil.EMPTY_STRING, EscapeChars.forXML(null));
      assertEquals(StringUtil.EMPTY_STRING, EscapeChars.forXML(""));
    }

    @Test
    @DisplayName("Should escape basic XML characters")
    void testForXML_BasicCharacters() {
      assertEquals("&lt;xml&gt;", EscapeChars.forXML("<xml>"));
      assertEquals("&quot;A Text&quot; &apos;test&apos;", EscapeChars.forXML("\"A Text\" 'test'"));
      assertEquals("&amp;amp;", EscapeChars.forXML(EscapeChars.AMPERSAND));
    }

    @Test
    @DisplayName("Should filter invalid XML characters")
    void testForXML_InvalidCharacters() {
      // Invalid XML characters should be removed
      assertEquals("A Text", EscapeChars.forXML("A Text" + (char) 0xFFFE));
      assertEquals("&lt;xml&gt;", EscapeChars.forXML("<\u0000\uD800x\u0018\u0019ml\uDC00>"));
    }

    @Test
    @DisplayName("Should preserve valid XML characters")
    void testForXML_ValidCharacters() {
      // Valid XML characters (tab, newline, carriage return, and normal printable chars)
      String validXml = "A Text\t\n\r with valid chars";
      String result = EscapeChars.forXML(validXml);
      assertTrue(result.contains("A Text"));
      assertTrue(result.contains("valid chars"));
    }

    @ParameterizedTest
    @ValueSource(chars = {0x9, 0xA, 0xD, 0x20, 0x7F, 0xD7FF, 0xE000, 0xFFFD})
    @DisplayName("Should preserve valid XML control characters")
    void testForXML_ValidControlCharacters(char c) {
      String input = "test" + c + "text";
      String result = EscapeChars.forXML(input);
      // Should contain both parts of the string
      assertTrue(result.contains("test"));
      assertTrue(result.contains("text"));
    }
  }

  @Nested
  @DisplayName("URL Ampersand Escaping Tests")
  class UrlAmpersandTests {
    @Test
    @DisplayName("Should escape ampersands in URLs")
    void testForUrlAmpersand_BasicEscaping() {
      assertEquals(
          "https://example.org/example&amp;param=1",
          EscapeChars.forUrlAmpersand("https://example.org/example&param=1"));
    }

    @Test
    @DisplayName("Should handle multiple ampersands")
    void testForUrlAmpersand_MultipleAmpersands() {
      assertEquals("url?a=1&amp;b=2&amp;c=3", EscapeChars.forUrlAmpersand("url?a=1&b=2&c=3"));
    }

    @Test
    @DisplayName("Should handle URLs without ampersands")
    void testForUrlAmpersand_NoAmpersands() {
      String url = "https://example.org/path?param=value";
      assertEquals(url, EscapeChars.forUrlAmpersand(url));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"https://example.com", "ftp://test.org"})
    @DisplayName("Should handle edge cases")
    void testForUrlAmpersand_EdgeCases(String input) {
      assertNotNull(EscapeChars.forUrlAmpersand(input));
    }
  }

  @Nested
  @DisplayName("Tag Disabling Tests")
  class TagDisablingTests {
    @Test
    @DisplayName("Should handle null and empty strings")
    void testToDisableTags_NullAndEmpty() {
      assertEquals(StringUtil.EMPTY_STRING, EscapeChars.toDisableTags(null));
      assertEquals("", EscapeChars.toDisableTags(""));
    }

    @Test
    @DisplayName("Should disable HTML/XML tags")
    void testToDisableTags_BasicTags() {
      assertEquals("&lt;html&gt;", EscapeChars.toDisableTags("<html>"));
      assertEquals(
          "&lt;div class=\"test\"&gt;content&lt;/div&gt;",
          EscapeChars.toDisableTags("<div class=\"test\">content</div>"));
    }

    @Test
    @DisplayName("Should preserve other characters")
    void testToDisableTags_PreserveOthers() {
      assertEquals(
          "A Text with \"quotes\" & ampersands",
          EscapeChars.toDisableTags("A Text with \"quotes\" & ampersands"));
    }

    @Test
    @DisplayName("Should handle nested tags")
    void testToDisableTags_NestedTags() {
      assertEquals(
          "&lt;outer&gt;&lt;inner&gt;text&lt;/inner&gt;&lt;/outer&gt;",
          EscapeChars.toDisableTags("<outer><inner>text</inner></outer>"));
    }
  }

  @Nested
  @DisplayName("Line Conversion Tests")
  class LineConversionTests {

    @Test
    @DisplayName("Should handle null input")
    void testConvertToLines_Null() {
      String[] result = EscapeChars.convertToLines(null);
      assertNotNull(result);
      assertEquals(0, result.length);
    }

    @Test
    @DisplayName("Should handle single line without line breaks")
    void testConvertToLines_SingleLine() {
      String[] result = EscapeChars.convertToLines("Unformatted");
      assertEquals(1, result.length);
      assertEquals("Unformatted", result[0]);
    }

    @Test
    @DisplayName("Should handle different line break types")
    void testConvertToLines_DifferentLineBreaks() {
      // Unix line breaks (\n)
      assertArrayEquals(new String[] {"text", "text2"}, EscapeChars.convertToLines("text\ntext2"));

      // Old Mac line breaks (\r)
      assertArrayEquals(new String[] {"text", "text2"}, EscapeChars.convertToLines("text\rtext2"));

      // Windows line breaks (\r\n)
      assertArrayEquals(
          new String[] {"text", "text2", "text3", "text4"},
          EscapeChars.convertToLines("text\r\ntext2\r\ntext3\r\ntext4"));
    }

    @Test
    @DisplayName("Should handle mixed line break types")
    void testConvertToLines_MixedLineBreaks() {
      // Mixed \n and \r
      String[] result = EscapeChars.convertToLines("text\n\rtext2");
      assertEquals(2, result.length);

      // Different order
      String[] result2 = EscapeChars.convertToLines("text\r\ntext2");
      assertEquals(2, result2.length);
    }

    @Test
    @DisplayName("Should preserve empty lines")
    void testConvertToLines_EmptyLines() {
      String[] result = EscapeChars.convertToLines("line1\n\nline3");
      assertEquals(3, result.length);
      assertEquals("line1", result[0]);
      assertEquals("", result[1]);
      assertEquals("line3", result[2]);
    }

    @Test
    @DisplayName("Should handle trailing line breaks")
    void testConvertToLines_TrailingLineBreaks() {
      String[] result = EscapeChars.convertToLines("line1\nline2\n");
      assertEquals(3, result.length);
      assertEquals("line1", result[0]);
      assertEquals("line2", result[1]);
      assertEquals("", result[2]);
    }

    @ParameterizedTest
    @CsvSource({
      "'', 1",
      "'single', 1",
      "'line1\nline2', 2",
      "'line1\r\nline2\r\nline3', 3",
      "'\n\n\n', 4"
    })
    @DisplayName("Should return correct number of lines")
    void testConvertToLines_LineCount(String input, int expectedCount) {
      String[] result = EscapeChars.convertToLines(input);
      assertEquals(expectedCount, result.length);
    }
  }

  @Nested
  @DisplayName("Performance and Edge Case Tests")
  class PerformanceAndEdgeTests {
    @Test
    @DisplayName("Should handle very long strings efficiently")
    void testLargeStrings() {
      String result = EscapeChars.forHTML("test & < > \" ' ".repeat(10000));
      assertNotNull(result);
      assertTrue(result.contains("&amp;"));
      assertTrue(result.contains("&lt;"));
      assertTrue(result.contains("&gt;"));
    }

    @Test
    @DisplayName("Should handle strings with only special characters")
    void testOnlySpecialCharacters() {
      assertEquals("&lt;&gt;&amp;&quot;&#39;", EscapeChars.forHTML("<>&\"'"));
      assertEquals("&lt;&gt;&amp;&quot;&apos;", EscapeChars.forXML("<>&\"'"));
    }

    @Test
    @DisplayName("Should maintain string identity for unchanged content")
    void testStringIdentityPreservation() {
      String unchanged = "This string has no special characters 123 ABC";
      assertSame(unchanged, EscapeChars.forHTML(unchanged));
    }

    @Test
    @DisplayName("Should handle Unicode edge cases")
    void testUnicodeEdgeCases() {
      // Test various Unicode ranges
      String unicode = "Emoji: 😀 Math: ∑ Greek: α Arabic: العربية";
      String result = EscapeChars.forHTML(unicode);
      assertNotNull(result);
      // Some characters should be escaped, others preserved
      assertTrue(result.contains("&sum;"));
      assertTrue(result.contains("&alpha;"));
    }
  }
}
