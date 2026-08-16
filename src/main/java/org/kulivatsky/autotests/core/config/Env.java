package org.kulivatsky.autotests.core.config;

/** Identifies the environment against which a test launch runs. */
public interface Env {
  /**
   * Reads the current environment from {@link Config}.
   *
   * @return active environment
   */
  static Env current() {
    return Config.current().env();
  }

  /** Built-in environment values understood by the default configuration. */
  enum Option implements Env {
    /** Developer workstation or other local runtime. */
    LOCAL,
    /** Shared test environment. */
    TEST,
    /** Pre-production or staging environment. */
    STAGE,
    /** Production environment. */
    PROD
  }
}
