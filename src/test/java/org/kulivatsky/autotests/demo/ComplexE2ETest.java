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

/**
 * End-to-end API example covering command creation, asynchronous state change, subsequent query,
 * classpath-file materialization, and multipart upload.
 */
public final class ComplexE2ETest {
  /** Local system-under-test stub owned by the current test method. */
  private DemoServer server;

  /** Starts a fresh local demo system before each test method. */
  @BeforeMethod
  public void setUp() {
    this.server = new DemoServer();
  }

  /** Stops all server executors and sockets after each test method. */
  @AfterMethod(alwaysRun = true)
  public void tearDown() {
    if (this.server != null) {
      this.server.close();
    }
  }

  /**
   * Demonstrates a multi-stage flow where the created order becomes complete asynchronously before
   * the test fetches it again and uploads an attachment.
   */
  @Test
  @TestInfo(
    name = "Create an order, await completion and upload an attachment",
    testType = REGRESSION,
    testSubject = E2E,
    testLevel = SYSTEM,
    project = "demo",
    service = "demo_api"
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
