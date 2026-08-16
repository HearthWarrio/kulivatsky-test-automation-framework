package org.kulivatsky.autotests.core.util;

import java.util.NoSuchElementException;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

/** Precompiled regular-expression extractors for common numeric text fragments. */
public final class RegexUtils {
  /** First decimal number containing either a dot or comma separator. */
  private static final Pattern DECIMAL = Pattern.compile("(\\d+[.,]\\d+)");
  /** Integer that may contain whitespace between digit groups. */
  private static final Pattern GROUPED_INTEGER = Pattern.compile("(\\d+(?:\\s+\\d+)*)");
  /** Simple uninterrupted digit sequence. */
  private static final Pattern INTEGER = Pattern.compile("\\d+");

  /** No instances. */
  private RegexUtils() {
  }

  /**
   * Extracts the first decimal-looking fragment and normalizes comma to dot.
   *
   * <p>The pattern requires digits on both sides of the separator. Signs, exponent notation, and
   * thousands separators are outside this helper's contract.</p>
   *
   * @param text text to inspect
   * @return normalized decimal fragment, or {@code null} when none is found
   */
  public static String extractDoubleValue(final String text) {
    final var matcher = DECIMAL.matcher(text);
    return matcher.find() ? matcher.group(1).replace(',', '.') : null;
  }

  /**
   * Extracts the first integer fragment and removes whitespace inside it.
   *
   * @param text text to inspect
   * @return digits without internal whitespace, or {@code null} when none are found
   */
  public static String extractIntValue(final String text) {
    final var matcher = GROUPED_INTEGER.matcher(text);
    return matcher.find() ? matcher.group(1).replaceAll("\\s", "") : null;
  }

  /**
   * Extracts the first uninterrupted digit sequence.
   *
   * @param text text to inspect
   * @return first digit sequence
   * @throws NoSuchElementException when the text contains no digits
   */
  public static String extractIntAsString(final String text) {
    return INTEGER.matcher(text).results().map(MatchResult::group).findFirst()
      .orElseThrow(() -> new NoSuchElementException("No integer value found in text"));
  }
}
