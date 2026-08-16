package org.kulivatsky.autotests.core.step;

import java.util.LinkedHashMap;
import java.util.Map;

/** Builds ordered parameter maps attached to framework steps. */
public final class Params {
  /** No instances. */
  private Params() {
  }

  /**
   * Converts an alternating sequence of names and values into a map.
   *
   * <p>Order is preserved because Allure displays parameters in map iteration order. Repeated names
   * replace the previous value without changing the name's original position.</p>
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
}
