package com.hybrid.framework.pages.actions;

import com.hybrid.framework.utils.WaitUtils;
import io.qameta.allure.Step;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Composable helper for native {@code <select>} elements and custom (non-native) dropdowns.
 */
public final class DropdownActions {

    private static final Logger LOG = LogManager.getLogger(DropdownActions.class);

    private final WaitUtils waitUtils;

    public DropdownActions(WaitUtils waitUtils) {
        this.waitUtils = waitUtils;
    }

    @Step("Select option by visible text: {visibleText}")
    public void selectByVisibleText(By locator, String visibleText) {
        Select select = new Select(waitUtils.waitForVisible(locator));
        select.selectByVisibleText(visibleText);
        LOG.debug("Selected visible text '{}' from {}", visibleText, locator);
    }

    @Step("Select option by value: {value}")
    public void selectByValue(By locator, String value) {
        Select select = new Select(waitUtils.waitForVisible(locator));
        select.selectByValue(value);
        LOG.debug("Selected value '{}' from {}", value, locator);
    }

    @Step("Select option by index: {index}")
    public void selectByIndex(By locator, int index) {
        Select select = new Select(waitUtils.waitForVisible(locator));
        select.selectByIndex(index);
        LOG.debug("Selected index {} from {}", index, locator);
    }

    public String getSelectedVisibleText(By locator) {
        Select select = new Select(waitUtils.waitForVisible(locator));
        return select.getFirstSelectedOption().getText();
    }

    public List<String> getAllVisibleOptions(By locator) {
        Select select = new Select(waitUtils.waitForVisible(locator));
        return select.getOptions().stream()
                .map(WebElement::getText)
                .collect(Collectors.toList());
    }

    public boolean isMultiple(By locator) {
        Select select = new Select(waitUtils.waitForVisible(locator));
        return select.isMultiple();
    }

    public void deselectAll(By locator) {
        Select select = new Select(waitUtils.waitForVisible(locator));
        select.deselectAll();
        LOG.debug("Deselected all options in {}", locator);
    }

    @Step("Open custom dropdown and select option")
    public void openAndSelect(By dropdownTrigger, By optionLocator) {
        waitUtils.waitForClickable(dropdownTrigger).click();
        waitUtils.waitForClickable(optionLocator).click();
    }

    @Step("Open custom dropdown and select option: {optionText}")
    public void openAndSelectByVisibleText(By dropdownTrigger, By optionsLocator, String optionText) {
        waitUtils.waitForClickable(dropdownTrigger).click();
        List<WebElement> options = waitUtils.waitForAllVisible(optionsLocator);
        for (WebElement option : options) {
            if (optionText.equals(option.getText().trim())) {
                option.click();
                return;
            }
        }
        throw new NoSuchElementException(
                "No option with visible text '" + optionText + "' in " + optionsLocator);
    }
}
