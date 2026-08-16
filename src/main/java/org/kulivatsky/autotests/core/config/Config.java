package org.kulivatsky.autotests.core.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Properties;

/**
 * Read-only access to framework launch configuration.
 *
 * <p>The default values come from {@code autotests.properties} on the classpath. A JVM system
 * property with the same key always wins, which allows CI jobs to override settings without
 * modifying packaged resources.</p>
 *
 * <p>The class is initialized once. Changes to {@link System#getProperties()} made after a value
 * has already been read still affect later calls because each lookup checks system properties at
 * call time; the classpath file itself is loaded only once.</p>
 */
public final class Config {
  /** Classpath resource that contains framework defaults. */
  private static final String RESOURCE = "autotests.properties";
  /** Process-wide configuration facade backed by the loaded classpath properties. */
  private static final Config CURRENT = new Config(loadProperties());

  /** Defaults loaded from {@link #RESOURCE}. */
  private final Properties properties;

  /**
   * Creates a configuration facade around already loaded defaults.
   *
   * @param properties classpath defaults; system properties are resolved later per lookup
   */
  private Config(final Properties properties) {
    this.properties = properties;
  }

  /**
   * Returns the process-wide configuration facade.
   *
   * @return singleton configuration instance
   */
  public static Config current() {
    return CURRENT;
  }

  /**
   * Reads {@code autotests.env}.
   *
   * @return configured environment, {@link Env.Option#LOCAL} when the property is absent or blank
   * @throws IllegalArgumentException if the configured text is not a known environment value
   */
  public Env.Option env() {
    return enumValue("autotests.env", Env.Option.class, Env.Option.LOCAL);
  }

  /**
   * Reads {@code autotests.launch}.
   *
   * @return configured launch mode, {@link Launch.Option#LOCAL} when the property is absent or blank
   * @throws IllegalArgumentException if the configured text is not a known launch value
   */
  public Launch.Option launch() {
    return enumValue("autotests.launch", Launch.Option.class, Launch.Option.LOCAL);
  }

  /**
   * Reads the comma-separated {@code autotests.testType} selection.
   *
   * @return selected test types; empty when no filter is configured
   * @throws IllegalArgumentException if any token is not a known test type
   */
  public List<TestType> testType() {
    return enumValues("autotests.testType", TestType.Option.class).stream().map(value -> (TestType) value).toList();
  }

  /**
   * Reads the comma-separated {@code autotests.testSubject} selection.
   *
   * @return selected test subjects; empty when no filter is configured
   * @throws IllegalArgumentException if any token is not a known subject
   */
  public List<TestSubject> testSubject() {
    return enumValues("autotests.testSubject", TestSubject.Option.class).stream().map(value -> (TestSubject) value).toList();
  }

  /**
   * Reads the comma-separated {@code autotests.testLevel} selection.
   *
   * @return selected test levels; empty when no filter is configured
   * @throws IllegalArgumentException if any token is not a known level
   */
  public List<TestLevel> testLevel() {
    return enumValues("autotests.testLevel", TestLevel.Option.class).stream().map(value -> (TestLevel) value).toList();
  }

  /**
   * Reads the comma-separated {@code autotests.project} selection.
   *
   * @return selected projects; empty when no filter is configured
   * @throws IllegalArgumentException if any token is not a known project
   */
  public List<Project> project() {
    return enumValues("autotests.project", Project.Option.class).stream().map(value -> (Project) value).toList();
  }

  /**
   * Reads the comma-separated {@code autotests.apiService} selection.
   *
   * @return selected services; empty when no filter is configured
   * @throws IllegalArgumentException if any token is not a known service
   */
  public List<APIService> apiService() {
    return enumValues("autotests.apiService", APIService.Option.class).stream().map(value -> (APIService) value).toList();
  }

