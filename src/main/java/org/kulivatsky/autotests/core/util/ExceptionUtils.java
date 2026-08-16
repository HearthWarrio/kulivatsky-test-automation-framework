package org.kulivatsky.autotests.core.util;

import java.io.PrintWriter;
import java.io.StringWriter;

public final class ExceptionUtils {
  private ExceptionUtils() {
  }

  public static String stackTrace(final Throwable throwable) {
    final StringWriter buffer = new StringWriter();
    throwable.printStackTrace(new PrintWriter(buffer));
    return buffer.toString();
  }

  public static RuntimeException propagate(final Throwable throwable) {
    return sneakyThrow(throwable);
  }

  public static <R> R sneakyThrow(final Throwable throwable) {
    return ExceptionUtils.<RuntimeException, R>throwUnchecked(throwable);
  }

  @SuppressWarnings("unchecked")
  private static <E extends Throwable, R> R throwUnchecked(final Throwable throwable) throws E {
    throw (E) throwable;
  }
}
