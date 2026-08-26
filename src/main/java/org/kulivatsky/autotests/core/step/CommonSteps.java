package org.kulivatsky.autotests.core.step;

import java.time.Duration;
import java.util.Arrays;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.kulivatsky.autotests.core.step.Params.params;

/** Reusable step descriptors for generic assertions and deliberate delays. */
public final class CommonSteps {
  /** No instances. */
  private CommonSteps() {
  }

  /**
   * Creates an assertion step that verifies all expected values are present in an iterable.
   *
   * @param origin actual values to inspect
   * @param values values that must be present
   * @param <T> element type
   * @return lazy assertion step
   */
  @SafeVarargs
  @SuppressWarnings("varargs")
  public static <T> RunnableStep listContains(final Iterable<T> origin, final T... values) {
    return new RunnableStep.Of("Collection contains expected values", params("expected", Arrays.toString(values)), () ->
      assertThat(origin).containsAll(Arrays.asList(values))
    );
  }

  /**
   * Creates an assertion step that verifies the actual map contains every expected entry.
   *
   * @param origin actual map
   * @param entries expected key/value pairs
   * @param <K> map key type
   * @param <V> map value type
   * @return lazy assertion step
   */
  public static <K, V> RunnableStep mapContains(final Map<K, V> origin, final Map<K, V> entries) {
    return new RunnableStep.Of("Map contains expected entries", params("expected", entries), () ->
      assertThat(origin).containsAllEntriesOf(entries)
    );
  }

  /**
   * Creates a delay step measured in whole seconds.
   *
   * @param seconds number of seconds to wait
   * @return lazy delay step measured in whole seconds
   */
  public static RunnableStep delay(final long seconds) {
    return delay(Duration.ofSeconds(seconds));
  }

  /**
   * Creates a delay step backed by {@link Thread#sleep(long)}.
   *
   * <p>If the worker thread is interrupted, the step restores the interrupt flag before propagating
   * the original {@link InterruptedException} through the throwable step boundary.</p>
   *
   * @param duration duration converted to milliseconds
   * @return lazy delay step
   */
  public static RunnableStep delay(final Duration duration) {
    return new RunnableStep.Of("Wait " + duration.toMillis() + " ms", params("duration", duration), () -> {
      try {
        Thread.sleep(duration.toMillis());
      } catch (final InterruptedException exception) {
        Thread.currentThread().interrupt();
        throw exception;
      }
    });
  }
}
