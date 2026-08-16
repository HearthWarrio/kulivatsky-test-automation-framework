package org.kulivatsky.autotests.core.storage;

import org.kulivatsky.autotests.core.config.Config;
import org.kulivatsky.autotests.core.config.Env;
import org.kulivatsky.autotests.core.config.Project;

import java.util.Locale;

public final class WebUrlStorage {
  private WebUrlStorage() {
  }

  public static String webUrlFor(final Project project, final Env env) {
    final String key = "web.%s.%s.url".formatted(normalize(project), normalize(env));
    return Config.current().property(key);
  }

  private static String normalize(final Object value) {
    return value.toString().toLowerCase(Locale.ROOT);
  }
}
