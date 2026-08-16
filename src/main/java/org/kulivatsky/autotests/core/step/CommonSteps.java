package org.kulivatsky.autotests.core.step;

import org.kulivatsky.autotests.core.util.Database;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.kulivatsky.autotests.core.step.Params.params;

public final class CommonSteps {
  private CommonSteps() {
  }

  @SafeVarargs
  @SuppressWarnings("varargs")
  public static <T> RunnableStep listContains(final Iterable<T> origin, final T... values) {
    return new RunnableStep.Of("Collection contains expected values", params("expected", Arrays.toString(values)), () ->
      assertThat(origin).containsAll(Arrays.asList(values))
    );
  }

  public static <K, V> RunnableStep mapContains(final Map<K, V> origin, final Map<K, V> entries) {
    return new RunnableStep.Of("Map contains expected entries", params("expected", entries), () ->
      assertThat(origin).containsAllEntriesOf(entries)
    );
  }

  public static RunnableStep delay(final long seconds) {
    return delay(Duration.ofSeconds(seconds));
  }

  public static RunnableStep delay(final Duration duration) {
    return new RunnableStep.Of("Wait " + duration.toMillis() + " ms", params("duration", duration), () ->
      Thread.sleep(duration.toMillis())
    );
  }

  public static SupplierStep<Integer> executeUpdate(final Database database,
                                                    final String sql,
                                                    final Object... args) {
    return new SupplierStep.Of<>("Execute database update", params("sql", sql, "args", Arrays.toString(args)), () ->
      database.executeUpdate(sql, args)
    );
  }

  public static SupplierStep<List<Map<String, Object>>> executeSelect(final Database database,
                                                                       final String sql,
                                                                       final Object... args) {
    return new SupplierStep.Of<>("Execute database query", params("sql", sql, "args", Arrays.toString(args)), () ->
      database.select(sql, args)
    );
  }

  public static SupplierStep<Map<String, Object>> executeSelectOne(final Database database,
                                                                    final String sql,
                                                                    final Object... args) {
    return new SupplierStep.Of<>("Execute single-row database query", params("sql", sql, "args", Arrays.toString(args)), () ->
      database.selectOne(sql, args)
    );
  }
}
