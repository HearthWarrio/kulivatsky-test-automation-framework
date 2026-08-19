package org.kulivatsky.autotests.core.listener;

import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.WebDriverRunner;
import com.codeborne.selenide.logevents.SelenideLogger;
import io.qameta.allure.selenide.AllureSelenide;
import io.restassured.RestAssured;
import io.restassured.config.RestAssuredConfig;
import org.kulivatsky.autotests.core.annotation.Disabled;
import org.kulivatsky.autotests.core.annotation.TestInfo;
import org.kulivatsky.autotests.core.config.Config;
import org.kulivatsky.autotests.core.config.Project;
import org.kulivatsky.autotests.core.config.Service;
import org.kulivatsky.autotests.core.storage.DBConnectionPoolStorage;
import org.kulivatsky.autotests.core.storage.RestAssuredConfigStorage;
import org.kulivatsky.autotests.core.util.AllureUtils;
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
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Opt-in TestNG integration for launch filtering, framework lifecycle, and browser diagnostics.
 *
 * <p>The framework does not register this listener through {@code META-INF/services}. A consumer
 * must enable it explicitly in the TestNG runner configuration, for example through suite XML or
 * Maven Surefire's {@code listener} provider property. Do not register it with TestNG's
 * {@code @Listeners} annotation: {@link IAnnotationTransformer} implementations must be known before
 * TestNG processes test annotations. Merely adding the framework JAR to a classpath therefore does
 * not mutate REST Assured or Selenide global state.</p>
 *
 * <p>Raw REST request/response bodies are not attached automatically. This is deliberate: generic
 * transport filters cannot know which customer fields are safe to publish in a report. Framework
 * steps and explicit attachments remain available and are masked by the reporting layer.</p>
 */
public final class TestNGListener implements IExecutionListener, IInvokedMethodListener, IAnnotationTransformer {
  /** Logger used for lifecycle messages and non-fatal diagnostic failures. */
  private static final Logger LOGGER = LoggerFactory.getLogger(TestNGListener.class);
  /** Unique Selenide listener key owned by this listener instance. */
  private final String selenideListenerName = TestNGListener.class.getName() + ".allure." + UUID.randomUUID();
  /** Test result associated with the thread currently executing a TestNG test method. */
  private static final ThreadLocal<ITestResult> CURRENT_TEST = new ThreadLocal<>();

  /** REST Assured configuration that was active before this listener changed global defaults. */
  private RestAssuredConfig previousRestAssuredConfig;
  /** Whether this listener has taken ownership of the current REST Assured configuration. */
  private boolean restAssuredConfigured;
  /** Whether the default Selenide driver should be closed after each test method. */
  private boolean closeBrowserAfterTest = true;

  /**
   * Returns the TestNG result bound to the current thread.
   *
   * @return result of the currently executing test method
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
   * Applies framework REST Assured defaults and registers Selenide's Allure event listener.
   *
   * <p>Request/response console logging on validation failure is disabled by default because it can
   * expose credentials or payload data in CI logs. Set {@code rest.logOnValidationFailure=true}
   * only in an environment where such output is acceptable.</p>
   */
  @Override
  public void onExecutionStart() {
    final Config config = Config.current();
    final boolean logOnValidationFailure = config.booleanProperty("rest.logOnValidationFailure", false);
    this.closeBrowserAfterTest = config.booleanProperty("browser.closeAfterTest", true);
    this.previousRestAssuredConfig = RestAssured.config;
    try {
      RestAssured.config = RestAssuredConfigStorage.config();
      this.restAssuredConfigured = true;
      if (logOnValidationFailure) {
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
      }
      SelenideLogger.addListener(
        this.selenideListenerName,
        new AllureSelenide().screenshots(false).savePageSource(false)
      );
    } catch (final RuntimeException ex) {
      try {
        SelenideLogger.removeListener(this.selenideListenerName);
      } catch (final RuntimeException cleanupException) {
        ex.addSuppressed(cleanupException);
      }
      restoreRestAssuredConfig();
      throw ex;
    }
  }

  /**
   * Releases framework-owned resources and restores REST Assured global state after the execution.
   *
   * <p>Cleanup is best-effort. Failure to remove a report listener or restore a global default is
   * logged rather than allowed to replace the test execution result.</p>
   */
  @Override
  public void onExecutionFinish() {
    try {
      DBConnectionPoolStorage.closeAll();
    } catch (final RuntimeException ex) {
      LOGGER.warn("Unable to close KTAF database pools", ex);
    }
    try {
      SelenideLogger.removeListener(this.selenideListenerName);
    } catch (final RuntimeException ex) {
      LOGGER.warn("Unable to remove KTAF Selenide listener", ex);
    } finally {
      restoreRestAssuredConfig();
    }
  }

