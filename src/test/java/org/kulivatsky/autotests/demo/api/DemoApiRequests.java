package org.kulivatsky.autotests.demo.api;

import io.restassured.response.Response;

import java.io.File;
import java.util.Map;

import static io.restassured.RestAssured.given;

/**
 * Raw REST Assured calls for the synthetic demo API.
 *
 * <p>This class contains transport calls only. Report step names and parameters live
 * in {@link DemoApiSteps}, keeping the example distinction between request layer and step layer
 * visible.</p>
 */
public final class DemoApiRequests {
  /** No instances. */
  private DemoApiRequests() {
  }

  /**
   * Fetches one order by id.
   *
   * @param baseUrl demo server base URL
   * @param orderId order identifier
   * @return raw REST Assured response; no assertion is performed here
   */
  public static Response getOrder(final String baseUrl, final long orderId) {
    return given()
      .baseUri(baseUrl)
      .when()
      .get("/api/orders/{orderId}", orderId);
  }

  /**
   * Creates an order from a small JSON body.
   *
   * @param baseUrl demo server base URL
   * @param product product name
   * @param quantity requested quantity
   * @return raw REST Assured response
   */
  public static Response createOrder(final String baseUrl, final String product, final int quantity) {
    return given()
      .baseUri(baseUrl)
      .contentType("application/json")
      .body(Map.of("product", product, "quantity", quantity))
      .when()
      .post("/api/orders");
  }

  /**
   * Uploads a file as multipart form field {@code file}.
   *
   * @param baseUrl demo server base URL
   * @param file file to upload
   * @return raw REST Assured response
   */
  public static Response upload(final String baseUrl, final File file) {
    return given()
      .baseUri(baseUrl)
      .multiPart("file", file)
      .when()
      .post("/api/uploads");
  }
}
