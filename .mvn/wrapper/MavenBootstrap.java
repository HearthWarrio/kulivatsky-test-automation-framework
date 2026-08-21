import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * JDK-only bootstrap used by {@code mvnw} and {@code mvnw.cmd}.
 *
 * <p>The bootstrap downloads the pinned Maven distribution, verifies its SHA-512 checksum, extracts
 * it without relying on platform utilities, and serializes the first installation with a file lock.
 * It writes only the resolved Maven home path to standard output so both launchers can execute the
 * installed Maven binary.</p>
 */
public final class MavenBootstrap {
  /** HTTP client that follows Maven Central redirects. */
  private static final HttpClient HTTP = HttpClient.newBuilder()
    .followRedirects(HttpClient.Redirect.ALWAYS)
    .build();

  /** No instances. */
  private MavenBootstrap() {
  }

  /**
   * Installs the configured Maven distribution when necessary and prints its home directory.
   *
   * @param args properties-file path followed by wrapper cache-root path
   * @throws Exception when configuration, download, verification, locking, or extraction fails
   */
  public static void main(final String[] args) throws Exception {
    if (args.length != 2) {
      throw new IllegalArgumentException("Expected: MavenBootstrap <wrapper-properties> <cache-root>");
    }
    final Path propertiesPath = Path.of(args[0]).toAbsolutePath().normalize();
    final Path cacheRoot = Path.of(args[1]).toAbsolutePath().normalize();
    final Properties properties = loadProperties(propertiesPath);
    final String mavenVersion = requiredProperty(properties, "mavenVersion");
    final URI distributionUri = URI.create(requiredProperty(properties, "distributionUrl"));
    final String expectedSha512 = requiredProperty(properties, "distributionSha512Sum").toLowerCase();
    final Path mavenHome = cacheRoot.resolve("apache-maven-" + mavenVersion);

    Files.createDirectories(cacheRoot);
    try (FileChannel channel = FileChannel.open(
      cacheRoot.resolve(".bootstrap.lock"),
      StandardOpenOption.CREATE,
      StandardOpenOption.WRITE
    )) {
      final FileLock lock = channel.lock();
      try {
        if (!isInstalled(mavenHome)) {
          final String archiveName = Path.of(distributionUri.getPath()).getFileName().toString();
          final Path archive = cacheRoot.resolve(archiveName);
          ensureArchive(distributionUri, archive, expectedSha512);
          installArchive(archive, cacheRoot, mavenHome, mavenVersion);
        }
      } finally {
        lock.release();
      }
    }

    System.out.println(mavenHome);
  }

  /** Loads wrapper properties as UTF-8 so line endings and platform defaults cannot corrupt values. */
  private static Properties loadProperties(final Path path) throws IOException {
    final Properties properties = new Properties();
    try (var reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
      properties.load(reader);
    }
    return properties;
  }

  /** Returns a required non-blank property or fails with the property name. */
  private static String requiredProperty(final Properties properties, final String key) {
    final String value = properties.getProperty(key);
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("Missing Maven wrapper property: " + key);
    }
    return value.trim();
  }

  /** Returns whether a complete Maven launcher is already present. */
  private static boolean isInstalled(final Path mavenHome) {
    return Files.isRegularFile(mavenHome.resolve("bin/mvn"))
      && Files.isRegularFile(mavenHome.resolve("bin/mvn.cmd"));
  }

  /** Reuses a verified cached archive or downloads and atomically publishes a fresh one. */
  private static void ensureArchive(final URI uri,
                                    final Path archive,
                                    final String expectedSha512) throws Exception {
    if (Files.isRegularFile(archive) && expectedSha512.equals(sha512(archive))) {
      return;
    }
    Files.deleteIfExists(archive);
    final Path temporary = Files.createTempFile(archive.getParent(), archive.getFileName().toString(), ".download");
    try {
      final HttpRequest request = HttpRequest.newBuilder(uri).GET().build();
      final HttpResponse<Path> response = HTTP.send(request, HttpResponse.BodyHandlers.ofFile(temporary));
      if (response.statusCode() < 200 || response.statusCode() >= 300) {
        throw new IOException("Maven download failed with HTTP status " + response.statusCode());
      }
      final String actualSha512 = sha512(temporary);
      if (!expectedSha512.equals(actualSha512)) {
        throw new IOException("Maven distribution checksum mismatch");
      }
      move(temporary, archive);
    } finally {
      Files.deleteIfExists(temporary);
    }
  }

  /** Computes a lower-case SHA-512 digest without loading the archive into memory. */
  private static String sha512(final Path file) throws IOException {
    final MessageDigest digest;
    try {
      digest = MessageDigest.getInstance("SHA-512");
    } catch (final NoSuchAlgorithmException ex) {
      throw new IllegalStateException("JDK does not provide SHA-512", ex);
    }
    try (InputStream input = Files.newInputStream(file)) {
      final byte[] buffer = new byte[64 * 1024];
      int read;
      while ((read = input.read(buffer)) >= 0) {
        if (read > 0) {
          digest.update(buffer, 0, read);
        }
      }
    }
    return HexFormat.of().formatHex(digest.digest());
  }

  /** Extracts a verified Maven ZIP to a temporary directory and publishes the expected Maven home. */
  private static void installArchive(final Path archive,
                                     final Path cacheRoot,
                                     final Path mavenHome,
                                     final String mavenVersion) throws IOException {
    final Path extractionRoot = Files.createTempDirectory(cacheRoot, ".maven-extract-");
    try {
      unzip(archive, extractionRoot);
      final Path extractedHome = extractionRoot.resolve("apache-maven-" + mavenVersion);
      if (!isInstalled(extractedHome)) {
        throw new IOException("Maven archive does not contain the expected distribution root");
      }
      deleteTree(mavenHome);
      move(extractedHome, mavenHome);
    } finally {
      deleteTree(extractionRoot);
    }
  }

  /** Extracts ZIP entries while rejecting paths that would escape the target directory. */
  private static void unzip(final Path archive, final Path targetRoot) throws IOException {
    try (ZipInputStream input = new ZipInputStream(Files.newInputStream(archive))) {
      ZipEntry entry;
      while ((entry = input.getNextEntry()) != null) {
        final Path target = targetRoot.resolve(entry.getName()).normalize();
        if (!target.startsWith(targetRoot)) {
          throw new IOException("Unsafe path in Maven distribution: " + entry.getName());
        }
        if (entry.isDirectory()) {
          Files.createDirectories(target);
        } else {
          Files.createDirectories(target.getParent());
          Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
        }
        input.closeEntry();
      }
    }
  }

  /** Moves a completed file/directory atomically when the filesystem supports it. */
  private static void move(final Path source, final Path target) throws IOException {
    try {
      Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    } catch (final AtomicMoveNotSupportedException ex) {
      Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
    }
  }

  /** Deletes a path tree when it exists; used only for wrapper-owned cache paths. */
  private static void deleteTree(final Path root) throws IOException {
    if (!Files.exists(root)) {
      return;
    }
    Files.walkFileTree(root, new SimpleFileVisitor<>() {
      /** Deletes each file as it is visited. */
      @Override
      public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
        Files.deleteIfExists(file);
        return FileVisitResult.CONTINUE;
      }

      /** Deletes a directory after all of its children have been removed. */
      @Override
      public FileVisitResult postVisitDirectory(final Path dir, final IOException error) throws IOException {
        if (error != null) {
          throw error;
        }
        Files.deleteIfExists(dir);
        return FileVisitResult.CONTINUE;
      }
    });
  }
}
