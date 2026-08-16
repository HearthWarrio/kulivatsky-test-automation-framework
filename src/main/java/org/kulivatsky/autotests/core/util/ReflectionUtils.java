package org.kulivatsky.autotests.core.util;

import java.lang.reflect.InvocationTargetException;
import java.util.Objects;

/** Constructs framework objects through a declared zero-argument constructor. */
public final class ReflectionUtils {
  /** No instances. */
  private ReflectionUtils() {
  }

  /**
   * Creates an instance by invoking the class's declared zero-argument constructor.
   *
   * <p>This method does not call {@code setAccessible(true)}. Private or otherwise inaccessible
   * constructors therefore fail instead of bypassing Java access control.</p>
   *
   * @param clazz class to instantiate
   * @param <T> instance type
   * @return newly constructed instance
   * @throws NullPointerException when {@code clazz} is null
   * @throws IllegalArgumentException when no zero-argument constructor exists, access is denied,
   *                                  the type cannot be instantiated, or the constructor throws
   */
  public static <T> T createInstanceByDefaultConstructor(final Class<T> clazz) {
    Objects.requireNonNull(clazz, "clazz");
    try {
      return clazz.getDeclaredConstructor().newInstance();
    } catch (final InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException ex) {
      throw new IllegalArgumentException("Unable to instantiate " + clazz.getName() + " using its default constructor", ex);
    }
  }
}
