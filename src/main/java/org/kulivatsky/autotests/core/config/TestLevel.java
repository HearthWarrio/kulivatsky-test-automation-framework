package org.kulivatsky.autotests.core.config;

import java.util.List;

public interface TestLevel {
  static List<TestLevel> current() {
    return Config.current().testLevel();
  }

  enum Option implements TestLevel {
    COMPONENT,
    INTEGRATION,
    SYSTEM
  }
}
