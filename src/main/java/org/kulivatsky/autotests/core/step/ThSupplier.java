package org.kulivatsky.autotests.core.step;

import org.kulivatsky.autotests.core.util.ExceptionUtils;

import java.util.function.Supplier;

/**
 * {@link Supplier}-shaped function that may throw a checked exception.
 *
 * @param <R> supplied value type
 * @param <E> declared exception type
 */
@FunctionalInterface
public interface ThSupplier<R, E extends Throwable> {
  /**
   * Produces a value.
   *
   * @return supplied value
   * @throws E exception produced while obtaining the value
   */
  R get() throws E;

  /**
   * Adapts a throwable supplier to {@link Supplier} while preserving the original failure object.
   *
   * @param supplier supplier to adapt
   * @param <R> supplied value type
   * @return standard supplier that rethrows failures unchecked
   */
  static <R> Supplier<R> unchecked(final ThSupplier<R, ?> supplier) {
    return () -> {
      try {
        return supplier.get();
      } catch (final Throwable ex) {
        throw ExceptionUtils.propagate(ex);
      }
    };
  }
}
