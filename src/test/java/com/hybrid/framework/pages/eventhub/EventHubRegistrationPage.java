package com.hybrid.framework.pages.eventhub;

import com.hybrid.framework.pages.BasePage;
import org.openqa.selenium.By;

/**
 * Page object for the EventHub registration page ({@code /register}).
 */
public class EventHubRegistrationPage extends BasePage {

    private final By pageHeading      = By.xpath("//h1[normalize-space()='Create your account']");
    private final By createAccountButton = By.id("register-btn");

    public boolean isRegistrationPageDisplayed() {
        return isDisplayed(createAccountButton) && isDisplayed(pageHeading);
    }

    public String getPageUrl() {
        return getCurrentUrl();
    }
}
