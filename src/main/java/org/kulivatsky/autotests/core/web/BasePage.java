package org.kulivatsky.autotests.core.web;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebDriverRunner;
import org.kulivatsky.autotests.core.annotation.Page;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$$x;
import static com.codeborne.selenide.Selenide.$x;
import static org.kulivatsky.autotests.core.step.Steps.step;

/**
 * Base class for Selenide page objects with optional page-open verification.
 *
 * <p>Text lookup normalizes caller whitespace, escapes the value as an XPath literal, considers
 * elements with a matching direct text node, and then selects the first visible match. Restricting
 * the XPath to direct text nodes avoids broad ancestors such as {@code body}; applying Selenide's
 * visibility filter prevents a hidden matching child from masking a visible sibling or parent.</p>
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
      try {
        textElement(page.title()).shouldBe(visible, DEFAULT_TIMEOUT);
      } catch (final AssertionError ex) {
        throw new AssertionError("Page title marker is not visible", ex);
      } catch (final RuntimeException ex) {
        throw new IllegalStateException("Unable to verify page title marker visibility", ex);
      }
    }
    if (!page.xPath().isBlank()) {
      $x(page.xPath()).shouldBe(visible, DEFAULT_TIMEOUT);
    }
  }

  /**
   * Checks whether an element containing the supplied text is currently visible.
   *
   * <p>The searched text is deliberately not copied into the report step name or parameters because
   * arbitrary page text may contain identifiers or other report-sensitive data.</p>
   *
   * @param label text fragment to search for
   * @return {@code true} when a matching element is visible
   */
  public boolean isLabelVisible(final String label) {
    return step("Check text visibility", () -> textElement(label).is(visible));
  }

  /**
   * Checks whether matching text becomes visible within an explicit duration.
   *
   * @param label text fragment to search for
   * @param duration maximum condition-check duration
   * @return {@code true} when a matching element is visible within the duration
   */
  public boolean isLabelVisible(final String label, final Duration duration) {
    return step("Check text visibility with timeout", () -> textElement(label).is(visible, duration));
  }

  /**
   * Waits for visible text and clicks the first visible matching element.
   *
   * @param text text fragment used to locate the element
   */
  public void clickByText(final String text) {
    step("Click element by visible text", () -> {
      try {
        textElement(text).shouldBe(visible).click();
      } catch (final AssertionError ex) {
        throw new AssertionError("No visible element matched the requested text", ex);
      } catch (final RuntimeException ex) {
        throw new IllegalStateException("Unable to click element by visible text", ex);
      }
    });
  }

  /**
   * Returns the current browser URL reported by Selenium.
   *
   * @return current browser URL as reported by Selenium
   */
  public String getUrl() {
    return step("Read current URL", () -> WebDriverRunner.getWebDriver().getCurrentUrl());
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
   * @param element element to wait for and click
   */
  public void safeClick(final SelenideElement element) {
    element.shouldBe(enabled).click();
  }

  /**
   * Waits until an input is enabled, then replaces its value.
   *
   * @param element input to wait for and update
   * @param value value written to the input
   */
  public void fillInput(final SelenideElement element, final String value) {
    element.shouldBe(enabled).setValue(value);
  }

  /**
   * Builds a locator for the first visible element with a direct text node containing a fragment.
   *
   * @param text non-blank text fragment; repeated whitespace is collapsed before lookup
   * @return lazy Selenide element resolved from the matching visible collection
   * @throws NullPointerException when {@code text} is null
   * @throws IllegalArgumentException when {@code text} is blank
   */
  private static SelenideElement textElement(final String text) {
    final String normalizedText = normalizeText(text);
    final String literal = xpathLiteral(normalizedText);
    final String xpath = "//*[text()[contains(normalize-space(.), %s)]]".formatted(literal);
    return $$x(xpath).findBy(visible);
  }

  /** Normalizes caller text in the same way XPath {@code normalize-space()} normalizes text nodes. */
  private static String normalizeText(final String text) {
    final String checkedText = Objects.requireNonNull(text, "text");
    if (checkedText.isBlank()) {
      throw new IllegalArgumentException("text must not be blank");
    }
    return checkedText.trim().replaceAll("\\s+", " ");
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
