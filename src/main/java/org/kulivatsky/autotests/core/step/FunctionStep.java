package org.kulivatsky.autotests.core.step;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Named test action that transforms an existing value into another value.
 *
 * @param <T> input type
 * @param <R> result type
 */
public interface FunctionStep<T, R> {
  /**
   * Returns the name shown for the step in the report.
   *
   * @return step name
   */
  String name();

  /**
   * Returns parameters attached to the Allure step.
   *
   * @return step parameters
   */
  Map<String, Object> params();

  /**
   * Returns the transformation executed by the step runner.
   *
   * @return throwable transformation
   */
  ThFunction<T, R, ?> action();

  /**
   * Executes this descriptor through {@link Steps} for the supplied value.
   *
   * @param value value passed to the transformation
   * @return transformation result
   */
  default R apply(final T value) {
    return Steps.step(this, value);
  }

  /**
   * Default immutable {@link FunctionStep} implementation.
   *
   * @param <T> input type
   * @param <R> result type
   */
  final class Of<T, R> implements FunctionStep<T, R> {
    /** Name written to Allure. */
    private final String name;
    /** Defensive, unmodifiable copy of parameters supplied at construction time. */
    private final Map<String, Object> params;
    /** Function executed by {@link Steps}. */
    private final ThFunction<T, R, ?> action;

    /**
     * Creates a transforming step without report parameters.
     *
     * @param name step name
     * @param action transformation to execute
     */
    public Of(final String name, final ThFunction<T, R, ?> action) {
      this(name, Map.of(), action);
    }

    /**
     * Creates a transforming step with report parameters.
     *
     * @param name step name
     * @param params parameters attached to the report step; copied on construction
     * @param action transformation to execute
     */
    public Of(final String name, final Map<String, Object> params, final ThFunction<T, R, ?> action) {
      this.name = name;
      this.params = Collections.unmodifiableMap(new LinkedHashMap<>(params));
      this.action = action;
    }

    /** {@inheritDoc} */
    @Override public String name() { return name; }

    /** {@inheritDoc} */
    @Override public Map<String, Object> params() { return params; }

    /** {@inheritDoc} */
    @Override public ThFunction<T, R, ?> action() { return action; }
  }
}
