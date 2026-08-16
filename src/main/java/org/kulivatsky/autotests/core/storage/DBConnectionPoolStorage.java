package org.kulivatsky.autotests.core.storage;

import org.apache.commons.dbcp2.BasicDataSource;
import org.kulivatsky.autotests.core.config.APIService;
import org.kulivatsky.autotests.core.config.Config;
import org.kulivatsky.autotests.core.config.Env;
import org.kulivatsky.autotests.core.util.Database;

import java.sql.SQLException;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class DBConnectionPoolStorage {
  private static final Map<String, BasicDataSource> STORAGE = new ConcurrentHashMap<>();

  static {
    Runtime.getRuntime().addShutdownHook(new Thread(DBConnectionPoolStorage::closeAll, "database-pool-shutdown"));
  }

  private DBConnectionPoolStorage() {
  }

  public static Database databaseFor(final APIService apiService, final Env env) {
    final String prefix = "db.%s.%s".formatted(normalize(apiService), normalize(env));
    final BasicDataSource dataSource = STORAGE.computeIfAbsent(prefix, DBConnectionPoolStorage::createDataSource);
    return Database.shared(dataSource);
  }

  public static BasicDataSource dbConnectionPoolFor(final APIService apiService, final Env env) {
    final String prefix = "db.%s.%s".formatted(normalize(apiService), normalize(env));
    return STORAGE.computeIfAbsent(prefix, DBConnectionPoolStorage::createDataSource);
  }

  public static void closeAll() {
    STORAGE.values().forEach(dataSource -> {
      try {
        dataSource.close();
      } catch (final SQLException ignored) {
        // Shutdown cleanup must not prevent JVM termination.
      }
    });
    STORAGE.clear();
  }

  private static BasicDataSource createDataSource(final String prefix) {
    final Config config = Config.current();
    final BasicDataSource dataSource = new BasicDataSource();
    dataSource.setDriverClassName(config.property(prefix + ".driver"));
    dataSource.setUrl(config.property(prefix + ".url"));
    dataSource.setUsername(config.property(prefix + ".username", ""));
    dataSource.setPassword(config.property(prefix + ".password", ""));
    dataSource.setInitialSize(config.intProperty(prefix + ".initialSize", 0));
    dataSource.setMaxTotal(config.intProperty(prefix + ".maxTotal", 5));
    return dataSource;
  }

  private static String normalize(final Object value) {
    return value.toString().toLowerCase(Locale.ROOT);
  }
}
