package org.kulivatsky.autotests.core.util;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/** Encodes and decodes UTF-8 text with the standard Base64 alphabet. */
public final class Base64Utils {
  /** No instances. */
  private Base64Utils() {
  }

  /**
   * Encodes UTF-8 text with the standard Base64 alphabet.
   *
   * @param origin text to encode
   * @return Base64 text without line breaks
   */
  public static String encodeToBase64(final String origin) {
    return Base64.getEncoder().encodeToString(origin.getBytes(StandardCharsets.UTF_8));
  }

  /**
   * Decodes standard Base64 bytes as UTF-8 text.
   *
   * @param origin Base64 input
   * @return decoded UTF-8 text
   * @throws IllegalArgumentException when the input is not valid Base64
   */
  public static String decodeFromBase64(final String origin) {
    return new String(Base64.getDecoder().decode(origin), StandardCharsets.UTF_8);
  }

  /**
   * Encodes the conventional {@code login:password} credential form as Base64.
   *
   * <p>This method performs encoding only. Base64 is not encryption and must not be treated as
   * secret storage.</p>
   *
   * @param login login text
   * @param password password text
   * @return Base64 encoding of the UTF-8 {@code login:password} sequence
   */
  public static String encodeCredentialsToBase64(final String login, final String password) {
    return encodeToBase64(login + ":" + password);
  }
}
