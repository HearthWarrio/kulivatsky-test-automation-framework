package org.kulivatsky.autotests.core.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Read-only access to framework launch configuration.
 *
 * <p>Every lookup uses the same precedence: JVM system property, environment variable, then the
 * classpath {@code autotests.properties} file. Environment-variable names are derived from property
 * keys by inserting an underscore before camel-case capitals, replacing punctuation with
 * underscores, and converting the result to upper case. For example,
 * {@code kafka.orders.local.bootstrapServers} maps to
 * {@code KAFKA_ORDERS_LOCAL_BOOTSTRAP_SERVERS}.</p>
 *
 * <p>The optional classpath file is loaded once, while system properties and environment variables
 * are read for every call. The framework JAR does not require or package a project-specific
 * {@code autotests.properties}; consumers may provide one of their own or configure the process
 * entirely through runtime properties and environment variables.</p>
 */
public final class Config {
  /** Classpath resource that contains framework defaults. */
  private static final String RESOURCE = "autotests.properties";
  /** Finds camel-case boundaries when deriving an environment-variable name. */
  private static final Pattern CAMEL_CASE_BOUNDARY = Pattern.compile("([a-z0-9])([A-Z])");
  /** Process-wide configuration facade backed by the loaded classpath properties. */
  private static final Config CURRENT = new Config(loadProperties());

  /** Defaults loaded from {@link #RESOURCE}. */
  private final Properties properties;

  /**
   * Creates a configuration facade around already loaded defaults.
   *
   * @param properties classpath defaults; external overrides are resolved later per lookup
   */
  private Config(final Properties properties) {
    this.properties = properties;
  }

  /** @return process-wide configuration facade */
  public static Config current() {
    return CURRENT;
  }

  /**
   * Reads {@code autotests.env}.
   *
   * @return configured environment, {@link Env.Option#LOCAL} when absent or blank
   */
  public Env.Option env() {
    return enumValue("autotests.env", Env.Option.class, Env.Option.LOCAL);
  }

  /**
   * Reads {@code autotests.launch}.
   *
   * @return configured launch mode, {@link Launch.Option#LOCAL} when absent or blank
   */
  public Launch.Option launch() {
    return enumValue("autotests.launch", Launch.Option.class, Launch.Option.LOCAL);
  }

  /** @return test types selected by {@code autotests.testType}; empty means no filter */
  public List<TestType> testType() {
    return enumValues("autotests.testType", TestType.Option.class).stream().map(value -> (TestType) value).toList();
  }

  /** @return test subjects selected by {@code autotests.testSubject}; empty means no filter */
  public List<TestSubject> testSubject() {
    return enumValues("autotests.testSubject", TestSubject.Option.class).stream().map(value -> (TestSubject) value).toList();
  }

  /** @return test levels selected by {@code autotests.testLevel}; empty means no filter */
  public List<TestLevel> testLevel() {
    return enumValues("autotests.testLevel", TestLevel.Option.class).stream().map(value -> (TestLevel) value).toList();
  }

  /**
   * Reads comma-separated project ids from {@code autotests.project}.
   *
   * @return normalized project identifiers; empty means no project filter
   */
  public List<Project> project() {
    return stringValues("autotests.project").stream().map(Project::of).toList();
  }

  /**
   * Reads comma-separated service ids from {@code autotests.service}.
   *
   * @return normalized service identifiers; empty means no service filter
   */
  public List<Service> service() {
    return stringValues("autotests.service").stream().map(Service::of).toList();
  }

  /**
   * Reads a mandatory property using system-property, environment-variable, classpath precedence.
   *
   * @param key property key
   * @return trimmed non-blank value
   * @throws NoSuchElementException when no source contains a non-blank value
   */
  public String property(final String key) {
    final String value = resolvedValue(key);
    if (value == null || value.isBlank()) {
      throw new NoSuchElementException("Missing configuration property: " + key);
    }
    return value.trim();
  }

  /**
   * Reads an optional property using system-property, environment-variable, classpath precedence.
   *
   * @param key property key
   * @param defaultValue value returned when every source is absent or blank
   * @return trimmed configured value or {@code defaultValue}
   */
  public String property(final String key, final String defaultValue) {
    final String value = resolvedValue(key);
    return value == null || value.isBlank() ? defaultValue : value.trim();
  }

