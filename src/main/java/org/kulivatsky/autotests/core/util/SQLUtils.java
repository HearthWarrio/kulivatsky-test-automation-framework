package org.kulivatsky.autotests.core.util;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Low-level JDBC operations with deterministic resource cleanup.
 *
 * <p>Every public method acquires a connection from the supplied {@link DataSource} and closes the
 * connection before returning. With a connection pool this returns the connection to the pool. A
 * statement is always a {@link PreparedStatement}; parameters are bound with
 * {@link PreparedStatement#setObject(int, Object)} in declaration order.</p>
 *
 * <p>Checked {@link SQLException}s are converted to {@link IllegalStateException} because SQL
 * failures at this layer are test-infrastructure/runtime failures rather than recoverable branch
 * conditions for normal test code.</p>
 */
public final class SQLUtils {
  /** No instances. */
  private SQLUtils() {
  }

  /**
   * Executes a JDBC update statement and returns the driver's update count.
   *
   * @param dataSource source of JDBC connections
   * @param sql SQL statement with zero or more {@code ?} placeholders
   * @param params values bound from left to right
   * @return number of rows affected according to {@link PreparedStatement#executeUpdate()}
   * @throws IllegalStateException when connection acquisition, preparation, binding, or execution fails
   */
  public static int executeUpdate(final DataSource dataSource,
                                  final String sql,
                                  final Object... params) {
    try (Connection connection = dataSource.getConnection();
         PreparedStatement statement = prepare(connection, sql, params)) {
      return statement.executeUpdate();
    } catch (final SQLException ex) {
      throw new IllegalStateException("SQL update failed", ex);
    }
  }

  /**
   * Executes a JDBC statement when the caller does not need a result set or update count.
   *
   * @param dataSource source of JDBC connections
   * @param sql SQL statement with zero or more {@code ?} placeholders
   * @param params values bound from left to right
   * @throws IllegalStateException when connection acquisition, preparation, binding, or execution fails
   */
  public static void execute(final DataSource dataSource,
                             final String sql,
                             final Object... params) {
    try (Connection connection = dataSource.getConnection();
         PreparedStatement statement = prepare(connection, sql, params)) {
      statement.execute();
    } catch (final SQLException ex) {
      throw new IllegalStateException("SQL execution failed", ex);
    }
  }

  /**
   * Executes a query and materializes the complete result set.
   *
   * <p>Each row is a {@link LinkedHashMap} keyed by JDBC column label, not column name. SQL aliases
   * therefore become map keys. Column order is preserved in each row map, and row order matches the
   * order returned by the database.</p>
   *
   * @param dataSource source of JDBC connections
   * @param sql SQL query with zero or more {@code ?} placeholders
   * @param params values bound from left to right
   * @return mutable list of mutable row maps; empty when the query returns no rows
   * @throws IllegalStateException when JDBC work fails
   */
  public static List<Map<String, Object>> select(final DataSource dataSource,
                                                  final String sql,
                                                  final Object... params) {
    try (Connection connection = dataSource.getConnection();
         PreparedStatement statement = prepare(connection, sql, params);
         ResultSet resultSet = statement.executeQuery()) {
      final List<Map<String, Object>> rows = new ArrayList<>();
      final ResultSetMetaData metadata = resultSet.getMetaData();
      while (resultSet.next()) {
        final Map<String, Object> row = new LinkedHashMap<>();
        for (int column = 1; column <= metadata.getColumnCount(); column++) {
          row.put(metadata.getColumnLabel(column), resultSet.getObject(column));
        }
        rows.add(row);
      }
      return rows;
    } catch (final SQLException ex) {
      throw new IllegalStateException("SQL select failed", ex);
    }
  }

  /**
   * Executes a query that is expected to return exactly one row.
   *
   * @param dataSource source of JDBC connections
   * @param sql SQL query with zero or more {@code ?} placeholders
   * @param params values bound from left to right
   * @return the only result row
   * @throws NoSuchElementException when the query returns no rows
   * @throws IllegalStateException when the query returns more than one row or JDBC work fails
   */
  public static Map<String, Object> selectOne(final DataSource dataSource,
                                               final String sql,
                                               final Object... params) {
    final List<Map<String, Object>> rows = select(dataSource, sql, params);
    if (rows.isEmpty()) {
      throw new NoSuchElementException("SQL query returned no rows");
    }
    if (rows.size() > 1) {
      throw new IllegalStateException("Expected one SQL row, got " + rows.size());
    }
    return rows.getFirst();
  }

  /**
   * Prepares a statement and binds positional parameters.
   *
   * <p>JDBC parameter indices start at one, while the Java array starts at zero. No type-specific
   * coercion is done here; the driver receives each value through {@code setObject}.</p>
   *
   * @param connection open JDBC connection
   * @param sql SQL text
   * @param params positional parameter values
   * @return prepared and fully bound statement; caller owns and must close it
   * @throws SQLException when preparation or parameter binding fails
   */
  private static PreparedStatement prepare(final Connection connection,
                                           final String sql,
                                           final Object... params) throws SQLException {
    final PreparedStatement statement = connection.prepareStatement(sql);
    for (int index = 0; index < params.length; index++) {
      statement.setObject(index + 1, params[index]);
    }
    return statement;
  }
}
