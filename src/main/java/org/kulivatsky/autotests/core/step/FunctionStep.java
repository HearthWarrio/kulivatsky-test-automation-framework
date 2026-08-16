package org.kulivatsky.autotests.core.step;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public interface FunctionStep<T, R> {
  String name();
  Map<String, Object> params();
  ThFunction<T, R, ?> action();

  default R apply(final T value) {
    return Steps.step(this, value);
  }


  final class Of<T, R> implements FunctionStep<T, R> {
    private final String name;
    private final Map<String, Object> params;
    private final ThFunction<T, R, ?> action;

    public Of(final String name, final ThFunction<T, R, ?> action) {
      this(name, Map.of(), action);
    }

    public Of(final String name, final Map<String, Object> params, final ThFunction<T, R, ?> action) {
      this.name = name;
      this.params = Collections.unmodifiableMap(new LinkedHashMap<>(params));
      this.action = action;
    }

    @Override public String name() { return name; }
    @Override public Map<String, Object> params() { return params; }
    @Override public ThFunction<T, R, ?> action() { return action; }
  }
}
