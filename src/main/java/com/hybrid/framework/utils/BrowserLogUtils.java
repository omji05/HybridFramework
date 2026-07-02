package com.hybrid.framework.utils;

import com.hybrid.framework.config.ConfigReader;
import com.hybrid.framework.driver.DriverManager;
import io.qameta.allure.Allure;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.logging.LogEntries;
import org.openqa.selenium.logging.LogEntry;
import org.openqa.selenium.logging.LogType;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Browser console log capture for UI test failures.
 * <p>
 * Chromium browsers require {@code goog:loggingPrefs} at launch
 * ({@link com.hybrid.framework.driver.BrowserFactory}). Respects
 * {@code browser.console.log.on.fail} in config.properties.
 * </p>
 */
public final class BrowserLogUtils {

    private static final Logger LOG = LogManager.getLogger(BrowserLogUtils.class);
    private static final DateTimeFormatter TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS").withZone(ZoneId.systemDefault());

    private BrowserLogUtils() {
        // Utility class — no instantiation
    }

    public static boolean isEnabled() {
        return ConfigReader.getInstance().getBoolean("browser.console.log.on.fail", true);
    }

    /**
     * Reads browser console logs from the current WebDriver session.
     *
     * @return formatted log text, or empty string when disabled or unavailable
     */
    public static String captureAsText() {
        if (!isEnabled() || !DriverManager.hasDriver()) {
            return "";
        }

        try {
            WebDriver driver = DriverManager.getDriver();
            if (!driver.manage().logs().getAvailableLogTypes().contains(LogType.BROWSER)) {
                LOG.debug("Browser console logs not available for this driver");
                return "";
            }

            LogEntries entries = driver.manage().logs().get(LogType.BROWSER);
            if (entries == null) {
                return "";
            }

            StringBuilder formatted = new StringBuilder();
            for (LogEntry entry : entries) {
                formatted.append('[')
                        .append(TIMESTAMP.format(Instant.ofEpochMilli(entry.getTimestamp())))
                        .append("] ")
                        .append(entry.getLevel())
                        .append(' ')
                        .append(entry.getMessage())
                        .append(System.lineSeparator());
            }
            return formatted.toString();
        } catch (Exception e) {
            LOG.warn("Failed to capture browser console logs: {}", e.getMessage());
            return "";
        }
    }

    /**
     * Captures and attaches browser console logs to the current Allure report.
     */
    public static void attachToAllure(String name) {
        String logs = captureAsText();
        if (!logs.isEmpty()) {
            Allure.addAttachment(
                    name,
                    "text/plain",
                    new ByteArrayInputStream(logs.getBytes(StandardCharsets.UTF_8)),
                    ".txt");
            LOG.debug("Browser console logs attached to Allure: {}", name);
        }
    }
}
