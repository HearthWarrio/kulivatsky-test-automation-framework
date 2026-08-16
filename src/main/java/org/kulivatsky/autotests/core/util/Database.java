package org.kulivatsky.autotests.core.util;

import org.apache.commons.dbcp2.BasicDataSource;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Thin JDBC facade with explicit data-source ownership semantics.
 *
 * <p>{@link #of(DataSource)} creates an owning facade: {@link #close()} closes the data source when
 * it is an Apache DBCP {@link BasicDataSource}. {@link #shared(DataSource)} creates a non-owning
 * facade intended for pools managed elsewhere, such as
 * {@link org.kulivatsky.autotests.core.storage.DBConnectionPoolStorage}.</p>
 *
 * <p>Individual SQL operations delegate to {@link SQLUtils}, which opens and closes a JDBC
 * connection per call. Closing a connection obtained from a pool returns it to the pool; it does
 * not close the pool itself.</p>
 */
public final class Database implements AutoCloseable {
  /** Data source used for every SQL operation. */
  private final DataSource dataSource;
  /** Whether this facade is allowed to close the underlying DBCP pool. */
  private final boolean ownsDataSource;

  /**
   * Creates a facade with explicit ownership.
   *
   * @param dataSource underlying JDBC data source
   * @param ownsDataSource whether {@link #close()} owns lifecycle of a DBCP data source
   */
  private Database(final DataSource dataSource, final boolean ownsDataSource) {
    this.dataSource = dataSource;
    this.ownsDataSource = ownsDataSource;
  }

  /**
   * Wraps a data source and makes this {@code Database} responsible for closing it when supported.
   *
   * @param dataSource data source to wrap
   * @return owning database facade
   */
  public static Database of(final DataSource dataSource) {
    return new Database(dataSource, true);
  }

  /**
   * Wraps a data source without taking ownership of its lifecycle.
   *
   * @param dataSource externally managed data source
   * @return non-owning database facade
   */
  public static Database shared(final DataSource dataSource) {
    return new Database(dataSource, false);
  }

  /**
   * Exposes the underlying data source for APIs that need direct JDBC access.
   *
   * @return wrapped data source
   */
  public DataSource dataSource() {
    return this.dataSource;
  }

  /**
   * Executes an INSERT, UPDATE, DELETE, or other JDBC update statement.
   *
   * @param sql SQL statement with {@code ?} placeholders
   * @param params values bound to placeholders from left to right
   * @return JDBC update count
   */
  public int executeUpdate(final String sql, final Object... params) {
    return SQLUtils.executeUpdate(this.dataSource, sql, params);
  }

  /**
   * Executes a SQL statement when no result or update count is required by the caller.
   *
   * @param sql SQL statement with {@code ?} placeholders
   * @param params values bound to placeholders from left to right
   */
  public void execute(final String sql, final Object... params) {
    SQLUtils.execute(this.dataSource, sql, params);
  }

  /**
   * Executes a query and maps every row by JDBC column label.
   *
   * @param sql SQL query with {@code ?} placeholders
   * @param params values bound to placeholders from left to right
   * @return rows in database result order
   */
  public List<Map<String, Object>> select(final String sql, final Object... params) {
    return SQLUtils.select(this.dataSource, sql, params);
  }

  /**
   * Executes a query that must return exactly one row.
   *
   * @param sql SQL query with {@code ?} placeholders
   * @param params values bound to placeholders from left to right
   * @return the only result row
   * @throws java.util.NoSuchElementException when no rows are returned
   * @throws IllegalStateException when more than one row is returned or JDBC execution fails
   */
  public Map<String, Object> selectOne(final String sql, final Object... params) {
    return SQLUtils.selectOne(this.dataSource, sql, params);
  }

  /**
   * Closes the owned data source when it is a {@link BasicDataSource}.
   *
   * <p>For a facade created with {@link #shared(DataSource)}, this method is intentionally a no-op.
   * For other {@link DataSource} implementations, no generic close contract exists, so this method
   * also leaves them untouched.</p>
   *
   * @throws IllegalStateException when an owned DBCP pool cannot be closed
   */
  @Override
  public void close() {
    if (this.ownsDataSource && this.dataSource instanceof BasicDataSource basicDataSource) {
      try {
        basicDataSource.close();
      } catch (final SQLException ex) {
        throw new IllegalStateException("Unable to close database connection pool", ex);
      }
    }
  }
}
