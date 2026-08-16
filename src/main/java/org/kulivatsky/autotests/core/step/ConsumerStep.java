package org.kulivatsky.autotests.core.step;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public interface ConsumerStep<T> {
  String name();
  Map<String, Object> params();
  ThConsumer<T, ?> action();

  default void accept(final T value) {
    Steps.step(this, value);
  }


  final class Of<T> implements ConsumerStep<T> {
    private final String name;
    private final Map<String, Object> params;
    private final ThConsumer<T, ?> action;

    public Of(final String name, final ThConsumer<T, ?> action) {
      this(name, Map.of(), action);
    }

    public Of(final String name, final Map<String, Object> params, final ThConsumer<T, ?> action) {
      this.name = name;
      this.params = Collections.unmodifiableMap(new LinkedHashMap<>(params));
      this.action = action;
    }

    @Override public String name() { return name; }
    @Override public Map<String, Object> params() { return params; }
    @Override public ThConsumer<T, ?> action() { return action; }
  }
}
