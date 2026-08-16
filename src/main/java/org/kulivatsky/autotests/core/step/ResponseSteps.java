package org.kulivatsky.autotests.core.step;

import io.restassured.response.Response;

import java.util.Objects;

import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.kulivatsky.autotests.core.step.Params.params;
import static org.kulivatsky.autotests.core.util.AllureUtils.attachText;

/** Lazy assertions and value extraction for REST Assured responses. */
public final class ResponseSteps {
  /** Maximum number of characters attached when a single extracted response value is reported. */
  private static final int ATTACHMENT_PREVIEW_LIMIT = 500;

  /** No instances. */
  private ResponseSteps() {
  }

  /**
   * Creates a step that checks the HTTP status code.
   *
   * @param expectedStatusCode expected status code
   * @return response-consuming assertion step
   */
  public static ConsumerStep<Response> responseStatusCodeIs(final int expectedStatusCode) {
    return new ConsumerStep.Of<>(
      "Response status code is " + expectedStatusCode,
      params("expectedStatusCode", expectedStatusCode),
      response -> response.then().statusCode(expectedStatusCode)
    );
  }

  /**
   * Creates a step that checks one response header for exact string equality.
   *
   * @param header header name
   * @param expectedValue exact expected value
   * @return response-consuming assertion step
   */
  public static ConsumerStep<Response> responseHeaderIs(final String header, final String expectedValue) {
    return new ConsumerStep.Of<>(
      "Response header has expected value",
      params("header", header, "expectedValue", expectedValue),
      response -> response.then().header(header, equalTo(expectedValue))
    );
  }

  /**
   * Creates a step that checks whether the raw response body contains text.
   *
   * @param expectedText text that must occur in the response body
   * @return response-consuming assertion step
   */
  public static ConsumerStep<Response> responseBodyContains(final String expectedText) {
    return new ConsumerStep.Of<>(
      "Response body contains expected text",
      params("expectedText", expectedText),
      response -> response.then().body(containsString(expectedText))
    );
  }

  /**
   * Creates a step that checks a REST Assured JSON-path expression for exact equality.
   *
   * @param jsonPath REST Assured JSON-path expression
   * @param expectedValue expected decoded value
   * @return response-consuming assertion step
   */
  public static ConsumerStep<Response> responseJsonValueIs(final String jsonPath, final Object expectedValue) {
    return new ConsumerStep.Of<>(
      "Response JSON value matches",
      params("jsonPath", jsonPath, "expectedValue", expectedValue),
      response -> response.then().body(jsonPath, equalTo(expectedValue))
    );
  }

  /**
   * Creates a step that validates the response body against a JSON schema on the classpath.
   *
   * @param classpathSchema classpath path understood by REST Assured's schema validator
   * @return response-consuming assertion step
   */
  public static ConsumerStep<Response> responseMatchesSchema(final String classpathSchema) {
    return new ConsumerStep.Of<>(
      "Response matches JSON schema",
      params("schema", classpathSchema),
      response -> response.then().body(matchesJsonSchemaInClasspath(classpathSchema))
    );
  }

  /**
   * Creates a step that reads one JSON-path value as text.
   *
   * <p>The decoded value is converted with {@link Objects#toString(Object, String)}. A null JSON
   * value therefore returns {@code null} and creates no attachment. Non-null text is attached to
   * Allure, truncated to 500 characters to avoid turning a small extraction step into a report
   * payload dump.</p>
   *
   * @param jsonPath REST Assured JSON-path expression
   * @return response-transforming step that produces the extracted text or {@code null}
   */
  public static FunctionStep<Response, String> responseValue(final String jsonPath) {
    return new FunctionStep.Of<>(
      "Read response value",
      params("jsonPath", jsonPath),
      response -> {
        final Object value = response.jsonPath().get(jsonPath);
        final String text = Objects.toString(value, null);
        if (text != null) {
          attachText("Response value", text.length() <= ATTACHMENT_PREVIEW_LIMIT
            ? text
            : text.substring(0, ATTACHMENT_PREVIEW_LIMIT) + "…");
        }
        return text;
      }
    );
  }
}
