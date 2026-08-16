package org.kulivatsky.autotests.demo.api;

import io.restassured.response.Response;
import org.kulivatsky.autotests.core.step.SupplierStep;

import java.io.File;

import static org.kulivatsky.autotests.core.step.Params.params;

/** Named lazy steps that wrap the raw demo API request layer. */
public final class DemoApiSteps {
  /** No instances. */
  private DemoApiSteps() {
  }

  /**
   * Creates a lazy step for fetching an order.
   *
   * @param baseUrl demo server base URL
   * @param orderId order identifier included in Allure parameters
   * @return step producing the raw response
   */
  public static SupplierStep<Response> getOrder(final String baseUrl, final long orderId) {
    return new SupplierStep.Of<>(
      "Get order by id",
      params("orderId", orderId),
      () -> DemoApiRequests.getOrder(baseUrl, orderId)
    );
  }

  /**
   * Creates a lazy step for order creation.
   *
   * @param baseUrl demo server base URL
   * @param product product name included in Allure parameters
   * @param quantity requested quantity included in Allure parameters
   * @return step producing the raw response
   */
  public static SupplierStep<Response> createOrder(final String baseUrl, final String product, final int quantity) {
    return new SupplierStep.Of<>(
      "Create order",
      params("product", product, "quantity", quantity),
      () -> DemoApiRequests.createOrder(baseUrl, product, quantity)
    );
  }

  /**
   * Creates a lazy multipart-upload step.
   *
   * @param baseUrl demo server base URL
   * @param file file to upload; only its filename is exposed as an Allure parameter
   * @return step producing the raw response
   */
  public static SupplierStep<Response> upload(final String baseUrl, final File file) {
    return new SupplierStep.Of<>(
      "Upload file",
      params("fileName", file.getName()),
      () -> DemoApiRequests.upload(baseUrl, file)
    );
  }
}
