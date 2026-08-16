package org.kulivatsky.autotests.core.step;

import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.WebDriverRunner;
import org.openqa.selenium.Cookie;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Set;

import static org.kulivatsky.autotests.core.step.Params.params;

/** Builds browser step descriptors and reads state from Selenide's default driver. */
public final class SelenideSteps {
  /** No instances. */
  private SelenideSteps() {
  }

  /**
   * Creates a step that opens a URL using Selenide's default driver.
   *
   * @param url URL accepted by {@link Selenide#open(String)}
   * @return lazy browser step
   */
  public static RunnableStep open(final String url) {
    return new RunnableStep.Of("Open page", params("url", url), () -> Selenide.open(url));
  }

  /**
   * Creates a step that closes Selenide's default WebDriver.
   *
   * @return lazy browser step
   */
  public static RunnableStep close() {
    return new RunnableStep.Of("Close browser", Selenide::closeWebDriver);
  }

  /**
   * Creates a step that refreshes the current page.
   *
   * @return lazy browser step
   */
  public static RunnableStep refresh() {
    return new RunnableStep.Of("Refresh page", Selenide::refresh);
  }

  /**
   * Creates a step that discards the current default browser session and opens a fresh one.
   *
   * <p>If a driver already exists, cookies and local storage are cleared before the driver is
   * closed. A new driver is then created by {@link Selenide#open(String)} according to the current
   * Selenide configuration.</p>
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
   * Creates a step that switches to a browser window by zero-based position.
   *
   * <p>Selenium exposes window handles as a set. This method copies the set to a list and indexes
   * that iteration order; callers should use it only when their driver provides a stable handle
   * order for the opened tabs.</p>
   *
   * @param number zero-based index in the current window-handle iteration order
   * @return lazy browser step
   * @throws IndexOutOfBoundsException when executed with an index outside the current handle list
   */
  public static RunnableStep switchToWindow(final int number) {
    return new RunnableStep.Of("Switch browser tab", params("tabIndex", number), () -> {
      final ArrayList<String> tabs = new ArrayList<>(WebDriverRunner.getWebDriver().getWindowHandles());
      if (number < 0 || number >= tabs.size()) {
        throw new IndexOutOfBoundsException("Browser tab index " + number + " is outside 0.." + (tabs.size() - 1));
      }
      WebDriverRunner.getWebDriver().switchTo().window(tabs.get(number));
    });
  }

  /**
   * Reads a cookie from the current default browser session.
   *
   * @param cookieName exact cookie name
   * @return cookie value
   * @throws NullPointerException when the named cookie does not exist
   */
  public static String getCookieValueByName(final String cookieName) {
    final Cookie cookie = WebDriverRunner.getWebDriver().manage().getCookieNamed(cookieName);
    return Objects.requireNonNull(cookie, "Cookie not found: " + cookieName).getValue();
  }

  /**
   * Returns cookies visible to the current default browser session.
   *
   * @return Selenium cookie set for the current page/domain
   */
  public static Set<Cookie> getCookies() {
    return WebDriverRunner.getWebDriver().manage().getCookies();
  }
}
