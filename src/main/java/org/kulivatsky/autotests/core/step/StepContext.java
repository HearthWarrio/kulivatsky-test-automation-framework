package org.kulivatsky.autotests.core.step;

import org.kulivatsky.autotests.core.util.ExceptionUtils;

import static org.kulivatsky.autotests.core.step.Steps.step;

public final class StepContext<C> {
  private final C context;

  StepContext(final C context) {
    this.context = context;
  }

  public StepContext<C> exec(final ConsumerStep<? super C> consumerStep) {
    step(consumerStep, this.context);
    return this;
  }

  public StepContext<C> exec(final ThConsumer<? super C, ?> consumer) {
    try {
      consumer.accept(this.context);
      return this;
    } catch (final Throwable ex) {
      return ExceptionUtils.sneakyThrow(ex);
    }
  }

  public StepContext<C> exec(final RunnableStep runnableStep) {
    step(runnableStep);
    return this;
  }

  public <R> StepContext<R> with(final FunctionStep<? super C, R> functionStep) {
    return new StepContext<>(step(functionStep, this.context));
  }

  public C res() {
    return this.context;
  }

  public <R> R res(final FunctionStep<? super C, R> functionStep) {
    return step(functionStep, this.context);
  }

  public <R> R res(final ThFunction<? super C, R, ?> function) {
    try {
      return function.apply(this.context);
    } catch (final Throwable ex) {
      return ExceptionUtils.sneakyThrow(ex);
    }
  }

  public C context() {
    return this.context;
  }
}
