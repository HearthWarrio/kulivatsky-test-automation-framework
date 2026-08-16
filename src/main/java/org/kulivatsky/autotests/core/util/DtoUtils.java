package org.kulivatsky.autotests.core.util;

import java.util.Map;

/** Converts map-shaped test data to DTOs through the framework's shared Jackson mapper. */
public final class DtoUtils {
  /** No instances. */
  private DtoUtils() {
  }

  /**
   * Converts a map into a POJO using Jackson's normal {@code convertValue} rules.
   *
   * @param map source property map
   * @param clazz target class
   * @param <T> target type
   * @return converted object
   * @throws IllegalArgumentException when Jackson cannot map the source into the target type
   */
  public static <T> T getPojoFromMap(final Map<String, Object> map, final Class<T> clazz) {
    return JsonUtils.objectMapper().convertValue(map, clazz);
  }
}
