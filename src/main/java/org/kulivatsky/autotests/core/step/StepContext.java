package org.kulivatsky.autotests.core.step;

import org.kulivatsky.autotests.core.util.ExceptionUtils;

import static org.kulivatsky.autotests.core.step.Steps.step;

/**
 * Fluent holder that passes one value through a sequence of test actions.
 *
 * <p>The context itself has no hidden execution state: it stores exactly one value. Named
 * {@link ConsumerStep} and {@link FunctionStep} instances are executed through {@link Steps} and
 * therefore appear in Allure. Raw throwable lambdas passed to {@link #exec(ThConsumer)} or
 * {@link #res(ThFunction)} run directly and do <strong>not</strong> create an extra report step.
 * That distinction is part of the DSL contract.</p>
 *
 * @param <C> current context value type
 */
public final class StepContext<C> {
  /** Value carried by this context instance. */
  private final C context;

  /**
   * Creates a context around a value produced by {@link StepContexts} or {@link #with(FunctionStep)}.
   *
   * @param context value to carry; null is allowed and is passed to subsequent actions unchanged
   */
  StepContext(final C context) {
    this.context = context;
  }

  /**
   * Executes a named consuming step against the current value and keeps the same context.
   *
   * @param consumerStep step to execute
   * @return this context instance for fluent chaining
   */
  public StepContext<C> exec(final ConsumerStep<? super C> consumerStep) {
    step(consumerStep, this.context);
    return this;
  }

  /**
   * Executes a raw consumer against the current value without creating an Allure step.
   *
   * <p>Use this overload for glue code that should stay inside the surrounding step structure. Use
   * {@link #exec(ConsumerStep)} when the action deserves its own named report step.</p>
   *
   * @param consumer action to execute
   * @return this context instance for fluent chaining
   */
  public StepContext<C> exec(final ThConsumer<? super C, ?> consumer) {
    try {
      consumer.accept(this.context);
      return this;
    } catch (final Throwable ex) {
      return ExceptionUtils.sneakyThrow(ex);
    }
  }

  /**
   * Executes an independent named step and leaves the carried value unchanged.
   *
   * @param runnableStep step to execute
   * @return this context instance for fluent chaining
   */
  public StepContext<C> exec(final RunnableStep runnableStep) {
    step(runnableStep);
    return this;
  }

  /**
   * Transforms the current value through a named step and starts a new typed context.
   *
   * @param functionStep transformation step
   * @param <R> new context type
   * @return new context carrying the transformation result
   */
  public <R> StepContext<R> with(final FunctionStep<? super C, R> functionStep) {
    return new StepContext<>(step(functionStep, this.context));
  }

  /**
   * Ends a fluent chain and returns the current value unchanged.
   *
   * @return carried context value
   */
  public C res() {
    return this.context;
  }

  /**
   * Ends a fluent chain by applying a named transformation step.
   *
   * @param functionStep transformation step
   * @param <R> result type
   * @return transformation result
   */
  public <R> R res(final FunctionStep<? super C, R> functionStep) {
    return step(functionStep, this.context);
  }

  /**
   * Ends a fluent chain by applying a raw transformation without creating an Allure step.
   *
   * @param function transformation to apply directly
   * @param <R> result type
   * @return transformation result
   */
  public <R> R res(final ThFunction<? super C, R, ?> function) {
    try {
      return function.apply(this.context);
    } catch (final Throwable ex) {
      return ExceptionUtils.sneakyThrow(ex);
    }
  }

  /**
   * Returns the currently carried value without ending or changing the context object.
   *
   * @return current value
   */
  public C context() {
    return this.context;
  }
}
