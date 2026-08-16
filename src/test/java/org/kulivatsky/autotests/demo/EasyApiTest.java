package org.kulivatsky.autotests.demo;

import org.kulivatsky.autotests.core.annotation.TestInfo;
import org.kulivatsky.autotests.demo.support.DemoServer;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.kulivatsky.autotests.core.config.APIService.Option.DEMO_API;
import static org.kulivatsky.autotests.core.config.Project.Option.DEMO;
import static org.kulivatsky.autotests.core.config.TestLevel.Option.COMPONENT;
import static org.kulivatsky.autotests.core.config.TestSubject.Option.BACKEND;
import static org.kulivatsky.autotests.core.config.TestType.Option.SMOKE;
import static org.kulivatsky.autotests.core.step.ResponseSteps.responseJsonValueIs;
import static org.kulivatsky.autotests.core.step.ResponseSteps.responseMatchesSchema;
import static org.kulivatsky.autotests.core.step.ResponseSteps.responseStatusCodeIs;
import static org.kulivatsky.autotests.core.step.StepContexts.stepContext;
import static org.kulivatsky.autotests.demo.api.DemoApiSteps.getOrder;

/**
 * Small API example showing a minimal test shape: request step, fluent response checks,
 * and launch metadata.
 */
public final class EasyApiTest {
  /** Local HTTP server owned by the current test method. */
  private DemoServer server;

  /** Starts an isolated local demo server before each test method. */
  @BeforeMethod
  public void setUp() {
    this.server = new DemoServer();
  }

  /** Stops the local server even when the test method fails. */
  @AfterMethod(alwaysRun = true)
  public void tearDown() {
    this.server.close();
  }

  /**
   * Demonstrates a read-only API smoke check with status, payload, and schema assertions.
   */
  @Test
  @TestInfo(
    name = "Get an existing order",
    testType = SMOKE,
    testSubject = BACKEND,
    testLevel = COMPONENT,
    project = DEMO,
    apiService = DEMO_API
  )
  public void getExistingOrder() {
    stepContext()
      .with(getOrder(this.server.baseUrl(), 42))
      .exec(responseStatusCodeIs(200))
      .exec(responseJsonValueIs("product", "Notebook"))
      .exec(responseJsonValueIs("status", "READY"))
      .exec(responseMatchesSchema("schema/demo/order-response.schema.json"));
  }
}
