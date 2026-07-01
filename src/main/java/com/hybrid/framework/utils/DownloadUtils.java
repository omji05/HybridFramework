package com.hybrid.framework.utils;

import com.hybrid.framework.config.ConfigReader;
import com.hybrid.framework.config.FrameworkConstants;
import com.hybrid.framework.driver.DriverManager;
import io.qameta.allure.Step;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.FluentWait;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.stream.Stream;

/**
 * Browser download verification — waits for files by name in the thread-local download directory.
 * <p>
 * Each parallel thread writes to its own subfolder under {@code download.dir} so downloads
 * do not collide during concurrent execution.
 * </p>
 */
public final class DownloadUtils {

    private static final Logger LOG = LogManager.getLogger(DownloadUtils.class);

    private static final String CHROME_PARTIAL_SUFFIX = ".crdownload";
    private static final String FIREFOX_PARTIAL_SUFFIX = ".part";

    private DownloadUtils() {
        // Utility class — no instantiation
    }
 
    /**
     * Returns the download directory for the current thread, creating it if needed.
     */
    public static Path getDownloadDirectory() {
        String baseDir = ConfigReader.getInstance()
                .getProperty("download.dir", FrameworkConstants.DOWNLOAD_DIR);
        Path dir = Path.of(baseDir, sanitizeThreadName(Thread.currentThread().getName()))
                .toAbsolutePath();
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new IllegalStateException("Could not create download directory: " + dir, e);
        }
        return dir;
    }

    /**
     * Creates the thread download directory and removes any files left from a prior run.
     */
    public static void prepareDownloadDirectory() {
        Path dir = getDownloadDirectory();
        if (!Files.isDirectory(dir)) {
            return;
        }
        try (Stream<Path> entries = Files.list(dir)) {
            entries.forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException e) {
                    LOG.warn("Could not delete download artifact {}: {}", path, e.getMessage());
                }
            });
        } catch (IOException e) {
            LOG.warn("Could not clear download directory {}: {}", dir, e.getMessage());
        }
        LOG.debug("Download directory prepared: {}", dir);
    }

    /**
     * Checks whether a file with the given name exists and is fully downloaded.
     */
    public static boolean isFileDownloaded(String fileName) {
        Path file = getDownloadDirectory().resolve(fileName);
        return Files.isRegularFile(file) && !hasPartialDownload(fileName);
    }

    /**
     * Waits until the named file is fully downloaded, using the configured timeout.
     *
     * @return absolute path to the downloaded file
     * @throws TimeoutException if the file does not appear in time
     */
    @Step("Wait for download: {fileName}")
    public static Path waitForDownload(String fileName) {
        int timeoutSeconds = ConfigReader.getInstance().getInt("download.wait.timeout", 30);
        return waitForDownload(fileName, timeoutSeconds);
    }

    /**
     * Waits until the named file is fully downloaded.
     *
     * @return absolute path to the downloaded file
     * @throws TimeoutException if the file does not appear in time
     */
    @Step("Wait for download: {fileName} (timeout {timeoutSeconds}s)")
    public static Path waitForDownload(String fileName, int timeoutSeconds) {
        Path downloadDir = getDownloadDirectory();
        Path target = downloadDir.resolve(fileName);

        LOG.info("Waiting up to {}s for download: {}", timeoutSeconds, target);

        WebDriver driver = DriverManager.getDriver();
        new FluentWait<>(driver)
                .withTimeout(Duration.ofSeconds(timeoutSeconds))
                .pollingEvery(Duration.ofMillis(500))
                .withMessage("File was not downloaded within " + timeoutSeconds + "s: " + fileName)
                .until(d -> isFileDownloaded(fileName) && hasNonZeroSize(target));

        LOG.info("Download verified: {}", target);
        return target;
    }

    private static boolean hasPartialDownload(String fileName) {
        Path downloadDir = getDownloadDirectory();
        return Files.exists(downloadDir.resolve(fileName + CHROME_PARTIAL_SUFFIX))
                || Files.exists(downloadDir.resolve(fileName + FIREFOX_PARTIAL_SUFFIX));
    }

    private static boolean hasNonZeroSize(Path file) {
        try {
            return Files.size(file) > 0;
        } catch (IOException e) {
            return false;
        }
    }

    private static String sanitizeThreadName(String threadName) {
        return threadName.replaceAll("[^a-zA-Z0-9_-]", "_");
    }
}
