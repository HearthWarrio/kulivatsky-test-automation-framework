package org.kulivatsky.autotests.core.util;

import java.lang.reflect.InvocationTargetException;
import java.util.Objects;

public final class ReflectionUtils {
  private ReflectionUtils() {
  }

  public static <T> T createInstanceByDefaultConstructor(final Class<T> clazz) {
    Objects.requireNonNull(clazz, "clazz");
    try {
      return clazz.getDeclaredConstructor().newInstance();
    } catch (final InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException ex) {
      throw new IllegalArgumentException("Unable to instantiate " + clazz.getName() + " using its default constructor", ex);
    }
  }
}