  /**
   * Reads a mandatory property.
   *
   * <p>A system property overrides the same key from {@code autotests.properties}. Leading and
   * trailing whitespace is removed before the value is returned.</p>
   *
   * @param key property key
   * @return non-blank property value
   * @throws NoSuchElementException if neither source contains a non-blank value
   */
  public String property(final String key) {
    final String value = System.getProperty(key, this.properties.getProperty(key));
    if (value == null || value.isBlank()) {
      throw new NoSuchElementException("Missing configuration property: " + key);
    }
    return value.trim();
  }

  /**
   * Reads an optional property.
   *
   * <p>A system property overrides the classpath value. Blank configured values are treated as
   * absent and therefore produce {@code defaultValue}.</p>
   *
   * @param key property key
   * @param defaultValue value returned when the property is absent or blank
   * @return trimmed configured value or {@code defaultValue}
   */
  public String property(final String key, final String defaultValue) {
    final String value = System.getProperty(key, this.properties.getProperty(key));
    return value == null || value.isBlank() ? defaultValue : value.trim();
  }

  /**
   * Reads an integer property.
   *
   * @param key property key
   * @param defaultValue value used when the property is absent or blank
   * @return parsed integer
   * @throws NumberFormatException if the resolved value is not a valid decimal integer
   */
  public int intProperty(final String key, final int defaultValue) {
    return Integer.parseInt(property(key, Integer.toString(defaultValue)));
  }

  /**
   * Reads a boolean property using {@link Boolean#parseBoolean(String)} semantics.
   *
   * @param key property key
   * @param defaultValue value used when the property is absent or blank
   * @return {@code true} only when the resolved text equals {@code true}, ignoring case
   */
  public boolean booleanProperty(final String key, final boolean defaultValue) {
    return Boolean.parseBoolean(property(key, Boolean.toString(defaultValue)));
  }

  /**
   * Resolves one enum value from a property, ignoring letter case.
   *
   * @param key property key
   * @param enumType enum class used for validation
   * @param defaultValue enum value used when the property is absent or blank
   * @param <E> enum type
   * @return resolved enum value
   * @throws IllegalArgumentException if the configured value does not exist in {@code enumType}
   */
  private <E extends Enum<E>> E enumValue(final String key, final Class<E> enumType, final E defaultValue) {
    final String value = property(key, defaultValue.name());
    try {
      return Enum.valueOf(enumType, value.toUpperCase(Locale.ROOT));
    } catch (final IllegalArgumentException ex) {
      throw new IllegalArgumentException("Unsupported value '" + value + "' for " + key, ex);
    }
  }

  /**
   * Resolves a comma-separated enum list from a property, ignoring letter case and surrounding
   * whitespace around each token.
   *
   * @param key property key
   * @param enumType enum class used for validation
   * @param <E> enum type
   * @return immutable list produced by the stream pipeline; empty when no value is configured
   * @throws IllegalArgumentException if any non-empty token is not declared by {@code enumType}
   */
  private <E extends Enum<E>> List<E> enumValues(final String key, final Class<E> enumType) {
    final String configured = property(key, "");
    if (configured.isBlank()) {
      return List.of();
    }
    return Arrays.stream(configured.split(","))
      .map(String::trim)
      .filter(value -> !value.isEmpty())
      .map(value -> Enum.valueOf(enumType, value.toUpperCase(Locale.ROOT)))
      .toList();
  }

  /**
   * Loads framework defaults from the classpath.
   *
   * @return loaded properties
   * @throws IllegalStateException if the resource is missing or cannot be read
   */
  private static Properties loadProperties() {
    final Properties loaded = new Properties();
    try (InputStream stream = Config.class.getClassLoader().getResourceAsStream(RESOURCE)) {
      if (stream == null) {
        throw new IllegalStateException("Configuration resource not found: " + RESOURCE);
      }
      loaded.load(stream);
      return loaded;
    } catch (final IOException ex) {
      throw new IllegalStateException("Unable to read configuration resource: " + RESOURCE, ex);
    }
  }
}
