package org.kulivatsky.autotests.core.util;

import org.awaitility.Awaitility;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

/** Polls eventually consistent state with Awaitility and preserves the last observed value where useful. */
public final class AwaitilityUtils {
  /** Default maximum wait used by the one-argument boolean helper. */
  private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(10);
  /** Polling interval used by every wait operation. */
  private static final Duration DEFAULT_POLL_INTERVAL = Duration.ofMillis(100);

  /** No instances. */
  private AwaitilityUtils() {
  }

  /**
   * Waits up to ten seconds for a supplied Boolean to become true.
   *
   * @param condition condition evaluated repeatedly
   */
  public static void waitForAssertTrue(final Supplier<Boolean> condition) {
    waitForAssertTrue(condition, DEFAULT_TIMEOUT);
  }

  /**
   * Waits until a supplied Boolean is true or the timeout expires.
   *
   * <p>The last observed value is attached to Allure if polling ends with an assertion/runtime
   * failure. A null value is treated as a failed AssertJ boolean assertion, not as true.</p>
   *
   * @param condition condition evaluated every 100 ms
   * @param timeout maximum wait duration
   */
  public static void waitForAssertTrue(final Supplier<Boolean> condition,
                                       final Duration timeout) {
    final AtomicReference<Boolean> lastValue = new AtomicReference<>();
    try {
      Awaitility.await()
        .atMost(timeout)
        .pollInterval(DEFAULT_POLL_INTERVAL)
        .untilAsserted(() -> {
          final Boolean current = condition.get();
          lastValue.set(current);
          assertThat(current).isTrue();
        });
    } catch (final RuntimeException | AssertionError ex) {
      AllureUtils.attachText("Last observed value", lastValue.get());
      throw ex;
    }
  }

  /**
   * Polls a value until it satisfies a predicate and returns the matching observation.
   *
   * <p>The supplier is evaluated first, its value is stored, and then the predicate is applied to
   * that exact value. The returned result is therefore the value that satisfied the predicate, not
   * a second supplier invocation made after Awaitility completes.</p>
   *
   * @param supplier value source polled every 100 ms
   * @param predicate condition applied to each observed value
   * @param timeout maximum wait duration
   * @param <T> observed value type
   * @return the first observed value that satisfies the predicate
   */
  public static <T> T waitForValue(final Supplier<T> supplier,
                                   final java.util.function.Predicate<T> predicate,
                                   final Duration timeout) {
    final AtomicReference<T> lastValue = new AtomicReference<>();
    Awaitility.await()
      .atMost(timeout)
      .pollInterval(DEFAULT_POLL_INTERVAL)
      .until(() -> {
        final T current = supplier.get();
        lastValue.set(current);
        return predicate.test(current);
      });
    return lastValue.get();
  }
}
