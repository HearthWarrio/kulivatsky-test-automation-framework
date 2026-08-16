package org.kulivatsky.autotests.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares the marker that identifies a Selenide page object and, optionally, how to verify that
 * the expected page is open.
 *
 * <p>{@link org.kulivatsky.autotests.core.web.BasePage} checks the supplied title and XPath from
 * its constructor. Either value may be omitted. When both are set, both checks must pass.</p>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Page {
  /**
   * Text that must be visible when the page object is created.
   *
   * @return expected page text, or an empty string to skip the text check
   */
  String title() default "";

  /**
   * XPath that must resolve to a visible element when the page object is created.
   *
   * @return XPath used as a page marker, or an empty string to skip the XPath check
   */
  String xPath() default "";
}
