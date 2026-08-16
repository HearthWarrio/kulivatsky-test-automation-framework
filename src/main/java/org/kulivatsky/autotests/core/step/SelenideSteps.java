package org.kulivatsky.autotests.core.step;

import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.WebDriverRunner;
import org.openqa.selenium.Cookie;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Set;

import static org.kulivatsky.autotests.core.step.Params.params;

public final class SelenideSteps {
  private SelenideSteps() {
  }

  public static RunnableStep open(final String url) {
    return new RunnableStep.Of("Open page", params("url", url), () -> Selenide.open(url));
  }

  public static RunnableStep close() {
    return new RunnableStep.Of("Close browser", Selenide::closeWebDriver);
  }

  public static RunnableStep refresh() {
    return new RunnableStep.Of("Refresh page", Selenide::refresh);
  }

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

  public static RunnableStep switchToWindow(final int number) {
    return new RunnableStep.Of("Switch browser tab", params("tabIndex", number), () -> {
      final ArrayList<String> tabs = new ArrayList<>(WebDriverRunner.getWebDriver().getWindowHandles());
      if (number < 0 || number >= tabs.size()) {
        throw new IndexOutOfBoundsException("Browser tab index " + number + " is outside 0.." + (tabs.size() - 1));
      }
      WebDriverRunner.getWebDriver().switchTo().window(tabs.get(number));
    });
  }

  public static String getCookieValueByName(final String cookieName) {
    final Cookie cookie = WebDriverRunner.getWebDriver().manage().getCookieNamed(cookieName);
    return Objects.requireNonNull(cookie, "Cookie not found: " + cookieName).getValue();
  }

  public static Set<Cookie> getCookies() {
    return WebDriverRunner.getWebDriver().manage().getCookies();
  }
}
