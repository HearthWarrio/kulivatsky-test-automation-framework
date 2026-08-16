package org.kulivatsky.autotests.core.listener;

import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.WebDriverRunner;
import com.codeborne.selenide.logevents.SelenideLogger;
import io.qameta.allure.Allure;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.restassured.AllureRestAssured;
import io.qameta.allure.selenide.AllureSelenide;
import io.restassured.RestAssured;
import org.kulivatsky.autotests.core.annotation.Disabled;
import org.kulivatsky.autotests.core.annotation.TestInfo;
import org.kulivatsky.autotests.core.config.Config;
import org.kulivatsky.autotests.core.storage.DBConnectionPoolStorage;
import org.kulivatsky.autotests.core.storage.RestAssuredConfigStorage;
import org.kulivatsky.autotests.core.storage.SelenideDriverStorage;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.IAnnotationTransformer;
import org.testng.IExecutionListener;
import org.testng.IInvokedMethod;
import org.testng.IInvokedMethodListener;
import org.testng.ITestResult;
import org.testng.annotations.ITestAnnotation;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;

public final class TestNGListener implements IExecutionListener, IInvokedMethodListener, IAnnotationTransformer {
  private static final Logger LOGGER = LoggerFactory.getLogger(TestNGListener.class);
  private static final ThreadLocal<ITestResult> CURRENT_TEST = new ThreadLocal<>();

  public static ITestResult currentTest() {
    final ITestResult result = CURRENT_TEST.get();
    if (result == null) {
      throw new NoSuchElementException("No TestNG test is running on the current thread");
    }
    return result;
  }

  @Override
  public void onExecutionStart() {
    RestAssured.config = RestAssuredConfigStorage.config();
    RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    RestAssured.filters(new AllureRestAssured());
    SelenideLogger.addListener("AllureSelenide", new AllureSelenide().screenshots(false).savePageSource(false));
  }

  @Override
  public void onExecutionFinish() {
    SelenideDriverStorage.closeAll();
    DBConnectionPoolStorage.closeAll();
  }

  @Override
  @SuppressWarnings("rawtypes")
  public void transform(final ITestAnnotation annotation,
                        final Class testClass,
                        final Constructor testConstructor,
                        final Method testMethod) {
    if (testMethod == null) {
      return;
    }
    if (testMethod.isAnnotationPresent(Disabled.class)) {
      annotation.setEnabled(false);
      return;
    }

    final TestInfo info = testMethod.getAnnotation(TestInfo.class);
    if (info == null) {
      return;
    }
    if (!info.name().isBlank()) {
      annotation.setDescription(info.name());
    }

    final Config config = Config.current();
    if (info.env().length > 0 && Arrays.stream(info.env()).noneMatch(config.env()::equals)) {
      annotation.setEnabled(false);
      return;
    }
    if (info.launch().length > 0 && Arrays.stream(info.launch()).noneMatch(config.launch()::equals)) {
      annotation.setEnabled(false);
      return;
    }
    if (!matchesSelection(info.testType(), config.testType())
      || !matchesSelection(info.testSubject(), config.testSubject())
      || !matchesSelection(info.testLevel(), config.testLevel())
      || !matchesSelection(info.project(), config.project())
      || !matchesSelection(info.apiService(), config.apiService())) {
      annotation.setEnabled(false);
    }
  }

  @Override
  public void beforeInvocation(final IInvokedMethod method, final ITestResult testResult) {
    if (!testResult.getMethod().isTest()) {
      return;
    }
    CURRENT_TEST.set(testResult);
    final TestInfo info = method.getTestMethod().getConstructorOrMethod().getMethod().getAnnotation(TestInfo.class);
    LOGGER.info("Running test: {}", info != null && !info.name().isBlank() ? info.name() : testResult.getName());
  }

  @Override
  public void afterInvocation(final IInvokedMethod method, final ITestResult testResult) {
    if (!testResult.getMethod().isTest()) {
      return;
    }
    try {
      if (testResult.getStatus() == ITestResult.FAILURE) {
        attachBrowserState();
        final var storedDriver = SelenideDriverStorage.get(testResult.id());
        if (storedDriver != null && storedDriver.hasWebDriverStarted()) {
          attachBrowserState(storedDriver.getWebDriver());
        }
      }
      SelenideDriverStorage.close(testResult.id());
      if (WebDriverRunner.hasWebDriverStarted()) {
        Selenide.closeWebDriver();
      }
    } finally {
      CURRENT_TEST.remove();
    }
  }

  private static void attachBrowserState() {
    if (WebDriverRunner.hasWebDriverStarted()) {
      attachBrowserState(WebDriverRunner.getWebDriver());
    }
  }

  private static void attachBrowserState(final WebDriver driver) {
    final AllureLifecycle lifecycle = Allure.getLifecycle();
    try {
      if (driver instanceof TakesScreenshot screenshotDriver) {
        lifecycle.addAttachment("Screenshot", "image/png", "png", screenshotDriver.getScreenshotAs(OutputType.BYTES));
      }
      lifecycle.addAttachment("Page source", "text/html", "html", driver.getPageSource().getBytes(StandardCharsets.UTF_8));
      lifecycle.addAttachment("Current URL", "text/plain", "txt", driver.getCurrentUrl().getBytes(StandardCharsets.UTF_8));
    } catch (final RuntimeException ex) {
      LOGGER.warn("Unable to attach browser diagnostics", ex);
    }
  }

  private static <T> boolean matchesSelection(final T[] allowedByTest, final List<? super T> selectedByLaunch) {
    if (allowedByTest.length == 0 || selectedByLaunch.isEmpty()) {
      return true;
    }
    return Arrays.stream(allowedByTest).anyMatch(selectedByLaunch::contains);
  }
}
