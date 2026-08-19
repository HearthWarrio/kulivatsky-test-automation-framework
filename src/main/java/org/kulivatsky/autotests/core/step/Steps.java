package org.kulivatsky.autotests.core.step;

import io.qameta.allure.Allure;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.model.Parameter;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StatusDetails;
import io.qameta.allure.model.StepResult;
import org.kulivatsky.autotests.core.util.AllureUtils;
import org.kulivatsky.autotests.core.util.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
  /** Logger used when reporting fails but the test action must remain authoritative. */
  private static final Logger LOGGER = LoggerFactory.getLogger(Steps.class);

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
   * Runs one action inside a best-effort Allure step lifecycle.
   *
   * <p>Reporting is deliberately subordinate to test execution. A failure while creating,
   * updating, redacting, or stopping an Allure step is logged but never replaces the action result
   * or the action's original exception. Fatal {@link VirtualMachineError}s are rethrown
   * immediately without allocating stack-trace/reporting structures.</p>
   *
   * @param name report step name
   * @param params parameters written to the Allure step
   * @param action action executed by the step
   * @param <R> action result type
   * @return action result
   */
  private static <R> R execute(final String name,
                               final Map<String, Object> params,
                               final ThSupplier<R, ?> action) {
    final AllureLifecycle lifecycle = lifecycle();
    final String uuid = UUID.randomUUID().toString();
    final boolean started = lifecycle != null && startStep(lifecycle, uuid, name, params);
    boolean fatalFailure = false;

    try {
      final R result = action.get();
      if (started) {
        updateStatus(lifecycle, uuid, Status.PASSED, null);
      }
      return result;
    } catch (final Throwable ex) {
      fatalFailure = ex instanceof VirtualMachineError;
      if (!fatalFailure && started) {
        final Status status = ex instanceof AssertionError ? Status.FAILED : Status.BROKEN;
        updateStatus(lifecycle, uuid, status, ex);
      }
      return ExceptionUtils.sneakyThrow(ex);
    } finally {
      if (started && !fatalFailure) {
        stopStep(lifecycle, uuid);
      }
    }
  }

  /** Returns the active Allure lifecycle, or null when reporting itself cannot be initialized. */
  private static AllureLifecycle lifecycle() {
    try {
      return Allure.getLifecycle();
    } catch (final RuntimeException ex) {
      LOGGER.warn("Unable to obtain Allure lifecycle; executing step without reporting", ex);
      return null;
    }
  }

  /** Starts an Allure step and returns whether a matching stop call is required. */
  private static boolean startStep(final AllureLifecycle lifecycle,
                                   final String uuid,
                                   final String name,
                                   final Map<String, Object> params) {
    try {
      final StepResult stepResult = new StepResult().setName(name).setParameters(
        params.entrySet().stream()
          .map(entry -> new Parameter()
            .setName(entry.getKey())
            .setValue(Params.reportValue(entry.getKey(), entry.getValue())))
          .toList()
      );
      lifecycle.startStep(uuid, stepResult);
      return true;
    } catch (final RuntimeException ex) {
      LOGGER.warn("Unable to start Allure step '{}'", name, ex);
      return false;
    }
  }

  /** Marks an Allure step without allowing reporting failures to escape into test code. */
  private static void updateStatus(final AllureLifecycle lifecycle,
                                   final String uuid,
                                   final Status status,
                                   final Throwable failure) {
    try {
      final StatusDetails details;
      if (failure == null) {
        details = null;
      } else {
        details = new StatusDetails()
          .setMessage(AllureUtils.redact(failure.getMessage()))
          .setTrace(AllureUtils.redact(ExceptionUtils.stackTrace(failure)));
      }
      lifecycle.updateStep(uuid, step -> {
        step.setStatus(status);
        if (details != null) {
          step.setStatusDetails(details);
        }
      });
    } catch (final RuntimeException ex) {
      LOGGER.warn("Unable to update Allure step {}", uuid, ex);
    }
  }

  /** Stops a previously started Allure step without allowing reporting failures to escape. */
  private static void stopStep(final AllureLifecycle lifecycle, final String uuid) {
    try {
      lifecycle.stopStep(uuid);
    } catch (final RuntimeException ex) {
      LOGGER.warn("Unable to stop Allure step {}", uuid, ex);
    }
  }

}
