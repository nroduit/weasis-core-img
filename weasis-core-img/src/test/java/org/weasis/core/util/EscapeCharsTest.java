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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

class EscapeCharsTest {

  /** Method under test: {@link EscapeChars#forHTML(String)} */
  @Test
  void testForHTML() {
    assertEquals("A Text", EscapeChars.forHTML("A Text"));
    assertEquals(StringUtil.EMPTY_STRING, EscapeChars.forHTML(StringUtil.EMPTY_STRING));
    assertEquals("&amp;amp;", EscapeChars.forHTML(EscapeChars.AMPERSAND));
    assertEquals("&lt;html&gt;", EscapeChars.forHTML("<html>"));
    assertEquals("&quot;test&quot;", EscapeChars.forHTML("\"test\""));
    assertEquals("&#39;test&#39;", EscapeChars.forHTML("\'test'"));
    assertEquals("test &amp; test &amp; test", EscapeChars.forHTML("test & test & test"));
    assertEquals("test &lt;&lt; 1", EscapeChars.forHTML("test << 1"));
    assertEquals("a&quot;b&lt;c&gt;d&amp;", EscapeChars.forHTML("a\"b<c>d&"));
    assertEquals("foo&amp;&amp;bar", EscapeChars.forHTML("foo&&bar"));
    assertEquals("&eacute;&agrave;&egrave;&ccedil;%&not;&deg;", EscapeChars.forHTML("éàèç%¬°"));
    // Non-escaped characters.
    String s =
        "!@#$%^*()_+=-/?\\|]}[{,.;:"
            + "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
            + "1234567890";
    assertSame(s, EscapeChars.forHTML(s));

    assertEquals(
        "&quot; \t ! # $ % &#39; (*) + , ; - . / : = ? @ [\\] ^ _ ` { | } ~",
        EscapeChars.forHTML("\" \t ! # $ % ' (*) + , ; - . / : = ? @ [\\] ^ _ ` { | } ~"));
  }

  /** Method under test: {@link EscapeChars#forUrlAmpersand(String)} */
  @Test
  void testForUrlAmpersand() {
    assertEquals(
        "https://example.org/example&amp;param=1",
        EscapeChars.forUrlAmpersand("https://example.org/example&param=1"));
  }

  /** Method under test: {@link EscapeChars#forXML(String)} */
  @Test
  void testForXML() {
    assertEquals("A Text", EscapeChars.forXML("A Text"));
    assertEquals(StringUtil.EMPTY_STRING, EscapeChars.forXML(StringUtil.EMPTY_STRING));
    assertEquals("&amp;amp;", EscapeChars.forXML(EscapeChars.AMPERSAND));
    assertEquals("&lt;xml&gt;", EscapeChars.forXML("<xml>"));
    assertEquals("&quot;A Text&quot; &apos;test&apos;", EscapeChars.forXML("\"A Text\" 'test'"));
    assertEquals("A Text", EscapeChars.forXML("A Text" + (char) 0xFFFE));
    assertEquals("&lt;xml&gt;", EscapeChars.forXML("<\u0000\uD800x\u0018\u0019ml\uDC00>"));
  }

  /** Method under test: {@link EscapeChars#toDisableTags(String)} */
  @Test
  void testToDisableTags() {
    assertEquals("A Text", EscapeChars.toDisableTags("A Text"));
    assertEquals("&lt;html&gt;", EscapeChars.toDisableTags("<html>"));
  }

  /** Method under test: {@link EscapeChars#convertToLines(String)} */
  @Test
  void testConvertToLines() {
    assertEquals(0, EscapeChars.convertToLines(null).length);
    String[] actualConvertToLinesResult = EscapeChars.convertToLines("Unformatted");
    assertEquals(1, actualConvertToLinesResult.length);
    assertEquals(2, EscapeChars.convertToLines("text\ntext2").length);
    assertEquals(2, EscapeChars.convertToLines("text\rtext2").length);
    assertEquals(2, EscapeChars.convertToLines("text\n\rtext2").length);
    assertEquals("Unformatted", actualConvertToLinesResult[0]);
    assertEquals(4, EscapeChars.convertToLines("text\r\ntext2\r\ntext3\r\ntext4").length);
  }
}
