package org.kulivatsky.autotests.core.step;

import io.restassured.response.Response;

import java.util.Objects;

import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.kulivatsky.autotests.core.step.Params.params;
import static org.kulivatsky.autotests.core.util.AllureUtils.attachText;

public final class ResponseSteps {
  private static final int ATTACHMENT_PREVIEW_LIMIT = 500;

  private ResponseSteps() {
  }

  public static ConsumerStep<Response> responseStatusCodeIs(final int expectedStatusCode) {
    return new ConsumerStep.Of<>(
      "Response status code is " + expectedStatusCode,
      params("expectedStatusCode", expectedStatusCode),
      response -> response.then().statusCode(expectedStatusCode)
    );
  }

  public static ConsumerStep<Response> responseHeaderIs(final String header, final String expectedValue) {
    return new ConsumerStep.Of<>(
      "Response header has expected value",
      params("header", header, "expectedValue", expectedValue),
      response -> response.then().header(header, equalTo(expectedValue))
    );
  }

  public static ConsumerStep<Response> responseBodyContains(final String expectedText) {
    return new ConsumerStep.Of<>(
      "Response body contains expected text",
      params("expectedText", expectedText),
      response -> response.then().body(containsString(expectedText))
    );
  }

  public static ConsumerStep<Response> responseJsonValueIs(final String jsonPath, final Object expectedValue) {
    return new ConsumerStep.Of<>(
      "Response JSON value matches",
      params("jsonPath", jsonPath, "expectedValue", expectedValue),
      response -> response.then().body(jsonPath, equalTo(expectedValue))
    );
  }

  public static ConsumerStep<Response> responseMatchesSchema(final String classpathSchema) {
    return new ConsumerStep.Of<>(
      "Response matches JSON schema",
      params("schema", classpathSchema),
      response -> response.then().body(matchesJsonSchemaInClasspath(classpathSchema))
    );
  }

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
