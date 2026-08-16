package org.kulivatsky.autotests.core.config;

import java.util.List;

public interface APIService {
  static List<APIService> current() {
    return Config.current().apiService();
  }

  enum Option implements APIService {
    DEMO_API,
    DEMO_DATABASE,
    DEMO_KAFKA
  }
}
