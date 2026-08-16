package org.kulivatsky.autotests.core.config;

public interface Launch {
  static Launch current() {
    return Config.current().launch();
  }

  enum Option implements Launch {
    LOCAL,
    CI
  }
}
