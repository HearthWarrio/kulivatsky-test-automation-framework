package org.kulivatsky.autotests.core.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Creates mutable lists and insertion-ordered maps for compact test-data setup. */
public final class CollectionUtils {
  /** No instances. */
  private CollectionUtils() {
  }

  /**
   * Creates an empty mutable map that preserves insertion order.
   *
   * @param <K> key type
   * @param <V> value type
   * @return new {@link LinkedHashMap}
   */
  public static <K, V> Map<K, V> map() {
    return new LinkedHashMap<>();
  }

  /**
   * Creates a mutable insertion-ordered map from alternating keys and values.
   *
   * <p>The first key/value pair has its own arguments so a call cannot accidentally create a map
   * from an entirely empty vararg. The remaining arguments must have even length. Runtime type
   * consistency is the caller's responsibility because Java cannot type-check heterogeneous
   * {@code Object...} pairs.</p>
   *
   * @param key first key
   * @param value first value
   * @param keysAndValues additional key, value pairs
   * @param <K> declared key type
   * @param <V> declared value type
   * @return mutable insertion-ordered map
   * @throws NullPointerException when {@code keysAndValues} itself is null
   * @throws IllegalArgumentException when the additional argument count is odd
   */
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

  /**
   * Creates an empty mutable list.
   *
   * @param <E> element type
   * @return new {@link ArrayList}
   */
  public static <E> List<E> list() {
    return new ArrayList<>();
  }

  /**
   * Copies elements into a new mutable list.
   *
   * @param elements elements to copy
   * @param <E> element type
   * @return independent mutable list preserving argument order
   * @throws NullPointerException when the vararg array itself is null
   */
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
