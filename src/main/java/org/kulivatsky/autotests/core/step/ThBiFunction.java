package org.kulivatsky.autotests.core.step;

import org.kulivatsky.autotests.core.util.ExceptionUtils;

import java.util.function.BiFunction;

/**
 * {@link BiFunction}-shaped transformation that may throw a checked exception.
 *
 * @param <T> first input type
 * @param <U> second input type
 * @param <R> result type
 * @param <E> declared exception type
 */
@FunctionalInterface
public interface ThBiFunction<T, U, R, E extends Throwable> {
  /**
   * Applies the transformation to two inputs.
   *
   * @param first first input
   * @param second second input
   * @return transformation result
   * @throws E exception produced by the transformation
   */
  R apply(T first, U second) throws E;

  /**
   * Adapts a throwable bi-function to {@link BiFunction} without adding an exception wrapper.
   *
   * @param function function to adapt
   * @param <T> first input type
   * @param <U> second input type
   * @param <R> result type
   * @return standard bi-function that rethrows failures unchecked
   */
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
