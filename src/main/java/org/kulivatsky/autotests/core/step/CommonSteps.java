package org.kulivatsky.autotests.core.step;

import org.kulivatsky.autotests.core.util.Database;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.kulivatsky.autotests.core.step.Params.params;

/** Reusable step descriptors for assertions, delays, and database operations. */
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
   * @return lazy assertion step; no assertion runs until the step is executed
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
   * @param seconds number of seconds to sleep
   * @return lazy delay step
   */
  public static RunnableStep delay(final long seconds) {
    return delay(Duration.ofSeconds(seconds));
  }

  /**
   * Creates a delay step backed by {@link Thread#sleep(long)}.
   *
   * <p>The step does not catch {@link InterruptedException}; the framework step runner records and
   * rethrows it as the original failure.</p>
   *
   * @param duration duration converted to milliseconds for {@code Thread.sleep}
   * @return lazy delay step
   */
  public static RunnableStep delay(final Duration duration) {
    return new RunnableStep.Of("Wait " + duration.toMillis() + " ms", params("duration", duration), () ->
      Thread.sleep(duration.toMillis())
    );
  }

  /**
   * Creates a database-update step.
   *
   * @param database database facade used for execution
   * @param sql SQL statement with JDBC placeholders
   * @param args values bound to placeholders in order
   * @return lazy step that returns the JDBC update count
   */
  public static SupplierStep<Integer> executeUpdate(final Database database,
                                                    final String sql,
                                                    final Object... args) {
    return new SupplierStep.Of<>("Execute database update", params("sql", sql, "args", Arrays.toString(args)), () ->
      database.executeUpdate(sql, args)
    );
  }

  /**
   * Creates a database-query step that returns every row.
   *
   * @param database database facade used for execution
   * @param sql SQL query with JDBC placeholders
   * @param args values bound to placeholders in order
   * @return lazy step producing rows as insertion-ordered column maps
   */
  public static SupplierStep<List<Map<String, Object>>> executeSelect(final Database database,
                                                                       final String sql,
                                                                       final Object... args) {
    return new SupplierStep.Of<>("Execute database query", params("sql", sql, "args", Arrays.toString(args)), () ->
      database.select(sql, args)
    );
  }

  /**
   * Creates a database-query step that requires exactly one result row.
   *
   * @param database database facade used for execution
   * @param sql SQL query with JDBC placeholders
   * @param args values bound to placeholders in order
   * @return lazy step producing the only result row
   * @see Database#selectOne(String, Object...)
   */
  public static SupplierStep<Map<String, Object>> executeSelectOne(final Database database,
                                                                    final String sql,
                                                                    final Object... args) {
    return new SupplierStep.Of<>("Execute single-row database query", params("sql", sql, "args", Arrays.toString(args)), () ->
      database.selectOne(sql, args)
    );
  }
}