  /**
   * Reads an integer property and reports the offending key/value when parsing fails.
   *
   * @param key property key
   * @param defaultValue value used when the property is absent or blank
   * @return configured integer or {@code defaultValue}
   * @throws IllegalArgumentException when the configured value is not a base-10 integer
   */
  public int intProperty(final String key, final int defaultValue) {
    final String value = property(key, Integer.toString(defaultValue));
    try {
      return Integer.parseInt(value);
    } catch (final NumberFormatException ex) {
      throw new IllegalArgumentException("Invalid integer value '" + value + "' for " + key, ex);
    }
  }

  /**
   * Reads a strict boolean property.
   *
   * <p>Only {@code true} and {@code false}, ignoring case, are accepted. Values such as
   * {@code tru}, {@code yes}, or {@code 1} fail fast instead of silently becoming false. This is
   * especially important for safety-related switches such as report masking.</p>
   *
   * @param key property key
   * @param defaultValue value used when the property is absent or blank
   * @return configured boolean or {@code defaultValue}
   * @throws IllegalArgumentException when a non-boolean value is configured
   */
  public boolean booleanProperty(final String key, final boolean defaultValue) {
    final String value = property(key, Boolean.toString(defaultValue));
    if ("true".equalsIgnoreCase(value)) {
      return true;
    }
    if ("false".equalsIgnoreCase(value)) {
      return false;
    }
    throw new IllegalArgumentException("Invalid boolean value '" + value + "' for " + key + "; expected true or false");
  }

  /**
   * Converts a property key to the environment-variable name understood by this class.
   *
   * @param key property key
   * @return upper-case environment-variable name
   */
  public static String environmentKey(final String key) {
    final Matcher matcher = CAMEL_CASE_BOUNDARY.matcher(key);
    final String withCamelCaseSeparated = matcher.replaceAll("$1_$2");
    return withCamelCaseSeparated.replaceAll("[^A-Za-z0-9]", "_").toUpperCase(Locale.ROOT);
  }

  /** Resolves a raw value from all supported sources without trimming it. */
  private String resolvedValue(final String key) {
    final String systemValue = System.getProperty(key);
    if (systemValue != null) {
      return systemValue;
    }
    final String environmentValue = System.getenv(environmentKey(key));
    return environmentValue != null ? environmentValue : this.properties.getProperty(key);
  }

  /** Resolves one case-insensitive enum property. */
  private <E extends Enum<E>> E enumValue(final String key, final Class<E> enumType, final E defaultValue) {
    final String value = property(key, defaultValue.name());
    try {
      return Enum.valueOf(enumType, value.toUpperCase(Locale.ROOT));
    } catch (final IllegalArgumentException ex) {
      throw new IllegalArgumentException("Unsupported value '" + value + "' for " + key, ex);
    }
  }

  /** Resolves a comma-separated case-insensitive enum list. */
  private <E extends Enum<E>> List<E> enumValues(final String key, final Class<E> enumType) {
    return stringValues(key).stream()
      .map(value -> {
        try {
          return Enum.valueOf(enumType, value.toUpperCase(Locale.ROOT));
        } catch (final IllegalArgumentException ex) {
          throw new IllegalArgumentException("Unsupported value '" + value + "' for " + key, ex);
        }
      })
      .toList();
  }

  /** Resolves a comma-separated list while removing blank tokens. */
  private List<String> stringValues(final String key) {
    final String configured = property(key, "");
    if (configured.isBlank()) {
      return List.of();
    }
    return Arrays.stream(configured.split(","))
      .map(String::trim)
      .filter(value -> !value.isEmpty())
      .toList();
  }

  /** Loads framework defaults from the classpath. */
  private static Properties loadProperties() {
    final Properties loaded = new Properties();
    try (InputStream stream = Config.class.getClassLoader().getResourceAsStream(RESOURCE)) {
      if (stream != null) {
        loaded.load(stream);
      }
      return loaded;
    } catch (final IOException ex) {
      throw new IllegalStateException("Unable to read configuration resource: " + RESOURCE, ex);
    }
  }
}
