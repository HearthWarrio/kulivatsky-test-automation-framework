package org.kulivatsky.autotests.core.storage;

import com.codeborne.selenide.SelenideDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.kulivatsky.autotests.core.listener.TestNGListener.currentTest;

public final class SelenideDriverStorage {
  private static final Logger LOGGER = LoggerFactory.getLogger(SelenideDriverStorage.class);
  private static final Map<String, SelenideDriver> STORAGE = new ConcurrentHashMap<>();

  static {
    Runtime.getRuntime().addShutdownHook(new Thread(SelenideDriverStorage::closeAll, "selenide-driver-shutdown"));
  }

  private SelenideDriverStorage() {
  }

  public static void put(final SelenideDriver driver) {
    put(currentTest().id(), driver);
  }

  public static void put(final String testId, final SelenideDriver driver) {
    STORAGE.putIfAbsent(testId, driver);
  }

  public static SelenideDriver get() {
    return get(currentTest().id());
  }

  public static SelenideDriver get(final String testId) {
    return STORAGE.get(testId);
  }

  public static void close(final String testId) {
    final SelenideDriver driver = STORAGE.remove(testId);
    closeDriver(testId, driver);
  }

  public static void closeAll() {
    STORAGE.forEach(SelenideDriverStorage::closeDriver);
    STORAGE.clear();
  }

  private static void closeDriver(final String testId, final SelenideDriver driver) {
    if (driver == null) {
      return;
    }
    try {
      driver.close();
    } catch (final RuntimeException ex) {
      LOGGER.warn("Unable to close Selenide driver for test {}", testId, ex);
    }
  }
}
