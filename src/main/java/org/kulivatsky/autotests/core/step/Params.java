package org.kulivatsky.autotests.core.step;

import java.util.LinkedHashMap;
import java.util.Map;

public final class Params {
  private Params() {
  }

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
