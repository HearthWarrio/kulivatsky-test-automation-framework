package org.kulivatsky.autotests.core.storage;

import io.restassured.config.HttpClientConfig;
import io.restassured.config.LogConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.config.SSLConfig;
import org.kulivatsky.autotests.core.config.Config;

/** Builds REST Assured configuration from framework properties. */
public final class RestAssuredConfigStorage {
  /** No instances. */
  private RestAssuredConfigStorage() {
  }

  /**
   * Builds a fresh REST Assured configuration for the current launch.
   *
   * <p>{@code rest.timeoutMs} is applied to both socket and connection timeouts. Relaxed HTTPS
   * validation is disabled by default and is enabled only when
   * {@code rest.relaxedHttpsValidation=true} is explicitly configured. Validation-failure logging
   * is also expressed through this returned configuration rather than a second REST Assured global
   * mutation.</p>
   *
   * @return newly built REST Assured configuration
   */
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
    if (config.booleanProperty("rest.logOnValidationFailure", false)) {
      result = result.logConfig(LogConfig.logConfig().enableLoggingOfRequestAndResponseIfValidationFails());
    }
    return result;
  }
}
