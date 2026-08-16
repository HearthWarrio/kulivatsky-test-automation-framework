package org.kulivatsky.autotests.core.util;

/**
 * Thread-safe write-once holder.
 *
 * <p>A {@code Val} starts either initialized or uninitialized. The first successful
 * {@link #set(Object)} initializes an empty holder; every later write fails. Unlike using null as a
 * sentinel, a holder may be intentionally initialized with null.</p>
 *
 * @param <T> stored value type
 */
public interface Val<T> {
  /**
   * Returns the initialized value.
   *
   * @return stored value, which may be null
   * @throws IllegalStateException when the holder has not been initialized
   */
  T get();

  /**
   * Initializes the holder exactly once.
   *
   * @param value value to store; null is allowed
   * @throws IllegalStateException when a value has already been set or supplied at construction
   */
  void set(T value);

  /**
   * Creates an uninitialized write-once holder.
   *
   * @param <T> stored value type
   * @return empty holder
   */
  static <T> Val<T> val() {
    return new Impl<>();
  }

  /**
   * Creates an already initialized write-once holder.
   *
   * @param value initial value, including null
   * @param <T> stored value type
   * @return initialized holder
   */
  static <T> Val<T> val(final T value) {
    return new Impl<>(value);
  }

  /** Default synchronized {@link Val} implementation. */
  final class Impl<T> implements Val<T> {
    /** Monitor guarding the initialized/value pair. */
    private final Object lock;
    /** Distinguishes uninitialized from initialized-with-null. */
    private volatile boolean initialized;
    /** Stored value. */
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
        if (this.initialized) {
          throw new IllegalStateException("Already initialized");
        }
        this.value = value;
        this.initialized = true;
      }
    }
  }
}
