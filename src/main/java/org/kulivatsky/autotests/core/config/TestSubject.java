package org.kulivatsky.autotests.core.config;

import java.util.List;

public interface TestSubject {
  static List<TestSubject> current() {
    return Config.current().testSubject();
  }

  enum Option implements TestSubject {
    BACKEND,
    UI,
    E2E
  }
}
