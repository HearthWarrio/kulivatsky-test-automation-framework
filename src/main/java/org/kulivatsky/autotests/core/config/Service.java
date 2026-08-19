package org.kulivatsky.autotests.core.config;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Identifies a logical system dependency such as an HTTP API, database, or message broker.
 *
 * <p>The framework deliberately does not ship a fixed service enum. A consuming project may use
 * its own enum by implementing this interface, or create an identifier with {@link #of(String)}.
 * The identifier is also used as the service fragment in configuration keys such as
 * {@code api.orders.local.url}, {@code db.orders.local.url}, or
 * {@code kafka.events.local.bootstrapServers}.</p>
 */
@FunctionalInterface
public interface Service {
  /**
   * Returns the configuration-safe service identifier.
   *
   * @return lower-case identifier containing letters, digits, or {@code _}
   */
  String id();

  /**
   * Reads services selected for the current launch.
   *
   * @return configured services; an empty list means no service filter
   */
  static List<Service> current() {
    return Config.current().service();
  }

  /**
   * Creates a service identifier without requiring a framework-specific enum.
   *
   * @param id service id, for example {@code orders_api} or {@code event_bus}
   * @return logical identifier whose normalized value is available through {@link #id()}
   * @throws IllegalArgumentException when the id is blank or contains characters unsuitable for configuration keys
   */
  static Service of(final String id) {
    final String normalized = normalize(id, "service");
    return () -> normalized;
  }

  /**
   * Validates and normalizes an identifier used in configuration keys.
   *
   * @param id input identifier
   * @param label label used in validation messages
   * @return normalized lower-case identifier
   */
  private static String normalize(final String id, final String label) {
    final String normalized = Objects.requireNonNull(id, label + " id")
      .trim()
      .toLowerCase(Locale.ROOT);
    if (normalized.isEmpty() || !normalized.matches("[a-z0-9_]+")) {
      throw new IllegalArgumentException(label + " id must match [a-z0-9_]+: " + id);
    }
    return normalized;
  }
}
