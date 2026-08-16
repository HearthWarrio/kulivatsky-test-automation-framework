package org.kulivatsky.autotests.core.util;

import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebDriverRunner;
import org.openqa.selenium.Keys;
import org.openqa.selenium.interactions.Actions;

import java.time.Duration;
import java.util.Objects;

import static com.codeborne.selenide.Condition.enabled;

/** Sends keyboard input through Selenium Actions on Selenide's current default WebDriver. */
public final class KeyboardUtils {
  /** No instances. */
  private KeyboardUtils() {
  }

  /**
   * Sends one Selenium key sequence to the currently focused browser element.
   *
   * @param keys key sequence accepted by Selenium Actions
   * @throws NullPointerException when the vararg array itself is null
   */
  public static void pressButton(final CharSequence... keys) {
    Objects.requireNonNull(keys, "keys");
    new Actions(WebDriverRunner.getWebDriver()).sendKeys(keys).perform();
  }

  /**
   * Sends a modifier-plus-key combination, releasing the modifier afterwards.
   *
   * @param first modifier key to hold down
   * @param second key/text sent while the modifier is held
   */
  public static void pressTwoButtonsCombination(final Keys first, final CharSequence second) {
    new Actions(WebDriverRunner.getWebDriver())
      .keyDown(first)
      .sendKeys(second)
      .keyUp(first)
      .perform();
  }

  /** Sends the left-arrow key. */
  public static void pressLeftButton() { pressButton(Keys.LEFT); }

  /** Sends the right-arrow key. */
  public static void pressRightButton() { pressButton(Keys.RIGHT); }

  /** Sends the space key. */
  public static void pressSpaceButton() { pressButton(Keys.SPACE); }

  /** Sends the down-arrow key. */
  public static void pressDownButton() { pressButton(Keys.DOWN); }

  /** Sends the backspace key. */
  public static void pressBackspaceButton() { pressButton(Keys.BACK_SPACE); }

  /** Sends the escape key. */
  public static void pressEscapeButton() { pressButton(Keys.ESCAPE); }

  /** Sends the enter key. */
  public static void pressEnterButton() { pressButton(Keys.ENTER); }

  /** Sends the tab key. */
  public static void pressTabButton() { pressButton(Keys.TAB); }

  /**
   * Focuses an enabled input and sends a fixed number of backspace presses.
   *
   * @param element input element to focus
   * @param count number of backspaces to send; non-positive values produce no key presses
   */
  public static void clearTextFieldViaBackspaceButton(final SelenideElement element, final int count) {
    element.shouldBe(enabled).click();
    for (int i = 0; i < count; i++) {
      pressBackspaceButton();
    }
  }

  /**
   * Clears a field by sending one backspace per character in its current {@code value} attribute.
   *
   * <p>This method is useful for controls where Selenium's direct {@code clear()} does not trigger
   * the same key-driven behavior as a user. A null value causes no backspace presses.</p>
   *
   * @param element input element to clear
   */
  public static void clearTextFieldViaBackspaceButton(final SelenideElement element) {
    element.shouldBe(enabled).click();
    final String value = element.getValue();
    if (value != null) {
      clearTextFieldViaBackspaceButton(element, value.length());
    }
  }

  /**
   * Clears the currently focused text field by sending Ctrl+A followed by backspace.
   *
   * <p>The modifier is Ctrl; callers targeting platforms where Command is expected
   * should use {@link #pressTwoButtonsCombination(Keys, CharSequence)} explicitly.</p>
   */
  public static void clearTextFieldViaBackspaceButton() {
    pressTwoButtonsCombination(Keys.CONTROL, "a");
    pressBackspaceButton();
  }

  /**
   * Types characters one at a time with a fixed delay before each key press.
   *
   * <p>If the sleep is interrupted, this method restores the thread interrupt flag and fails rather
   * than silently continuing with partial input.</p>
   *
   * @param characters characters to send in sequence
   * @param milliseconds delay before each character
   * @throws IllegalArgumentException when the delay is negative
   * @throws IllegalStateException when the thread is interrupted during a delay
   */
  public static void sendKeysWithDelay(final CharSequence characters, final long milliseconds) {
    if (milliseconds < 0) {
      throw new IllegalArgumentException("milliseconds must not be negative");
    }
    for (int i = 0; i < characters.length(); i++) {
      try {
        Thread.sleep(Duration.ofMillis(milliseconds));
      } catch (final InterruptedException ex) {
        Thread.currentThread().interrupt();
        throw new IllegalStateException("Keyboard input was interrupted", ex);
      }
      pressButton(String.valueOf(characters.charAt(i)));
    }
  }
}
