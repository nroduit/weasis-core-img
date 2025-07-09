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

import java.util.HashMap;
import java.util.Map;

public final class EscapeChars {

  public static final String AMPERSAND = "&amp;";
  public static final String OPEN_TAG = "&lt;";
  public static final String CLOSE_TAG = "&gt;";

  private static final Map<Character, String> TAG_ENCODE_CHARS =
      Map.of(
          '<', OPEN_TAG,
          '>', CLOSE_TAG);

  private static final Map<Character, String> XML_ENCODE_CHARS =
      Map.of(
          '<', OPEN_TAG,
          '>', CLOSE_TAG,
          '&', AMPERSAND,
          '"', "&quot;",
          '\'', "&apos;");

  private static final Map<Character, String> HTML_ENCODE_CHARS = createHtmlEncodeChars();

  private static Map<Character, String> createHtmlEncodeChars() {

    // Basic XML characters
    var builder = new HashMap<>(XML_ENCODE_CHARS);
    // Override XML quote for HTML
    builder.put('\'', "&#39;");

    // Special characters
    builder.putAll(
        Map.of(
            'Œ',
            "&OElig;",
            'œ',
            "&oelig;",
            'Š',
            "&Scaron;",
            'š',
            "&scaron;",
            'Ÿ',
            "&Yuml;",
            'ˆ',
            "&circ;",
            '˜',
            "&tilde;",
            '–',
            "&ndash;",
            '—',
            "&mdash;",
            '€',
            "&euro;"));

    // Unicode whitespace and control characters
    builder.putAll(
        Map.of(
            '\u2002',
            "&ensp;",
            '\u2003',
            "&emsp;",
            '\u2009',
            "&thinsp;",
            '\u200C',
            "&zwnj;",
            '\u200D',
            "&zwj;",
            '\u200E',
            "&lrm;",
            '\u200F',
            "&rlm;"));

    // Quotation marks
    builder.put('‘', "&lsquo;");
    builder.put('’', "&rsquo;");
    builder.put('‚', "&sbquo;");
    builder.put('“', "&ldquo;");
    builder.put('”', "&rdquo;");
    builder.put('„', "&bdquo;");

    // Other special characters
    builder.putAll(
        Map.of(
            '†', "&dagger;", '‡', "&Dagger;", '‰', "&permil;", '‹', "&lsaquo;", '›', "&rsaquo;"));

    // ISO 8859-1 characters
    addIso88591Characters(builder);

    // Mathematical, Greek and Symbolic characters
    addMathematicalAndGreekCharacters(builder);

    return Map.copyOf(builder);
  }

  private static void addIso88591Characters(Map<Character, String> builder) {
    // Non-breaking space and punctuation
    builder.putAll(
        Map.of(
            '\u00A0',
            "&nbsp;",
            '¡',
            "&iexcl;",
            '¢',
            "&cent;",
            '£',
            "&pound;",
            '¤',
            "&curren;",
            '¥',
            "&yen;",
            '¦',
            "&brvbar;",
            '§',
            "&sect;",
            '¨',
            "&uml;",
            '©',
            "&copy;"));

    builder.putAll(
        Map.of(
            'ª',
            "&ordf;",
            '«',
            "&laquo;",
            '¬',
            "&not;",
            '\u00AD',
            "&shy;",
            '®',
            "&reg;",
            '¯',
            "&macr;",
            '°',
            "&deg;",
            '±',
            "&plusmn;",
            '²',
            "&sup2;",
            '³',
            "&sup3;"));

    builder.putAll(
        Map.of(
            '´',
            "&acute;",
            'µ',
            "&micro;",
            '¶',
            "&para;",
            '·',
            "&middot;",
            '¸',
            "&cedil;",
            '¹',
            "&sup1;",
            'º',
            "&ordm;",
            '»',
            "&raquo;",
            '¼',
            "&frac14;",
            '½',
            "&frac12;"));

    builder.putAll(Map.of('¾', "&frac34;", '¿', "&iquest;", '×', "&times;", '÷', "&divide;"));

    // Latin characters with diacritics
    addLatinCharacters(builder);
  }

