package org.kulivatsky.autotests.core.util;

import io.qameta.allure.Allure;

import java.nio.charset.StandardCharsets;

public final class AllureUtils {
  private AllureUtils() {
  }

  public static void attachText(final String name, final Object value) {
    attachBytes(name, "text/plain", "txt", String.valueOf(value).getBytes(StandardCharsets.UTF_8));
  }

  public static void attachJson(final String name, final String json) {
    attachBytes(name, "application/json", "json", json.getBytes(StandardCharsets.UTF_8));
  }

  public static void attachBytes(final String name,
                                 final String contentType,
                                 final String extension,
                                 final byte[] content) {
    Allure.getLifecycle().addAttachment(name, contentType, extension, content);
  }

  public static void attachUtf8(final String name,
                                final String contentType,
                                final String extension,
                                final String content) {
    attachBytes(name, contentType, extension, content.getBytes(StandardCharsets.UTF_8));
  }
}
