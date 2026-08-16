package org.kulivatsky.autotests.core.storage;

import org.kulivatsky.autotests.core.config.Config;
import org.kulivatsky.autotests.core.config.Env;
import org.kulivatsky.autotests.core.config.Project;

import java.util.Locale;

/** Resolves browser application base URLs from framework configuration. */
public final class WebUrlStorage {
  /** No instances. */
  private WebUrlStorage() {
  }

  /**
   * Resolves {@code web.<project>.<env>.url}.
   *
   * @param project logical project
   * @param env target environment
   * @return configured web base URL
   * @throws java.util.NoSuchElementException when the required property is absent or blank
   */
  public static String webUrlFor(final Project project, final Env env) {
    final String key = "web.%s.%s.url".formatted(normalize(project), normalize(env));
    return Config.current().property(key);
  }

  /**
   * Converts a project/environment identifier to a stable configuration-key fragment.
   *
   * @param value identifier
   * @return lower-case text using {@link Locale#ROOT}
   */
  private static String normalize(final Object value) {
    return value.toString().toLowerCase(Locale.ROOT);
  }
}
