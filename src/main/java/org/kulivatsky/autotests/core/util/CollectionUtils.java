package org.kulivatsky.autotests.core.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class CollectionUtils {

  private CollectionUtils() {
  }

  public static <K, V> Map<K, V> map() {
    return new LinkedHashMap<>();
  }

  @SuppressWarnings("unchecked")
  public static <K, V> Map<K, V> map(final K key, final V value, final Object... keysAndValues) {
    Objects.requireNonNull(keysAndValues, "keysAndValues is null");
    final int keyAndValuesLength = keysAndValues.length;
    if (keyAndValuesLength % 2 != 0) {
      throw new IllegalArgumentException("The number of keysAndValues elements is odd");
    }
    final Map<Object, Object> map = new LinkedHashMap<>((int) Math.ceil((keyAndValuesLength / 2.0 + 1) / 0.75));
    map.put(key, value);
    for (int idx = 0; idx < keyAndValuesLength; idx = idx + 2) {
      map.put(keysAndValues[idx], keysAndValues[idx + 1]);
    }
    return (Map<K, V>) map;
  }

  public static <E> List<E> list() {
    return new ArrayList<>();
  }

  @SafeVarargs
  @SuppressWarnings("varargs")
  public static <E> List<E> list(final E... elements) {
    Objects.requireNonNull(elements, "elements is null");
    if (elements.length == 0) {
      return new ArrayList<>();
    }
    return new ArrayList<>(Arrays.asList(elements));
  }
}
