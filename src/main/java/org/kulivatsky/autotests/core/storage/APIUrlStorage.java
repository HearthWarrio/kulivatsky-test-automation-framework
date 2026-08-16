package org.kulivatsky.autotests.core.storage;

import org.kulivatsky.autotests.core.config.APIService;
import org.kulivatsky.autotests.core.config.Config;
import org.kulivatsky.autotests.core.config.Env;

import java.util.Locale;

public final class APIUrlStorage {
  private APIUrlStorage() {
  }

  public static String apiUrlFor(final APIService apiService, final Env env) {
    return Config.current().property(key("api", apiService, env, "url"));
  }

  private static String key(final String prefix,
                            final Object service,
                            final Object env,
                            final String suffix) {
    return String.join(".", prefix, normalize(service), normalize(env), suffix);
  }

  private static String normalize(final Object value) {
    return value.toString().toLowerCase(Locale.ROOT);
  }
}
