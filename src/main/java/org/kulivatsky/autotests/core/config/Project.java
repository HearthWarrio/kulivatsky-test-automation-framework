package org.kulivatsky.autotests.core.config;

import java.util.List;

public interface Project {
  static List<Project> current() {
    return Config.current().project();
  }

  enum Option implements Project {
    DEMO
  }
}
