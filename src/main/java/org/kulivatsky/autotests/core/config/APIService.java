package org.kulivatsky.autotests.core.config;

import java.util.List;

/**
 * Marker type for logical services that can be selected for a test launch.
 *
 * <p>The interface keeps framework code independent from a particular enum implementation while
 * {@link Option} supplies the built-in demo values.</p>
 */
public interface APIService {
  /**
   * Reads the services selected for the current launch.
   *
   * @return configured services; an empty list means no service filter
   */
  static List<APIService> current() {
    return Config.current().apiService();
  }

  /** Built-in service identifiers used by the portable demo tests. */
  enum Option implements APIService {
    /** Demo HTTP API. */
    DEMO_API,
    /** Demo relational database. */
    DEMO_DATABASE,
    /** Demo Kafka broker. */
    DEMO_KAFKA
  }
}
