package org.kulivatsky.autotests.core.util;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class RandomStringUtils {
  private static final char[] LOWER_CASE_CHARS = "абвгдежзиклмнопрстуфхцчшщэюя".toCharArray();
  private static final char[] UPPER_CASE_CHARS = "АБВГДЕЖЗИКЛМНОПРСТУФХЦЧШЩЭЮЯ".toCharArray();

  private RandomStringUtils() {
  }

  public static String generateRandomCyrillicLowerCaseString(final int size) {
    return random(size, LOWER_CASE_CHARS);
  }

  public static String generateRandomCyrillicUpperCaseString(final int size) {
    return random(size, UPPER_CASE_CHARS);
  }

  public static String randomCyrillicStringWithFirstCapital(final int length) {
    if (length < 1) {
      return "";
    }
    return generateRandomCyrillicUpperCaseString(1)
      + generateRandomCyrillicLowerCaseString(length - 1);
  }

  public static String getUniqueString() {
    return Instant.now().toEpochMilli() + "-" + UUID.randomUUID();
  }

  public static String generateRandomSurname(final int size) {
    requireMinimum(size, 3, "surname");
    return "А" + generateRandomCyrillicLowerCaseString(size - 3) + "ов";
  }

  public static String generateRandomFirstName(final int size) {
    requireMinimum(size, 1, "first name");
    return "А" + generateRandomCyrillicLowerCaseString(size - 1);
  }

  public static String generateRandomMiddleName(final int size) {
    requireMinimum(size, 4, "middle name");
    return "А" + generateRandomCyrillicLowerCaseString(size - 4) + "вич";
  }

  public static String generateRandomPersonName(final int sizeSurname,
                                                final int sizeFirstName,
                                                final int sizeMiddleName) {
    return generateRandomSurname(sizeSurname) + " "
      + generateRandomFirstName(sizeFirstName) + " "
      + generateRandomMiddleName(sizeMiddleName);
  }

  private static String random(final int size, final char[] alphabet) {
    if (size < 0) {
      throw new IllegalArgumentException("size must not be negative");
    }
    final StringBuilder result = new StringBuilder(size);
    final ThreadLocalRandom random = ThreadLocalRandom.current();
    for (int i = 0; i < size; i++) {
      result.append(alphabet[random.nextInt(alphabet.length)]);
    }
    return result.toString();
  }

  private static void requireMinimum(final int size, final int minimum, final String valueName) {
    if (size < minimum) {
      throw new IllegalArgumentException(valueName + " size must be at least " + minimum);
    }
  }
}
