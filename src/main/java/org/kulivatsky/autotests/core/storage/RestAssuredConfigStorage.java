package org.kulivatsky.autotests.core.storage;

import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.config.SSLConfig;
import org.kulivatsky.autotests.core.config.APIService;
import org.kulivatsky.autotests.core.config.Config;
import org.kulivatsky.autotests.core.config.Env;

public final class RestAssuredConfigStorage {
  private RestAssuredConfigStorage() {
  }

  public static RestAssuredConfig config() {
    final Config config = Config.current();
    final int timeout = config.intProperty("rest.timeoutMs", 30_000);
    RestAssuredConfig result = RestAssuredConfig.config().httpClient(
      HttpClientConfig.httpClientConfig()
        .setParam("http.socket.timeout", timeout)
        .setParam("http.connection.timeout", timeout)
    );
    if (config.booleanProperty("rest.relaxedHttpsValidation", false)) {
      result = result.sslConfig(new SSLConfig().relaxedHTTPSValidation());
    }
    return result;
  }

  public static RestAssuredConfig configFor(final APIService apiService, final Env env) {
    return config();
  }
}
