package org.kulivatsky.autotests.core.util;

import com.codeborne.selenide.Configuration;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.stream.Stream;

/** Reads, compares, and locates files used by download and fixture assertions. */
public final class FileUtils {
  /** No instances. */
  private FileUtils() {
  }

  /**
   * Reads a file with the JDK's default UTF-8 behavior of {@link Files#readString(Path)}.
   *
   * @param path filesystem path
   * @return complete file contents
   * @throws IllegalStateException when the file cannot be read
   */
  public static String readFile(final String path) {
    try {
      return Files.readString(Path.of(path));
    } catch (final IOException ex) {
      throw new IllegalStateException("Unable to read file: " + path, ex);
    }
  }

  /**
   * Compares two files byte-for-byte.
   *
   * @param actual actual file
   * @param expected expected file
   * @return {@code true} only when both files have identical bytes and length
   * @throws NullPointerException when either file argument is null
   * @throws IllegalStateException when either file cannot be read
   */
  public static boolean compareFiles(final File actual, final File expected) {
    Objects.requireNonNull(actual, "actual");
    Objects.requireNonNull(expected, "expected");
    try {
      return Files.mismatch(actual.toPath(), expected.toPath()) == -1L;
    } catch (final IOException ex) {
      throw new IllegalStateException("Unable to compare files", ex);
    }
  }

  /**
   * Recursively searches a directory tree for the first regular file with an exact filename.
   *
   * <p>No ordering guarantee is imposed beyond the order produced by {@link Files#find}; if more
   * than one matching file exists, callers must not assume which one is returned.</p>
   *
   * @param rootFolder directory from which recursive search starts
   * @param fileName exact final path component to match
   * @return first matching file
   * @throws IllegalStateException when traversal fails or no file is found
   */
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

  /**
   * Searches Selenide's configured downloads directory for an exact filename.
   *
   * @param fileName filename to find
   * @return first matching downloaded file
   */
  public static File searchFile(final String fileName) {
    return searchFile(Configuration.downloadsFolder, fileName);
  }
}
