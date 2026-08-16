package org.kulivatsky.autotests.demo.api;

import io.restassured.response.Response;
import org.kulivatsky.autotests.core.step.SupplierStep;

import java.io.File;

import static org.kulivatsky.autotests.core.step.Params.params;

public final class DemoApiSteps {
  private DemoApiSteps() {
  }

  public static SupplierStep<Response> getOrder(final String baseUrl, final long orderId) {
    return new SupplierStep.Of<>(
      "Get order by id",
      params("orderId", orderId),
      () -> DemoApiRequests.getOrder(baseUrl, orderId)
    );
  }

  public static SupplierStep<Response> createOrder(final String baseUrl, final String product, final int quantity) {
    return new SupplierStep.Of<>(
      "Create order",
      params("product", product, "quantity", quantity),
      () -> DemoApiRequests.createOrder(baseUrl, product, quantity)
    );
  }

  public static SupplierStep<Response> upload(final String baseUrl, final File file) {
    return new SupplierStep.Of<>(
      "Upload file",
      params("fileName", file.getName()),
      () -> DemoApiRequests.upload(baseUrl, file)
    );
  }
}
