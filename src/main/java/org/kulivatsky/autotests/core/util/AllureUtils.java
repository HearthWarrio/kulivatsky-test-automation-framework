package org.kulivatsky.autotests.core.util;

import io.qameta.allure.Allure;
import org.kulivatsky.autotests.core.config.Config;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/** Writes bounded, redacted text and raw binary attachments to the active Allure lifecycle. */
public final class AllureUtils {
  /** Replacement used for values recognized as credentials or session material. */
  private static final String MASK = "***";
  /** Header-like secrets such as Authorization or Cookie. */
  private static final Pattern HEADER_SECRET = Pattern.compile(
    "(?i)(authorization|proxy-authorization|cookie|set-cookie|x-api-key)(\\s*[:=]\\s*)([^\\r\\n]+)"
  );
  /** Quoted JSON/property-style secret values, including values that contain spaces. */
  private static final Pattern QUOTED_NAMED_SECRET = Pattern.compile(
    "(?i)(\\\"?(?:password|passwd|pwd|secret|token|access_token|refresh_token|api[_-]?key|authorization|cookie|session[_-]?id)\\\"?\\s*[:=]\\s*)([\\\"'])([^\\r\\n]*?)\\2"
  );
  /** Unquoted JSON/property-style secret values. */
  private static final Pattern BARE_NAMED_SECRET = Pattern.compile(
    "(?i)(\\b(?:password|passwd|pwd|secret|token|access_token|refresh_token|api[_-]?key|authorization|cookie|session[_-]?id)\\b\\s*[:=]\\s*)([^\\s,};&\"']+)"
  );
  /** Secret-like URL query parameters. */
  private static final Pattern QUERY_SECRET = Pattern.compile(
    "(?i)([?&](?:password|secret|token|access_token|refresh_token|api[_-]?key|session[_-]?id)=)([^&#\\s]+)"
  );

  /** No instances. */
  private AllureUtils() {
  }

  /**
   * Attaches an object's string representation as redacted UTF-8 plain text.
   *
   * @param name attachment name shown in Allure
   * @param value value converted with {@link String#valueOf(Object)}
   */
  public static void attachText(final String name, final Object value) {
    attachUtf8(name, "text/plain", "txt", String.valueOf(value));
  }

  /**
   * Attaches already serialized JSON after redaction and size limiting.
   *
   * @param name attachment name shown in Allure
   * @param json JSON text to attach
   */
  public static void attachJson(final String name, final String json) {
    attachUtf8(name, "application/json", "json", json);
  }

  /**
   * Adds raw bytes without text redaction or truncation.
   *
   * <p>Use this method only for genuinely binary artifacts such as screenshots. Textual content
   * should go through {@link #attachUtf8(String, String, String, String)} so credentials are not
   * copied into the report by accident.</p>
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
   * Redacts and bounds text before attaching it as UTF-8.
   *
   * <p>The maximum character count is controlled by {@code report.attachment.maxChars} and defaults
   * to 20,000. A non-positive configured value disables truncation but does not disable masking.</p>
   *
   * @param name attachment name
   * @param contentType MIME type reported to Allure
   * @param extension file extension without a leading dot
   * @param content text to encode and attach; {@code null} is attached as the literal text {@code "null"}
   */
  public static void attachUtf8(final String name,
                                final String contentType,
                                final String extension,
                                final String content) {
    final String safe = limit(redact(String.valueOf(content)));
    attachBytes(name, contentType, extension, safe.getBytes(StandardCharsets.UTF_8));
  }

  /**
   * Redacts common credential, token, cookie, and session-id representations from arbitrary text.
   *
   * <p>Masking is enabled by default and can be disabled only with
   * {@code report.masking.enabled=false}. The method is intentionally conservative: it recognizes
   * common header, property/JSON, and URL-query forms, but callers should still avoid attaching
   * unnecessary payloads.</p>
   *
   * @param text text that may contain secrets
   * @return redacted text, or {@code null} when the input is null
   */
  public static String redact(final String text) {
    if (text == null || !Config.current().booleanProperty("report.masking.enabled", true)) {
      return text;
    }
    String result = HEADER_SECRET.matcher(text).replaceAll("$1$2" + MASK);
    result = QUOTED_NAMED_SECRET.matcher(result).replaceAll("$1$2" + MASK + "$2");
    result = BARE_NAMED_SECRET.matcher(result).replaceAll("$1" + MASK);
    return QUERY_SECRET.matcher(result).replaceAll("$1" + MASK);
  }

  /** Applies the configured character limit to a text attachment. */
  private static String limit(final String text) {
    final int limit = Config.current().intProperty("report.attachment.maxChars", 20_000);
    if (text == null || limit <= 0 || text.length() <= limit) {
      return text;
    }
    return text.substring(0, limit) + "\n...[truncated by framework]";
  }
}
