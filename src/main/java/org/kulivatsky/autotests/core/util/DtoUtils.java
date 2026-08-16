package org.kulivatsky.autotests.core.util;

import java.util.Map;

public final class DtoUtils {
  private DtoUtils() {
  }

  public static <T> T getPojoFromMap(final Map<String, Object> map, final Class<T> clazz) {
    return JsonUtils.objectMapper().convertValue(map, clazz);
  }
}
