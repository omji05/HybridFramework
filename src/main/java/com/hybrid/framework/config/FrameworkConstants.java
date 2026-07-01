package com.hybrid.framework.config;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Centralized, immutable constants for the framework.
 * All paths and magic strings are defined here to avoid duplication.
 */
public final class FrameworkConstants {

    private FrameworkConstants() {
        // Utility class — no instantiation
    }

    // ──────────────────────────────────────────────────────────────
    // Project paths
    // ──────────────────────────────────────────────────────────────

    public static final String PROJECT_ROOT = System.getProperty("user.dir");
    public static final Path RESOURCES_PATH = Paths.get(PROJECT_ROOT, "src", "test", "resources");
    public static final Path CONFIG_FILE = RESOURCES_PATH.resolve("config.properties");
    public static final Path TESTDATA_DIR = RESOURCES_PATH.resolve("testdata");
    public static final Path PAYLOADS_DIR = RESOURCES_PATH.resolve("payloads");
    public static final Path SCHEMAS_DIR = RESOURCES_PATH.resolve("schemas");
    public static final Path FEATURES_DIR = RESOURCES_PATH.resolve("features");

    // ──────────────────────────────────────────────────────────────
    // Report paths
    // ──────────────────────────────────────────────────────────────

    public static final String EXTENT_REPORT_DIR = PROJECT_ROOT + "/target/reports/extent/";
    public static final String EXTENT_REPORT_FILE = EXTENT_REPORT_DIR + "ExtentReport.html";
    public static final String SCREENSHOT_DIR = PROJECT_ROOT + "/target/reports/screenshots/";
    public static final String DOWNLOAD_DIR = PROJECT_ROOT + "/target/downloads/";
    public static final String ALLURE_RESULTS_DIR = PROJECT_ROOT + "/target/allure-results/";

    // ──────────────────────────────────────────────────────────────
    // Timeouts (seconds)
    // ──────────────────────────────────────────────────────────────

    public static final int DEFAULT_IMPLICIT_WAIT = 10;
    public static final int DEFAULT_EXPLICIT_WAIT = 15;
    public static final int DEFAULT_PAGE_LOAD_TIMEOUT = 30;
    public static final int FLUENT_WAIT_TIMEOUT = 20;
    public static final int FLUENT_WAIT_POLLING = 2;

    // ──────────────────────────────────────────────────────────────
    // Retry
    // ──────────────────────────────────────────────────────────────

    public static final int MAX_RETRY_COUNT = 2;

    // ──────────────────────────────────────────────────────────────
    // Security
    // ──────────────────────────────────────────────────────────────

    public static final String AES_SECRET_KEY = "HybridFrmwk@2026"; // 16-char key for AES-128

    // ──────────────────────────────────────────────────────────────
    // Date / Report metadata
    // ──────────────────────────────────────────────────────────────

    public static final String DATE_FORMAT = "yyyy-MM-dd_HH-mm-ss";
    public static final String REPORT_TITLE = "Hybrid Automation Report";
    public static final String REPORT_NAME = "Test Execution Report";
}
