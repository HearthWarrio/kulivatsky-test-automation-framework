package org.kulivatsky.autotests.demo.web;

import com.codeborne.selenide.SelenideElement;
import io.qameta.allure.Step;
import org.kulivatsky.autotests.core.annotation.Page;
import org.kulivatsky.autotests.core.web.BasePage;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.$;

/** Page object for the local catalog used by {@code MediumUiTest}. */
@Page(title = "Demo Catalog")
public final class DemoCatalogPage extends BasePage {
  /** Button that mutates the demo cart state. */
  private final SelenideElement addItemButton = $("#add-item");
  /** Element that displays the current cart summary. */
  private final SelenideElement cart = $("#cart");

  /**
   * Clicks the catalog's add-to-cart button.
   *
   * @return this page object for fluent assertions/actions
   */
  @Step("Add product to cart")
  public DemoCatalogPage addProductToCart() {
    safeClick(this.addItemButton);
    return this;
  }

  /**
   * Waits until the cart summary reports exactly one item.
   *
   * @return this page object for further chaining
   */
  @Step("Cart contains one item")
  public DemoCatalogPage cartContainsOneItem() {
    this.cart.shouldHave(text("1 item in cart"));
    return this;
  }
}
