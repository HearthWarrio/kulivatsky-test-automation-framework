package org.kulivatsky.autotests.core.util;

import org.kulivatsky.autotests.core.step.ThBiFunction;
import org.kulivatsky.autotests.core.step.ThTriFunction;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Map-like contract addressed by a pair of keys instead of one composite key supplied by callers.
 *
 * <p>The built-in {@link Concurrent} implementation stores an internal immutable key object in a
 * {@link ConcurrentHashMap}. Equality uses both key components, so {@code (A, B)} and {@code (B, A)}
 * are different mappings.</p>
 *
 * @param <K1> first-key type
 * @param <K2> second-key type
 * @param <V> value type
 */
public interface BiKeyMap<K1, K2, V> {
  /**
   * Returns the number of key pairs currently mapped.
   *
   * @return mapping count
   */
  int size();

  /**
   * Tests whether the map contains no mappings.
   *
   * @return {@code true} when size is zero
   */
  boolean isEmpty();

  /**
   * Tests whether an exact key pair is present.
   *
   * @param key1 first key component
   * @param key2 second key component
   * @return {@code true} when the pair is mapped
   */
  boolean containsKey(K1 key1, K2 key2);

  /**
   * Tests whether at least one mapping has the supplied value.
   *
   * @param value value to search for
   * @return {@code true} when present
   */
  boolean containsValue(V value);

  /**
   * Reads the value mapped to an exact key pair.
   *
   * @param key1 first key component
   * @param key2 second key component
   * @return mapped value, or {@code null} when absent
   */
  V get(K1 key1, K2 key2);

  /**
   * Associates a value with an exact key pair.
   *
   * @param key1 first key component
   * @param key2 second key component
   * @param value value to store
   * @return previous value, or {@code null} when no mapping existed
   */
  V put(K1 key1, K2 key2, V value);

  /**
   * Removes one exact key pair.
   *
   * @param key1 first key component
   * @param key2 second key component
   * @return removed value, or {@code null} when absent
   */
  V remove(K1 key1, K2 key2);

  /**
   * Returns the map's composite keys.
   *
   * @return key view; mutability follows the implementation's backing map
   */
  Set<BiKey<K1, K2>> keys();

  /**
   * Returns the map's values view.
   *
   * @return value collection backed by the implementation
   */
  Collection<V> values();

  /**
   * Atomically recomputes a mapping from both key components and the current value.
   *
   * @param key1 first key component
   * @param key2 second key component
   * @param remappingFunction function receiving key1, key2, and current value; null result removes the mapping
   * @return resulting value, or {@code null} when removed/no value remains
   */
  V compute(K1 key1, K2 key2,
            ThTriFunction<? super K1, ? super K2, ? super V, ? extends V, Throwable> remappingFunction);

  /**
   * Atomically recomputes a mapping only when the key pair is already present.
   *
   * @param key1 first key component
   * @param key2 second key component
   * @param remappingFunction function receiving key1, key2, and current non-null value
   * @return resulting value, or {@code null} when absent or removed
   */
  V computeIfPresent(K1 key1, K2 key2,
                     ThTriFunction<? super K1, ? super K2, ? super V, ? extends V, Throwable> remappingFunction);

  /**
   * Atomically computes a value only when the key pair has no mapping.
   *
   * @param key1 first key component
   * @param key2 second key component
   * @param mappingFunction function receiving both key components
   * @return existing or newly computed value
   */
  V computeIfAbsent(K1 key1, K2 key2,
                    ThBiFunction<? super K1, ? super K2, ? extends V, Throwable> mappingFunction);

  /**
   * Read-only view of one two-part map key.
   *
   * @param <K1> first-key type
   * @param <K2> second-key type
   */
  interface BiKey<K1, K2> {
    /**
     * Returns the first key component.
     *
     * @return first key
     */
    K1 key1();

    /**
     * Returns the second key component.
     *
     * @return second key
     */
    K2 key2();
  }

  /**
   * Thread-safe {@link BiKeyMap} backed by {@link ConcurrentHashMap}.
   *
   * <p>Like {@code ConcurrentHashMap}, null keys and null stored values are not supported. The
   * compute methods inherit the backing map's atomicity for each composite key. Throwable mapping
   * functions are adapted without wrapping their original failures.</p>
   *
   * @param <K1> first-key type
   * @param <K2> second-key type
   * @param <V> value type
   */
  final class Concurrent<K1, K2, V> implements BiKeyMap<K1, K2, V> {
    /** Backing map keyed by immutable two-part key objects. */
    private final ConcurrentHashMap<BiKey<K1, K2>, V> map = new ConcurrentHashMap<>();

