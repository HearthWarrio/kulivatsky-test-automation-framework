package org.kulivatsky.autotests.core.config;

/** Identifies how the current test run was started. */
public interface Launch {
  /**
   * Reads the active launch mode from {@link Config}.
   *
   * @return active launch mode
   */
  static Launch current() {
    return Config.current().launch();
  }

  /** Built-in launch modes. */
  enum Option implements Launch {
    /** Run started directly by a developer or local tool. */
    LOCAL,
    /** Run started by a continuous-integration job. */
    CI
  }
}
