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

/**
 * Process-wide cache of database connection pools keyed by service and environment.
 *
 * <p>Each unique {@code db.<service>.<env>} prefix owns one {@link BasicDataSource}. Calls for the
 * same pair reuse that pool. The returned {@link Database} from {@link #databaseFor(APIService, Env)}
 * is a non-owning facade: closing it must not close a pool shared with other tests.</p>
 *
 * <p>Pools are closed both from the TestNG execution listener and from a JVM shutdown hook. Repeated
 * cleanup is safe because the storage is cleared after closing.</p>
 */
public final class DBConnectionPoolStorage {
  /** Cached pools indexed by their configuration prefix. */
  private static final Map<String, BasicDataSource> STORAGE = new ConcurrentHashMap<>();

  static {
    Runtime.getRuntime().addShutdownHook(new Thread(DBConnectionPoolStorage::closeAll, "database-pool-shutdown"));
  }

  /** No instances. */
  private DBConnectionPoolStorage() {
  }

  /**
   * Returns a non-owning {@link Database} facade over the shared pool for a service/environment.
   *
   * @param apiService logical database service
   * @param env target environment
   * @return database facade backed by the cached pool
   */
  public static Database databaseFor(final APIService apiService, final Env env) {
    final String prefix = "db.%s.%s".formatted(normalize(apiService), normalize(env));
    final BasicDataSource dataSource = STORAGE.computeIfAbsent(prefix, DBConnectionPoolStorage::createDataSource);
    return Database.shared(dataSource);
  }

  /**
   * Returns the shared Apache DBCP pool itself.
   *
   * <p>The storage owns this object. Callers that close it directly invalidate the cached pool for
   * every other user of the same service/environment pair. Prefer {@link #databaseFor(APIService, Env)}
   * unless direct pool configuration or inspection is required.</p>
   *
   * @param apiService logical database service
   * @param env target environment
   * @return cached connection pool
   */
  public static BasicDataSource dbConnectionPoolFor(final APIService apiService, final Env env) {
    final String prefix = "db.%s.%s".formatted(normalize(apiService), normalize(env));
    return STORAGE.computeIfAbsent(prefix, DBConnectionPoolStorage::createDataSource);
  }

  /**
   * Closes every cached pool and clears the storage.
   *
   * <p>Cleanup is best-effort: an {@link SQLException} from one pool is ignored so shutdown can
   * continue and the JVM is not kept alive by cleanup code.</p>
   */
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

  /**
   * Creates a new DBCP pool from properties under one database prefix.
   *
   * @param prefix configuration prefix such as {@code db.demo_database.local}
   * @return configured but lazily connected data source
   */
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

  /**
   * Converts service/environment identifiers to stable configuration-key fragments.
   *
   * @param value identifier
   * @return lower-case string using {@link Locale#ROOT}
   */
  private static String normalize(final Object value) {
    return value.toString().toLowerCase(Locale.ROOT);
  }
}
