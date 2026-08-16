package org.kulivatsky.autotests.core.config;

import java.util.List;

/** Describes the architectural scope of a test. */
public interface TestLevel {
  /**
   * Reads test levels selected for the current launch.
   *
   * @return configured levels; an empty list means no level filter
   */
  static List<TestLevel> current() {
    return Config.current().testLevel();
  }

  /** Built-in test levels. */
  enum Option implements TestLevel {
    /** A single component is the main test boundary. */
    COMPONENT,
    /** Several components or external interfaces are exercised together. */
    INTEGRATION,
    /** The system is exercised through a complete externally visible flow. */
    SYSTEM
  }
}
