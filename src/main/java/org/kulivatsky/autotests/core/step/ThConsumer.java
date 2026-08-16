package org.kulivatsky.autotests.core.step;

import org.kulivatsky.autotests.core.util.ExceptionUtils;

import java.util.function.Consumer;

/**
 * {@link Consumer}-shaped action that may throw a checked exception.
 *
 * @param <T> consumed value type
 * @param <E> declared exception type
 */
@FunctionalInterface
public interface ThConsumer<T, E extends Throwable> {
  /**
   * Consumes a value.
   *
   * @param value value passed to the action
   * @throws E exception produced by the action
   */
  void accept(T value) throws E;

  /**
   * Adapts a throwable consumer to {@link Consumer} without adding an exception wrapper.
   *
   * @param consumer consumer to adapt
   * @param <T> consumed value type
   * @return standard consumer that rethrows failures unchecked
   */
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
