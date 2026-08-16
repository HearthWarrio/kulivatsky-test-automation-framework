package org.kulivatsky.autotests.core.web;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebDriverRunner;
import io.qameta.allure.Step;
import org.kulivatsky.autotests.core.annotation.Page;

import java.time.Duration;
import java.util.List;

import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$x;

public abstract class BasePage {
  private static final String CONTAINS_TEXT_TEMPLATE = "//*[contains(normalize-space(.), '%s')]";
  private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(10);

  protected BasePage() {
    final Page page = this.getClass().getAnnotation(Page.class);
    if (page == null) {
      return;
    }
    if (!page.title().isBlank()) {
      $x(CONTAINS_TEXT_TEMPLATE.formatted(page.title())).shouldBe(visible, DEFAULT_TIMEOUT);
    }
    if (!page.xPath().isBlank()) {
      $x(page.xPath()).shouldBe(visible, DEFAULT_TIMEOUT);
    }
  }

  @Step("Text '{label}' is visible")
  public boolean isLabelVisible(final String label) {
    return $x(CONTAINS_TEXT_TEMPLATE.formatted(label)).is(visible);
  }

  @Step("Text '{label}' is visible")
  public boolean isLabelVisible(final String label, final Duration duration) {
    return $x(CONTAINS_TEXT_TEMPLATE.formatted(label)).is(visible, duration);
  }

  @Step("Click text '{text}'")
  public void clickByText(final String text) {
    $x(CONTAINS_TEXT_TEMPLATE.formatted(text)).shouldBe(visible).click();
  }

  @Step("Read current URL")
  public String getUrl() {
    return WebDriverRunner.getWebDriver().getCurrentUrl();
  }

  public static List<String> extractTexts(final ElementsCollection elements) {
    return elements.texts();
  }

  public void safeClick(final SelenideElement element) {
    element.shouldBe(enabled).click();
  }

  public void fillInput(final SelenideElement element, final String value) {
    element.shouldBe(enabled).setValue(value);
  }
}
