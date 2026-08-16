package org.kulivatsky.autotests.core.util;

import com.codeborne.selenide.Configuration;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.stream.Stream;

public final class FileUtils {
  private FileUtils() {
  }

  public static String readFile(final String path) {
    try {
      return Files.readString(Path.of(path));
    } catch (final IOException ex) {
      throw new IllegalStateException("Unable to read file: " + path, ex);
    }
  }

  public static boolean compareFiles(final File actual, final File expected) {
    Objects.requireNonNull(actual, "actual");
    Objects.requireNonNull(expected, "expected");
    try {
      return Files.mismatch(actual.toPath(), expected.toPath()) == -1L;
    } catch (final IOException ex) {
      throw new IllegalStateException("Unable to compare files", ex);
    }
  }

  public static File searchFile(final String rootFolder, final String fileName) {
    final Path root = Path.of(rootFolder);
    try (Stream<Path> files = Files.find(root, Integer.MAX_VALUE,
      (path, attributes) -> attributes.isRegularFile() && path.getFileName().toString().equals(fileName))) {
      return files.findFirst()
        .map(Path::toFile)
        .orElseThrow(() -> new FileNotFoundException(fileName + " not found under " + root));
    } catch (final IOException ex) {
      throw new IllegalStateException("Unable to search for file: " + fileName, ex);
    }
  }

  public static File searchFile(final String fileName) {
    return searchFile(Configuration.downloadsFolder, fileName);
  }
}