  /**
   * Applies {@link Disabled} and {@link TestInfo} filters before a test method is scheduled.
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
      || !matchesIds(info.project(), config.project().stream().map(Project::id).toList())
      || !matchesIds(info.service(), config.service().stream().map(Service::id).toList())) {
      annotation.setEnabled(false);
    }
  }

  /** Binds the current test result to its worker thread before test execution. */
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
   * Attaches browser diagnostics after failures and optionally closes Selenide's default driver.
   *
   * <p>The thread-local binding is always cleared, including when diagnostics or browser shutdown
   * fail. Diagnostic failures are logged and never replace the original test result. Browser
   * shutdown is controlled by {@code browser.closeAfterTest} and defaults to {@code true}; consumers
   * that deliberately reuse one session across methods may disable it.</p>
   */
  @Override
  public void afterInvocation(final IInvokedMethod method, final ITestResult testResult) {
    if (!testResult.getMethod().isTest()) {
      return;
    }
    try {
      if (testResult.getStatus() == ITestResult.FAILURE) {
        attachBrowserState();
      }
      if (this.closeBrowserAfterTest && WebDriverRunner.hasWebDriverStarted()) {
        try {
          Selenide.closeWebDriver();
        } catch (final RuntimeException ex) {
          LOGGER.warn("Unable to close Selenide WebDriver after test {}", testResult.getName(), ex);
        }
      }
    } finally {
      CURRENT_TEST.remove();
    }
  }

  /**
   * Attaches explicitly enabled diagnostics for Selenide's default driver when one exists.
   *
   * <p>Browser artifacts are disabled by default because screenshots cannot be redacted and page
   * source may contain business data that does not match credential-oriented masking rules.</p>
   */
  private static void attachBrowserState() {
    if (WebDriverRunner.hasWebDriverStarted()) {
      attachBrowserState(WebDriverRunner.getWebDriver());
    }
  }

  /**
   * Adds only the browser diagnostics explicitly enabled by report configuration.
   *
   * <p>{@code report.browser.screenshot.enabled}, {@code report.browser.pageSource.enabled}, and
   * {@code report.browser.url.enabled} all default to {@code false}. A screenshot is raw binary data
   * and therefore cannot pass through the text masker. Page source and URL use normal text redaction
   * when enabled, but redaction cannot classify arbitrary business data, so neither is implicit.</p>
   *
   * @param driver driver from which diagnostics are collected
   */
  private static void attachBrowserState(final WebDriver driver) {
    try {
      final Config config = Config.current();
      if (config.booleanProperty("report.browser.screenshot.enabled", false)
        && driver instanceof TakesScreenshot screenshotDriver) {
        AllureUtils.attachBytes("Screenshot", "image/png", "png", screenshotDriver.getScreenshotAs(OutputType.BYTES));
      }
      if (config.booleanProperty("report.browser.pageSource.enabled", false)) {
        AllureUtils.attachUtf8("Page source", "text/html", "html", driver.getPageSource());
      }
      if (config.booleanProperty("report.browser.url.enabled", false)) {
        AllureUtils.attachText("Current URL", driver.getCurrentUrl());
      }
    } catch (final RuntimeException ex) {
      LOGGER.warn("Unable to attach browser diagnostics", ex);
    }
  }

  /** Restores the REST Assured configuration captured at execution start. */
  private void restoreRestAssuredConfig() {
    if (!this.restAssuredConfigured && this.previousRestAssuredConfig == null) {
      return;
    }
    try {
      RestAssured.config = this.previousRestAssuredConfig;
    } catch (final RuntimeException ex) {
      LOGGER.warn("Unable to restore REST Assured configuration", ex);
    } finally {
      this.previousRestAssuredConfig = null;
      this.restAssuredConfigured = false;
    }
  }

  /** Returns whether generic launch selections intersect or either side is unrestricted. */
  private static <T> boolean matchesSelection(final T[] allowedByTest, final List<? super T> selectedByLaunch) {
    if (allowedByTest.length == 0 || selectedByLaunch.isEmpty()) {
      return true;
    }
    return Arrays.stream(allowedByTest).anyMatch(selectedByLaunch::contains);
  }

  /**
   * Returns whether normalized ids intersect or either side is unrestricted.
   *
   * <p>Annotation values are validated here rather than silently ignored. Project and service ids
   * share the same configuration-safe syntax: lower-case letters, digits, and underscores after
   * normalization.</p>
   */
  private static boolean matchesIds(final String[] allowedByTest, final List<String> selectedByLaunch) {
    if (allowedByTest.length == 0 || selectedByLaunch.isEmpty()) {
      return true;
    }
    return Arrays.stream(allowedByTest)
      .map(TestNGListener::normalizeId)
      .anyMatch(selectedByLaunch::contains);
  }

  /** Validates and normalizes an id declared directly in {@link TestInfo}. */
  private static String normalizeId(final String value) {
    final String normalized = value.trim().toLowerCase(Locale.ROOT);
    if (normalized.isEmpty() || !normalized.matches("[a-z0-9_]+")) {
      throw new IllegalArgumentException("TestInfo project/service id must match [a-z0-9_]+: " + value);
    }
    return normalized;
  }
}
