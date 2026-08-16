package org.kulivatsky.autotests.core.step;

import org.kulivatsky.autotests.core.util.ExceptionUtils;

/**
 * {@link Runnable}-shaped action that may throw a checked exception.
 *
 * @param <E> declared exception type
 */
@FunctionalInterface
public interface ThRunnable<E extends Throwable> {
  /**
   * Runs the action.
   *
   * @throws E exception produced by the action
   */
  void run() throws E;

  /**
   * Adapts a throwable action to a standard {@link Runnable} without wrapping the original
   * exception in another exception type.
   *
   * <p>The adapter uses {@link ExceptionUtils#propagate(Throwable)}. This is intentional: test code
   * sees the original assertion or checked exception in Allure/TestNG instead of an extra wrapper.</p>
   *
   * @param runnable action to adapt
   * @return standard runnable that rethrows the original failure unchecked
   */
  static Runnable unchecked(final ThRunnable<?> runnable) {
    return () -> {
      try {
        runnable.run();
      } catch (final Throwable ex) {
        throw ExceptionUtils.propagate(ex);
      }
    };
  }
}
