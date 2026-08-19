package org.kulivatsky.autotests.core.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.io.InputStream;

/** Owns the shared Jackson mapper and the JSON conversions used by framework code. */
public final class JsonUtils {
  /**
   * Process-wide mapper with Java time support and ISO-style date/time serialization rather than
   * numeric timestamps.
   *
   * <p>The instance is private to framework code. {@link #objectMapper()} returns a configured copy
   * so callers cannot mutate JSON behavior for unrelated or parallel tests.</p>
   */
  private static final ObjectMapper MAPPER = new ObjectMapper()
    .registerModule(new JavaTimeModule())
    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

  /** No instances. */
  private JsonUtils() {
  }

  /**
   * Returns an independently configurable copy of the framework Jackson mapper.
   *
   * <p>Changing the returned mapper never changes serialization or parsing performed by this
   * utility class.</p>
   *
   * @return configured mapper copy owned by the caller
   */
  public static ObjectMapper objectMapper() {
    return MAPPER.copy();
  }

  /**
   * Parses JSON text into a Jackson tree.
   *
   * @param json JSON document
   * @return parsed tree
   * @throws IllegalArgumentException when the text is not valid JSON
   */
  public static JsonNode jsonNode(final String json) {
    try {
      return MAPPER.readTree(json);
    } catch (final JsonProcessingException ex) {
      throw new IllegalArgumentException("Invalid JSON", ex);
    }
  }

  /**
   * Serializes an object with the shared mapper.
   *
   * @param value object to serialize
   * @return compact JSON text
   * @throws IllegalArgumentException when Jackson cannot serialize the value
   */
  public static String toJson(final Object value) {
    try {
      return MAPPER.writeValueAsString(value);
    } catch (final JsonProcessingException ex) {
      throw new IllegalArgumentException("Unable to serialize value to JSON", ex);
    }
  }

  /**
   * Serializes an object with Jackson's default pretty printer.
   *
   * @param value object to serialize
   * @return formatted JSON text
   * @throws IllegalArgumentException when Jackson cannot serialize the value
   */
  public static String prettyJson(final Object value) {
    try {
      return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(value);
    } catch (final JsonProcessingException ex) {
      throw new IllegalArgumentException("Unable to serialize value to JSON", ex);
    }
  }

  /**
   * Reads and parses a JSON resource from the classpath.
   *
   * @param resourcePath classpath-relative resource path
   * @return parsed JSON tree
   * @throws IllegalArgumentException when the resource does not exist
   * @throws IllegalStateException when the resource cannot be read or parsed
   */
  public static JsonNode readResource(final String resourcePath) {
    try (InputStream input = JsonUtils.class.getClassLoader().getResourceAsStream(resourcePath)) {
      if (input == null) {
        throw new IllegalArgumentException("Classpath resource not found: " + resourcePath);
      }
      return MAPPER.readTree(input);
    } catch (final IOException ex) {
      throw new IllegalStateException("Unable to read JSON resource: " + resourcePath, ex);
    }
  }
}
