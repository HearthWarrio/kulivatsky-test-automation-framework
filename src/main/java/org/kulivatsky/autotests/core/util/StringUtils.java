package org.kulivatsky.autotests.core.util;

import java.util.List;

public final class StringUtils {
  private StringUtils() {
  }

  public static String getSubstring(final String string, final int beginIndex, final int endIndex) {
    return string.substring(beginIndex, endIndex);
  }

  public static String stringWithDelimiter(final List<String> list) {
    return String.join(", ", list);
  }

  public static String normalizeLineEndings(final String text) {
    return text.replace("\r\n", "\n").replace('\r', '\n').trim();
  }

  public static String keepOnlyDigitsAndSeparators(final String text) {
    return text.replaceAll("[^\\d,.]", "").trim();
  }
}
