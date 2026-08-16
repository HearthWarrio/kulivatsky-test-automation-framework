package org.kulivatsky.autotests.core.step;

import org.kulivatsky.autotests.core.util.ExceptionUtils;

import java.util.function.Function;

@FunctionalInterface
public interface ThFunction<T, R, E extends Throwable> {
  R apply(T value) throws E;

  static <T, R> Function<T, R> unchecked(final ThFunction<T, R, ?> function) {
    return value -> {
      try {
        return function.apply(value);
      } catch (final Throwable ex) {
        throw ExceptionUtils.propagate(ex);
      }
    };
  }
}
