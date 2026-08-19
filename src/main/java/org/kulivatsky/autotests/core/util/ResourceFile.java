package org.kulivatsky.autotests.core.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Materializes a classpath resource as a {@link File} for APIs that cannot consume streams.
 *
 * <p>When the resource already uses the {@code file:} protocol, its real filesystem file is
 * returned. When the resource lives inside a JAR or another non-file classpath location, the bytes
 * are copied to a temporary file and that file is scheduled for deletion on JVM exit.</p>
 *
 * <p>This distinction is important for test libraries distributed as JARs: converting a JAR entry
 * URL directly to {@link File} does not work.</p>
 */
public final class ResourceFile {
  /** One materialized temporary file per non-file classpath resource URL. */
  private static final Map<String, File> MATERIALIZED = new ConcurrentHashMap<>();

  /** No instances. */
  private ResourceFile() {
  }

  /**
   * Returns a filesystem file containing a classpath resource.
   *
   * @param resourcePath classpath-relative resource path, without a leading {@code classpath:}
   * @return original file for {@code file:} resources, otherwise a temporary copy
   * @throws IllegalArgumentException when the classpath resource does not exist
   * @throws IllegalStateException when its URI is invalid or a temporary copy cannot be created
   */
  public static File file(final String resourcePath) {
    final URL resource = ResourceFile.class.getClassLoader().getResource(resourcePath);
    if (resource == null) {
      throw new IllegalArgumentException("Classpath resource not found: " + resourcePath);
    }
    if ("file".equalsIgnoreCase(resource.getProtocol())) {
      try {
        return Path.of(resource.toURI()).toFile();
      } catch (final URISyntaxException ex) {
        throw new IllegalStateException("Invalid classpath resource URI: " + resourcePath, ex);
      }
    }

    final String cacheKey = resource.toExternalForm();
    synchronized (MATERIALIZED) {
      final File cached = MATERIALIZED.get(cacheKey);
      if (cached != null && cached.isFile()) {
        return cached;
      }
      final File materialized = materialize(resource, resourcePath);
      MATERIALIZED.put(cacheKey, materialized);
      return materialized;
    }
  }

  /**
   * Copies one non-file classpath resource to a temporary file.
   *
   * @param resource resolved classpath URL
   * @param resourcePath original classpath-relative path used for diagnostics and suffix selection
   * @return materialized temporary file
   */
  private static File materialize(final URL resource, final String resourcePath) {
    final String suffix = suffix(resourcePath);
    try (InputStream input = resource.openStream()) {
      final Path temp = Files.createTempFile("autotest-resource-", suffix);
      Files.copy(input, temp, StandardCopyOption.REPLACE_EXISTING);
      temp.toFile().deleteOnExit();
      return temp.toFile();
    } catch (final IOException ex) {
      throw new IllegalStateException("Unable to materialize classpath resource: " + resourcePath, ex);
    }
  }

  /**
   * Preserves the original filename suffix for a temporary materialized resource.
   *
   * <p>Dots in parent directory names are ignored. A path such as
   * {@code schema.v1/order} therefore receives {@code .tmp}, while
   * {@code schema.v1/order.json} receives {@code .json}.</p>
   *
   * @param path resource path
   * @return substring from the filename's final dot onward, or {@code .tmp} when no suffix exists
   */
  private static String suffix(final String path) {
    final int separator = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
    final int dot = path.lastIndexOf('.');
    return dot > separator && dot < path.length() - 1 ? path.substring(dot) : ".tmp";
  }
}
