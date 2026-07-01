package com.hybrid.framework.pages;

import com.hybrid.framework.driver.DriverManager;
import com.hybrid.framework.pages.actions.DropdownActions;
import com.hybrid.framework.utils.WaitUtils;
import io.qameta.allure.Step;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import java.util.List;

/**
 * Base class for all Page Objects — provides common UI interaction methods.
 * <p>
 * Encapsulates {@link By} locators and raw Selenium calls behind clean, readable methods.
 * Subclasses declare locators as {@code private final By} fields and resolve elements
 * at interaction time to avoid stale element references.
 * </p>
 *
 * <b>Design principles:</b>
 * <ul>
 *   <li>All locators are private in subclasses — hidden via encapsulation.</li>
 *   <li>Fluent interface — action methods return {@code this} for chaining.</li>
 *   <li>Built-in waits — every interaction waits for visibility/clickability.</li>
 *   <li>Composed action helpers — domain-specific interactions (e.g. {@link #dropdown()})
 *       live in separate classes rather than bloating this base.</li>
 * </ul>
 */
public abstract class BasePage {

    protected final Logger log = LogManager.getLogger(this.getClass());
    protected final WaitUtils waitUtils;
    private final DropdownActions dropdownActions;

    protected BasePage() {
        this.waitUtils = new WaitUtils(getDriver());
        this.dropdownActions = new DropdownActions(waitUtils);
        log.debug("Initialized page: {}", this.getClass().getSimpleName());
    }

    // ──────────────────────────────────────────────────────────────
    // Driver access
    // ──────────────────────────────────────────────────────────────

    protected WebDriver getDriver() {
        return DriverManager.getDriver();
    }

    /**
     * Composed helper for native and custom dropdown interactions.
     */
    protected DropdownActions dropdown() {
        return dropdownActions;
    }

    // ──────────────────────────────────────────────────────────────
    // Core interactions (encapsulated)
    // ──────────────────────────────────────────────────────────────

    /**
     * Clicks an element after waiting for it to be clickable.
     */
    protected void click(WebElement element) {
        waitUtils.waitForClickable(element).click();
        log.debug("Clicked element: {}", describeElement(element));
    }

    /**
     * Clicks an element located by a By locator.
     */
    protected void click(By locator) {
        WebElement element = waitUtils.waitForClickable(locator);
        element.click();
        log.debug("Clicked element: {}", locator);
    }

    /**
     * Clears a field and types the given text.
     */
    protected void type(WebElement element, String text) {
        waitUtils.waitForVisible(element).clear();
        element.sendKeys(text);
        log.debug("Typed '{}' into element: {}", text, describeElement(element));
    }

    /**
     * Clears a field located by a By locator and types the given text.
     */
    protected void type(By locator, String text) {
        WebElement element = waitUtils.waitForVisible(locator);
        element.clear();
        element.sendKeys(text);
        log.debug("Typed '{}' into element: {}", text, locator);
    }

    /**
     * Retrieves visible text from an element.
     */
    protected String getText(WebElement element) {
        String text = waitUtils.waitForVisible(element).getText();
        log.debug("Got text '{}' from element: {}", text, describeElement(element));
        return text;
    }

    /**
     * Retrieves visible text from an element located by a By locator.
     */
    protected String getText(By locator) {
        WebElement element = waitUtils.waitForVisible(locator);
        String text = element.getText();
        log.debug("Got text '{}' from element: {}", text, locator);
        return text;
    }

    /**
     * Returns the HTML DOM attribute value (e.g. {@code type}, {@code placeholder}, {@code href}).
     */
    protected String getDomAttribute(WebElement element, String attribute) {
        return waitUtils.waitForVisible(element).getDomAttribute(attribute);
    }

    /**
     * Returns the HTML DOM attribute value for an element located by a By locator.
     */
    protected String getDomAttribute(By locator, String attribute) {
        return waitUtils.waitForVisible(locator).getDomAttribute(attribute);
    }

    /**
     * Returns the live DOM property value (e.g. {@code value}, {@code checked}, {@code selected}).
     */
    protected String getDomProperty(WebElement element, String property) {
        return waitUtils.waitForVisible(element).getDomProperty(property);
    }

    /**
     * Returns the live DOM property value for an element located by a By locator.
     */
    protected String getDomProperty(By locator, String property) {
        return waitUtils.waitForVisible(locator).getDomProperty(property);
    }

    /**
     * Checks if an element is displayed.
     */
    protected boolean isDisplayed(WebElement element) {
        try {
            return element.isDisplayed();
        } catch (NoSuchElementException | StaleElementReferenceException e) {
            return false;
        }
    }

    /**
     * Checks if an element is displayed by locator.
     */
    protected boolean isDisplayed(By locator) {
        try {
            return getDriver().findElement(locator).isDisplayed();
        } catch (NoSuchElementException | StaleElementReferenceException e) {
            return false;
        }
    }

    /**
     * Finds all elements matching a locator.
     */
    protected List<WebElement> findElements(By locator) {
        return getDriver().findElements(locator);
    }

    // ──────────────────────────────────────────────────────────────
    // Advanced interactions
    // ──────────────────────────────────────────────────────────────

    /**
     * Hovers over an element.
     */
    protected void hoverOver(WebElement element) {
        new Actions(getDriver())
                .moveToElement(waitUtils.waitForVisible(element))
                .perform();
        log.debug("Hovered over element: {}", describeElement(element));
    }

    /**
     * Scrolls to an element so it is in the viewport.
     */
    protected void scrollToElement(By locator) {
        new Actions(getDriver())
                .scrollToElement(waitUtils.waitForPresence(locator))
                .perform();
        log.debug("Scrolled to element: {}", locator);
    }

    /**
     * Clicks an element using JavaScript (for elements intercepted by overlays).
     */
    protected void jsClick(WebElement element) {
        ((JavascriptExecutor) getDriver()).executeScript("arguments[0].click();", element);
        log.debug("JS-clicked element: {}", describeElement(element));
    }

    /**
     * Switches to an iframe by WebElement.
     */
    protected void switchToFrame(WebElement frameElement) {
        getDriver().switchTo().frame(frameElement);
        log.debug("Switched to iframe");
    }

    /**
     * Switches back to the default content from an iframe.
     */
    protected void switchToDefaultContent() {
        getDriver().switchTo().defaultContent();
    }

    // ──────────────────────────────────────────────────────────────
    // Navigation
    // ──────────────────────────────────────────────────────────────

    /**
     * Navigates to a URL.
     */
    @Step("Navigate to: {url}")
    protected void navigateTo(String url) {
        getDriver().get(url);
        log.info("Navigated to: {}", url);
    }

    /**
     * Returns the current page title.
     */
    protected String getPageTitle() {
        return getDriver().getTitle();
    }

    /**
     * Returns the current page URL.
     */
    protected String getCurrentUrl() {
        return getDriver().getCurrentUrl();
    }

    // ──────────────────────────────────────────────────────────────
    // Private helpers
    // ──────────────────────────────────────────────────────────────

    private String describeElement(WebElement element) {
        try {
            return element.getTagName() + "[" + element.getText().substring(0, Math.min(30, element.getText().length())) + "]";
        } catch (Exception e) {
            return element.toString();
        }
    }
}
