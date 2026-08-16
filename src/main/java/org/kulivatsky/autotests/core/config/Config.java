package org.kulivatsky.autotests.core.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Properties;

public final class Config {
  private static final String RESOURCE = "autotests.properties";
  private static final Config CURRENT = new Config(loadProperties());

  private final Properties properties;

  private Config(final Properties properties) {
    this.properties = properties;
  }

  public static Config current() {
    return CURRENT;
  }

  public Env.Option env() {
    return enumValue("autotests.env", Env.Option.class, Env.Option.LOCAL);
  }

  public Launch.Option launch() {
    return enumValue("autotests.launch", Launch.Option.class, Launch.Option.LOCAL);
  }

  public List<TestType> testType() {
    return enumValues("autotests.testType", TestType.Option.class).stream().map(value -> (TestType) value).toList();
  }

  public List<TestSubject> testSubject() {
    return enumValues("autotests.testSubject", TestSubject.Option.class).stream().map(value -> (TestSubject) value).toList();
  }

  public List<TestLevel> testLevel() {
    return enumValues("autotests.testLevel", TestLevel.Option.class).stream().map(value -> (TestLevel) value).toList();
  }

  public List<Project> project() {
    return enumValues("autotests.project", Project.Option.class).stream().map(value -> (Project) value).toList();
  }

  public List<APIService> apiService() {
    return enumValues("autotests.apiService", APIService.Option.class).stream().map(value -> (APIService) value).toList();
  }

  public String property(final String key) {
    final String value = System.getProperty(key, this.properties.getProperty(key));
    if (value == null || value.isBlank()) {
      throw new NoSuchElementException("Missing configuration property: " + key);
    }
    return value.trim();
  }

  public String property(final String key, final String defaultValue) {
    final String value = System.getProperty(key, this.properties.getProperty(key));
    return value == null || value.isBlank() ? defaultValue : value.trim();
  }

  public int intProperty(final String key, final int defaultValue) {
    return Integer.parseInt(property(key, Integer.toString(defaultValue)));
  }

  public boolean booleanProperty(final String key, final boolean defaultValue) {
    return Boolean.parseBoolean(property(key, Boolean.toString(defaultValue)));
  }

  private <E extends Enum<E>> E enumValue(final String key, final Class<E> enumType, final E defaultValue) {
    final String value = property(key, defaultValue.name());
    try {
      return Enum.valueOf(enumType, value.toUpperCase(Locale.ROOT));
    } catch (final IllegalArgumentException ex) {
      throw new IllegalArgumentException("Unsupported value '" + value + "' for " + key, ex);
    }
  }

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
