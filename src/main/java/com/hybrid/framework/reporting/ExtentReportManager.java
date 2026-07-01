package com.hybrid.framework.reporting;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;
import com.hybrid.framework.config.ConfigReader;
import com.hybrid.framework.config.FrameworkConstants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Minimal Extent Reports holder for BDD execution.
 * <p>
 * Lifecycle is driven by {@link ExtentCucumberPlugin} (init on first Cucumber run,
 * flush when that run finishes). Allure and Cucumber HTML/JSON reports are unaffected.
 * </p>
 */
public final class ExtentReportManager {

    private static final Logger LOG = LogManager.getLogger(ExtentReportManager.class);
    private static ExtentReports extentReports;

    private ExtentReportManager() {
    }

    public static boolean isEnabled() {
        return ConfigReader.getInstance().getBoolean("extent.report.enabled", true);
    }

    public static synchronized ExtentReports getReports() {
        if (!isEnabled()) {
            return null;
        }
        if (extentReports == null) {
            String timestamp = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern(FrameworkConstants.DATE_FORMAT));
            String reportDir = FrameworkConstants.EXTENT_REPORT_DIR;
            new File(reportDir).mkdirs();

            String reportPath = reportDir + "ExtentReport_" + timestamp + ".html";

            ExtentSparkReporter sparkReporter = new ExtentSparkReporter(reportPath);
            sparkReporter.config().setTheme(Theme.STANDARD);
            sparkReporter.config().setDocumentTitle(FrameworkConstants.REPORT_TITLE);
            sparkReporter.config().setReportName(FrameworkConstants.REPORT_NAME);
            sparkReporter.config().setTimeStampFormat("yyyy-MM-dd HH:mm:ss");

            extentReports = new ExtentReports();
            extentReports.attachReporter(sparkReporter);
            extentReports.setSystemInfo("OS", System.getProperty("os.name"));
            extentReports.setSystemInfo("Java Version", System.getProperty("java.version"));
            extentReports.setSystemInfo("Browser", ConfigReader.getInstance().getBrowser());
            extentReports.setSystemInfo("Environment",
                    ConfigReader.getInstance().getProperty("environment", "QA"));

            LOG.info("Extent Report: {}", reportPath);
        }
        return extentReports;
    }

    public static synchronized void flushReports() {
        if (extentReports != null) {
            extentReports.flush();
        }
    }
}