  private static void addLatinCharacters(Map<Character, String> builder) {
    // Uppercase Latin letters
    String[] upperEntities = {
      "&Agrave;", "&Aacute;", "&Acirc;", "&Atilde;", "&Auml;", "&Aring;", "&AElig;", "&Ccedil;",
      "&Egrave;", "&Eacute;", "&Ecirc;", "&Euml;", "&Igrave;", "&Iacute;", "&Icirc;", "&Iuml;",
      "&ETH;", "&Ntilde;", "&Ograve;", "&Oacute;", "&Ocirc;", "&Otilde;", "&Ouml;", "&Oslash;",
      "&Ugrave;", "&Uacute;", "&Ucirc;", "&Uuml;", "&Yacute;", "&THORN;"
    };

    // Lowercase Latin letters
    String[] lowerEntities = {
      "&agrave;", "&aacute;", "&acirc;", "&atilde;", "&auml;", "&aring;", "&aelig;", "&ccedil;",
      "&egrave;", "&eacute;", "&ecirc;", "&euml;", "&igrave;", "&iacute;", "&icirc;", "&iuml;",
      "&eth;", "&ntilde;", "&ograve;", "&oacute;", "&ocirc;", "&otilde;", "&ouml;", "&oslash;",
      "&ugrave;", "&uacute;", "&ucirc;", "&uuml;", "&yacute;", "&thorn;", "&yuml;"
    };

    // Character arrays - removed × and ÷ as they're not accented letters and are handled separately
    char[] upperChars = "ÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖØÙÚÛÜÝÞ".toCharArray();
    char[] lowerChars = "àáâãäåæçèéêëìíîïðñòóôõöøùúûüýþÿ".toCharArray();

    // Map uppercase characters to their HTML entities
    for (int i = 0; i < upperChars.length; i++) {
      builder.put(upperChars[i], upperEntities[i]);
    }

    // Map lowercase characters to their HTML entities
    for (int i = 0; i < lowerChars.length; i++) {
      builder.put(lowerChars[i], lowerEntities[i]);
    }

    // Add special characters separately
    builder.put('ß', "&szlig;");
    builder.put('×', "&times;");
    builder.put('÷', "&divide;");
  }

  private static void addMathematicalAndGreekCharacters(Map<Character, String> builder) {
    builder.put('ƒ', "&fnof;");

    // Greek uppercase
    String greekUpper = "ΑΒΓΔΕΖΗΘΙΚΛΜΝΞΟΠΡΣΤΥΦΧΨΩ";
    String[] greekUpperEntities = {
      "&Alpha;", "&Beta;", "&Gamma;", "&Delta;", "&Epsilon;", "&Zeta;", "&Eta;", "&Theta;",
      "&Iota;", "&Kappa;", "&Lambda;", "&Mu;", "&Nu;", "&Xi;", "&Omicron;", "&Pi;",
      "&Rho;", "&Sigma;", "&Tau;", "&Upsilon;", "&Phi;", "&Chi;", "&Psi;", "&Omega;"
    };

    // Greek lowercase
    String greekLower = "αβγδεζηθικλμνξοπρσςτυφχψω";
    String[] greekLowerEntities = {
      "&alpha;",
      "&beta;",
      "&gamma;",
      "&delta;",
      "&epsilon;",
      "&zeta;",
      "&eta;",
      "&theta;",
      "&iota;",
      "&kappa;",
      "&lambda;",
      "&mu;",
      "&nu;",
      "&xi;",
      "&omicron;",
      "&pi;",
      "&rho;",
      "&sigma;",
      "&sigmaf;",
      "&tau;",
      "&upsilon;",
      "&phi;",
      "&chi;",
      "&psi;",
      "&omega;"
    };

    for (int i = 0; i < greekUpper.length(); i++) {
      builder.put(greekUpper.charAt(i), greekUpperEntities[i]);
    }

    for (int i = 0; i < greekLower.length(); i++) {
      builder.put(greekLower.charAt(i), greekLowerEntities[i]);
    }

    // Special Greek symbols
    builder.putAll(Map.of('ϑ', "&thetasym;", 'ϒ', "&upsih;", 'ϖ', "&piv;"));

    // Mathematical and other symbols
    addMathematicalSymbols(builder);
  }

  private static void addMathematicalSymbols(Map<Character, String> builder) {
    builder.putAll(
        Map.of(
            '•',
            "&bull;",
            '…',
            "&hellip;",
            '′',
            "&prime;",
            '″',
            "&Prime;",
            '‾',
            "&oline;",
            '⁄',
            "&frasl;",
            '℘',
            "&weierp;",
            'ℑ',
            "&image;",
            'ℜ',
            "&real;",
            '™',
            "&trade;"));

    builder.putAll(
        Map.of(
            'ℵ',
            "&alefsym;",
            '←',
            "&larr;",
            '↑',
            "&uarr;",
            '→',
            "&rarr;",
            '↓',
            "&darr;",
            '↔',
            "&harr;",
            '↵',
            "&crarr;",
            '⇐',
            "&lArr;",
            '⇑',
            "&uArr;",
            '⇒',
            "&rArr;"));

    builder.putAll(
        Map.of(
            '⇓',
            "&dArr;",
            '⇔',
            "&hArr;",
            '∀',
            "&forall;",
            '∂',
            "&part;",
            '∃',
            "&exist;",
            '∅',
            "&empty;",
            '∇',
            "&nabla;",
            '∈',
            "&isin;",
            '∉',
            "&notin;",
            '∋',
            "&ni;"));

    builder.putAll(
        Map.of(
            '∏',
            "&prod;",
            '∑',
            "&sum;",
            '−',
            "&minus;",
            '∗',
            "&lowast;",
            '√',
            "&radic;",
            '∝',
            "&prop;",
            '∞',
            "&infin;",
            '∠',
            "&ang;",
            '∧',
            "&and;",
            '∨',
            "&or;"));

    builder.putAll(
        Map.of(
            '∩',
            "&cap;",
            '∪',
            "&cup;",
            '∫',
            "&int;",
            '∴',
            "&there4;",
            '∼',
            "&sim;",
            '≅',
            "&cong;",
            '≈',
            "&asymp;",
            '≠',
            "&ne;",
            '≡',
            "&equiv;",
            '≤',
            "&le;"));

    builder.putAll(
        Map.of(
            '≥',
            "&ge;",
            '⊂',
            "&sub;",
            '⊃',
            "&sup;",
            '⊄',
            "&nsub;",
            '⊆',
            "&sube;",
            '⊇',
            "&supe;",
            '⊕',
            "&oplus;",
            '⊗',
            "&otimes;",
            '⊥',
            "&perp;",
            '⋅',
            "&sdot;"));

    builder.putAll(
        Map.of(
            '⌈',
            "&lceil;",
            '⌉',
            "&rceil;",
            '⌊',
            "&lfloor;",
            '⌋',
            "&rfloor;",
            '〈',
            "&lang;",
            '〉',
            "&rang;",
            '◊',
            "&loz;",
            '♠',
            "&spades;",
            '♣',
            "&clubs;",
            '♥',
            "&hearts;"));

    builder.put('♦', "&diams;");
  }

