package org.kulivatsky.autotests.core.config;

import java.util.List;

/** Identifies the execution purpose of a test. */
public interface TestType {
  /**
   * Reads test types selected for the current launch.
   *
   * @return configured types; an empty list means no type filter
   */
  static List<TestType> current() {
    return Config.current().testType();
  }

  /** Built-in test types. */
  enum Option implements TestType {
    /** Small, fast set used to detect obvious breakage. */
    SMOKE,
    /** Broader set intended to detect regressions in existing behavior. */
    REGRESSION
  }
}
