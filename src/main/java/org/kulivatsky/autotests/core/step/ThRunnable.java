package org.kulivatsky.autotests.core.step;

import org.kulivatsky.autotests.core.util.ExceptionUtils;

@FunctionalInterface
public interface ThRunnable<E extends Throwable> {
  void run() throws E;

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