  private static boolean isInvalidXml(char c) {
    return !(c == 0x9
        || c == 0xA
        || c == 0xD
        || (c >= 0x20 && c <= 0xD7FF)
        || (c >= 0xE000 && c <= 0xFFFD));
  }

  private static String encode(String text, Map<Character, String> encodingMap) {
    if (!StringUtil.hasText(text)) {
      return "";
    }

    StringBuilder result = null;
    int lastProcessedIndex = -1;
    char[] chars = text.toCharArray();
    boolean isXmlEncoding = encodingMap == XML_ENCODE_CHARS;

    for (int i = 0; i < chars.length; i++) {
      char ch = chars[i];
      String encoded = encodingMap.get(ch);

      if (encoded != null) {
        result = appendSegment(chars, result, lastProcessedIndex, i, encoded);
        lastProcessedIndex = i;
      } else if (isXmlEncoding && isInvalidXml(ch)) {
        result = appendSegment(chars, result, lastProcessedIndex, i, null);
        lastProcessedIndex = i;
      }
    }

    if (result == null) {
      return text;
    }

    // Append remaining characters
    if (lastProcessedIndex + 1 < chars.length) {
      result.append(chars, lastProcessedIndex + 1, chars.length - (lastProcessedIndex + 1));
    }

    return result.toString();
  }

  private static StringBuilder appendSegment(
      char[] chars, StringBuilder buffer, int lastIndex, int currentIndex, String encoded) {
    if (buffer == null) {
      buffer = new StringBuilder(chars.length * 2); // Better initial capacity
    }
    int segmentLength = currentIndex - (lastIndex + 1);
    if (segmentLength > 0) {
      buffer.append(chars, lastIndex + 1, segmentLength);
    }
    if (encoded != null) {
      buffer.append(encoded);
    }
    return buffer;
  }

  private EscapeChars() {}

  /**
   * Escapes special characters in a string for safe inclusion in HTML documents.
   *
   * @param text the input string to be escaped
   * @return the escaped string with special HTML characters replaced by their corresponding
   *     entities
   */
  public static String forHTML(String text) {
    return encode(text, HTML_ENCODE_CHARS);
  }

  /**
   * Replaces all occurrences of the ampersand character '&' in the given URL string with an escaped
   * equivalent.
   *
   * @param url the input URL string where ampersand characters are to be replaced
   * @return the URL string with all '&' characters replaced by their escaped equivalent
   */
  public static String forUrlAmpersand(String url) {
    if (!StringUtil.hasText(url)) {
      return "";
    }
    return url.replace("&", AMPERSAND);
  }

  /**
   * Escapes special characters in a string for safe inclusion in XML documents.
   *
   * @param text the input string to be escaped
   * @return the escaped string with special XML characters replaced by their corresponding entities
   */
  public static String forXML(String text) {
    return encode(text, XML_ENCODE_CHARS);
  }

  /**
   * Return a string with '<' and '>' characters replaced by their escaped equivalents.
   *
   * @param text the input String
   * @return the String with '<' and '>' escaped characters
   */
  public static String toDisableTags(String text) {
    return encode(text, TAG_ENCODE_CHARS);
  }

  /**
   * Converts a string containing LF, CR/LF, LF/CR or CR into a set of lines.
   *
   * @param unformatted the input String
   * @return an array of strings containing the lines derived from the input string
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
    String delimiter = determineLineDelimiter(lfPos, crPos);
    return unformatted.split(delimiter, -1); // Use -1 to preserve trailing empty strings
  }

  private static String determineLineDelimiter(int lfPos, int crPos) {
    if (lfPos == -1) return "\r";
    if (crPos == -1) return "\n";
    return crPos < lfPos ? "\r\n" : "\n\r";
  }
}
