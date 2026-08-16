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

/**
 * Base class for Selenide page objects with optional page-open verification.
 *
 * <p>When a subclass has {@link Page}, construction may wait for two independent page markers:
 * visible text from {@link Page#title()} and a visible element from {@link Page#xPath()}. Blank
 * markers are skipped. A subclass without {@code @Page} is allowed and receives no constructor-time
 * verification.</p>
 *
 * <p>Page-object constructors therefore perform browser checks. Instantiate a page only after the
 * browser has navigated to the expected screen.</p>
 */
public abstract class BasePage {
  /** XPath template matching any element whose normalized text contains the supplied fragment. */
  private static final String CONTAINS_TEXT_TEMPLATE = "//*[contains(normalize-space(.), '%s')]";
  /** Maximum wait for constructor-time page markers. */
  private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(10);

  /**
   * Verifies page markers declared by the concrete page class.
   *
   * <p>Title verification runs before XPath verification. Selenide assertion failures are allowed to
   * propagate so construction fails immediately when the page object does not match the browser.</p>
   */
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

  /**
   * Checks whether an element containing the supplied normalized text is currently visible.
   *
   * <p>{@code SelenideElement.is(...)} performs a condition
   * check rather than {@code shouldBe}; the method therefore returns a boolean instead of failing an
   * assertion when the text is absent.</p>
   *
   * @param label text fragment to search for
   * @return {@code true} when a matching element is visible
   */
  @Step("Text '{label}' is visible")
  public boolean isLabelVisible(final String label) {
    return $x(CONTAINS_TEXT_TEMPLATE.formatted(label)).is(visible);
  }

  /**
   * Checks whether matching text becomes visible within an explicit duration.
   *
   * @param label text fragment to search for
   * @param duration maximum condition-check duration
   * @return {@code true} when a matching element is visible within the duration
   */
  @Step("Text '{label}' is visible")
  public boolean isLabelVisible(final String label, final Duration duration) {
    return $x(CONTAINS_TEXT_TEMPLATE.formatted(label)).is(visible, duration);
  }

  /**
   * Waits for visible text and clicks the matching element.
   *
   * @param text text fragment used to locate the element
   */
  @Step("Click text '{text}'")
  public void clickByText(final String text) {
    $x(CONTAINS_TEXT_TEMPLATE.formatted(text)).shouldBe(visible).click();
  }

  /**
   * Reads the current URL from Selenide's default WebDriver.
   *
   * @return browser URL as reported by Selenium
   */
  @Step("Read current URL")
  public String getUrl() {
    return WebDriverRunner.getWebDriver().getCurrentUrl();
  }

  /**
   * Copies visible element texts from a Selenide collection into a list.
   *
   * @param elements collection whose texts are read
   * @return texts in collection order
   */
  public static List<String> extractTexts(final ElementsCollection elements) {
    return elements.texts();
  }

  /**
   * Waits until an element is enabled, then clicks it.
   *
   * @param element element to click
   */
  public void safeClick(final SelenideElement element) {
    element.shouldBe(enabled).click();
  }

  /**
   * Waits until an input is enabled, then replaces its value through Selenide.
   *
   * @param element input element
   * @param value new field value
   */
  public void fillInput(final SelenideElement element, final String value) {
    element.shouldBe(enabled).setValue(value);
  }
}
