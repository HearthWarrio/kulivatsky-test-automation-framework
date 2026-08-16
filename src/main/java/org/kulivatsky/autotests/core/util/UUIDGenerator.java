package org.kulivatsky.autotests.core.util;

import java.util.UUID;

public final class UUIDGenerator {
  private UUIDGenerator() {
  }

  public static String generateRandomUUIDAsString() {
    return UUID.randomUUID().toString();
  }
}
