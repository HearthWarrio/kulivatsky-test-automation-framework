package org.kulivatsky.autotests.core.step;

import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.WebDriverRunner;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebDriver;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

import static org.kulivatsky.autotests.core.step.Params.params;
import static org.kulivatsky.autotests.core.step.Params.secret;

/** Builds browser step descriptors and reads state from Selenide's default driver. */
public final class SelenideSteps {
  /** No instances. */
  private SelenideSteps() {
  }

  /** @return step that opens a URL using Selenide's default driver */
  public static RunnableStep open(final String url) {
    return new RunnableStep.Of("Open page", params("url", url), () -> Selenide.open(url));
  }

  /** @return step that closes Selenide's default WebDriver */
  public static RunnableStep close() {
    return new RunnableStep.Of("Close browser", Selenide::closeWebDriver);
  }

  /** @return step that refreshes the current page */
  public static RunnableStep refresh() {
    return new RunnableStep.Of("Refresh page", Selenide::refresh);
  }

  /**
   * Creates a step that discards the current browser session and opens a fresh one.
   *
   * @param url first URL opened in the fresh session
   * @return lazy browser step
   */
  public static RunnableStep resetBrowserAndOpen(final String url) {
    return new RunnableStep.Of("Reset browser and open page", params("url", url), () -> {
      if (WebDriverRunner.hasWebDriverStarted()) {
        Selenide.clearBrowserCookies();
        Selenide.clearBrowserLocalStorage();
        Selenide.closeWebDriver();
      }
      Selenide.open(url);
    });
  }

  /**
   * Switches to an exact Selenium window handle.
   *
   * @param handle handle returned by Selenium
   * @return lazy browser step
   */
  public static RunnableStep switchToWindow(final String handle) {
    return new RunnableStep.Of("Switch browser window", params("handle", handle), () ->
      WebDriverRunner.getWebDriver().switchTo().window(handle)
    );
  }

  /**
   * Switches to the first open window whose title equals the requested value.
   *
   * <p>The original window is restored when no match exists.</p>
   *
   * @param title exact browser title
   * @return lazy browser step
   */
  public static RunnableStep switchToWindowByTitle(final String title) {
    return new RunnableStep.Of("Switch browser window by title", params("title", secret(title)), () ->
      switchToMatchingWindow(driver -> Objects.equals(driver.getTitle(), title), "title", title)
    );
  }

  /**
   * Switches to the first open window whose URL equals the requested value.
   *
   * <p>The original window is restored when no match exists.</p>
   *
   * @param url exact browser URL
   * @return lazy browser step
   */
  public static RunnableStep switchToWindowByUrl(final String url) {
    return new RunnableStep.Of("Switch browser window by URL", params("url", url), () ->
      switchToMatchingWindow(driver -> Objects.equals(driver.getCurrentUrl(), url), "URL", url)
    );
  }

  /**
   * Reads a cookie from the current browser session.
   *
   * @param cookieName exact cookie name
   * @return cookie value
   * @throws NullPointerException when the named cookie does not exist
   */
  public static String getCookieValueByName(final String cookieName) {
    final Cookie cookie = WebDriverRunner.getWebDriver().manage().getCookieNamed(cookieName);
    return Objects.requireNonNull(cookie, "Cookie not found: " + cookieName).getValue();
  }

  /** @return cookies visible to the current browser session */
  public static Set<Cookie> getCookies() {
    return WebDriverRunner.getWebDriver().manage().getCookies();
  }

  /**
   * Tries each current handle and restores the original handle when matching fails.
   *
   * <p>The original window is restored both when no match exists and when reading a candidate
   * title/URL throws. A restoration failure is attached as a suppressed exception to the original
   * matching failure rather than replacing it.</p>
   */
  private static void switchToMatchingWindow(final java.util.function.Predicate<WebDriver> matcher,
                                             final String selectorName,
                                             final String selectorValue) {
    final WebDriver driver = WebDriverRunner.getWebDriver();
    final String originalHandle = driver.getWindowHandle();
    try {
      for (final String handle : driver.getWindowHandles()) {
        driver.switchTo().window(handle);
        if (matcher.test(driver)) {
          return;
        }
      }
    } catch (final RuntimeException ex) {
      restoreWindow(driver, originalHandle, ex);
      throw ex;
    }
    final NoSuchElementException notFound = new NoSuchElementException(
      "No browser window matched the requested " + selectorName
    );
    restoreWindow(driver, originalHandle, notFound);
    throw notFound;
  }

  /** Restores a previously active window without replacing the primary failure. */
  private static void restoreWindow(final WebDriver driver,
                                    final String handle,
                                    final RuntimeException primaryFailure) {
    try {
      driver.switchTo().window(handle);
    } catch (final RuntimeException restoreFailure) {
      primaryFailure.addSuppressed(restoreFailure);
    }
  }
}
