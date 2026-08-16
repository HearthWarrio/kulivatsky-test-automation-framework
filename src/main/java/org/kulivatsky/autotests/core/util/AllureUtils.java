package org.kulivatsky.autotests.core.util;

import io.qameta.allure.Allure;

import java.nio.charset.StandardCharsets;

/** Writes text, JSON, and raw byte attachments to the active Allure lifecycle. */
public final class AllureUtils {
  /** No instances. */
  private AllureUtils() {
  }

  /**
   * Attaches an object's string representation as UTF-8 plain text.
   *
   * @param name attachment name shown in Allure
   * @param value value converted with {@link String#valueOf(Object)}; null becomes {@code "null"}
   */
  public static void attachText(final String name, final Object value) {
    attachBytes(name, "text/plain", "txt", String.valueOf(value).getBytes(StandardCharsets.UTF_8));
  }

  /**
   * Attaches an already serialized JSON document as UTF-8.
   *
   * <p>No parsing or validation is performed here.</p>
   *
   * @param name attachment name shown in Allure
   * @param json JSON text to attach
   */
  public static void attachJson(final String name, final String json) {
    attachBytes(name, "application/json", "json", json.getBytes(StandardCharsets.UTF_8));
  }

  /**
   * Adds raw bytes to the active Allure lifecycle.
   *
   * @param name attachment name
   * @param contentType MIME type reported to Allure
   * @param extension file extension without a leading dot
   * @param content attachment bytes
   */
  public static void attachBytes(final String name,
                                 final String contentType,
                                 final String extension,
                                 final byte[] content) {
    Allure.getLifecycle().addAttachment(name, contentType, extension, content);
  }

  /**
   * Encodes text as UTF-8 and attaches it with caller-supplied metadata.
   *
   * @param name attachment name
   * @param contentType MIME type reported to Allure
   * @param extension file extension without a leading dot
   * @param content text to encode and attach
   */
  public static void attachUtf8(final String name,
                                final String contentType,
                                final String extension,
                                final String content) {
    attachBytes(name, contentType, extension, content.getBytes(StandardCharsets.UTF_8));
  }
}
