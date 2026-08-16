package org.kulivatsky.autotests.core.step;

import io.qameta.allure.Allure;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.model.Parameter;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StatusDetails;
import io.qameta.allure.model.StepResult;
import org.kulivatsky.autotests.core.util.ExceptionUtils;

import java.util.Map;
import java.util.UUID;

public final class Steps {
  private Steps() {
  }

  public static void step(final String name, final ThRunnable<?> action) {
    execute(name, Map.of(), () -> {
      action.run();
      return null;
    });
  }

  public static <R> R step(final String name, final ThSupplier<R, ?> action) {
    return execute(name, Map.of(), action);
  }

  public static void step(final RunnableStep step) {
    execute(step.name(), step.params(), () -> {
      step.action().run();
      return null;
    });
  }

  public static <R> R step(final SupplierStep<R> step) {
    return execute(step.name(), step.params(), step.action());
  }

  public static <T> void step(final ConsumerStep<? super T> step, final T value) {
    execute(step.name(), step.params(), () -> {
      step.action().accept(value);
      return null;
    });
  }

  public static <T, R> R step(final FunctionStep<? super T, R> step, final T value) {
    return execute(step.name(), step.params(), () -> step.action().apply(value));
  }

  private static <R> R execute(final String name,
                               final Map<String, Object> params,
                               final ThSupplier<R, ?> action) {
    final AllureLifecycle lifecycle = Allure.getLifecycle();
    final String uuid = UUID.randomUUID().toString();
    final StepResult stepResult = new StepResult().setName(name).setParameters(
      params.entrySet().stream()
        .map(entry -> new Parameter().setName(entry.getKey()).setValue(String.valueOf(entry.getValue())))
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
        .setMessage(ex.getMessage())
        .setTrace(ExceptionUtils.stackTrace(ex));
      lifecycle.updateStep(uuid, step -> step.setStatus(status).setStatusDetails(details));
      return ExceptionUtils.sneakyThrow(ex);
    } finally {
      lifecycle.stopStep(uuid);
    }
  }
}
