package org.kulivatsky.autotests.core.step;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public interface SupplierStep<R> {
  String name();
  Map<String, Object> params();
  ThSupplier<R, ?> action();

  default R get() {
    return Steps.step(this);
  }


  final class Of<R> implements SupplierStep<R> {
    private final String name;
    private final Map<String, Object> params;
    private final ThSupplier<R, ?> action;

    public Of(final String name, final ThSupplier<R, ?> action) {
      this(name, Map.of(), action);
    }

    public Of(final String name, final Map<String, Object> params, final ThSupplier<R, ?> action) {
      this.name = name;
      this.params = Collections.unmodifiableMap(new LinkedHashMap<>(params));
      this.action = action;
    }

    @Override public String name() { return name; }
    @Override public Map<String, Object> params() { return params; }
    @Override public ThSupplier<R, ?> action() { return action; }
  }
}
