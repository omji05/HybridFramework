package com.hybrid.framework.runners;

import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;
import org.testng.annotations.DataProvider;

/**
 * Cucumber TestNG runner — entry point for BDD test execution.
 * <p>
 * Configured for:
 * <ul>
 *   <li>Feature files in {@code src/test/resources/features}</li>
 *   <li>Step definitions in {@code com.hybrid.framework.stepdefs}</li>
 *   <li>Allure reporting plugin</li>
 *   <li>Parallel scenario execution via overridden DataProvider</li>
 * </ul>
 * </p>
 *
 * <b>Run specific tags:</b>
 * <pre>
 *   mvn test -Psmoke
 *   mvn test -Dbdd.tags="@api and not @regression"
 *   mvn test -Dcucumber.filter.tags="@smoke"
 * </pre>
 * The {@code bdd.tags} Maven property is mapped to the official
 * Cucumber system property {@code cucumber.filter.tags} in {@code pom.xml}.
 */
@CucumberOptions(
        features = "src/test/resources/features",
        glue = {"com.hybrid.framework.stepdefs"},
        plugin = {
                "pretty",
                "html:target/cucumber-reports/cucumber.html",
                "json:target/cucumber-reports/cucumber.json",
                "io.qameta.allure.cucumber7jvm.AllureCucumber7Jvm",
                "rerun:target/rerun.txt",
                "com.hybrid.framework.reporting.ExtentCucumberPlugin"
        },
        monochrome = false,
        dryRun = false
)
public class TestRunner extends AbstractTestNGCucumberTests {

    /**
     * Enables parallel scenario execution.
     * Set {@code parallel = true} and configure thread count via {@code parallel.thread.count} in config.
     */
    @Override
    @DataProvider(parallel = true)
    public Object[][] scenarios() {
        return super.scenarios();
    }
}
