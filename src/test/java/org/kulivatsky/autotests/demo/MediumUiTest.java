package org.kulivatsky.autotests.demo;

import com.codeborne.selenide.Configuration;
import org.kulivatsky.autotests.core.annotation.TestInfo;
import org.kulivatsky.autotests.demo.support.DemoServer;
import org.kulivatsky.autotests.demo.web.DemoCatalogPage;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.kulivatsky.autotests.core.config.TestLevel.Option.SYSTEM;
import static org.kulivatsky.autotests.core.config.TestSubject.Option.UI;
import static org.kulivatsky.autotests.core.config.TestType.Option.REGRESSION;
import static org.kulivatsky.autotests.core.step.SelenideSteps.open;
import static org.kulivatsky.autotests.core.step.Steps.step;

/** UI example showing Selenide configuration, framework navigation steps, and a page object. */
public final class MediumUiTest {
  /** Local server that hosts the demo catalog page. */
  private DemoServer server;
  /** Selenide headless setting that existed before this demo method changed global configuration. */
  private boolean previousHeadless;
  /** Selenide browser size that existed before this demo method changed global configuration. */
  private String previousBrowserSize;
  /** Whether the previous global Selenide configuration has been captured for restoration. */
  private boolean configurationCaptured;

  /** Configures a headless browser and starts the local catalog server. */
  @BeforeMethod
  public void setUp() {
    this.previousHeadless = Configuration.headless;
    this.previousBrowserSize = Configuration.browserSize;
    this.configurationCaptured = true;
    Configuration.headless = true;
    Configuration.browserSize = "1440x900";
    this.server = new DemoServer();
  }

  /** Stops the local server and restores the Selenide globals changed by {@link #setUp()}. */
  @AfterMethod(alwaysRun = true)
  public void tearDown() {
    try {
      if (this.server != null) {
        this.server.close();
      }
    } finally {
      if (this.configurationCaptured) {
        Configuration.headless = this.previousHeadless;
        Configuration.browserSize = this.previousBrowserSize;
        this.configurationCaptured = false;
      }
    }
  }

  /** Demonstrates a simple page-object flow that mutates and verifies browser UI state. */
  @Test(groups = "ui")
  @TestInfo(
    name = "Add a catalog product to the cart",
    testType = REGRESSION,
    testSubject = UI,
    testLevel = SYSTEM,
    project = "demo",
    service = "demo_web"
  )
  public void addProductToCart() {
    step(open(this.server.url("/catalog")));
    new DemoCatalogPage()
      .addProductToCart()
      .cartContainsOneItem();
  }
}
