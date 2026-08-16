package org.kulivatsky.autotests.core.util;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class Base64Utils {
  private Base64Utils() {
  }

  public static String encodeToBase64(final String origin) {
    return Base64.getEncoder().encodeToString(origin.getBytes(StandardCharsets.UTF_8));
  }

  public static String decodeFromBase64(final String origin) {
    return new String(Base64.getDecoder().decode(origin), StandardCharsets.UTF_8);
  }

  public static String encodeCredentialsToBase64(final String login, final String password) {
    return encodeToBase64(login + ":" + password);
  }
}
