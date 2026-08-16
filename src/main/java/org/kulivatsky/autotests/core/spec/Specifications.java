package org.kulivatsky.autotests.core.spec;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.specification.RequestSpecification;

import java.time.Duration;

import static org.kulivatsky.autotests.core.step.CommonSteps.delay;
import static org.kulivatsky.autotests.core.step.Steps.step;

public final class Specifications {
  private Specifications() {
  }

  public static RequestSpecification withDelay(final RequestSpecification spec, final Duration duration) {
    return new RequestSpecBuilder()
      .addRequestSpecification(spec)
      .addFilter((requestSpec, responseSpec, context) -> {
        step(delay(duration));
        return context.next(requestSpec, responseSpec);
      })
      .build();
  }
}
