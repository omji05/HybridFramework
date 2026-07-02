package com.hybrid.framework.stepdefs;

import com.hybrid.framework.api.ApiUtils;
import com.hybrid.framework.driver.DriverManager;
import com.hybrid.framework.utils.BrowserLogUtils;
import com.hybrid.framework.utils.DownloadUtils;
import com.hybrid.framework.utils.ScreenshotUtils;
import com.hybrid.framework.wiremock.WireMockSupport;
import io.cucumber.java.After;
import io.cucumber.java.AfterAll;
import io.cucumber.java.Before;
import io.cucumber.java.BeforeAll;
import io.cucumber.java.Scenario;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

/**
 * Cucumber Hooks — lifecycle management for BDD scenarios.
 * <p>
 * PicoContainer injects dependencies per scenario; {@link com.hybrid.framework.context.TestContext}
 * is discarded automatically after each scenario (no static reset required).
 * </p>
 */
public class Hooks {

    private static final Logger LOG = LogManager.getLogger(Hooks.class);

    //executes for all but guarded by wireMockEnabled()
    @BeforeAll
    public static void startWireMockSuite() {
        if (wireMockEnabled()) {
            LOG.info("Starting WireMock (suite-level, parallel-safe)");
            WireMockSupport.startOnce();
        }
    }

    @AfterAll
    public static void stopWireMockSuite() {
        if (wireMockEnabled()) {
            LOG.info("Stopping WireMock (suite-level)");
            WireMockSupport.stopOnce();
        }
    }

    @Before(order = 0)
    public void setLogContext(Scenario scenario) {
        ThreadContext.put("thread", Thread.currentThread().getName());
        ThreadContext.put("test", scenario.getName());
    }

    @Before(value = "@ui", order = 1)
    public void initBrowserForUI(Scenario scenario) {
        LOG.info("──── Scenario [UI]: {} ────", scenario.getName());
        DownloadUtils.prepareDownloadDirectory();
        DriverManager.initDriver();
    }

    @Before(value = "@api", order = 1)
    public void initForAPI(Scenario scenario) {
        LOG.info("──── Scenario [API]: {} ────", scenario.getName());
    }

    @Before(value = "@wiremock", order = 1)
    public void initForWireMock(Scenario scenario) {
        if (!wireMockEnabled() || !WireMockSupport.isRunning()) {
            throw new IllegalStateException(
                    "@wiremock scenario requires -Pmock (sets wiremock.enabled=true)");
        }
        LOG.info("──── Scenario [WireMock]: {} ────", scenario.getName());
    }

    @Before(value = "@hybrid", order = 2)
    public void hybridDataSetup(Scenario scenario) {
        LOG.info("Hybrid data setup for scenario: {}", scenario.getName());
    }

    @After(order = 1)
    public void tearDown(Scenario scenario) {
        if (scenario.isFailed()) {
            LOG.error("Scenario FAILED: {}", scenario.getName());

            if (DriverManager.hasDriver()) {
                byte[] screenshot = ScreenshotUtils.captureAsBytes();
                if (screenshot.length > 0) {
                    scenario.attach(screenshot, "image/png", "Failure Screenshot");
                }
                ScreenshotUtils.attachToAllure("Failure: " + scenario.getName());

                String consoleLogs = BrowserLogUtils.captureAsText();
                if (!consoleLogs.isEmpty()) {
                    scenario.attach(consoleLogs, "text/plain", "Browser Console");
                }
                BrowserLogUtils.attachToAllure("Browser Console: " + scenario.getName());
            }
        } else {
            LOG.info("Scenario PASSED: {}", scenario.getName());
        }

        if (DriverManager.hasDriver()) {
            DriverManager.quitDriver();
        }

        ApiUtils.clearAuthentication();
        ThreadContext.clearMap();
    }

    //wiremock.enabled is set to true in the pom.xml file when mock profile is run
    //by default it is false
    private static boolean wireMockEnabled() {
        return Boolean.parseBoolean(System.getProperty("wiremock.enabled", "false"));
    }
}
