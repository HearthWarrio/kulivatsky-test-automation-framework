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

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface TestInfo {
  String name() default "";
  Env.Option[] env() default {};
  Launch.Option[] launch() default {};
  TestType.Option[] testType() default {};
  TestSubject.Option[] testSubject() default {};
  TestLevel.Option[] testLevel() default {};
  Project.Option[] project() default {};
  APIService.Option[] apiService() default {};
}
