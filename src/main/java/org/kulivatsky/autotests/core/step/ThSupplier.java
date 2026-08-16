package org.kulivatsky.autotests.core.step;

import org.kulivatsky.autotests.core.util.ExceptionUtils;

import java.util.function.Supplier;

@FunctionalInterface
public interface ThSupplier<R, E extends Throwable> {
  R get() throws E;

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
