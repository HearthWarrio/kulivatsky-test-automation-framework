package org.kulivatsky.autotests.core.config;

import java.util.List;

/** Identifies the part of the product primarily exercised by a test. */
public interface TestSubject {
  /**
   * Reads subjects selected for the current launch.
   *
   * @return configured subjects; an empty list means no subject filter
   */
  static List<TestSubject> current() {
    return Config.current().testSubject();
  }

  /** Built-in test subjects. */
  enum Option implements TestSubject {
    /** Service, API, messaging, or persistence behavior. */
    BACKEND,
    /** Browser user interface behavior. */
    UI,
    /** A flow crossing multiple system boundaries from entry point to observable result. */
    E2E
  }
}
