package org.kulivatsky.autotests.core.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class ResourceFile {
  private ResourceFile() {
  }

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

  private static String suffix(final String path) {
    final int dot = path.lastIndexOf('.');
    return dot >= 0 ? path.substring(dot) : ".tmp";
  }
}
