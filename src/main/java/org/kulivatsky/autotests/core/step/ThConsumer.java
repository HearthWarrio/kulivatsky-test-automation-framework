package org.kulivatsky.autotests.core.step;

import org.kulivatsky.autotests.core.util.ExceptionUtils;

import java.util.function.Consumer;

@FunctionalInterface
public interface ThConsumer<T, E extends Throwable> {
  void accept(T value) throws E;

  static <T> Consumer<T> unchecked(final ThConsumer<T, ?> consumer) {
    return value -> {
      try {
        consumer.accept(value);
      } catch (final Throwable ex) {
        throw ExceptionUtils.propagate(ex);
      }
    };
  }
}
