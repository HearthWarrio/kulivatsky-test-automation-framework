package org.kulivatsky.autotests.core.util;

import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebDriverRunner;
import org.openqa.selenium.Keys;
import org.openqa.selenium.interactions.Actions;

import java.time.Duration;
import java.util.Objects;

import static com.codeborne.selenide.Condition.enabled;

public final class KeyboardUtils {
  private KeyboardUtils() {
  }

  public static void pressButton(final CharSequence... keys) {
    Objects.requireNonNull(keys, "keys");
    new Actions(WebDriverRunner.getWebDriver()).sendKeys(keys).perform();
  }

  public static void pressTwoButtonsCombination(final Keys first, final CharSequence second) {
    new Actions(WebDriverRunner.getWebDriver())
      .keyDown(first)
      .sendKeys(second)
      .keyUp(first)
      .perform();
  }

  public static void pressLeftButton() { pressButton(Keys.LEFT); }
  public static void pressRightButton() { pressButton(Keys.RIGHT); }
  public static void pressSpaceButton() { pressButton(Keys.SPACE); }
  public static void pressDownButton() { pressButton(Keys.DOWN); }
  public static void pressBackspaceButton() { pressButton(Keys.BACK_SPACE); }
  public static void pressEscapeButton() { pressButton(Keys.ESCAPE); }
  public static void pressEnterButton() { pressButton(Keys.ENTER); }
  public static void pressTabButton() { pressButton(Keys.TAB); }

  public static void clearTextFieldViaBackspaceButton(final SelenideElement element, final int count) {
    element.shouldBe(enabled).click();
    for (int i = 0; i < count; i++) {
      pressBackspaceButton();
    }
  }

  public static void clearTextFieldViaBackspaceButton(final SelenideElement element) {
    element.shouldBe(enabled).click();
    final String value = element.getValue();
    if (value != null) {
      clearTextFieldViaBackspaceButton(element, value.length());
    }
  }

  public static void clearTextFieldViaBackspaceButton() {
    pressTwoButtonsCombination(Keys.CONTROL, "a");
    pressBackspaceButton();
  }

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
