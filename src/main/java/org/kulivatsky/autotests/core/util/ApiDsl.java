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
 * Declarative builder for lazy REST Assured request steps.
 */
public final class ApiDsl {
  private final String description;
  private String method = "GET";
  private String path = "";
  private String baseUri;
  private RestAssuredConfig restAssuredConfig;
  private final Map<String, String> headers = new LinkedHashMap<>();
  private final Map<String, Object> queryParams = new LinkedHashMap<>();
  private final Map<String, Object> pathParams = new LinkedHashMap<>();
  private final Map<String, Object> formParams = new LinkedHashMap<>();
  private Object body;
  private ContentType contentType = ContentType.JSON;

  private ApiDsl(final String description) {
    this.description = Objects.requireNonNull(description, "description");
  }

  public static ApiDsl step(final String description) {
    return new ApiDsl(description);
  }

  public ApiDsl get(final String path) { return method("GET", path); }
  public ApiDsl post(final String path) { return method("POST", path); }
  public ApiDsl put(final String path) { return method("PUT", path); }
  public ApiDsl patch(final String path) { return method("PATCH", path); }
  public ApiDsl delete(final String path) { return method("DELETE", path); }
  public ApiDsl options(final String path) { return method("OPTIONS", path); }
  public ApiDsl head(final String path) { return method("HEAD", path); }

  private ApiDsl method(final String method, final String path) {
    this.method = Objects.requireNonNull(method, "method");
    this.path = Objects.requireNonNull(path, "path");
    return this;
  }

  public ApiDsl baseUri(final String baseUri) {
    this.baseUri = Objects.requireNonNull(baseUri, "baseUri");
    return this;
  }

  public ApiDsl config(final RestAssuredConfig config) {
    this.restAssuredConfig = config;
    return this;
  }

  public ApiDsl header(final String key, final String value) {
    this.headers.put(key, value);
    return this;
  }

  public ApiDsl headers(final Map<String, String> value) {
    this.headers.putAll(value);
    return this;
  }

  public ApiDsl query(final String key, final Object value) {
    this.queryParams.put(key, value);
    return this;
  }

  public ApiDsl query(final Map<String, ?> params) {
    this.queryParams.putAll(params);
    return this;
  }

  public ApiDsl pathParam(final String key, final Object value) {
    this.pathParams.put(key, value);
    return this;
  }

  public ApiDsl pathParam(final Map<String, ?> params) {
    this.pathParams.putAll(params);
    return this;
  }

  public ApiDsl formParam(final String key, final Object value) {
    this.formParams.put(key, value);
    return this;
  }

  public ApiDsl body(final Object body) {
    this.body = body;
    return this;
  }

  public ApiDsl contentType(final ContentType contentType) {
    this.contentType = Objects.requireNonNull(contentType, "contentType");
    return this;
  }

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

  public static final class RequestExecutor {
    private RequestExecutor() {
    }

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
