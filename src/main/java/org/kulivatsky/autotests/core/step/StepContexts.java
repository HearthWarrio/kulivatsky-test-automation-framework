package org.kulivatsky.autotests.core.step;

import org.kulivatsky.autotests.core.util.ExceptionUtils;

import static org.kulivatsky.autotests.core.step.Steps.step;

/** Entry points for fluent {@link StepContext} chains. */
public final class StepContexts {
  /** No instances. */
  private StepContexts() {
  }

  /**
   * Starts a chain before a context value exists.
   *
   * @return empty chain that can execute setup actions or obtain a value through {@code with}
   */
  public static EmptyStepContext stepContext() {
    return new EmptyStepContext();
  }

  /**
   * Starts a chain with an existing value.
   *
   * @param context initial context value
   * @param <C> context type
   * @return context wrapper carrying {@code context}
   */
  public static <C> StepContext<C> stepContext(final C context) {
    return new StepContext<>(context);
  }

  /**
   * Fluent chain state used before any value has been produced.
   *
   * <p>Like {@link StepContext}, named step descriptors create Allure steps while raw throwable
   * lambdas execute directly.</p>
   */
  public static final class EmptyStepContext {
    /** Instances are created only through {@link StepContexts#stepContext()}. */
    private EmptyStepContext() {
    }

    /**
     * Executes a named no-input step and stays in the empty state.
     *
     * @param runnableStep step to execute
     * @return this empty context
     */
    public EmptyStepContext exec(final RunnableStep runnableStep) {
      step(runnableStep);
      return this;
    }

    /**
     * Executes a raw no-input action without creating an Allure step.
     *
     * @param action action to run
     * @return this empty context
     */
    public EmptyStepContext exec(final ThRunnable<?> action) {
      try {
        action.run();
        return this;
      } catch (final Throwable ex) {
        return ExceptionUtils.sneakyThrow(ex);
      }
    }

    /**
     * Executes a named supplier step and moves the chain into a typed context.
     *
     * @param supplierStep step that produces the first context value
     * @param <C> produced value type
     * @return context carrying the produced value
     */
    public <C> StepContext<C> with(final SupplierStep<C> supplierStep) {
      return new StepContext<>(step(supplierStep));
    }

    /**
     * Obtains the first context value from a raw supplier without creating an Allure step.
     *
     * @param supplier value supplier
     * @param <C> produced value type
     * @return context carrying the supplied value
     */
    public <C> StepContext<C> with(final ThSupplier<? extends C, ?> supplier) {
      try {
        return new StepContext<>(supplier.get());
      } catch (final Throwable ex) {
        return ExceptionUtils.sneakyThrow(ex);
      }
    }

    /**
     * Obtains the first context value from an ad-hoc named Allure step.
     *
     * @param name report step name
     * @param supplier value supplier
     * @param <C> produced value type
     * @return context carrying the supplied value
     */
    public <C> StepContext<C> with(final String name, final ThSupplier<C, ?> supplier) {
      return new StepContext<>(step(name, supplier));
    }

    /**
     * Executes a named supplier step and returns its value without creating a fluent context.
     *
     * @param supplierStep step to execute
     * @param <R> result type
     * @return supplied result
     */
    public <R> R res(final SupplierStep<R> supplierStep) {
      return step(supplierStep);
    }

    /**
     * Executes a raw supplier directly and returns its value without creating an Allure step.
     *
     * @param supplier supplier to invoke
     * @param <R> result type
     * @return supplied result
     */
    public <R> R res(final ThSupplier<? extends R, ?> supplier) {
      try {
        return supplier.get();
      } catch (final Throwable ex) {
        return ExceptionUtils.sneakyThrow(ex);
      }
    }
  }
}
