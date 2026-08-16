package org.kulivatsky.autotests.core.util;

import java.util.UUID;

/** UUID generation entry point used by test data. */
public final class UUIDGenerator {
  /** No instances. */
  private UUIDGenerator() {
  }

  /**
   * Generates a random version-4 UUID and returns its canonical text form.
   *
   * @return string such as {@code 123e4567-e89b-12d3-a456-426614174000}
   */
  public static String generateRandomUUIDAsString() {
    return UUID.randomUUID().toString();
  }
}
