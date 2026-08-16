package org.kulivatsky.autotests.core.util;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.RestAssuredConfig;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.kulivatsky.autotests.core.step.SupplierStep;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Builder for lazy REST Assured request steps.
 *
 * <p>The builder collects request metadata first and produces a {@link SupplierStep} only when
 * {@link #build()} is called. Building does not perform network I/O. The HTTP request is sent later,
 * when the returned step is executed through the framework step DSL.</p>
 *
 * <p>Each mutator returns the same builder instance. The class is therefore intended for
 * single-threaded request construction; it is not safe to share one instance between parallel
 * tests.</p>
 */
public final class ApiDsl {
  /** Name used for the resulting report step. */
  private final String description;
  /** HTTP method sent by the built request. */
  private String method = "GET";
  /** Request path relative to {@link #baseUri}. */
  private String path = "";
  /** Required REST Assured base URI. */
  private String baseUri;
  /** Optional request-specific REST Assured configuration. */
  private RestAssuredConfig restAssuredConfig;
  /** Headers accumulated in insertion order. */
  private final Map<String, String> headers = new LinkedHashMap<>();
  /** Query parameters accumulated in insertion order. */
  private final Map<String, Object> queryParams = new LinkedHashMap<>();
  /** Path parameters accumulated in insertion order. */
  private final Map<String, Object> pathParams = new LinkedHashMap<>();
  /** Form parameters accumulated in insertion order. */
  private final Map<String, Object> formParams = new LinkedHashMap<>();
  /** Optional request body; null means no body is configured. */
  private Object body;
  /** Content type sent by the request. */
  private ContentType contentType = ContentType.JSON;

  /**
   * Starts a request builder for one named test step.
   *
   * @param description report step name
   */
  private ApiDsl(final String description) {
    this.description = Objects.requireNonNull(description, "description");
  }

  /**
   * Starts a new request-step definition.
   *
   * @param description name that will appear in the step report
   * @return empty request builder using GET and JSON by default
   */
  public static ApiDsl step(final String description) {
    return new ApiDsl(description);
  }

  /**
   * Selects HTTP GET and the request path.
   *
   * @param path path relative to the configured base URI
   * @return this builder
   */
  public ApiDsl get(final String path) { return method("GET", path); }

  /**
   * Selects HTTP POST and the request path.
   *
   * @param path path relative to the configured base URI
   * @return this builder
   */
  public ApiDsl post(final String path) { return method("POST", path); }

  /**
   * Selects HTTP PUT and the request path.
   *
   * @param path path relative to the configured base URI
   * @return this builder
   */
  public ApiDsl put(final String path) { return method("PUT", path); }

  /**
   * Selects HTTP PATCH and the request path.
   *
   * @param path path relative to the configured base URI
   * @return this builder
   */
  public ApiDsl patch(final String path) { return method("PATCH", path); }

  /**
   * Selects HTTP DELETE and the request path.
   *
   * @param path path relative to the configured base URI
   * @return this builder
   */
  public ApiDsl delete(final String path) { return method("DELETE", path); }

  /**
   * Selects HTTP OPTIONS and the request path.
   *
   * @param path path relative to the configured base URI
   * @return this builder
   */
  public ApiDsl options(final String path) { return method("OPTIONS", path); }

  /**
   * Selects HTTP HEAD and the request path.
   *
   * @param path path relative to the configured base URI
   * @return this builder
   */
  public ApiDsl head(final String path) { return method("HEAD", path); }

  /**
   * Replaces the currently selected HTTP method and path.
   *
   * @param method HTTP method text
   * @param path request path
   * @return this builder
   */
  private ApiDsl method(final String method, final String path) {
    this.method = Objects.requireNonNull(method, "method");
    this.path = Objects.requireNonNull(path, "path");
    return this;
  }

  /**
   * Sets the REST Assured base URI.
   *
   * @param baseUri absolute base URI
   * @return this builder
   */
  public ApiDsl baseUri(final String baseUri) {
    this.baseUri = Objects.requireNonNull(baseUri, "baseUri");
    return this;
  }

  /**
   * Sets request-specific REST Assured configuration.
   *
   * @param config configuration applied only to the built request; null restores global/default use
   * @return this builder
   */
  public ApiDsl config(final RestAssuredConfig config) {
    this.restAssuredConfig = config;
    return this;
  }

  /**
   * Adds or replaces one HTTP header.
   *
   * @param key header name
   * @param value header value
   * @return this builder
   */
  public ApiDsl header(final String key, final String value) {
    this.headers.put(key, value);
    return this;
  }

  /**
   * Adds all supplied HTTP headers, replacing existing values with the same key.
   *
   * @param value headers to merge
   * @return this builder
   */
  public ApiDsl headers(final Map<String, String> value) {
    this.headers.putAll(value);
    return this;
  }

  /**
   * Adds or replaces one query parameter.
   *
   * @param key parameter name
   * @param value parameter value passed to REST Assured
   * @return this builder
   */
  public ApiDsl query(final String key, final Object value) {
    this.queryParams.put(key, value);
    return this;
  }

  /**
   * Adds all supplied query parameters.
   *
   * @param params parameters to merge
   * @return this builder
   */
  public ApiDsl query(final Map<String, ?> params) {
    this.queryParams.putAll(params);
    return this;
  }

  /**
   * Adds or replaces one named path parameter.
   *
   * @param key placeholder name
   * @param value placeholder value
   * @return this builder
   */
  public ApiDsl pathParam(final String key, final Object value) {
    this.pathParams.put(key, value);
    return this;
  }

  /**
   * Adds all supplied path parameters.
   *
   * @param params placeholder values to merge
   * @return this builder
   */
  public ApiDsl pathParam(final Map<String, ?> params) {
    this.pathParams.putAll(params);
    return this;
  }

  /**
   * Adds or replaces one form parameter.
   *
   * @param key form field name
   * @param value form field value
   * @return this builder
   */
  public ApiDsl formParam(final String key, final Object value) {
    this.formParams.put(key, value);
    return this;
  }

  /**
   * Sets the request body object passed to REST Assured.
   *
   * @param body body object; null removes the body from the built specification
   * @return this builder
   */
  public ApiDsl body(final Object body) {
    this.body = body;
    return this;
  }

  /**
   * Sets the request content type.
   *
   * @param contentType REST Assured content type
   * @return this builder
   */
  public ApiDsl contentType(final ContentType contentType) {
    this.contentType = Objects.requireNonNull(contentType, "contentType");
    return this;
  }

  /**
   * Freezes the current builder state into a lazy request step.
   *
   * <p>The request specification is built immediately, so headers, parameters, body, base URI, and
   * content type are captured at build time. The actual HTTP call remains lazy. Method, path, and
   * request-specific config are read from this builder when the returned step executes, because the
   * step action retains this builder instance.</p>
   *
   * @return lazy step producing the REST Assured response
   * @throws IllegalStateException when no non-blank base URI has been configured
   */
  public SupplierStep<Response> build() {
    if (this.baseUri == null || this.baseUri.isBlank()) {
      throw new IllegalStateException("Base URI must be configured before building the request step");
    }

    final RequestSpecBuilder builder = new RequestSpecBuilder()
      .setBaseUri(this.baseUri)
      .setAccept(ContentType.JSON)
      .setContentType(this.contentType);

    this.headers.forEach(builder::addHeader);
    this.queryParams.forEach(builder::addQueryParam);
    this.pathParams.forEach(builder::addPathParam);
    this.formParams.forEach(builder::addFormParam);
    if (this.body != null) {
      builder.setBody(this.body);
    }

    final RequestSpecification specification = builder.build();
    return new SupplierStep.Of<>(
      this.description,
      () -> RequestExecutor.execute(specification, this.method, this.path, this.restAssuredConfig)
    );
  }

  /**
   * Performs the final REST Assured method dispatch for requests built by {@link ApiDsl}.
   *
   * <p>The class is public so low-level callers can reuse the exact dispatch semantics without
   * rebuilding the DSL. It owns no resources and stores no state.</p>
   */
  public static final class RequestExecutor {
    /** No instances. */
    private RequestExecutor() {
    }

    /**
     * Executes one REST Assured request using the supplied specification.
     *
     * @param specification complete request specification
     * @param method HTTP method; matching is case-insensitive
     * @param path request path passed to REST Assured
     * @param config optional request-specific config; null leaves REST Assured defaults/global config in effect
     * @return HTTP response
     * @throws IllegalArgumentException when the method is not one of GET, POST, PUT, PATCH, DELETE, OPTIONS, HEAD
     */
    public static Response execute(final RequestSpecification specification,
                                   final String method,
                                   final String path,
                                   final RestAssuredConfig config) {
      final var request = RestAssured.given(specification);
      if (config != null) {
        request.config(config);
      }

      return switch (method.toUpperCase(Locale.ROOT)) {
        case "POST" -> request.post(path);
        case "PUT" -> request.put(path);
        case "PATCH" -> request.patch(path);
        case "DELETE" -> request.delete(path);
        case "OPTIONS" -> request.options(path);
        case "HEAD" -> request.head(path);
        case "GET" -> request.get(path);
        default -> throw new IllegalArgumentException("Unsupported HTTP method: " + method);
      };
    }
  }
}
