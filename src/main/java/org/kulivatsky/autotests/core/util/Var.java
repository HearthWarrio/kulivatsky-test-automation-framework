package org.kulivatsky.autotests.core.util;

public interface Var<T> {

  T get();

  void set(T value);

  static <T> Var<T> var() {
    return new Impl<>();
  }

  static <T> Var<T> var(final T value) {
    return new Impl<>(value);
  }

  final class Impl<T> implements Var<T> {
    private final Object lock;
    private volatile boolean initialized;
    private volatile T value;

    public Impl() {
      this(new Object(), false, null);
    }

    public Impl(final T value) {
      this(new Object(), true, value);
    }

    private Impl(final Object lock,
                 final boolean initialized,
                 final T value) {
      this.lock = lock;
      this.initialized = initialized;
      this.value = value;
    }

    @Override
    public T get() {
      synchronized (this.lock) {
        if (this.initialized) {
          return this.value;
        }
        throw new IllegalStateException("Not initialized yet");
      }
    }

    @Override
    public void set(final T value) {
      synchronized (this.lock) {
        this.value = value;
        this.initialized = true;
      }
    }
  }
}
