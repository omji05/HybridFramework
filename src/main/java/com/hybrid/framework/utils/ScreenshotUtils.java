package com.hybrid.framework.utils;

import com.hybrid.framework.config.ConfigReader;
import com.hybrid.framework.config.FrameworkConstants;
import com.hybrid.framework.driver.DriverManager;
import io.qameta.allure.Allure;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Screenshot capture utility.
 * <p>
 * Captures screenshots as files (for Extent Reports) and as byte arrays
 * (for Allure attachments). Respects the {@code screenshot.enabled} flag
 * in config.properties.
 * </p>
 */
public final class ScreenshotUtils {

    private static final Logger LOG = LogManager.getLogger(ScreenshotUtils.class);

    private ScreenshotUtils() {
        // Utility class — no instantiation
    }

    /**
     * Checks whether screenshots are globally enabled.
     */
    public static boolean isEnabled() {
        return ConfigReader.getInstance().getBoolean("screenshot.enabled", true);
    }

    /**
     * Captures a screenshot and saves it to the reports/screenshots directory.
     *
     * @param testName used in the filename
     * @return the absolute path to the saved screenshot, or null if disabled
     */
    public static String captureScreenshot(String testName) {
        if (!isEnabled()) {
            LOG.debug("Screenshots disabled — skipping capture");
            return null;
        }

        if (!DriverManager.hasDriver()) {
            LOG.warn("No WebDriver available for screenshot capture");
            return null;
        }

        try {
            WebDriver driver = DriverManager.getDriver();
            File source = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);

            String timestamp = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern(FrameworkConstants.DATE_FORMAT));
            String fileName = sanitize(testName) + "_" + timestamp + ".png";

            Path targetDir = Paths.get(FrameworkConstants.SCREENSHOT_DIR);
            Files.createDirectories(targetDir);

            Path targetPath = targetDir.resolve(fileName);
            Files.copy(source.toPath(), targetPath);

            LOG.info("Screenshot saved: {}", targetPath.toAbsolutePath());
            return targetPath.toAbsolutePath().toString();
        } catch (IOException e) {
            LOG.error("Failed to save screenshot: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Captures a screenshot as a Base64 string (useful for Extent Reports).
     */
    public static String captureAsBase64() {
        if (!DriverManager.hasDriver()) return null;
        try {
            return ((TakesScreenshot) DriverManager.getDriver())
                    .getScreenshotAs(OutputType.BASE64);
        } catch (Exception e) {
            LOG.error("Failed to capture Base64 screenshot: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Captures a screenshot as bytes (useful for Allure attachments).
     */
    public static byte[] captureAsBytes() {
        if (!DriverManager.hasDriver()) return new byte[0];
        try {
            return ((TakesScreenshot) DriverManager.getDriver())
                    .getScreenshotAs(OutputType.BYTES);
        } catch (Exception e) {
            LOG.error("Failed to capture byte[] screenshot: {}", e.getMessage());
            return new byte[0];
        }
    }

    /**
     * Captures and attaches a screenshot to the current Allure report.
     */
    public static void attachToAllure(String name) {
        byte[] screenshot = captureAsBytes();
        if (screenshot.length > 0) {
            Allure.addAttachment(name, "image/png", new ByteArrayInputStream(screenshot), ".png");
            LOG.debug("Screenshot attached to Allure: {}", name);
        }
    }

    // ──────────────────────────────────────────────────────────────

    private static String sanitize(String name) {
        return name.replaceAll("[^a-zA-Z0-9_-]", "_");
    }
}
