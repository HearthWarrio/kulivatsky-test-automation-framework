package org.kulivatsky.autotests.core.storage;

import com.codeborne.selenide.SelenideDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.kulivatsky.autotests.core.listener.TestNGListener.currentTest;

/**
 * Thread-safe registry of explicitly created {@link SelenideDriver} instances by TestNG test id.
 *
 * <p>The registry is separate from Selenide's default static driver. It is used when a test creates
 * an additional driver and wants the framework listener to collect failure diagnostics and close it
 * after the test. Driver ownership transfers to this storage once {@link #put(SelenideDriver)} or
 * {@link #put(String, SelenideDriver)} succeeds.</p>
 */
public final class SelenideDriverStorage {
  /** Logger used when cleanup of a stored driver fails. */
  private static final Logger LOGGER = LoggerFactory.getLogger(SelenideDriverStorage.class);
  /** Drivers keyed by TestNG result id. */
  private static final Map<String, SelenideDriver> STORAGE = new ConcurrentHashMap<>();

  static {
    Runtime.getRuntime().addShutdownHook(new Thread(SelenideDriverStorage::closeAll, "selenide-driver-shutdown"));
  }

  /** No instances. */
  private SelenideDriverStorage() {
  }

  /**
   * Stores a driver under the id of the test currently running on this thread.
   *
   * @param driver driver whose lifecycle becomes managed by the storage
   * @throws java.util.NoSuchElementException when no TestNG test is bound to the current thread
   */
  public static void put(final SelenideDriver driver) {
    put(currentTest().id(), driver);
  }

  /**
   * Stores a driver under an explicit test id if that id is not already registered.
   *
   * <p>{@link Map#putIfAbsent(Object, Object)} means a second driver for the same id is not adopted
   * or closed here. Callers should not register multiple owned drivers under one id.</p>
   *
   * @param testId TestNG result id or another stable test-scoped key
   * @param driver driver to register
   */
  public static void put(final String testId, final SelenideDriver driver) {
    STORAGE.putIfAbsent(testId, driver);
  }

  /**
   * Returns the driver registered for the current TestNG test.
   *
   * @return stored driver, or {@code null} when the current test has no explicit driver
   * @throws java.util.NoSuchElementException when no TestNG test is bound to the current thread
   */
  public static SelenideDriver get() {
    return get(currentTest().id());
  }

  /**
   * Returns the driver registered under an explicit id.
   *
   * @param testId storage key
   * @return stored driver, or {@code null} when no mapping exists
   */
  public static SelenideDriver get(final String testId) {
    return STORAGE.get(testId);
  }

  /**
   * Removes and closes the driver registered under an id.
   *
   * @param testId storage key
   */
  public static void close(final String testId) {
    final SelenideDriver driver = STORAGE.remove(testId);
    closeDriver(testId, driver);
  }

  /**
   * Closes every registered driver and clears the registry.
   *
   * <p>Individual close failures are logged by {@link #closeDriver(String, SelenideDriver)} and do
   * not stop cleanup of the remaining drivers.</p>
   */
  public static void closeAll() {
    STORAGE.forEach(SelenideDriverStorage::closeDriver);
    STORAGE.clear();
  }

  /**
   * Closes one driver without allowing cleanup failure to mask the test result or block shutdown.
   *
   * @param testId id used only for diagnostic logging
   * @param driver driver to close; {@code null} is accepted as a no-op
   */
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
