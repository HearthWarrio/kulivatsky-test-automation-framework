package org.kulivatsky.autotests.core.config;

import java.util.List;

public interface TestType {
  static List<TestType> current() {
    return Config.current().testType();
  }

  enum Option implements TestType {
    SMOKE,
    REGRESSION
  }
}
