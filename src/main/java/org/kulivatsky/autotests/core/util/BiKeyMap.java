package org.kulivatsky.autotests.core.util;

import org.kulivatsky.autotests.core.step.ThBiFunction;
import org.kulivatsky.autotests.core.step.ThTriFunction;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public interface BiKeyMap<K1, K2, V> {
  int size();
  boolean isEmpty();
  boolean containsKey(K1 key1, K2 key2);
  boolean containsValue(V value);
  V get(K1 key1, K2 key2);
  V put(K1 key1, K2 key2, V value);
  V remove(K1 key1, K2 key2);
  Set<BiKey<K1, K2>> keys();
  Collection<V> values();

  V compute(K1 key1, K2 key2,
            ThTriFunction<? super K1, ? super K2, ? super V, ? extends V, Throwable> remappingFunction);

  V computeIfPresent(K1 key1, K2 key2,
                     ThTriFunction<? super K1, ? super K2, ? super V, ? extends V, Throwable> remappingFunction);

  V computeIfAbsent(K1 key1, K2 key2,
                    ThBiFunction<? super K1, ? super K2, ? extends V, Throwable> mappingFunction);

  interface BiKey<K1, K2> {
    K1 key1();
    K2 key2();
  }

  final class Concurrent<K1, K2, V> implements BiKeyMap<K1, K2, V> {
    private final ConcurrentHashMap<BiKey<K1, K2>, V> map = new ConcurrentHashMap<>();

    @Override public int size() { return this.map.size(); }
    @Override public boolean isEmpty() { return this.map.isEmpty(); }
    @Override public boolean containsKey(final K1 key1, final K2 key2) { return this.map.containsKey(new BiKeyImpl<>(key1, key2)); }
    @Override public boolean containsValue(final V value) { return this.map.containsValue(value); }
    @Override public V get(final K1 key1, final K2 key2) { return this.map.get(new BiKeyImpl<>(key1, key2)); }
    @Override public V put(final K1 key1, final K2 key2, final V value) { return this.map.put(new BiKeyImpl<>(key1, key2), value); }
    @Override public V remove(final K1 key1, final K2 key2) { return this.map.remove(new BiKeyImpl<>(key1, key2)); }
    @Override public Set<BiKey<K1, K2>> keys() { return this.map.keySet(); }
    @Override public Collection<V> values() { return this.map.values(); }

    @Override
    public V compute(final K1 key1, final K2 key2,
                     final ThTriFunction<? super K1, ? super K2, ? super V, ? extends V, Throwable> remappingFunction) {
      Objects.requireNonNull(remappingFunction, "remappingFunction");
      return this.map.compute(new BiKeyImpl<>(key1, key2), (key, value) ->
        ThTriFunction.unchecked(remappingFunction).apply(key.key1(), key.key2(), value));
    }

    @Override
    public V computeIfPresent(final K1 key1, final K2 key2,
                              final ThTriFunction<? super K1, ? super K2, ? super V, ? extends V, Throwable> remappingFunction) {
      Objects.requireNonNull(remappingFunction, "remappingFunction");
      return this.map.computeIfPresent(new BiKeyImpl<>(key1, key2), (key, value) ->
        ThTriFunction.unchecked(remappingFunction).apply(key.key1(), key.key2(), value));
    }

    @Override
    public V computeIfAbsent(final K1 key1, final K2 key2,
                             final ThBiFunction<? super K1, ? super K2, ? extends V, Throwable> mappingFunction) {
      Objects.requireNonNull(mappingFunction, "mappingFunction");
      return this.map.computeIfAbsent(new BiKeyImpl<>(key1, key2), key ->
        ThBiFunction.unchecked(mappingFunction).apply(key.key1(), key.key2()));
    }
  }

  final class BiKeyImpl<K1, K2> implements BiKey<K1, K2> {
    private final K1 key1;
    private final K2 key2;

    private BiKeyImpl(final K1 key1, final K2 key2) {
      this.key1 = key1;
      this.key2 = key2;
    }

    @Override public K1 key1() { return this.key1; }
    @Override public K2 key2() { return this.key2; }

    @Override
    public boolean equals(final Object other) {
      return other instanceof BiKeyImpl<?, ?> key
        && Objects.equals(this.key1, key.key1)
        && Objects.equals(this.key2, key.key2);
    }

    @Override
    public int hashCode() {
      return Objects.hash(this.key1, this.key2);
    }
  }
}
