package org.kulivatsky.autotests.demo;

import io.restassured.response.Response;
import org.kulivatsky.autotests.core.annotation.TestInfo;
import org.kulivatsky.autotests.core.util.AwaitilityUtils;
import org.kulivatsky.autotests.core.util.ResourceFile;
import org.kulivatsky.autotests.demo.api.DemoApiRequests;
import org.kulivatsky.autotests.demo.support.DemoServer;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.time.Duration;

import static org.kulivatsky.autotests.core.config.APIService.Option.DEMO_API;
import static org.kulivatsky.autotests.core.config.Project.Option.DEMO;
import static org.kulivatsky.autotests.core.config.TestLevel.Option.SYSTEM;
import static org.kulivatsky.autotests.core.config.TestSubject.Option.E2E;
import static org.kulivatsky.autotests.core.config.TestType.Option.REGRESSION;
import static org.kulivatsky.autotests.core.step.ResponseSteps.responseJsonValueIs;
import static org.kulivatsky.autotests.core.step.ResponseSteps.responseMatchesSchema;
import static org.kulivatsky.autotests.core.step.ResponseSteps.responseStatusCodeIs;
import static org.kulivatsky.autotests.core.step.StepContexts.stepContext;
import static org.kulivatsky.autotests.core.step.Steps.step;
import static org.kulivatsky.autotests.demo.api.DemoApiSteps.createOrder;
import static org.kulivatsky.autotests.demo.api.DemoApiSteps.getOrder;
import static org.kulivatsky.autotests.demo.api.DemoApiSteps.upload;

public final class ComplexE2ETest {
  private DemoServer server;

  @BeforeMethod
  public void setUp() {
    this.server = new DemoServer();
  }

  @AfterMethod(alwaysRun = true)
  public void tearDown() {
    this.server.close();
  }

  @Test
  @TestInfo(
    name = "Create an order, await completion and upload an attachment",
    testType = REGRESSION,
    testSubject = E2E,
    testLevel = SYSTEM,
    project = DEMO,
    apiService = DEMO_API
  )
  public void createOrderAwaitCompletionAndUploadAttachment() {
    final Response created = stepContext()
      .with(createOrder(this.server.baseUrl(), "Keyboard", 1))
      .exec(responseStatusCodeIs(201))
      .exec(responseJsonValueIs("status", "PENDING"))
      .exec(responseMatchesSchema("schema/demo/order-response.schema.json"))
      .res();

    final long orderId = created.jsonPath().getLong("id");
    AwaitilityUtils.waitForAssertTrue(
      () -> "COMPLETED".equals(DemoApiRequests.getOrder(this.server.baseUrl(), orderId).jsonPath().getString("status")),
      Duration.ofSeconds(3)
    );

    stepContext()
      .with(getOrder(this.server.baseUrl(), orderId))
      .exec(responseStatusCodeIs(200))
      .exec(responseJsonValueIs("status", "COMPLETED"));

    final File attachment = ResourceFile.file("files/sample-upload.txt");
    stepContext()
      .with(upload(this.server.baseUrl(), attachment))
      .exec(responseStatusCodeIs(201))
      .exec(responseJsonValueIs("fileName", "sample-upload.txt"))
      .exec(responseJsonValueIs("status", "STORED"));
  }
}
