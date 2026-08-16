package org.kulivatsky.autotests.core.step;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Named test action with no input and no return value.
 *
 * <p>A {@code RunnableStep} is a description of work, not the execution itself. Call
 * {@link #run()} or pass it to {@link Steps#step(RunnableStep)} to execute it inside an Allure
 * step.</p>
 */
public interface RunnableStep {
  /**
   * Returns the name shown for the step in the report.
   *
   * @return step name
   */
  String name();

  /**
   * Returns parameters attached to the Allure step.
   *
   * @return step parameters; implementations should expose stable iteration order
   */
  Map<String, Object> params();

  /**
   * Returns the action executed when the step runs.
   *
   * @return throwable action
   */
  ThRunnable<?> action();

  /** Executes this descriptor through the framework step lifecycle. */
  default void run() {
    Steps.step(this);
  }

  /** Default immutable {@link RunnableStep} implementation. */
  final class Of implements RunnableStep {
    /** Name written to Allure. */
    private final String name;
    /** Defensive, unmodifiable copy of parameters supplied at construction time. */
    private final Map<String, Object> params;
    /** Action executed by {@link Steps}. */
    private final ThRunnable<?> action;

    /**
     * Creates a step without report parameters.
     *
     * @param name step name
     * @param action action to execute
     */
    public Of(final String name, final ThRunnable<?> action) {
      this(name, Map.of(), action);
    }

    /**
     * Creates a step with report parameters.
     *
     * <p>The parameter map is copied immediately. Later changes to the caller's map therefore do
     * not change this step descriptor.</p>
     *
     * @param name step name
     * @param params parameters attached to the report step
     * @param action action to execute
     */
    public Of(final String name, final Map<String, Object> params, final ThRunnable<?> action) {
      this.name = name;
      this.params = Collections.unmodifiableMap(new LinkedHashMap<>(params));
      this.action = action;
    }

    /** {@inheritDoc} */
    @Override public String name() { return name; }

    /** {@inheritDoc} */
    @Override public Map<String, Object> params() { return params; }

    /** {@inheritDoc} */
    @Override public ThRunnable<?> action() { return action; }
  }
}
