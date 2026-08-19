package org.kulivatsky.autotests.core.step;

import org.kulivatsky.autotests.core.util.AllureUtils;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** Builds ordered parameter maps attached to framework steps. */
public final class Params {
  /** Visible replacement written to reports instead of a sensitive value. */
  private static final String MASK = "***";

  /** No instances. */
  private Params() {
  }

  /**
   * Converts an alternating sequence of names and values into a map.
   *
   * <p>Order is preserved because Allure displays parameters in map iteration order. Repeated names
   * replace the previous value without changing the name's original position. Values are not
   * stringified here; masking is applied by the step runner immediately before report output.</p>
   *
   * @param nameValuePairs alternating parameter name, parameter value pairs
   * @return mutable insertion-ordered map; empty when no pairs are supplied
   * @throws IllegalArgumentException when the argument count is odd or a parameter name is null
   */
  public static Map<String, Object> params(final Object... nameValuePairs) {
    if (nameValuePairs.length % 2 != 0) {
      throw new IllegalArgumentException("Step parameters must be provided as name/value pairs");
    }
    final Map<String, Object> result = new LinkedHashMap<>();
    for (int index = 0; index < nameValuePairs.length; index += 2) {
      final Object name = nameValuePairs[index];
      if (name == null) {
        throw new IllegalArgumentException("Step parameter name must not be null");
      }
      result.put(name.toString(), nameValuePairs[index + 1]);
    }
    return result;
  }

  /**
   * Marks a value as sensitive even when its parameter name would not be recognized automatically.
   *
   * <p>The supplied object is deliberately not retained by the returned marker. The method is for
   * report metadata only: test code should keep the real value in the variable used by the actual
   * operation. When this marker is passed through {@link #params(Object...)}, the step runner writes
   * {@code ***}.</p>
   *
   * @param value value that must never be written to Allure step parameters
   * @return report-only sensitive wrapper
   */
  public static Object secret(final Object value) {
    return Sensitive.VALUE;
  }

  /**
   * Converts one parameter value to report text while applying explicit and name-based masking.
   *
   * @param name parameter name
   * @param value raw parameter value
   * @return safe text for Allure
   */
  static String reportValue(final String name, final Object value) {
    if (value instanceof Sensitive || isSensitiveName(name)) {
      return MASK;
    }
    return AllureUtils.redact(String.valueOf(value));
  }

  /** Returns whether a parameter name commonly denotes credentials or session material. */
  private static boolean isSensitiveName(final String name) {
    final String normalized = name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    return normalized.contains("password")
      || normalized.contains("passwd")
      || normalized.equals("pwd")
      || normalized.contains("secret")
      || normalized.contains("token")
      || normalized.contains("authorization")
      || normalized.contains("cookie")
      || normalized.contains("credential")
      || normalized.contains("apikey")
      || normalized.contains("privatekey")
      || normalized.contains("sessionid");
  }

  /** Marker placed in report metadata instead of retaining the sensitive value itself. */
  private enum Sensitive {
    /** Singleton marker; the original value is deliberately not stored in the parameter map. */
    VALUE
  }
}
