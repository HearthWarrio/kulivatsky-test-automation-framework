package org.kulivatsky.autotests.core.util;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/** Random and unique string generators used for synthetic test data. */
public final class RandomStringUtils {
  /** Cyrillic lowercase alphabet used by the random generators. */
  private static final char[] LOWER_CASE_CHARS = "абвгдежзиклмнопрстуфхцчшщэюя".toCharArray();
  /** Cyrillic uppercase alphabet used by the random generators. */
  private static final char[] UPPER_CASE_CHARS = "АБВГДЕЖЗИКЛМНОПРСТУФХЦЧШЩЭЮЯ".toCharArray();

  /** No instances. */
  private RandomStringUtils() {
  }

  /**
   * Generates a lowercase Cyrillic string of exact length.
   *
   * @param size requested character count
   * @return random string
   * @throws IllegalArgumentException when {@code size} is negative
   */
  public static String generateRandomCyrillicLowerCaseString(final int size) {
    return random(size, LOWER_CASE_CHARS);
  }

  /**
   * Generates an uppercase Cyrillic string of exact length.
   *
   * @param size requested character count
   * @return random string
   * @throws IllegalArgumentException when {@code size} is negative
   */
  public static String generateRandomCyrillicUpperCaseString(final int size) {
    return random(size, UPPER_CASE_CHARS);
  }

  /**
   * Generates a Cyrillic string whose first character is uppercase and the rest lowercase.
   *
   * @param length requested character count
   * @return generated text; empty when {@code length < 1}
   */
  public static String randomCyrillicStringWithFirstCapital(final int length) {
    if (length < 1) {
      return "";
    }
    return generateRandomCyrillicUpperCaseString(1)
      + generateRandomCyrillicLowerCaseString(length - 1);
  }

  /**
   * Builds a collision-resistant test string from current epoch milliseconds and a random UUID.
   *
   * @return value in {@code <epochMillis>-<uuid>} form
   */
  public static String getUniqueString() {
    return Instant.now().toEpochMilli() + "-" + UUID.randomUUID();
  }

  /**
   * Generates a synthetic Cyrillic surname beginning with {@code А} and ending with {@code ов}.
   *
   * @param size exact surname length
   * @return generated surname
   * @throws IllegalArgumentException when {@code size < 3}
   */
  public static String generateRandomSurname(final int size) {
    requireMinimum(size, 3, "surname");
    return "А" + generateRandomCyrillicLowerCaseString(size - 3) + "ов";
  }

  /**
   * Generates a synthetic Cyrillic first name beginning with {@code А}.
   *
   * @param size exact name length
   * @return generated first name
   * @throws IllegalArgumentException when {@code size < 1}
   */
  public static String generateRandomFirstName(final int size) {
    requireMinimum(size, 1, "first name");
    return "А" + generateRandomCyrillicLowerCaseString(size - 1);
  }

  /**
   * Generates a synthetic Cyrillic patronymic beginning with {@code А} and ending with {@code вич}.
   *
   * @param size exact patronymic length
   * @return generated patronymic
   * @throws IllegalArgumentException when {@code size < 4}
   */
  public static String generateRandomMiddleName(final int size) {
    requireMinimum(size, 4, "middle name");
    return "А" + generateRandomCyrillicLowerCaseString(size - 4) + "вич";
  }

  /**
   * Generates a space-separated synthetic surname, first name, and patronymic.
   *
   * @param sizeSurname surname length
   * @param sizeFirstName first-name length
   * @param sizeMiddleName patronymic length
   * @return generated full name
   */
  public static String generateRandomPersonName(final int sizeSurname,
                                                final int sizeFirstName,
                                                final int sizeMiddleName) {
    return generateRandomSurname(sizeSurname) + " "
      + generateRandomFirstName(sizeFirstName) + " "
      + generateRandomMiddleName(sizeMiddleName);
  }

  /**
   * Generates a string by independently sampling characters from an alphabet.
   *
   * @param size requested character count
   * @param alphabet source characters
   * @return generated string
   * @throws IllegalArgumentException when {@code size} is negative
   */
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

  /**
   * Validates a length constraint used by structured name generators.
   *
   * @param size requested size
   * @param minimum minimum legal size
   * @param valueName value description used in the exception message
   * @throws IllegalArgumentException when {@code size < minimum}
   */
  private static void requireMinimum(final int size, final int minimum, final String valueName) {
    if (size < minimum) {
      throw new IllegalArgumentException(valueName + " size must be at least " + minimum);
    }
  }
}
