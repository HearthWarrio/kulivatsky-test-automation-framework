package org.kulivatsky.autotests.core.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.Cleaner;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Materializes a classpath resource as a {@link File} for APIs that cannot consume streams.
 *
 * <p>When the resource already uses the {@code file:} protocol, its real filesystem file is
 * returned. When the resource lives inside a JAR or another non-file classpath location, each call
 * receives a fresh temporary copy. A caller may therefore modify or delete its copy without
 * corrupting the resource returned to another test. Temporary copies are reclaimed after the
 * returned {@code File} becomes unreachable rather than being retained until JVM exit.</p>
 *
 * <p>This distinction is important for test libraries distributed as JARs: converting a JAR entry
 * URL directly to {@link File} does not work.</p>
 */
public final class ResourceFile {
  /** Deletes temporary materializations after their returned {@link File} objects become unreachable. */
  private static final Cleaner CLEANER = Cleaner.create();

  /** No instances. */
  private ResourceFile() {
  }

  /**
   * Returns a filesystem file containing a classpath resource.
   *
   * <p>The consuming thread's context class loader is checked before the framework class loader.
   * This lets a project keep fixtures in its own test JAR even when KTAF is loaded by a parent or
   * isolated class loader.</p>
   *
   * @param resourcePath classpath-relative resource path, without a leading {@code classpath:}
   * @return original file for {@code file:} resources, otherwise a fresh temporary copy
   * @throws IllegalArgumentException when the classpath resource does not exist
   * @throws IllegalStateException when its URI is invalid or a temporary copy cannot be created
   */
  public static File file(final String resourcePath) {
    final URL resource = resourceUrl(resourcePath);
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
    return materialize(resource, resourcePath);
  }

  /** Resolves a resource from the consumer context first and the framework class loader second. */
  private static URL resourceUrl(final String resourcePath) {
    final ClassLoader contextLoader = Thread.currentThread().getContextClassLoader();
    if (contextLoader != null) {
      final URL resource = contextLoader.getResource(resourcePath);
      if (resource != null) {
        return resource;
      }
    }
    final ClassLoader frameworkLoader = ResourceFile.class.getClassLoader();
    return frameworkLoader == contextLoader ? null : frameworkLoader.getResource(resourcePath);
  }

  /**
   * Copies one non-file classpath resource to a new temporary file.
   *
   * <p>The copy is never reused by a later call. This is intentional because the returned
   * {@link File} is mutable and may be handed to third-party upload APIs that rewrite or delete it.</p>
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
      final File file = temp.toFile();
      CLEANER.register(file, () -> deleteTemporaryFile(temp));
      return file;
    } catch (final IOException ex) {
      throw new IllegalStateException("Unable to materialize classpath resource: " + resourcePath, ex);
    }
  }

  /** Deletes a cleaner-owned temporary file without letting cleanup failure reach application code. */
  private static void deleteTemporaryFile(final Path path) {
    try {
      Files.deleteIfExists(path);
    } catch (final IOException ignored) {
      // Cleaner actions cannot report a useful failure to the test that originally consumed the file.
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
