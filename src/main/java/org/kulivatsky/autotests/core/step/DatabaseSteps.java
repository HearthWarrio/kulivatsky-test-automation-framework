package org.kulivatsky.autotests.core.step;

import org.kulivatsky.autotests.core.util.Database;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.kulivatsky.autotests.core.step.Params.params;
import static org.kulivatsky.autotests.core.step.Params.secret;

/** Database step descriptors that keep SQL execution out of the generic step collection. */
public final class DatabaseSteps {
  /** No instances. */
  private DatabaseSteps() {
  }

  /**
   * Creates a database-update step.
   *
   * <p>Bound argument values are masked in Allure by default because SQL parameters frequently
   * contain identifiers, credentials, or personal data. The SQL text itself remains visible.</p>
   *
   * @param database database facade used for execution
   * @param sql SQL statement with JDBC placeholders
   * @param args values bound to placeholders in order
   * @return lazy step that returns the JDBC update count
   */
  public static SupplierStep<Integer> executeUpdate(final Database database,
                                                     final String sql,
                                                     final Object... args) {
    return new SupplierStep.Of<>(
      "Execute database update",
      params("sql", sql, "args", secret(Arrays.toString(args))),
      () -> database.executeUpdate(sql, args)
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
    return new SupplierStep.Of<>(
      "Execute database query",
      params("sql", sql, "args", secret(Arrays.toString(args))),
      () -> database.select(sql, args)
    );
  }

  /**
   * Creates a database-query step that requires exactly one result row.
   *
   * @param database database facade used for execution
   * @param sql SQL query with JDBC placeholders
   * @param args values bound to placeholders in order
   * @return lazy step producing the only result row
   */
  public static SupplierStep<Map<String, Object>> executeSelectOne(final Database database,
                                                                    final String sql,
                                                                    final Object... args) {
    return new SupplierStep.Of<>(
      "Execute single-row database query",
      params("sql", sql, "args", secret(Arrays.toString(args))),
      () -> database.selectOne(sql, args)
    );
  }
}
