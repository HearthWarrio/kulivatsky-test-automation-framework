package org.kulivatsky.autotests.core.spec;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.specification.RequestSpecification;

import java.time.Duration;

import static org.kulivatsky.autotests.core.step.CommonSteps.delay;
import static org.kulivatsky.autotests.core.step.Steps.step;

/** Request-specification decorators used by REST Assured tests. */
public final class Specifications {
  /** No instances. */
  private Specifications() {
  }

  /**
   * Copies an existing request specification and inserts a delay immediately before the HTTP call.
   *
   * <p>The delay runs inside an Allure step and is applied by a REST Assured filter. The input
   * specification is not modified.</p>
   *
   * @param spec source specification to copy
   * @param duration delay before request execution
   * @return new specification containing the source settings and delay filter
   */
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
