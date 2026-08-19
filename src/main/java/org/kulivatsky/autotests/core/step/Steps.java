package org.kulivatsky.autotests.core.step;

import io.qameta.allure.Allure;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.model.Parameter;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StatusDetails;
import io.qameta.allure.model.StepResult;
import org.kulivatsky.autotests.core.util.AllureUtils;
import org.kulivatsky.autotests.core.util.ExceptionUtils;

import java.util.Map;
import java.util.UUID;

/**
 * Executes framework step descriptors and reports their lifecycle to Allure.
 *
 * <p>This class is the boundary between the step DSL and Allure. Descriptor types such as
 * {@link RunnableStep} and {@link SupplierStep} only store a name, parameters, and an action; the
 * action does not become an Allure step until it is passed here.</p>
 *
 * <p>Failures are rethrown as the original {@link Throwable}. Assertions are marked
 * {@link Status#FAILED}; every other throwable is marked {@link Status#BROKEN}. The original stack
 * trace is copied into Allure status details before the throwable leaves this class.</p>
 */
public final class Steps {
  /** No instances. */
  private Steps() {
  }

  /**
   * Executes an ad-hoc action as a named Allure step with no parameters.
   *
   * @param name report step name
   * @param action action to execute
   */
  public static void step(final String name, final ThRunnable<?> action) {
    execute(name, Map.of(), () -> {
      action.run();
      return null;
    });
  }

  /**
   * Executes an ad-hoc value-producing action as a named Allure step with no parameters.
   *
   * @param name report step name
   * @param action action to execute
   * @param <R> returned value type
   * @return value produced by {@code action}
   */
  public static <R> R step(final String name, final ThSupplier<R, ?> action) {
    return execute(name, Map.of(), action);
  }

  /**
   * Executes a no-input step descriptor.
   *
   * @param step descriptor containing the report metadata and action
   */
  public static void step(final RunnableStep step) {
    execute(step.name(), step.params(), () -> {
      step.action().run();
      return null;
    });
  }

  /**
   * Executes a value-producing step descriptor.
   *
   * @param step descriptor containing the report metadata and supplier
   * @param <R> returned value type
   * @return value produced by the step
   */
  public static <R> R step(final SupplierStep<R> step) {
    return execute(step.name(), step.params(), step.action());
  }

  /**
   * Executes a consuming step descriptor with an existing context value.
   *
   * @param step descriptor containing the report metadata and consumer
   * @param value context value passed to the consumer
   * @param <T> context value type
   */
  public static <T> void step(final ConsumerStep<? super T> step, final T value) {
    execute(step.name(), step.params(), () -> {
      step.action().accept(value);
      return null;
    });
  }

  /**
   * Executes a transforming step descriptor with an existing context value.
   *
   * @param step descriptor containing the report metadata and function
   * @param value context value passed to the function
   * @param <T> input value type
   * @param <R> result type
   * @return transformation result
   */
  public static <T, R> R step(final FunctionStep<? super T, R> step, final T value) {
    return execute(step.name(), step.params(), () -> step.action().apply(value));
  }

  /**
   * Runs one action inside a complete Allure step lifecycle.
   *
   * <p>A fresh UUID is created for every invocation so nested and parallel steps cannot overwrite
   * one another. Parameters are converted to strings only when the Allure model is built; the
   * original map remains untouched.</p>
   *
   * <p>{@code stopStep} is always called from {@code finally}. This matters when the action throws:
   * leaving a started step open corrupts the nesting of every later step on the same Allure
   * lifecycle.</p>
   *
   * @param name report step name
   * @param params parameters written to the Allure step
   * @param action action executed between {@code startStep} and {@code stopStep}
   * @param <R> action result type
   * @return action result
   */
  private static <R> R execute(final String name,
                               final Map<String, Object> params,
                               final ThSupplier<R, ?> action) {
    final AllureLifecycle lifecycle = Allure.getLifecycle();
    final String uuid = UUID.randomUUID().toString();
    final StepResult stepResult = new StepResult().setName(name).setParameters(
      params.entrySet().stream()
        .map(entry -> new Parameter().setName(entry.getKey()).setValue(Params.reportValue(entry.getKey(), entry.getValue())))
        .toList()
    );

    lifecycle.startStep(uuid, stepResult);
    try {
      final R result = action.get();
      lifecycle.updateStep(uuid, step -> step.setStatus(Status.PASSED));
      return result;
    } catch (final Throwable ex) {
      final Status status = ex instanceof AssertionError ? Status.FAILED : Status.BROKEN;
      final StatusDetails details = new StatusDetails()
        .setMessage(AllureUtils.redact(ex.getMessage()))
        .setTrace(AllureUtils.redact(ExceptionUtils.stackTrace(ex)));
      lifecycle.updateStep(uuid, step -> step.setStatus(status).setStatusDetails(details));
      return ExceptionUtils.sneakyThrow(ex);
    } finally {
      lifecycle.stopStep(uuid);
    }
  }
}
