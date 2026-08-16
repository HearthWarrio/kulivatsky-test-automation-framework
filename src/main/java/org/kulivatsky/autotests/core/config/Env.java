package org.kulivatsky.autotests.core.config;

public interface Env {
  static Env current() {
    return Config.current().env();
  }

  enum Option implements Env {
    LOCAL,
    TEST,
    STAGE,
    PROD
  }
}
