package org.kulivatsky.autotests.core.step;

import org.kulivatsky.autotests.core.util.ExceptionUtils;

import java.util.function.Function;

/**
 * {@link Function}-shaped transformation that may throw a checked exception.
 *
 * @param <T> input type
 * @param <R> result type
 * @param <E> declared exception type
 */
@FunctionalInterface
public interface ThFunction<T, R, E extends Throwable> {
  /**
   * Transforms the supplied value.
   *
   * @param value input value
   * @return transformation result
   * @throws E exception produced by the transformation
   */
  R apply(T value) throws E;

  /**
   * Adapts a throwable function to {@link Function} without adding an exception wrapper.
   *
   * @param function function to adapt
   * @param <T> input type
   * @param <R> result type
   * @return standard function that rethrows failures unchecked
   */
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
