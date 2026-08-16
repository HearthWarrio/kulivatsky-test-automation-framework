package org.kulivatsky.autotests.core.util;

import org.apache.commons.dbcp2.BasicDataSource;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public final class Database implements AutoCloseable {
  private final DataSource dataSource;
  private final boolean ownsDataSource;

  private Database(final DataSource dataSource, final boolean ownsDataSource) {
    this.dataSource = dataSource;
    this.ownsDataSource = ownsDataSource;
  }

  public static Database of(final DataSource dataSource) {
    return new Database(dataSource, true);
  }

  public static Database shared(final DataSource dataSource) {
    return new Database(dataSource, false);
  }

  public DataSource dataSource() {
    return this.dataSource;
  }

  public int executeUpdate(final String sql, final Object... params) {
    return SQLUtils.executeUpdate(this.dataSource, sql, params);
  }

  public void execute(final String sql, final Object... params) {
    SQLUtils.execute(this.dataSource, sql, params);
  }

  public List<Map<String, Object>> select(final String sql, final Object... params) {
    return SQLUtils.select(this.dataSource, sql, params);
  }

  public Map<String, Object> selectOne(final String sql, final Object... params) {
    return SQLUtils.selectOne(this.dataSource, sql, params);
  }

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
