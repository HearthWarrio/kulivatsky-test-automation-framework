package org.kulivatsky.autotests.demo.api;

import io.restassured.response.Response;

import java.io.File;
import java.util.Map;

import static io.restassured.RestAssured.given;

public final class DemoApiRequests {
  private DemoApiRequests() {
  }

  public static Response getOrder(final String baseUrl, final long orderId) {
    return given()
      .baseUri(baseUrl)
      .when()
      .get("/api/orders/{orderId}", orderId);
  }

  public static Response createOrder(final String baseUrl, final String product, final int quantity) {
    return given()
      .baseUri(baseUrl)
      .contentType("application/json")
      .body(Map.of("product", product, "quantity", quantity))
      .when()
      .post("/api/orders");
  }

  public static Response upload(final String baseUrl, final File file) {
    return given()
      .baseUri(baseUrl)
      .multiPart("file", file)
      .when()
      .post("/api/uploads");
  }
}
