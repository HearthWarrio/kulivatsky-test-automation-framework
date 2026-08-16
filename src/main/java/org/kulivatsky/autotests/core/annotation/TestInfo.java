package org.kulivatsky.autotests.core.annotation;

import org.kulivatsky.autotests.core.config.APIService;
import org.kulivatsky.autotests.core.config.Env;
import org.kulivatsky.autotests.core.config.Launch;
import org.kulivatsky.autotests.core.config.Project;
import org.kulivatsky.autotests.core.config.TestLevel;
import org.kulivatsky.autotests.core.config.TestSubject;
import org.kulivatsky.autotests.core.config.TestType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Describes a test and declares the launch selections in which it is allowed to run.
 *
 * <p>The arrays are filters, not labels only. During TestNG annotation transformation,
 * {@link org.kulivatsky.autotests.core.listener.TestNGListener} compares non-empty arrays with the
 * current configuration. A test is disabled when at least one configured dimension does not match.</p>
 *
 * <p>An empty array means "no restriction for this dimension".</p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface TestInfo {
  /**
   * Human-readable test name used as the TestNG description and in log output.
   *
   * @return test name, or an empty string to keep TestNG's method name
   */
  String name() default "";

  /**
   * Environments in which the test may run.
   *
   * @return allowed environments; empty means any environment
   */
  Env.Option[] env() default {};

  /**
   * Launch modes in which the test may run.
   *
   * @return allowed launch modes; empty means any launch mode
   */
  Launch.Option[] launch() default {};

  /**
   * Test-type selections accepted by the test.
   *
   * @return allowed test types; empty means any configured test type
   */
  TestType.Option[] testType() default {};

  /**
   * Test subjects accepted by the test.
   *
   * @return allowed subjects; empty means any configured subject
   */
  TestSubject.Option[] testSubject() default {};

  /**
   * Test levels accepted by the test.
   *
   * @return allowed levels; empty means any configured level
   */
  TestLevel.Option[] testLevel() default {};

  /**
   * Projects to which the test belongs.
   *
   * @return allowed projects; empty means any configured project
   */
  Project.Option[] project() default {};

  /**
   * Services exercised by the test.
   *
   * @return allowed API services; empty means any configured service
   */
  APIService.Option[] apiService() default {};
}
