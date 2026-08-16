package org.kulivatsky.autotests.core.util;

import java.io.PrintWriter;
import java.io.StringWriter;

/** Renders and rethrows failures at DSL boundaries without replacing the original throwable object. */
public final class ExceptionUtils {
  /** No instances. */
  private ExceptionUtils() {
  }

  /**
   * Renders a throwable and its complete stack trace to text.
   *
   * @param throwable failure to render
   * @return the same text format produced by {@link Throwable#printStackTrace()}
   */
  public static String stackTrace(final Throwable throwable) {
    final StringWriter buffer = new StringWriter();
    throwable.printStackTrace(new PrintWriter(buffer));
    return buffer.toString();
  }

  /**
   * Rethrows a throwable without wrapping it and presents the expression as a
   * {@link RuntimeException} to the Java compiler.
   *
   * <p>This is a "sneaky throw" adapter. Runtime behavior preserves the original exception class,
   * message, stack trace, and identity.</p>
   *
   * @param throwable throwable to rethrow
   * @return never returns; the return type exists only for expression-oriented call sites
   */
  public static RuntimeException propagate(final Throwable throwable) {
    return sneakyThrow(throwable);
  }

  /**
   * Rethrows a throwable without requiring the caller to declare its checked type.
   *
   * <p>Use only at framework boundaries that accept checked exceptions, such as the
   * throwable functional interfaces in {@code core.step}. It should not be used to hide checked
   * exceptions from ordinary application code.</p>
   *
   * @param throwable throwable to rethrow
   * @param <R> synthetic return type inferred by the caller
   * @return never returns
   */
  public static <R> R sneakyThrow(final Throwable throwable) {
    return ExceptionUtils.<RuntimeException, R>throwUnchecked(throwable);
  }

  /**
   * Performs the unchecked generic cast used by {@link #sneakyThrow(Throwable)}.
   *
   * @param throwable throwable to rethrow
   * @param <E> exception type presented to the compiler
   * @param <R> synthetic return type
   * @return never returns
   * @throws E always, using the original throwable object
   */
  @SuppressWarnings("unchecked")
  private static <E extends Throwable, R> R throwUnchecked(final Throwable throwable) throws E {
    throw (E) throwable;
  }
}
