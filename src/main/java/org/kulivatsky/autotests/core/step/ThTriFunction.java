package org.kulivatsky.autotests.core.step;

import org.kulivatsky.autotests.core.util.ExceptionUtils;

/**
 * Three-argument transformation that may throw a checked exception.
 *
 * @param <T> first input type
 * @param <U> second input type
 * @param <V> third input type
 * @param <R> result type
 * @param <E> declared exception type
 */
@FunctionalInterface
public interface ThTriFunction<T, U, V, R, E extends Throwable> {
  /**
   * Applies the transformation to three inputs.
   *
   * @param first first input
   * @param second second input
   * @param third third input
   * @return transformation result
   * @throws E exception produced by the transformation
   */
  R apply(T first, U second, V third) throws E;

  /**
   * Returns an equivalent tri-function whose compile-time exception type is unchecked.
   *
   * <p>The original failure object is rethrown; it is not wrapped in a new
   * {@link RuntimeException}.</p>
   *
   * @param function function to adapt
   * @param <T> first input type
   * @param <U> second input type
   * @param <V> third input type
   * @param <R> result type
   * @return tri-function with an unchecked declared exception type
   */
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
