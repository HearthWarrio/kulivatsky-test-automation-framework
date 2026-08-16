package org.kulivatsky.autotests.core.util;

import org.awaitility.Awaitility;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

public final class AwaitilityUtils {
  private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(10);
  private static final Duration DEFAULT_POLL_INTERVAL = Duration.ofMillis(100);

  private AwaitilityUtils() {
  }

  public static void waitForAssertTrue(final Supplier<Boolean> condition) {
    waitForAssertTrue(condition, DEFAULT_TIMEOUT);
  }

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
