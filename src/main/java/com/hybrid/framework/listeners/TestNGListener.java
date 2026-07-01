package com.hybrid.framework.listeners;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.ISuite;
import org.testng.ISuiteListener;

/**
 * Lightweight TestNG suite listener for BDD execution.
 * <p>
 * Scenario pass/fail logging and screenshots are handled by {@link com.hybrid.framework.stepdefs.Hooks}.
 * Extent step reporting is handled by {@link com.hybrid.framework.reporting.ExtentCucumberPlugin}.
 * Allure reporting is handled by {@code AllureCucumber7Jvm} and {@code ScreenshotUtils}.
 * </p>
 */
public class TestNGListener implements ISuiteListener {

    private static final Logger LOG = LogManager.getLogger(TestNGListener.class);

    @Override
    public void onStart(ISuite suite) {
        LOG.info("═══ Suite started: {} ═══", suite.getName());
    }

    @Override
    public void onFinish(ISuite suite) {
        LOG.info("═══ Suite finished: {} ═══", suite.getName());
    }
}
