package org.kulivatsky.autotests.core.util;

/**
 * Thread-safe mutable holder with an explicit uninitialized state.
 *
 * <p>Unlike {@link Val}, a {@code Var} may be written repeatedly. The separate initialization flag
 * allows null to be a legitimate stored value while an untouched holder still fails on read.</p>
 *
 * @param <T> stored value type
 */
public interface Var<T> {
  /**
   * Returns the current value.
   *
   * @return stored value, which may be null
   * @throws IllegalStateException when no value has been assigned yet
   */
  T get();

  /**
   * Replaces the current value and marks the holder initialized.
   *
   * @param value new value, including null
   */
  void set(T value);

  /**
   * Creates an uninitialized mutable holder.
   *
   * @param <T> stored value type
   * @return empty holder
   */
  static <T> Var<T> var() {
    return new Impl<>();
  }

  /**
   * Creates an initialized mutable holder.
   *
   * @param value initial value, including null
   * @param <T> stored value type
   * @return initialized holder
   */
  static <T> Var<T> var(final T value) {
    return new Impl<>(value);
  }

  /** Default synchronized {@link Var} implementation. */
  final class Impl<T> implements Var<T> {
    /** Monitor guarding the initialized/value pair. */
    private final Object lock;
    /** Distinguishes uninitialized from initialized-with-null. */
    private volatile boolean initialized;
    /** Current stored value. */
    private volatile T value;

    /** Creates an uninitialized holder. */
    public Impl() {
      this(new Object(), false, null);
    }

    /**
     * Creates an initialized holder.
     *
     * @param value initial value, including null
     */
    public Impl(final T value) {
      this(new Object(), true, value);
    }

    /**
     * Initializes all state explicitly.
     *
     * @param lock monitor used for compound state operations
     * @param initialized initial initialization flag
     * @param value initial stored value
     */
    private Impl(final Object lock,
                 final boolean initialized,
                 final T value) {
      this.lock = lock;
      this.initialized = initialized;
      this.value = value;
    }

    /** {@inheritDoc} */
    @Override
    public T get() {
      synchronized (this.lock) {
        if (this.initialized) {
          return this.value;
        }
        throw new IllegalStateException("Not initialized yet");
      }
    }

    /** {@inheritDoc} */
    @Override
    public void set(final T value) {
      synchronized (this.lock) {
        this.value = value;
        this.initialized = true;
      }
    }
  }
}
