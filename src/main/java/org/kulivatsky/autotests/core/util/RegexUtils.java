package org.kulivatsky.autotests.core.util;

import java.util.NoSuchElementException;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

public final class RegexUtils {
  private static final Pattern DECIMAL = Pattern.compile("(\\d+[.,]\\d+)");
  private static final Pattern GROUPED_INTEGER = Pattern.compile("(\\d+(?:\\s+\\d+)*)");
  private static final Pattern INTEGER = Pattern.compile("\\d+");

  private RegexUtils() {
  }

  public static String extractDoubleValue(final String text) {
    final var matcher = DECIMAL.matcher(text);
    return matcher.find() ? matcher.group(1).replace(',', '.') : null;
  }

  public static String extractIntValue(final String text) {
    final var matcher = GROUPED_INTEGER.matcher(text);
    return matcher.find() ? matcher.group(1).replaceAll("\\s", "") : null;
  }

  public static String extractIntAsString(final String text) {
    return INTEGER.matcher(text).results().map(MatchResult::group).findFirst()
      .orElseThrow(() -> new NoSuchElementException("No integer value found in text"));
  }
}
