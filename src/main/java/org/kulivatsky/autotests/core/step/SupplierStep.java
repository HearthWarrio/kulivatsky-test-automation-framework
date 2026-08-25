package org.kulivatsky.autotests.core.step;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Named test action that produces a value and requires no input.
 *
 * @param <R> value produced by the step
 */
public interface SupplierStep<R> {
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
   * Returns the value-producing action executed by the step runner.
   *
   * @return throwable supplier
   */
  ThSupplier<R, ?> action();

  /**
   * Executes this descriptor through {@link Steps} and returns its value.
   *
   * @return value produced by the action
   */
  default R get() {
    return Steps.step(this);
  }

  /**
   * Default immutable {@link SupplierStep} implementation.
   *
   * @param <R> value produced by the step
   */
  final class Of<R> implements SupplierStep<R> {
    /** Name written to Allure. */
    private final String name;
    /** Defensive, unmodifiable copy of parameters supplied at construction time. */
    private final Map<String, Object> params;
    /** Supplier executed by {@link Steps}. */
    private final ThSupplier<R, ?> action;

    /**
     * Creates a value-producing step without report parameters.
     *
     * @param name step name
     * @param action supplier to execute
     */
    public Of(final String name, final ThSupplier<R, ?> action) {
      this(name, Map.of(), action);
    }

    /**
     * Creates a value-producing step with report parameters.
     *
     * @param name step name
     * @param params parameters attached to the report step; copied on construction
     * @param action supplier to execute
     */
    public Of(final String name, final Map<String, Object> params, final ThSupplier<R, ?> action) {
      this.name = name;
      this.params = Collections.unmodifiableMap(new LinkedHashMap<>(params));
      this.action = action;
    }

    /** {@inheritDoc} */
    @Override public String name() { return name; }

    /** {@inheritDoc} */
    @Override public Map<String, Object> params() { return params; }

    /** {@inheritDoc} */
    @Override public ThSupplier<R, ?> action() { return action; }
  }
}
