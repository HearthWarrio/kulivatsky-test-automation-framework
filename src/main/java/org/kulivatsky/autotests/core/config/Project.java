package org.kulivatsky.autotests.core.config;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Identifies the product or test project to which a test belongs.
 *
 * <p>No project names are compiled into the framework. Consumers may implement this interface in
 * their own enum or create an identifier with {@link #of(String)}.</p>
 */
@FunctionalInterface
public interface Project {
  /**
   * Returns the configuration-safe project identifier.
   *
   * @return lower-case identifier containing letters, digits, or {@code _}
   */
  String id();

  /**
   * Reads projects selected for the current launch.
   *
   * @return configured projects; an empty list means no project filter
   */
  static List<Project> current() {
    return Config.current().project();
  }

  /**
   * Creates a project identifier without requiring a framework-specific enum.
   *
   * @param id project id, for example {@code storefront}
   * @return logical identifier whose normalized value is available through {@link #id()}
   * @throws IllegalArgumentException when the id is blank or contains characters unsuitable for configuration keys
   */
  static Project of(final String id) {
    final String normalized = normalize(id);
    return () -> normalized;
  }

  /**
   * Validates and normalizes a project identifier.
   *
   * @param id input identifier
   * @return normalized lower-case identifier
   */
  private static String normalize(final String id) {
    final String normalized = Objects.requireNonNull(id, "project id")
      .trim()
      .toLowerCase(Locale.ROOT);
    if (normalized.isEmpty() || !normalized.matches("[a-z0-9_]+")) {
      throw new IllegalArgumentException("project id must match [a-z0-9_]+: " + id);
    }
    return normalized;
  }
}
