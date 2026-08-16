package org.kulivatsky.autotests.demo;

import com.codeborne.selenide.Configuration;
import org.kulivatsky.autotests.core.annotation.TestInfo;
import org.kulivatsky.autotests.demo.support.DemoServer;
import org.kulivatsky.autotests.demo.web.DemoCatalogPage;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.kulivatsky.autotests.core.config.Project.Option.DEMO;
import static org.kulivatsky.autotests.core.config.TestLevel.Option.SYSTEM;
import static org.kulivatsky.autotests.core.config.TestSubject.Option.UI;
import static org.kulivatsky.autotests.core.config.TestType.Option.REGRESSION;
import static org.kulivatsky.autotests.core.step.SelenideSteps.open;
import static org.kulivatsky.autotests.core.step.Steps.step;

public final class MediumUiTest {
  private DemoServer server;

  @BeforeMethod
  public void setUp() {
    Configuration.headless = true;
    Configuration.browserSize = "1440x900";
    this.server = new DemoServer();
  }

  @AfterMethod(alwaysRun = true)
  public void tearDown() {
    this.server.close();
  }

  @Test(groups = "ui")
  @TestInfo(
    name = "Add a catalog product to the cart",
    testType = REGRESSION,
    testSubject = UI,
    testLevel = SYSTEM,
    project = DEMO
  )
  public void addProductToCart() {
    step(open(this.server.url("/catalog")));
    new DemoCatalogPage()
      .addProductToCart()
      .cartContainsOneItem();
  }
}
