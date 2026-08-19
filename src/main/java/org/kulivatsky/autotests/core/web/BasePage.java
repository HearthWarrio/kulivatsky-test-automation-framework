package org.kulivatsky.autotests.core.web;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebDriverRunner;
import io.qameta.allure.Step;
import org.kulivatsky.autotests.core.annotation.Page;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$x;

/**
 * Base class for Selenide page objects with optional page-open verification.
 *
 * <p>Text lookup uses an escaped XPath literal and selects the deepest visible element containing
 * the requested text. This avoids two common failures of raw string interpolation: apostrophes in
 * user-facing text breaking the XPath, and broad ancestor elements such as {@code body} matching
 * merely because a descendant contains the same text.</p>
 */
public abstract class BasePage {
  /** Maximum wait for constructor-time page markers. */
  private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(10);

  /**
   * Verifies page markers declared by the concrete page class.
   *
   * <p>Title verification runs before explicit XPath verification. A subclass without
   * {@link Page} receives no constructor-time browser checks.</p>
   */
  protected BasePage() {
    final Page page = this.getClass().getAnnotation(Page.class);
    if (page == null) {
      return;
    }
    if (!page.title().isBlank()) {
      textElement(page.title()).shouldBe(visible, DEFAULT_TIMEOUT);
    }
    if (!page.xPath().isBlank()) {
      $x(page.xPath()).shouldBe(visible, DEFAULT_TIMEOUT);
    }
  }

  /**
   * Checks whether the deepest element containing the supplied text is currently visible.
   *
   * @param label text fragment to search for
   * @return {@code true} when a matching element is visible
   */
  @Step("Text '{label}' is visible")
  public boolean isLabelVisible(final String label) {
    return textElement(label).is(visible);
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
    return textElement(label).is(visible, duration);
  }

  /**
   * Waits for visible text and clicks the deepest matching element.
   *
   * @param text text fragment used to locate the element
   */
  @Step("Click text '{text}'")
  public void clickByText(final String text) {
    textElement(text).shouldBe(visible).click();
  }

  /** @return current browser URL as reported by Selenium */
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

  /** Waits until an element is enabled, then clicks it. */
  public void safeClick(final SelenideElement element) {
    element.shouldBe(enabled).click();
  }

  /** Waits until an input is enabled, then replaces its value. */
  public void fillInput(final SelenideElement element, final String value) {
    element.shouldBe(enabled).setValue(value);
  }

  /**
   * Builds a locator for the deepest element whose normalized descendant text contains a fragment.
   *
   * @param text non-blank text fragment
   * @return lazy Selenide element
   * @throws NullPointerException when {@code text} is null
   * @throws IllegalArgumentException when {@code text} is blank
   */
  private static SelenideElement textElement(final String text) {
    final String checkedText = Objects.requireNonNull(text, "text");
    if (checkedText.isBlank()) {
      throw new IllegalArgumentException("text must not be blank");
    }
    final String literal = xpathLiteral(checkedText);
    final String xpath = "//*[contains(normalize-space(.), %1$s) and not(.//*[contains(normalize-space(.), %1$s)])]"
      .formatted(literal);
    return $x(xpath);
  }

  /**
   * Quotes arbitrary text as an XPath 1.0 string literal.
   *
   * <p>When the text contains both quote types, the result is a {@code concat(...)} expression.
   * No input text is inserted into XPath syntax without quoting.</p>
   *
   * @param text raw text
   * @return XPath expression evaluating to exactly that text
   */
  private static String xpathLiteral(final String text) {
    if (!text.contains("'")) {
      return "'" + text + "'";
    }
    if (!text.contains("\"")) {
      return "\"" + text + "\"";
    }
    final String[] parts = text.split("'", -1);
    final StringBuilder result = new StringBuilder("concat(");
    for (int index = 0; index < parts.length; index++) {
      if (index > 0) {
        result.append(", \"'\", ");
      }
      result.append('\'').append(parts[index]).append('\'');
    }
    return result.append(')').toString();
  }
}
