package org.kulivatsky.autotests.core.step;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public interface RunnableStep {
  String name();
  Map<String, Object> params();
  ThRunnable<?> action();

  default void run() {
    Steps.step(this);
  }


  final class Of implements RunnableStep {
    private final String name;
    private final Map<String, Object> params;
    private final ThRunnable<?> action;

    public Of(final String name, final ThRunnable<?> action) {
      this(name, Map.of(), action);
    }

    public Of(final String name, final Map<String, Object> params, final ThRunnable<?> action) {
      this.name = name;
      this.params = Collections.unmodifiableMap(new LinkedHashMap<>(params));
      this.action = action;
    }

    @Override public String name() { return name; }
    @Override public Map<String, Object> params() { return params; }
    @Override public ThRunnable<?> action() { return action; }
  }
}
