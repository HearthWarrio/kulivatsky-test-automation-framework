package org.kulivatsky.autotests.core.step;

import org.kulivatsky.autotests.core.util.ExceptionUtils;

import java.util.function.BiFunction;

@FunctionalInterface
public interface ThBiFunction<T, U, R, E extends Throwable> {
  R apply(T first, U second) throws E;

  static <T, U, R> BiFunction<T, U, R> unchecked(final ThBiFunction<T, U, R, ?> function) {
    return (first, second) -> {
      try {
        return function.apply(first, second);
      } catch (final Throwable ex) {
        return ExceptionUtils.sneakyThrow(ex);
      }
    };
  }
}
