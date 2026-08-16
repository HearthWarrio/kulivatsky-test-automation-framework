package org.kulivatsky.autotests.core.util;

import java.util.List;

/** Small string transformations kept in one place for readable test code. */
public final class StringUtils {
  /** No instances. */
  private StringUtils() {
  }

  /**
   * Returns the half-open substring {@code [beginIndex, endIndex)}.
   *
   * @param string source string
   * @param beginIndex inclusive start index
   * @param endIndex exclusive end index
   * @return selected substring
   * @throws IndexOutOfBoundsException under the same conditions as {@link String#substring(int, int)}
   */
  public static String getSubstring(final String string, final int beginIndex, final int endIndex) {
    return string.substring(beginIndex, endIndex);
  }

  /**
   * Joins strings with a comma followed by one space.
   *
   * @param list values to join
   * @return joined text; empty for an empty list
   */
  public static String stringWithDelimiter(final List<String> list) {
    return String.join(", ", list);
  }

  /**
   * Converts CRLF and lone CR line endings to LF, then trims leading/trailing whitespace.
   *
   * @param text source text
   * @return normalized text
   */
  public static String normalizeLineEndings(final String text) {
    return text.replace("\r\n", "\n").replace('\r', '\n').trim();
  }

  /**
   * Removes every character except decimal digits, comma, and dot, then trims the result.
   *
   * <p>This method does not validate that the result is a syntactically valid number.</p>
   *
   * @param text source text
   * @return filtered text
   */
  public static String keepOnlyDigitsAndSeparators(final String text) {
    return text.replaceAll("[^\\d,.]", "").trim();
  }
}
