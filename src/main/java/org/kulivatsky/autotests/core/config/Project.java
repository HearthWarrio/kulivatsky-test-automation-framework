package org.kulivatsky.autotests.core.config;

import java.util.List;

/** Identifies a logical project used for launch filtering and test metadata. */
public interface Project {
  /**
   * Reads projects selected for the current launch.
   *
   * @return configured projects; an empty list means no project filter
   */
  static List<Project> current() {
    return Config.current().project();
  }

  /** Built-in project identifiers. */
  enum Option implements Project {
    /** Self-contained demo project shipped with the framework. */
    DEMO
  }
}
