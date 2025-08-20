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

/**
 * Utility class for escaping special characters in various formats including HTML, XML, and URLs.
 * This class provides static methods to safely encode strings for use in web contexts.
 */
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
    var builder = new HashMap<>(XML_ENCODE_CHARS);
    builder.put('\'', "&#39;"); // Override XML quote for HTML

    addSpecialCharacters(builder);
    addUnicodeCharacters(builder);
    addQuotationMarks(builder);
    addIso88591Characters(builder);
    addMathematicalAndGreekCharacters(builder);

    return Map.copyOf(builder);
  }

  private static void addSpecialCharacters(Map<Character, String> builder) {
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
  }

  private static void addUnicodeCharacters(Map<Character, String> builder) {
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
  }

  private static void addQuotationMarks(Map<Character, String> builder) {
    builder.putAll(
        Map.of(
            '‘', "&lsquo;", '’', "&rsquo;", '‚', "&sbquo;", '“', "&ldquo;", '”', "&rdquo;", '„',
            "&bdquo;"));

    builder.putAll(
        Map.of(
            '†', "&dagger;", '‡', "&Dagger;", '‰', "&permil;", '‹', "&lsaquo;", '›', "&rsaquo;"));
  }

  private static void addIso88591Characters(Map<Character, String> builder) {
    addPunctuationCharacters(builder);
    addLatinCharacters(builder);
  }

  private static void addPunctuationCharacters(Map<Character, String> builder) {
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
  }

  private static void addLatinCharacters(Map<Character, String> builder) {
    var latinMappings =
        Map.of(
            'À',
            "&Agrave;",
            'Á',
            "&Aacute;",
            'Â',
            "&Acirc;",
            'Ã',
            "&Atilde;",
            'Ä',
            "&Auml;",
            'Å',
            "&Aring;",
            'Æ',
            "&AElig;",
            'Ç',
            "&Ccedil;",
            'È',
            "&Egrave;",
            'É',
            "&Eacute;");
    builder.putAll(latinMappings);

    var moreLatinMappings =
        Map.of(
            'Ê',
            "&Ecirc;",
            'Ë',
            "&Euml;",
            'Ì',
            "&Igrave;",
            'Í',
            "&Iacute;",
            'Î',
            "&Icirc;",
            'Ï',
            "&Iuml;",
            'Ð',
            "&ETH;",
            'Ñ',
            "&Ntilde;",
            'Ò',
            "&Ograve;",
            'Ó',
            "&Oacute;");
    builder.putAll(moreLatinMappings);

    var additionalLatinMappings =
        Map.of(
            'Ô',
            "&Ocirc;",
            'Õ',
            "&Otilde;",
            'Ö',
            "&Ouml;",
            'Ø',
            "&Oslash;",
            'Ù',
            "&Ugrave;",
            'Ú',
            "&Uacute;",
            'Û',
            "&Ucirc;",
            'Ü',
            "&Uuml;",
            'Ý',
            "&Yacute;",
            'Þ',
            "&THORN;");
    builder.putAll(additionalLatinMappings);

    // Lowercase variants
    addLowercaseLatinCharacters(builder);
  }

  private static void addLowercaseLatinCharacters(Map<Character, String> builder) {
    var lowerLatinMappings =
        Map.of(
            'à',
            "&agrave;",
            'á',
            "&aacute;",
            'â',
            "&acirc;",
            'ã',
            "&atilde;",
            'ä',
            "&auml;",
            'å',
            "&aring;",
            'æ',
            "&aelig;",
            'ç',
            "&ccedil;",
            'è',
            "&egrave;",
            'é',
            "&eacute;");
    builder.putAll(lowerLatinMappings);

    var moreLowerLatinMappings =
        Map.of(
            'ê',
            "&ecirc;",
            'ë',
            "&euml;",
            'ì',
            "&igrave;",
            'í',
            "&iacute;",
            'î',
            "&icirc;",
            'ï',
            "&iuml;",
            'ð',
            "&eth;",
            'ñ',
            "&ntilde;",
            'ò',
            "&ograve;",
            'ó',
            "&oacute;");
    builder.putAll(moreLowerLatinMappings);

    var additionalLowerLatinMappings =
        Map.of(
            'ô',
            "&ocirc;",
            'õ',
            "&otilde;",
            'ö',
            "&ouml;",
            'ø',
            "&oslash;",
            'ù',
            "&ugrave;",
            'ú',
            "&uacute;",
            'û',
            "&ucirc;",
            'ü',
            "&uuml;",
            'ý',
            "&yacute;",
            'þ',
            "&thorn;");
    builder.putAll(additionalLowerLatinMappings);

    builder.putAll(Map.of('ÿ', "&yuml;", 'ß', "&szlig;"));
  }

  private static void addMathematicalAndGreekCharacters(Map<Character, String> builder) {
    builder.put('ƒ', "&fnof;");
    addGreekCharacters(builder);
    addMathematicalSymbols(builder);
  }

  private static void addGreekCharacters(Map<Character, String> builder) {
    // Greek uppercase letters
    var greekUpperMappings =
        Map.of(
            'Α',
            "&Alpha;",
            'Β',
            "&Beta;",
            'Γ',
            "&Gamma;",
            'Δ',
            "&Delta;",
            'Ε',
            "&Epsilon;",
            'Ζ',
            "&Zeta;",
            'Η',
            "&Eta;",
            'Θ',
            "&Theta;",
            'Ι',
            "&Iota;",
            'Κ',
            "&Kappa;");
    builder.putAll(greekUpperMappings);

    var moreGreekUpperMappings =
        Map.of(
            'Λ',
            "&Lambda;",
            'Μ',
            "&Mu;",
            'Ν',
            "&Nu;",
            'Ξ',
            "&Xi;",
            'Ο',
            "&Omicron;",
            'Π',
            "&Pi;",
            'Ρ',
            "&Rho;",
            'Σ',
            "&Sigma;",
            'Τ',
            "&Tau;",
            'Υ',
            "&Upsilon;");
    builder.putAll(moreGreekUpperMappings);

    builder.putAll(Map.of('Φ', "&Phi;", 'Χ', "&Chi;", 'Ψ', "&Psi;", 'Ω', "&Omega;"));

    // Greek lowercase letters
    addGreekLowercaseCharacters(builder);

    // Special Greek symbols
    builder.putAll(Map.of('ϑ', "&thetasym;", 'ϒ', "&upsih;", 'ϖ', "&piv;"));
  }

  private static void addGreekLowercaseCharacters(Map<Character, String> builder) {
    var greekLowerMappings =
        Map.of(
            'α',
            "&alpha;",
            'β',
            "&beta;",
            'γ',
            "&gamma;",
            'δ',
            "&delta;",
            'ε',
            "&epsilon;",
            'ζ',
            "&zeta;",
            'η',
            "&eta;",
            'θ',
            "&theta;",
            'ι',
            "&iota;",
            'κ',
            "&kappa;");
    builder.putAll(greekLowerMappings);

    var moreGreekLowerMappings =
        Map.of(
            'λ',
            "&lambda;",
            'μ',
            "&mu;",
            'ν',
            "&nu;",
            'ξ',
            "&xi;",
            'ο',
            "&omicron;",
            'π',
            "&pi;",
            'ρ',
            "&rho;",
            'σ',
            "&sigma;",
            'ς',
            "&sigmaf;",
            'τ',
            "&tau;");
    builder.putAll(moreGreekLowerMappings);

    builder.putAll(
        Map.of('υ', "&upsilon;", 'φ', "&phi;", 'χ', "&chi;", 'ψ', "&psi;", 'ω', "&omega;"));
  }

  private static void addMathematicalSymbols(Map<Character, String> builder) {
    addBasicMathSymbols(builder);
    addArrowSymbols(builder);
    addLogicalSymbols(builder);
    addSetSymbols(builder);
    addOtherMathSymbols(builder);
  }

  private static void addBasicMathSymbols(Map<Character, String> builder) {
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
  }

  private static void addArrowSymbols(Map<Character, String> builder) {
    builder.putAll(
        Map.of(
            '←', "&larr;", '↑', "&uarr;", '→', "&rarr;", '↓', "&darr;", '↔', "&harr;", '↵',
            "&crarr;", '⇐', "&lArr;", '⇑', "&uArr;", '⇒', "&rArr;", '⇓', "&dArr;"));
    builder.put('⇔', "&hArr;");
  }

  private static void addLogicalSymbols(Map<Character, String> builder) {
    builder.putAll(
        Map.of(
            'ℵ',
            "&alefsym;",
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
            "&ni;",
            '∧',
            "&and;"));
    builder.put('∨', "&or;");
  }

  private static void addSetSymbols(Map<Character, String> builder) {
    builder.putAll(
        Map.of(
            '∩',
            "&cap;",
            '∪',
            "&cup;",
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
            "&perp;"));
  }

  private static void addOtherMathSymbols(Map<Character, String> builder) {
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
            '∫',
            "&int;",
            '∴',
            "&there4;"));

    builder.putAll(
        Map.of(
            '∼', "&sim;", '≅', "&cong;", '≈', "&asymp;", '≠', "&ne;", '≡', "&equiv;", '≤', "&le;",
            '≥', "&ge;", '⋅', "&sdot;", '⌈', "&lceil;", '⌉', "&rceil;"));

    builder.putAll(
        Map.of(
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
            "&hearts;",
            '♦',
            "&diams;"));
  }

  /** Checks if a character is invalid XML according to XML 1.0 specification. */
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

      if (encoded != null || (isXmlEncoding && isInvalidXml(ch))) {
        result = appendSegment(chars, result, lastProcessedIndex, i, encoded);
        lastProcessedIndex = i;
      }
    }

    return result == null ? text : appendRemainingChars(result, chars, lastProcessedIndex);
  }

  private static StringBuilder appendSegment(
      char[] chars, StringBuilder buffer, int lastIndex, int currentIndex, String encoded) {
    if (buffer == null) {
      buffer = new StringBuilder(chars.length * 2);
    }

    int segmentLength = currentIndex - lastIndex - 1;
    if (segmentLength > 0) {
      buffer.append(chars, lastIndex + 1, segmentLength);
    }

    if (encoded != null) {
      buffer.append(encoded);
    }

    return buffer;
  }

  private static String appendRemainingChars(
      StringBuilder result, char[] chars, int lastProcessedIndex) {
    if (lastProcessedIndex + 1 < chars.length) {
      result.append(chars, lastProcessedIndex + 1, chars.length - lastProcessedIndex - 1);
    }
    return result.toString();
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
   * Converts a string containing LF, CR/LF, LF/CR or CR into a set of lines. Properly handles mixed
   * line break types by normalizing all line breaks.
   *
   * @param unformatted the input String
   * @return an array of strings containing the lines derived from the input string
   */
  public static String[] convertToLines(String unformatted) {
    if (unformatted == null) {
      return new String[0];
    }

    if (unformatted.indexOf('\n') == -1 && unformatted.indexOf('\r') == -1) {
      return new String[] {unformatted};
    }

    // Normalize all line breaks to \n for consistent splitting
    return normalizeLineBreaks(unformatted).split("\n", -1);
  }

  /** Normalizes all types of line breaks to a single \n character. */
  private static String normalizeLineBreaks(String text) {
    // First replace \r\n and \n\r with \n to handle compound separators
    return text.replace("\r\n", "\n").replace("\n\r", "\n").replace("\r", "\n");
  }
}
