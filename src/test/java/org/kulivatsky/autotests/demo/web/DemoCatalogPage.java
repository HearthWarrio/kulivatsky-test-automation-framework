package org.kulivatsky.autotests.demo.web;

import com.codeborne.selenide.SelenideElement;
import io.qameta.allure.Step;
import org.kulivatsky.autotests.core.annotation.Page;
import org.kulivatsky.autotests.core.web.BasePage;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.$;

@Page(title = "Demo Catalog")
public final class DemoCatalogPage extends BasePage {
  private final SelenideElement addItemButton = $("#add-item");
  private final SelenideElement cart = $("#cart");

  @Step("Add product to cart")
  public DemoCatalogPage addProductToCart() {
    safeClick(this.addItemButton);
    return this;
  }

  @Step("Cart contains one item")
  public DemoCatalogPage cartContainsOneItem() {
    this.cart.shouldHave(text("1 item in cart"));
    return this;
  }
}
