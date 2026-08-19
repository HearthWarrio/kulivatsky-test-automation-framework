package org.kulivatsky.autotests.core.storage;

import org.apache.commons.dbcp2.BasicDataSource;
import org.kulivatsky.autotests.core.config.Service;
import org.kulivatsky.autotests.core.config.Config;
import org.kulivatsky.autotests.core.config.Env;
import org.kulivatsky.autotests.core.util.Database;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Process-wide cache of database connection pools keyed by service and environment.
 *
 * <p>Each unique {@code db.<service>.<env>} prefix owns one {@link BasicDataSource}. Calls for the
 * same pair reuse that pool. The returned {@link Database} from {@link #databaseFor(Service, Env)}
 * is a non-owning facade: closing it must not close a pool shared with other tests.</p>
 *
 * <p>Pools are closed both from the TestNG execution listener and from a JVM shutdown hook. Repeated
 * cleanup is safe because the storage is cleared after closing.</p>
 */
public final class DBConnectionPoolStorage {
  /** Logger used for best-effort shutdown diagnostics. */
  private static final Logger LOGGER = LoggerFactory.getLogger(DBConnectionPoolStorage.class);
  /** Cached pools indexed by their configuration prefix. */
  private static final Map<String, BasicDataSource> STORAGE = new ConcurrentHashMap<>();
  /** Serializes pool lookup/replacement with process-wide shutdown cleanup. */
  private static final Object LIFECYCLE_LOCK = new Object();

  static {
    Runtime.getRuntime().addShutdownHook(new Thread(DBConnectionPoolStorage::closeAll, "database-pool-shutdown"));
  }

  /** No instances. */
  private DBConnectionPoolStorage() {
  }

  /**
   * Returns a non-owning {@link Database} facade over the shared pool for a service/environment.
   *
   * @param service logical database service
   * @param env target environment
   * @return database facade backed by the cached pool
   */
  public static Database databaseFor(final Service service, final Env env) {
    final String prefix = "db.%s.%s".formatted(service.id(), normalize(env));
    return Database.shared(dataSourceFor(prefix));
  }

  /**
   * Returns the shared Apache DBCP pool itself.
   *
   * <p>The storage owns this object. Prefer {@link #databaseFor(Service, Env)} unless direct pool
   * inspection is required. If a caller closes the returned pool, the next storage lookup detects
   * the closed instance and recreates it from configuration.</p>
   *
   * @param service logical database service
   * @param env target environment
   * @return cached connection pool
   */
  public static BasicDataSource dbConnectionPoolFor(final Service service, final Env env) {
    final String prefix = "db.%s.%s".formatted(service.id(), normalize(env));
    return dataSourceFor(prefix);
  }

  /**
   * Closes every cached pool and clears the storage.
   *
   * <p>Lookup and shutdown are serialized so a pool cannot be inserted into the cache while
   * {@code closeAll()} is clearing it. Cleanup remains best-effort: one close failure is logged and
   * does not prevent the remaining pools from being released.</p>
   */
  public static void closeAll() {
    synchronized (LIFECYCLE_LOCK) {
      STORAGE.values().forEach(dataSource -> {
        try {
          dataSource.close();
        } catch (final SQLException ex) {
          LOGGER.warn("Unable to close database connection pool", ex);
        }
      });
      STORAGE.clear();
    }
  }

  /**
   * Returns a live cached pool, recreating it if a caller closed the exposed DBCP instance.
   *
   * @param prefix database configuration prefix
   * @return live shared pool
   */
  private static BasicDataSource dataSourceFor(final String prefix) {
    synchronized (LIFECYCLE_LOCK) {
      final BasicDataSource existing = STORAGE.get(prefix);
      if (existing != null && !existing.isClosed()) {
        return existing;
      }
      if (existing != null) {
        STORAGE.remove(prefix, existing);
      }
      final BasicDataSource created = createDataSource(prefix);
      STORAGE.put(prefix, created);
      return created;
    }
  }

  /**
   * Creates a new DBCP pool from properties under one database prefix.
   *
   * @param prefix configuration prefix such as {@code db.orders_database.test}
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
