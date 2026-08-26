package org.kulivatsky.autotests.core.listener;

import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.WebDriverRunner;
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
import org.kulivatsky.autotests.core.util.ExceptionUtils;
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
 * <p>Multiple TestNG executions in one JVM share one reference-counted framework lifecycle. The
 * first active execution installs KTAF's REST Assured global configuration, and the last active
 * execution restores it and closes framework-owned database pools. One execution therefore cannot
 * restore or close process-wide state while another KTAF execution is still active.</p>
 */
public final class TestNGListener implements IExecutionListener, IInvokedMethodListener, IAnnotationTransformer {
  /** Logger used for lifecycle messages and non-fatal diagnostic failures. */
  private static final Logger LOGGER = LoggerFactory.getLogger(TestNGListener.class);
  /** Lock protecting process-wide REST Assured and database-pool ownership. */
  private static final Object GLOBAL_STATE_LOCK = new Object();
  /** Test result associated with the thread currently executing a TestNG test method. */
  private static final ThreadLocal<ITestResult> CURRENT_TEST = new ThreadLocal<>();
  /** Number of TestNG executions currently using the shared KTAF lifecycle. */
  private static int activeExecutions;
  /** REST Assured configuration that existed before the first active KTAF execution. */
  private static RestAssuredConfig previousRestAssuredConfig;
  /** Whether the shared REST Assured configuration is currently installed. */
  private static boolean globalStateInstalled;

  /** Whether the default Selenide driver should be closed after each test method. */
  private boolean closeBrowserAfterTest = true;
  /** Guards this listener instance from incrementing/decrementing the global lifecycle twice. */
  private boolean executionRegistered;

  /** Creates a listener with the default per-test browser cleanup policy. */
  public TestNGListener() {
  }

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
   * Registers this TestNG execution with the shared framework lifecycle.
   *
   * <p>Request/response validation-failure logging is disabled by default because it can expose
   * credentials or payload data in CI logs. When explicitly enabled, it is part of the REST Assured
   * configuration installed by {@code RestAssuredConfigStorage}, so the complete previous config is
   * restored when the last KTAF execution finishes.</p>
   */
  @Override
  public void onExecutionStart() {
    this.closeBrowserAfterTest = Config.current().booleanProperty("browser.closeAfterTest", true);
    synchronized (GLOBAL_STATE_LOCK) {
      if (this.executionRegistered) {
        return;
      }
      if (activeExecutions == 0) {
        installGlobalState();
      }
      activeExecutions++;
      this.executionRegistered = true;
    }
  }

  /**
   * Releases this execution's reference to framework-owned process-wide state.
   *
   * <p>Only the last active KTAF execution closes shared database pools and restores REST Assured
   * configuration. Cleanup is best-effort and does not replace
   * test results with ordinary cleanup failures.</p>
   */
  @Override
  public void onExecutionFinish() {
    synchronized (GLOBAL_STATE_LOCK) {
      if (!this.executionRegistered) {
        return;
      }
      this.executionRegistered = false;
      if (activeExecutions > 0) {
        activeExecutions--;
      }
      if (activeExecutions == 0) {
        releaseGlobalState();
      }
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
    final TestInfo info = testMethod.getAnnotation(TestInfo.class);
    final List<String> testProjects = info == null ? List.of() : normalizeIds(info.project());
    final List<String> testServices = info == null ? List.of() : normalizeIds(info.service());
    if (testMethod.isAnnotationPresent(Disabled.class)) {
      annotation.setEnabled(false);
      return;
    }
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
      || !matchesIds(testProjects, config.project().stream().map(Project::id).toList())
      || !matchesIds(testServices, config.service().stream().map(Service::id).toList())) {
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
        cleanup(
          "Unable to close Selenide WebDriver after test " + testResult.getName(),
          Selenide::closeWebDriver
        );
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
    } catch (final Throwable ex) {
      ExceptionUtils.rethrowIfFatal(ex);
      LOGGER.warn("Unable to attach browser diagnostics", ex);
    }
  }

  /** Installs the one process-wide REST Assured state owned by active KTAF executions. */
  private static void installGlobalState() {
    previousRestAssuredConfig = RestAssured.config;
    try {
      RestAssured.config = RestAssuredConfigStorage.config();
      globalStateInstalled = true;
    } catch (final Throwable ex) {
      try {
        RestAssured.config = previousRestAssuredConfig;
      } catch (final Throwable cleanupException) {
        ex.addSuppressed(cleanupException);
      } finally {
        previousRestAssuredConfig = null;
        globalStateInstalled = false;
      }
      ExceptionUtils.sneakyThrow(ex);
    }
  }

  /** Releases process-wide resources after the final active KTAF execution finishes. */
  private static void releaseGlobalState() {
    cleanup("Unable to close KTAF database pools", DBConnectionPoolStorage::closeAll);
    if (globalStateInstalled) {
      cleanup("Unable to restore REST Assured configuration", () -> RestAssured.config = previousRestAssuredConfig);
    }
    previousRestAssuredConfig = null;
    globalStateInstalled = false;
  }

  /** Runs one lifecycle cleanup action without allowing an ordinary failure to abort later cleanup. */
  private static void cleanup(final String message, final Runnable action) {
    try {
      action.run();
    } catch (final Throwable ex) {
      ExceptionUtils.rethrowIfFatal(ex);
      LOGGER.warn(message, ex);
    }
  }

  /** Returns whether generic launch selections intersect or either side is unrestricted. */
  private static <T> boolean matchesSelection(final T[] allowedByTest, final List<? super T> selectedByLaunch) {
    if (allowedByTest.length == 0 || selectedByLaunch.isEmpty()) {
      return true;
    }
    return Arrays.stream(allowedByTest).anyMatch(selectedByLaunch::contains);
  }

  /** Returns whether normalized ids intersect or either side is unrestricted. */
  private static boolean matchesIds(final List<String> allowedByTest, final List<String> selectedByLaunch) {
    return allowedByTest.isEmpty() || selectedByLaunch.isEmpty()
      || allowedByTest.stream().anyMatch(selectedByLaunch::contains);
  }

  /** Validates every project/service id even when no corresponding launch filter is configured. */
  private static List<String> normalizeIds(final String[] values) {
    return Arrays.stream(values).map(TestNGListener::normalizeId).toList();
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
