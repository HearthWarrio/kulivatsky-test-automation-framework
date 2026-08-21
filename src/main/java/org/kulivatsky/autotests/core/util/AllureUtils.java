package org.kulivatsky.autotests.core.util;

import io.qameta.allure.Allure;
import org.kulivatsky.autotests.core.config.Config;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/** Writes bounded, redacted text and raw binary attachments to the active Allure lifecycle. */
public final class AllureUtils {
  /** Replacement used for values recognized as credentials or session material. */
  private static final String MASK = "***";
  /** Extra characters scanned beyond the visible attachment boundary so a secret is not cut mid-token. */
  private static final int REDACTION_OVERLAP = 512;
  /** Header-like secrets such as Authorization or Cookie. */
  private static final Pattern HEADER_SECRET = Pattern.compile(
    "(?i)(authorization|proxy-authorization|cookie|set-cookie|x-api-key)(\\s*[:=]\\s*)([^\\r\\n]+)"
  );
  /** Quoted JSON/property-style secret values, including values that contain spaces. */
  private static final Pattern QUOTED_NAMED_SECRET = Pattern.compile(
    "(?i)(\\\"?(?:password|passwd|pwd|secret|client[_-]?secret|token|access_token|refresh_token|api[_-]?key|authorization|cookie|session[_-]?id)\\\"?\\s*[:=]\\s*)([\\\"'])([^\\r\\n]*?)(\\2|$)"
  );
  /** Unquoted JSON/property-style secret values. */
  private static final Pattern BARE_NAMED_SECRET = Pattern.compile(
    "(?i)(\\b(?:password|passwd|pwd|secret|client[_-]?secret|token|access_token|refresh_token|api[_-]?key|authorization|cookie|session[_-]?id)\\b\\s*[:=]\\s*)([^\\s,};&\"']+)"
  );
  /** Secret-like URL query parameters. */
  private static final Pattern QUERY_SECRET = Pattern.compile(
    "(?i)([?&](?:password|secret|client[_-]?secret|token|access_token|refresh_token|api[_-]?key|session[_-]?id)=)([^&#\\s]+)"
  );
  /** Assertion messages that name a sensitive field before showing expected and actual values. */
  private static final Pattern SENSITIVE_ASSERTION_VALUES = Pattern.compile(
    "(?i)(\\b(?:password|passwd|pwd|secret|client[_-]?secret|token|access_token|refresh_token|api[_-]?key|authorization|cookie|session[_-]?id)\\b[^\\r\\n]{0,160}?\\bexpected\\s*[:=]\\s*)((?:(?:bearer|basic)\\s+)?[^\\s,;]+)([^\\r\\n]{0,96}?\\bactual\\s*[:=]\\s*)((?:(?:bearer|basic)\\s+)?[^\\s,;]+)"
  );
  /** Credentials embedded in URL user-info, for example {@code https://user:password@host}. */
  private static final Pattern URL_USER_INFO_SECRET = Pattern.compile(
    "(?i)(\\b[a-z][a-z0-9+.-]*://)([^\\s/@:]+):([^\\s/@]+)@"
  );
  /** Standalone Authorization-scheme credentials that may appear outside a named header. */
  private static final Pattern AUTH_SCHEME_SECRET = Pattern.compile(
    "(?i)(\\b(?:bearer|basic)\\s+)([^\\s,;]+)"
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
   * <p>When the report-size limit truncates the document, the attachment is deliberately emitted as
   * plain text rather than mislabeled as valid JSON.</p>
   *
   * @param name attachment name shown in Allure
   * @param json JSON text to attach
   */
  public static void attachJson(final String name, final String json) {
    final String source = String.valueOf(json);
    final String safe = safeAttachmentText(source);
    if (requiresTruncation(source)) {
      attachBytes(name, "text/plain", "txt", safe.getBytes(StandardCharsets.UTF_8));
      return;
    }
    attachBytes(name, "application/json", "json", safe.getBytes(StandardCharsets.UTF_8));
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
    final String safe = safeAttachmentText(String.valueOf(content));
    attachBytes(name, contentType, extension, safe.getBytes(StandardCharsets.UTF_8));
  }

  /**
   * Redacts common credential, token, cookie, URL-user-info, and session-id representations.
   *
   * <p>Masking is enabled by default and can be disabled only with
   * {@code report.masking.enabled=false}. The recognizers intentionally cover both named values and
   * common standalone {@code Bearer}/{@code Basic} credentials, but callers should still avoid
   * attaching data that the report does not need.</p>
   *
   * @param text text that may contain secrets
   * @return redacted text, or {@code null} when the input is null
   */
  public static String redact(final String text) {
    if (text == null || !Config.current().booleanProperty("report.masking.enabled", true)) {
      return text;
    }
    String result = HEADER_SECRET.matcher(text).replaceAll("$1$2" + MASK);
    result = SENSITIVE_ASSERTION_VALUES.matcher(result).replaceAll("$1" + MASK + "$3" + MASK);
    result = QUOTED_NAMED_SECRET.matcher(result).replaceAll("$1$2" + MASK + "$4");
    result = BARE_NAMED_SECRET.matcher(result).replaceAll("$1" + MASK);
    result = QUERY_SECRET.matcher(result).replaceAll("$1" + MASK);
    result = URL_USER_INFO_SECRET.matcher(result).replaceAll("$1***:***@");
    return AUTH_SCHEME_SECRET.matcher(result).replaceAll("$1" + MASK);
  }

  /** Returns whether the configured report limit requires the supplied text to be truncated. */
  private static boolean requiresTruncation(final String text) {
    final int limit = Config.current().intProperty("report.attachment.maxChars", 20_000);
    return limit > 0 && text.length() > limit;
  }

  /**
   * Bounds the amount of text sent through regular-expression redaction, then applies the visible limit.
   *
   * <p>When truncation is enabled, a small overlap past the visible boundary is retained during
   * redaction. This lets a secret whose key starts near the boundary be masked before the visible
   * prefix is cut. The complete source string is never scanned merely to produce a small report
   * attachment.</p>
   *
   * @param text text that may contain report-sensitive data
   * @return redacted attachment text, possibly truncated
   */
  private static String safeAttachmentText(final String text) {
    final int limit = Config.current().intProperty("report.attachment.maxChars", 20_000);
    if (limit <= 0 || text.length() <= limit) {
      return redact(text);
    }
    final long requestedScanLength = (long) limit + REDACTION_OVERLAP;
    final int scanLength = (int) Math.min(text.length(), requestedScanLength);
    final String redactedPrefix = redact(text.substring(0, scanLength));
    final String visible = redactedPrefix.length() <= limit ? redactedPrefix : redactedPrefix.substring(0, limit);
    return visible + "\n...[truncated by framework]";
  }
}
