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

/**
 * TestNG integration point for launch filtering, framework lifecycle, and failure diagnostics.
 *
 * <p>The listener is loaded through TestNG's service-provider mechanism from
 * {@code META-INF/services/org.testng.ITestNGListener}. It has three jobs:</p>
 * <ul>
 *   <li>configure REST Assured and Selenide once per TestNG execution;</li>
 *   <li>disable tests whose {@link TestInfo} metadata does not match the current launch;</li>
 *   <li>bind the currently running test to its thread and attach browser evidence on failure.</li>
 * </ul>
 *
 * <p>{@link #currentTest()} is thread-local. Code that calls it outside a running TestNG
 * test method fails immediately instead of accidentally sharing another test's state.</p>
 */
public final class TestNGListener implements IExecutionListener, IInvokedMethodListener, IAnnotationTransformer {
  /** Logger used for lifecycle messages and non-fatal cleanup failures. */
  private static final Logger LOGGER = LoggerFactory.getLogger(TestNGListener.class);
  /** Test result associated with the thread currently executing a TestNG test method. */
  private static final ThreadLocal<ITestResult> CURRENT_TEST = new ThreadLocal<>();

  /**
   * Returns the TestNG result bound to the current thread.
   *
   * @return result of the test method currently being invoked
   * @throws NoSuchElementException when called outside a running test invocation on this thread
   */
  public static ITestResult currentTest() {
    final ITestResult result = CURRENT_TEST.get();
    if (result == null) {
      throw new NoSuchElementException("No TestNG test is running on the current thread");
    }
    return result;
  }

  /**
   * Applies process-wide HTTP reporting and Selenide reporting defaults before tests start.
   *
   * <p>REST Assured receives the framework timeout/SSL configuration and an Allure filter.
   * Selenide keeps its normal screenshots/page-source behavior disabled in the Allure listener
   * because this class attaches those artifacts explicitly only when a test fails.</p>
   */
  @Override
  public void onExecutionStart() {
    RestAssured.config = RestAssuredConfigStorage.config();
    RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    RestAssured.filters(new AllureRestAssured());
    SelenideLogger.addListener("AllureSelenide", new AllureSelenide().screenshots(false).savePageSource(false));
  }

  /**
   * Closes framework-managed browser drivers and database pools after the TestNG run.
   *
   * <p>Both storages also have JVM shutdown hooks; calling them here releases resources promptly
   * during a normal test run instead of waiting for process termination.</p>
   */
  @Override
  public void onExecutionFinish() {
    SelenideDriverStorage.closeAll();
    DBConnectionPoolStorage.closeAll();
  }

  /**
   * Applies {@link Disabled} and {@link TestInfo} metadata to TestNG before method execution.
   *
   * <p>A non-empty annotation selection acts as an allow-list for that dimension. If the launch
   * selection is empty, the dimension is not filtered. The first mismatch disables the method.</p>
   *
   * @param annotation mutable TestNG annotation being transformed
   * @param testClass declaring class supplied by TestNG; may be {@code null}
   * @param testConstructor constructor supplied by TestNG; may be {@code null}
   * @param testMethod test method supplied by TestNG; may be {@code null}
   */
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

  /**
   * Binds a test result to the current thread immediately before a TestNG test method runs.
   *
   * <p>Configuration methods are ignored. The binding is consumed by per-test storages such as
   * {@link SelenideDriverStorage}.</p>
   *
   * @param method invoked TestNG method
   * @param testResult result object for the invocation
   */
  @Override
  public void beforeInvocation(final IInvokedMethod method, final ITestResult testResult) {
    if (!testResult.getMethod().isTest()) {
      return;
    }
    CURRENT_TEST.set(testResult);
    final TestInfo info = method.getTestMethod().getConstructorOrMethod().getMethod().getAnnotation(TestInfo.class);
    LOGGER.info("Running test: {}", info != null && !info.name().isBlank() ? info.name() : testResult.getName());
  }

  /**
   * Performs failure diagnostics and per-test browser cleanup after a TestNG test method.
   *
   * <p>When the test failed, diagnostics are collected from both Selenide's default driver and a
   * driver stored under the test id. Cleanup runs regardless of diagnostic failures, and the
   * thread-local binding is removed in a {@code finally} block to prevent state leaking into the
   * next invocation on a reused worker thread.</p>
   *
   * @param method invoked TestNG method
   * @param testResult completed result object
   */
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

  /** Attaches diagnostics for Selenide's default driver when that driver has been started. */
  private static void attachBrowserState() {
    if (WebDriverRunner.hasWebDriverStarted()) {
      attachBrowserState(WebDriverRunner.getWebDriver());
    }
  }

  /**
   * Adds a screenshot, page source, and current URL to the active Allure test when possible.
   *
   * <p>Diagnostic collection is best-effort: a stale or already closed browser must
   * not hide the original test failure. Any runtime exception is logged and swallowed.</p>
   *
   * @param driver driver from which diagnostics are collected
   */
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

  /**
   * Tests one launch-filter dimension.
   *
   * @param allowedByTest values accepted by the test annotation
   * @param selectedByLaunch values requested for the current launch
   * @param <T> selection value type
   * @return {@code true} when either side is unrestricted, or when at least one value intersects
   */
  private static <T> boolean matchesSelection(final T[] allowedByTest, final List<? super T> selectedByLaunch) {
    if (allowedByTest.length == 0 || selectedByLaunch.isEmpty()) {
      return true;
    }
    return Arrays.stream(allowedByTest).anyMatch(selectedByLaunch::contains);
  }
}
