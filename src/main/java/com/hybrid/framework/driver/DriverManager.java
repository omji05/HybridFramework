package com.hybrid.framework.driver;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;

/**
 * Thread-safe WebDriver lifecycle manager using ThreadLocal.
 * <p>
 * Each thread in a parallel execution gets its own isolated WebDriver instance.
 * This prevents cross-thread contamination and makes the framework safe for
 * concurrent test execution.
 * </p>
 *
 * <b>Usage pattern:</b>
 * <pre>
 *   DriverManager.initDriver("chrome");
 *   WebDriver driver = DriverManager.getDriver();
 *   // ... test execution ...
 *   DriverManager.quitDriver();
 * </pre>
 * Use {@link #setDriver(WebDriver)} when binding a pre-built driver (e.g. remote grid).
 */
public final class DriverManager {

    private static final Logger LOG = LogManager.getLogger(DriverManager.class);
    private static final ThreadLocal<WebDriver> DRIVER_THREAD_LOCAL = new ThreadLocal<>();

    private DriverManager() {
        // Utility class — no instantiation
    }

    // ──────────────────────────────────────────────────────────────
    // Public API
    // ──────────────────────────────────────────────────────────────

    /**
     * Returns the WebDriver instance bound to the current thread.
     *
     * @return WebDriver for the calling thread
     * @throws IllegalStateException if no driver has been initialized for this thread
     */
    public static WebDriver getDriver() {
        WebDriver driver = DRIVER_THREAD_LOCAL.get();
        if (driver == null) {
            throw new IllegalStateException(
                    "WebDriver has not been initialized for thread: " + Thread.currentThread().getName()
                            + ". Call DriverManager.initDriver() or setDriver() first.");
        }
        return driver;
    }

    /**
     * Creates a WebDriver via {@link BrowserFactory} and binds it to the current thread.
     *
     * @param browserName one of: chrome, firefox, edge (case-insensitive)
     */
    public static void initDriver(String browserName) {
        setDriver(BrowserFactory.createDriver(browserName));
    }

    /**
     * Creates a WebDriver using the browser from config.properties and binds it to the current thread.
     */
    public static void initDriver() {
        setDriver(BrowserFactory.createDriver());
    }

    /**
     * Binds a WebDriver instance to the current thread.
     *
     * @param driver the WebDriver to bind
     */
    public static void setDriver(WebDriver driver) {
        if (driver == null) {
            throw new IllegalArgumentException("WebDriver instance cannot be null.");
        }
        DRIVER_THREAD_LOCAL.set(driver);
        LOG.debug("WebDriver set for thread: {} [{}]",
                Thread.currentThread().getName(), driver.getClass().getSimpleName());
    }

    /**
     * Quits the WebDriver and removes the ThreadLocal reference,
     * preventing memory leaks.
     */
    public static void quitDriver() {
        WebDriver driver = DRIVER_THREAD_LOCAL.get();
        if (driver != null) {
            LOG.info("Quitting WebDriver for thread: {}", Thread.currentThread().getName());
            try {
                driver.quit();
            } catch (Exception e) {
                LOG.warn("Exception while quitting WebDriver: {}", e.getMessage());
            } finally {
                DRIVER_THREAD_LOCAL.remove();
            }
        }
    }

    /**
     * Checks whether a WebDriver is currently available for this thread.
     */
    public static boolean hasDriver() {
        return DRIVER_THREAD_LOCAL.get() != null;
    }
}
