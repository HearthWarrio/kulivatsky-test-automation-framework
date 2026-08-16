package org.kulivatsky.autotests.core.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

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
   * @param path resource path
   * @return substring from the final dot onward, or {@code .tmp} when no dot exists
   */
  private static String suffix(final String path) {
    final int dot = path.lastIndexOf('.');
    return dot >= 0 ? path.substring(dot) : ".tmp";
  }
}
