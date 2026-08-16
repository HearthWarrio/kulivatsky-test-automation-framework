package org.kulivatsky.autotests.core.step;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Named test action that consumes an existing value and returns nothing.
 *
 * @param <T> value consumed by the step
 */
public interface ConsumerStep<T> {
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
   * Returns the action executed with the context value.
   *
   * @return throwable consumer
   */
  ThConsumer<T, ?> action();

  /**
   * Executes this descriptor through {@link Steps} for the supplied value.
   *
   * @param value value passed to the action
   */
  default void accept(final T value) {
    Steps.step(this, value);
  }

  /** Default immutable {@link ConsumerStep} implementation. */
  final class Of<T> implements ConsumerStep<T> {
    /** Name written to Allure. */
    private final String name;
    /** Defensive, unmodifiable copy of parameters supplied at construction time. */
    private final Map<String, Object> params;
    /** Consumer executed by {@link Steps}. */
    private final ThConsumer<T, ?> action;

    /**
     * Creates a consuming step without report parameters.
     *
     * @param name step name
     * @param action consumer to execute
     */
    public Of(final String name, final ThConsumer<T, ?> action) {
      this(name, Map.of(), action);
    }

    /**
     * Creates a consuming step with report parameters.
     *
     * @param name step name
     * @param params parameters attached to the report step; copied on construction
     * @param action consumer to execute
     */
    public Of(final String name, final Map<String, Object> params, final ThConsumer<T, ?> action) {
      this.name = name;
      this.params = Collections.unmodifiableMap(new LinkedHashMap<>(params));
      this.action = action;
    }

    /** {@inheritDoc} */
    @Override public String name() { return name; }

    /** {@inheritDoc} */
    @Override public Map<String, Object> params() { return params; }

    /** {@inheritDoc} */
    @Override public ThConsumer<T, ?> action() { return action; }
  }
}
