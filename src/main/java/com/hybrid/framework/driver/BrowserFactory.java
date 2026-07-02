package com.hybrid.framework.driver;

import com.hybrid.framework.config.ConfigReader;
import com.hybrid.framework.utils.DownloadUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.chromium.ChromiumOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Factory pattern for creating browser-specific WebDriver instances.
 * <p>
 * Supports Chrome, Firefox, and Edge. Uses WebDriverManager for automatic
 * driver binary management when {@code remote.url} is unset. When
 * {@code remote.url} is set, connects to Selenium Grid via {@link RemoteWebDriver}.
 * Each browser can be launched in headless mode via the {@code headless} property.
 * </p>
 */
public final class BrowserFactory {

    private static final Logger LOG = LogManager.getLogger(BrowserFactory.class);

    private BrowserFactory() {
        // Utility class — no instantiation
    }

    /**
     * Creates and returns a fully configured WebDriver instance.
     *
     * @param browserName one of: chrome, firefox, edge (case-insensitive)
     * @return configured WebDriver
     */
    public static WebDriver createDriver(String browserName) {
        ConfigReader config = ConfigReader.getInstance();
        boolean headless = config.isHeadless();
        String normalizedBrowser = browserName.trim().toLowerCase();

        String remoteUrl = config.getProperty("remote.url", "").trim();
        if (!remoteUrl.isEmpty()) {
            WebDriver driver = createRemoteDriver(remoteUrl, normalizedBrowser, headless);
            configureDriver(driver, config);
            return driver;
        }

        WebDriver driver;
        switch (normalizedBrowser) {
            case "chrome": {
                driver = new ChromeDriver(buildChromeOptions(headless));
                LOG.info("Chrome browser launched [headless={}]", headless);
                break;
            }
            case "firefox": {
                driver = new FirefoxDriver(buildFirefoxOptions(headless));
                LOG.info("Firefox browser launched [headless={}]", headless);
                break;
            }
            case "edge": {
                driver = new EdgeDriver(buildEdgeOptions(headless));
                LOG.info("Edge browser launched [headless={}]", headless);
                break;
            }
            default: throw new IllegalArgumentException(
                    "Unsupported browser: " + browserName + ". Supported: chrome, firefox, edge.");
        }

        configureDriver(driver, config);
        return driver;
    }

    /**
     * Convenience overload — uses the browser name from config.properties.
     * if browser name is not set, it will use the browser name from config.properties.
     */
    public static WebDriver createDriver() {
        return createDriver(ConfigReader.getInstance().getBrowser());
    }

    // ──────────────────────────────────────────────────────────────
    // Remote Grid
    // ──────────────────────────────────────────────────────────────

    private static WebDriver createRemoteDriver(String remoteUrl, String browserName, boolean headless) {
        try {
            URL gridUrl = new URL(remoteUrl);
            WebDriver driver = switch (browserName) {
                case "chrome" -> new RemoteWebDriver(gridUrl, buildChromeOptions(headless));
                case "firefox" -> new RemoteWebDriver(gridUrl, buildFirefoxOptions(headless));
                case "edge" -> new RemoteWebDriver(gridUrl, buildEdgeOptions(headless));
                default -> throw new IllegalArgumentException(
                        "Unsupported browser: " + browserName + ". Supported: chrome, firefox, edge.");
            };
            LOG.info("{} browser launched via Selenium Grid [{}]", browserName, remoteUrl);
            return driver;
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Invalid remote.url: " + remoteUrl, e);
        }
    }

    // ──────────────────────────────────────────────────────────────
    // Browser options builders
    // ──────────────────────────────────────────────────────────────

    private static ChromeOptions buildChromeOptions(boolean headless) {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--start-maximized");
        options.addArguments("--disable-notifications");
        options.addArguments("--disable-popup-blocking");
        options.addArguments("--remote-allow-origins=*");
        enableBrowserConsoleLogging(options);
        options.setExperimentalOption("prefs", buildChromiumDownloadPrefs());
        if (headless) {
            options.addArguments("--headless=new");
            options.addArguments("--window-size=1920,1080");
        }
        return options;
    }

    private static FirefoxOptions buildFirefoxOptions(boolean headless) {
        FirefoxOptions options = new FirefoxOptions();
        String downloadDir = DownloadUtils.getDownloadDirectory().toString();
        options.addPreference("browser.download.folderList", 2);
        options.addPreference("browser.download.dir", downloadDir);
        options.addPreference("browser.download.useDownloadDir", true);
        options.addPreference("browser.download.manager.showWhenStarting", false);
        options.addPreference("browser.helperApps.neverAsk.saveToDisk",
                "application/octet-stream,application/pdf,text/plain,text/csv,image/jpeg,image/png");
        if (headless) {
            options.addArguments("-headless");
        }
        return options;
    }

    private static EdgeOptions buildEdgeOptions(boolean headless) {
        EdgeOptions options = new EdgeOptions();
        options.addArguments("--start-maximized");
        enableBrowserConsoleLogging(options);
        options.setExperimentalOption("prefs", buildChromiumDownloadPrefs());
        if (headless) {
            options.addArguments("--headless=new");
            options.addArguments("--window-size=1920,1080");
        }
        return options;
    }

    private static void enableBrowserConsoleLogging(ChromiumOptions<?> options) {
        if (!ConfigReader.getInstance().getBoolean("browser.console.log.on.fail", true)) {
            return;
        }
        options.setCapability("goog:loggingPrefs", Map.of("browser", "SEVERE"));
    }

    private static Map<String, Object> buildChromiumDownloadPrefs() {
        Map<String, Object> prefs = new HashMap<>();
        String downloadDir = DownloadUtils.getDownloadDirectory().toString();
        prefs.put("download.default_directory", downloadDir);
        prefs.put("download.prompt_for_download", false);
        prefs.put("download.directory_upgrade", true);
        prefs.put("safebrowsing.enabled", true);
        LOG.debug("Chromium download directory: {}", downloadDir);
        return prefs;
    }

    // ──────────────────────────────────────────────────────────────
    // Driver configuration
    // ──────────────────────────────────────────────────────────────

    private static void configureDriver(WebDriver driver, ConfigReader config) {
        int implicitWait = config.getImplicitWait();
        int pageLoadTimeout = config.getInt("page.load.timeout", 30);

        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(implicitWait));
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(pageLoadTimeout));
        if (!config.isHeadless()) {
            driver.manage().window().maximize();
        }

        LOG.debug("Driver timeouts configured — implicit: {}s, pageLoad: {}s",
                implicitWait, pageLoadTimeout);
    }
}
