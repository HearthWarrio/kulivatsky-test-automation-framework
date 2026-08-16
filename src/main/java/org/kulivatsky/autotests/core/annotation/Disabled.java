package org.kulivatsky.autotests.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a TestNG test method as intentionally disabled.
 *
 * <p>{@link org.kulivatsky.autotests.core.listener.TestNGListener} reads this annotation before
 * TestNG executes the method and switches the method off. The mandatory reason keeps the decision
 * visible in source code instead of hiding it in suite configuration.</p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Disabled {
  /**
   * Explains why the test must not run.
   *
   * @return human-readable disable reason
   */
  String reason();
}
