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

import java.text.CharacterIterator;
import java.text.StringCharacterIterator;

public final class EscapeChars {

  public static final String AMPERSAND = "&amp;";

  private EscapeChars() {}

  /** Escape characters for HTML string. */
  public static String forHTML(String aText) {
    if (!StringUtil.hasText(aText)) {
      return "";
    }
    final StringBuilder result = new StringBuilder();
    final StringCharacterIterator iterator = new StringCharacterIterator(aText);
    char character = iterator.current();
    while (character != CharacterIterator.DONE) {
      if (character == '<') {
        result.append("&lt;");
      } else if (character == '>') {
        result.append("&gt;");
      } else if (character == '&') {
        result.append(AMPERSAND);
      } else if (character == '\"') {
        result.append("&quot;");
      } else if (character == '\t') {
        addChar(9, result);
      } else if (character == '!') {
        addChar(33, result);
      } else if (character == '#') {
        addChar(35, result);
      } else if (character == '$') {
        addChar(36, result);
      } else if (character == '%') {
        addChar(37, result);
      } else if (character == '\'') {
        addChar(39, result);
      } else if (character == '(') {
        addChar(40, result);
      } else if (character == ')') {
        addChar(41, result);
      } else if (character == '*') {
        addChar(42, result);
      } else if (character == '+') {
        addChar(43, result);
      } else if (character == ',') {
        addChar(44, result);
      } else if (character == '-') {
        addChar(45, result);
      } else if (character == '.') {
        addChar(46, result);
      } else if (character == '/') {
        addChar(47, result);
      } else if (character == ':') {
        addChar(58, result);
      } else if (character == ';') {
        addChar(59, result);
      } else if (character == '=') {
        addChar(61, result);
      } else if (character == '?') {
        addChar(63, result);
      } else if (character == '@') {
        addChar(64, result);
      } else if (character == '[') {
        addChar(91, result);
      } else if (character == '\\') {
        addChar(92, result);
      } else if (character == ']') {
        addChar(93, result);
      } else if (character == '^') {
        addChar(94, result);
      } else if (character == '_') {
        addChar(95, result);
      } else if (character == '`') {
        addChar(96, result);
      } else if (character == '{') {
        addChar(123, result);
      } else if (character == '|') {
        addChar(124, result);
      } else if (character == '}') {
        addChar(125, result);
      } else if (character == '~') {
        addChar(126, result);
      } else {
        result.append(character);
      }
      character = iterator.next();
    }
    return result.toString();
  }

  /** Escape all ampersand characters in a URL. */
  public static String forUrlAmpersand(String aURL) {
    return aURL.replace("&", AMPERSAND);
  }

  /** Escape characters for XML 1.0 data. */
  public static String forXML(String aText) {
    if (!StringUtil.hasText(aText)) {
      return "";
    }
    final StringBuilder result = new StringBuilder();
    final StringCharacterIterator iterator = new StringCharacterIterator(aText);
    char c = iterator.current();
    while (c != CharacterIterator.DONE) {
      if (c == '<') {
        result.append("&lt;");
      } else if (c == '>') {
        result.append("&gt;");
      } else if (c == '\"') {
        result.append("&quot;");
      } else if (c == '\'') {
        result.append("&apos;");
      } else if (c == '&') {
        result.append(AMPERSAND);
      }
      /*
       * This method ensures that the output String has only valid XML unicode characters as specified by the XML
       * 1.0 standard. For reference, please see <a href="http://www.w3.org/TR/2000/REC-xml-20001006#NT-Char">
       */
      else if (c == 0x9
          || c == 0xA
          || c == 0xD
          || c >= 0x20 && c <= 0xD7FF
          || c >= 0xE000 && c <= 0xFFFD) {
        // if not special char, add it
        result.append(c);
      }
      c = iterator.next();
    }
    return result.toString();
  }

  /** Return a string with '<' and '>' characters replaced by their escaped equivalents. */
  public static String toDisableTags(String aText) {
    final StringBuilder result = new StringBuilder();
    final StringCharacterIterator iterator = new StringCharacterIterator(aText);
    char character = iterator.current();
    while (character != CharacterIterator.DONE) {
      if (character == '<') {
        result.append("&lt;");
      } else if (character == '>') {
        result.append("&gt;");
      } else {
        result.append(character);
      }
      character = iterator.next();
    }
    return result.toString();
  }

  private static void addChar(Integer aIdx, StringBuilder aBuilder) {
    String padding = "";
    if (aIdx <= 9) {
      padding = "00";
    } else if (aIdx <= 99) {
      padding = "0";
    }

    aBuilder.append("&#");
    aBuilder.append(padding);
    aBuilder.append(aIdx.toString());
    aBuilder.append(";");
  }

  /**
   * Converts a string contain LF, CR/LF, LF/CR or CR into a set of lines by themselves.
   *
   * @param unformatted the input String
   * @return Array of strings, one per line.
   */
  public static String[] convertToLines(String unformatted) {
    if (unformatted == null) {
      return new String[0];
    }
    int lfPos = unformatted.indexOf('\n');
    int crPos = unformatted.indexOf('\r');
    if (crPos < 0 && lfPos < 0) {
      return new String[] {unformatted};
    }
    String regex;
    if (lfPos == -1) {
      regex = "\r";
    } else if (crPos == -1) {
      regex = "\n";
    } else if (crPos < lfPos) {
      regex = "\r\n";
    } else {
      regex = "\n\r";
    }
    return unformatted.split(regex);
  }
}