    /** {@inheritDoc} */
    @Override public int size() { return this.map.size(); }

    /** {@inheritDoc} */
    @Override public boolean isEmpty() { return this.map.isEmpty(); }

    /** {@inheritDoc} */
    @Override public boolean containsKey(final K1 key1, final K2 key2) { return this.map.containsKey(new BiKeyImpl<>(key1, key2)); }

    /** {@inheritDoc} */
    @Override public boolean containsValue(final V value) { return this.map.containsValue(value); }

    /** {@inheritDoc} */
    @Override public V get(final K1 key1, final K2 key2) { return this.map.get(new BiKeyImpl<>(key1, key2)); }

    /** {@inheritDoc} */
    @Override public V put(final K1 key1, final K2 key2, final V value) { return this.map.put(new BiKeyImpl<>(key1, key2), value); }

    /** {@inheritDoc} */
    @Override public V remove(final K1 key1, final K2 key2) { return this.map.remove(new BiKeyImpl<>(key1, key2)); }

    /** {@inheritDoc} */
    @Override public Set<BiKey<K1, K2>> keys() { return this.map.keySet(); }

    /** {@inheritDoc} */
    @Override public Collection<V> values() { return this.map.values(); }

    /** {@inheritDoc} */
    @Override
    public V compute(final K1 key1, final K2 key2,
                     final ThTriFunction<? super K1, ? super K2, ? super V, ? extends V, Throwable> remappingFunction) {
      Objects.requireNonNull(remappingFunction, "remappingFunction");
      return this.map.compute(new BiKeyImpl<>(key1, key2), (key, value) ->
        ThTriFunction.unchecked(remappingFunction).apply(key.key1(), key.key2(), value));
    }

    /** {@inheritDoc} */
    @Override
    public V computeIfPresent(final K1 key1, final K2 key2,
                              final ThTriFunction<? super K1, ? super K2, ? super V, ? extends V, Throwable> remappingFunction) {
      Objects.requireNonNull(remappingFunction, "remappingFunction");
      return this.map.computeIfPresent(new BiKeyImpl<>(key1, key2), (key, value) ->
        ThTriFunction.unchecked(remappingFunction).apply(key.key1(), key.key2(), value));
    }

    /** {@inheritDoc} */
    @Override
    public V computeIfAbsent(final K1 key1, final K2 key2,
                             final ThBiFunction<? super K1, ? super K2, ? extends V, Throwable> mappingFunction) {
      Objects.requireNonNull(mappingFunction, "mappingFunction");
      return this.map.computeIfAbsent(new BiKeyImpl<>(key1, key2), key ->
        ThBiFunction.unchecked(mappingFunction).apply(key.key1(), key.key2()));
    }
  }

  /**
   * Immutable composite key used internally by {@link Concurrent}.
   *
   * <p>Equality and hash code include both components in order.</p>
   *
   * @param <K1> first-key type
   * @param <K2> second-key type
   */
  final class BiKeyImpl<K1, K2> implements BiKey<K1, K2> {
    /** First key component. */
    private final K1 key1;
    /** Second key component. */
    private final K2 key2;

    /**
     * Creates an immutable composite key.
     *
     * @param key1 non-null first component
     * @param key2 non-null second component
     * @throws NullPointerException when either key component is null
     */
    private BiKeyImpl(final K1 key1, final K2 key2) {
      this.key1 = Objects.requireNonNull(key1, "key1");
      this.key2 = Objects.requireNonNull(key2, "key2");
    }

    /** {@inheritDoc} */
    @Override public K1 key1() { return this.key1; }

    /** {@inheritDoc} */
    @Override public K2 key2() { return this.key2; }

    /**
     * Compares both key components in order.
     *
     * @param other object to compare
     * @return {@code true} only for another {@code BiKeyImpl} with equal components
     */
    @Override
    public boolean equals(final Object other) {
      return other instanceof BiKeyImpl<?, ?> key
        && Objects.equals(this.key1, key.key1)
        && Objects.equals(this.key2, key.key2);
    }

    /**
     * Hashes both key components in the same order used by {@link #equals(Object)}.
     *
     * @return composite hash code
     */
    @Override
    public int hashCode() {
      return Objects.hash(this.key1, this.key2);
    }
  }
}
