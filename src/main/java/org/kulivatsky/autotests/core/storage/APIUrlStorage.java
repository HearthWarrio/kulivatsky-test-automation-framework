package org.kulivatsky.autotests.core.storage;

import org.kulivatsky.autotests.core.config.Service;
import org.kulivatsky.autotests.core.config.Config;
import org.kulivatsky.autotests.core.config.Env;

import java.util.Locale;

/** Resolves HTTP base URLs from the framework configuration. */
public final class APIUrlStorage {
  /** No instances. */
  private APIUrlStorage() {
  }

  /**
   * Resolves {@code api.<service>.<env>.url} for a service/environment pair.
   *
   * @param service logical service identifier
   * @param env target environment
   * @return configured base URL
   * @throws java.util.NoSuchElementException when the required property is absent or blank
   */
  public static String apiUrlFor(final Service service, final Env env) {
    return Config.current().property(key("api", service, env, "url"));
  }

  /**
   * Builds a configuration key from normalized components.
   *
   * @param prefix key prefix
   * @param service service identifier
   * @param env environment identifier
   * @param suffix key suffix
   * @return dot-separated configuration key
   */
  private static String key(final String prefix,
                            final Service service,
                            final Object env,
                            final String suffix) {
    return String.join(".", prefix, service.id(), normalize(env), suffix);
  }

  /**
   * Converts enum-like configuration identifiers to locale-stable lower case.
   *
   * @param value value whose {@code toString()} representation forms part of a key
   * @return lower-case key fragment
   */
  private static String normalize(final Object value) {
    return value.toString().toLowerCase(Locale.ROOT);
  }
}
