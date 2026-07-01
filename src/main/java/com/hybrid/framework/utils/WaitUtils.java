package com.hybrid.framework.utils;

import com.hybrid.framework.config.ConfigReader;
import com.hybrid.framework.config.FrameworkConstants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;
import java.util.function.Function;

/**
 * Custom wait strategies — wraps Selenium's explicit/fluent waits with
 * sensible defaults and clean API.
 */
public class WaitUtils {

    private static final Logger LOG = LogManager.getLogger(WaitUtils.class);
    private final WebDriver driver;
    private final int explicitWaitSeconds;

    public WaitUtils(WebDriver driver) {
        this.driver = driver;
        this.explicitWaitSeconds = ConfigReader.getInstance().getExplicitWait();
    }

    // ──────────────────────────────────────────────────────────────
    // Explicit Waits
    // ──────────────────────────────────────────────────────────────

    /**
     * Waits for an element to be visible.
     */
    public WebElement waitForVisible(WebElement element) {
        return getExplicitWait().until(ExpectedConditions.visibilityOf(element));
    }

    /**
     * Waits for an element located by a By locator to be visible.
     */
    public WebElement waitForVisible(By locator) {
        return getExplicitWait().until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    /**
     * Waits for an element to be clickable.
     */
    public WebElement waitForClickable(WebElement element) {
        return getExplicitWait().until(ExpectedConditions.elementToBeClickable(element));
    }

    /**
     * Waits for an element located by a By locator to be clickable.
     */
    public WebElement waitForClickable(By locator) {
        return getExplicitWait().until(ExpectedConditions.elementToBeClickable(locator));
    }

    /**
     * Waits for an element to be present in the DOM (may not be visible).
     */
    public WebElement waitForPresence(By locator) {
        return getExplicitWait().until(ExpectedConditions.presenceOfElementLocated(locator));
    }

    /**
     * Waits for all elements matching a locator to be visible.
     */
    public List<WebElement> waitForAllVisible(By locator) {
        return getExplicitWait().until(ExpectedConditions.visibilityOfAllElementsLocatedBy(locator));
    }

    /**
     * Waits for an element to become invisible / disappear.
     */
    public boolean waitForInvisible(By locator) {
        return getExplicitWait().until(ExpectedConditions.invisibilityOfElementLocated(locator));
    }

    /**
     * Waits for text to be present in an element.
     */
    public boolean waitForTextPresent(By locator, String text) {
        return getExplicitWait().until(ExpectedConditions.textToBePresentInElementLocated(locator, text));
    }

    /**
     * Waits for a URL to contain a specific substring.
     */
    public boolean waitForUrlContains(String urlFragment) {
        return getExplicitWait().until(ExpectedConditions.urlContains(urlFragment));
    }

    /**
     * Waits for the page title to contain a specific substring.
     */
    public boolean waitForTitleContains(String titleFragment) {
        return getExplicitWait().until(ExpectedConditions.titleContains(titleFragment));
    }

    /**
     * Waits for a JavaScript alert to be present.
     */
    public void waitForAlert() {
        getExplicitWait().until(ExpectedConditions.alertIsPresent());
    }

    // ──────────────────────────────────────────────────────────────
    // Fluent Wait
    // ──────────────────────────────────────────────────────────────

    /**
     * Fluent wait — polls at regular intervals, ignoring specified exceptions.
     */
    public <T> T fluentWait(Function<WebDriver, T> condition) {
        return getFluentWait().until(condition);
    }

    /**
     * Fluent wait for an element with custom timeout and polling.
     */
    public WebElement fluentWaitForElement(By locator, int timeoutSeconds, int pollingMillis) {
        FluentWait<WebDriver> wait = new FluentWait<>(driver)
                .withTimeout(Duration.ofSeconds(timeoutSeconds))
                .pollingEvery(Duration.ofMillis(pollingMillis))
                .ignoring(NoSuchElementException.class)
                .ignoring(StaleElementReferenceException.class);

        return wait.until(d -> {
            WebElement element = d.findElement(locator);
            return element.isDisplayed() ? element : null;
        });
    }

    // ──────────────────────────────────────────────────────────────
    // Page Load Waits
    // ──────────────────────────────────────────────────────────────

    /**
     * Waits for the page to be fully loaded (document.readyState == 'complete').
     */
    public void waitForPageLoad() {
        getExplicitWait().until((ExpectedCondition<Boolean>) d ->
                ((JavascriptExecutor) d).executeScript("return document.readyState").equals("complete"));
        LOG.debug("Page fully loaded");
    }

    /**
     * Waits for all AJAX calls to complete (jQuery).
     */
    public void waitForAjax() {
        getExplicitWait().until((ExpectedCondition<Boolean>) d -> {
            JavascriptExecutor js = (JavascriptExecutor) d;
            return (Boolean) js.executeScript("return (typeof jQuery !== 'undefined') ? jQuery.active == 0 : true");
        });
        LOG.debug("AJAX calls completed");
    }

    // ──────────────────────────────────────────────────────────────
    // Wait builders
    // ──────────────────────────────────────────────────────────────

    private WebDriverWait getExplicitWait() {
        return new WebDriverWait(driver, Duration.ofSeconds(explicitWaitSeconds));
    }

    private WebDriverWait getExplicitWait(int seconds) {
        return new WebDriverWait(driver, Duration.ofSeconds(seconds));
    }

    private FluentWait<WebDriver> getFluentWait() {
        return new FluentWait<>(driver)
                .withTimeout(Duration.ofSeconds(FrameworkConstants.FLUENT_WAIT_TIMEOUT))
                .pollingEvery(Duration.ofSeconds(FrameworkConstants.FLUENT_WAIT_POLLING))
                .ignoring(NoSuchElementException.class)
                .ignoring(StaleElementReferenceException.class)
                .ignoring(ElementClickInterceptedException.class);
    }
}
