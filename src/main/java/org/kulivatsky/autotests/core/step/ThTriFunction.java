package org.kulivatsky.autotests.core.step;

import org.kulivatsky.autotests.core.util.ExceptionUtils;

@FunctionalInterface
public interface ThTriFunction<T, U, V, R, E extends Throwable> {
  R apply(T first, U second, V third) throws E;

  static <T, U, V, R> ThTriFunction<T, U, V, R, RuntimeException> unchecked(
    final ThTriFunction<T, U, V, R, ?> function
  ) {
    return (first, second, third) -> {
      try {
        return function.apply(first, second, third);
      } catch (final Throwable ex) {
        return ExceptionUtils.sneakyThrow(ex);
      }
    };
  }
}
