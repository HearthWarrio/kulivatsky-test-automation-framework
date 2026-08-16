package org.kulivatsky.autotests.core.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.io.InputStream;

public final class JsonUtils {
  private static final ObjectMapper MAPPER = new ObjectMapper()
    .registerModule(new JavaTimeModule())
    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

  private JsonUtils() {
  }

  public static ObjectMapper objectMapper() {
    return MAPPER;
  }

  public static JsonNode jsonNode(final String json) {
    try {
      return MAPPER.readTree(json);
    } catch (final JsonProcessingException ex) {
      throw new IllegalArgumentException("Invalid JSON", ex);
    }
  }

  public static String toJson(final Object value) {
    try {
      return MAPPER.writeValueAsString(value);
    } catch (final JsonProcessingException ex) {
      throw new IllegalArgumentException("Unable to serialize value to JSON", ex);
    }
  }

  public static String prettyJson(final Object value) {
    try {
      return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(value);
    } catch (final JsonProcessingException ex) {
      throw new IllegalArgumentException("Unable to serialize value to JSON", ex);
    }
  }

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
