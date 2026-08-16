package org.kulivatsky.autotests.core.step;

import org.kulivatsky.autotests.core.util.ExceptionUtils;

import static org.kulivatsky.autotests.core.step.Steps.step;

public final class StepContexts {
  private StepContexts() {
  }

  public static EmptyStepContext stepContext() {
    return new EmptyStepContext();
  }

  public static <C> StepContext<C> stepContext(final C context) {
    return new StepContext<>(context);
  }

  public static final class EmptyStepContext {
    private EmptyStepContext() {
    }

    public EmptyStepContext exec(final RunnableStep runnableStep) {
      step(runnableStep);
      return this;
    }

    public EmptyStepContext exec(final ThRunnable<?> action) {
      try {
        action.run();
        return this;
      } catch (final Throwable ex) {
        return ExceptionUtils.sneakyThrow(ex);
      }
    }

    public <C> StepContext<C> with(final SupplierStep<C> supplierStep) {
      return new StepContext<>(step(supplierStep));
    }

    public <C> StepContext<C> with(final ThSupplier<? extends C, ?> supplier) {
      try {
        return new StepContext<>(supplier.get());
      } catch (final Throwable ex) {
        return ExceptionUtils.sneakyThrow(ex);
      }
    }

    public <C> StepContext<C> with(final String name, final ThSupplier<C, ?> supplier) {
      return new StepContext<>(step(name, supplier));
    }

    public <R> R res(final SupplierStep<R> supplierStep) {
      return step(supplierStep);
    }

    public <R> R res(final ThSupplier<? extends R, ?> supplier) {
      try {
        return supplier.get();
      } catch (final Throwable ex) {
        return ExceptionUtils.sneakyThrow(ex);
      }
    }
  }
}
