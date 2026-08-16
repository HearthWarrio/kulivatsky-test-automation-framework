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

public final class SQLUtils {
  private SQLUtils() {
  }

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
