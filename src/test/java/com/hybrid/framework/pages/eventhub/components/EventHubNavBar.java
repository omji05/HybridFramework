package com.hybrid.framework.pages.eventhub.components;

import com.hybrid.framework.pages.BasePage;
import com.hybrid.framework.pages.eventhub.EventHubBookingsPage;
import com.hybrid.framework.pages.eventhub.EventHubEventsPage;
import com.hybrid.framework.pages.eventhub.EventHubHomePage;
import com.hybrid.framework.pages.eventhub.EventHubLoginPage;
import io.qameta.allure.Step;
import org.openqa.selenium.By;

/**
 * Navigation bar shown on authenticated EventHub pages (hidden on /login and /register).
 */
public class EventHubNavBar extends BasePage {

    private final By userEmailDisplay = By.cssSelector("#user-email-display");
    private final By logoutButton     = By.cssSelector("#logout-btn");
    private final By homeNavLink      = By.cssSelector("#nav-home");
    private final By eventsNavLink    = By.cssSelector("#nav-events");
    private final By bookingsNavLink  = By.cssSelector("#nav-bookings");

    @Step("Navigate to Home via nav bar")
    public EventHubHomePage goToHome() {
        click(homeNavLink);
        waitUtils.waitForPageLoad();
        return new EventHubHomePage();
    }

    @Step("Navigate to Events via nav bar")
    public EventHubEventsPage goToEvents() {
        click(eventsNavLink);
        waitUtils.waitForPageLoad();
        return new EventHubEventsPage();
    }

    @Step("Navigate to My Bookings via nav bar")
    public EventHubBookingsPage goToBookings() {
        click(bookingsNavLink);
        waitUtils.waitForPageLoad();
        return new EventHubBookingsPage();
    }

    @Step("Sign out of EventHub")
    public EventHubLoginPage logout() {
        click(logoutButton);
        waitUtils.fluentWait(driver -> {
            String url = driver.getCurrentUrl();
            return url != null && url.contains("/login");
        });
        return new EventHubLoginPage();
    }

    @Step("Verify nav bar shows authenticated user email: {email}")
    public boolean isAuthenticatedAs(String email) {
        if (!isDisplayed(userEmailDisplay)) {
            return false;
        }
        String displayed = getText(userEmailDisplay).trim();
        return displayed.equalsIgnoreCase(email.trim());
    }

    @Step("Get authenticated user email from nav bar")
    public String getAuthenticatedUserEmail() {
        if (!isDisplayed(userEmailDisplay)) {
            return "";
        }
        return getText(userEmailDisplay).trim();
    }

    @Step("Verify user is authenticated via nav bar state")
    public boolean isUserAuthenticated() {
        return isDisplayed(logoutButton) && isDisplayed(userEmailDisplay);
    }

    @Step("Verify authenticated home navigation is available")
    public boolean isHomeNavigationDisplayed() {
        return isDisplayed(homeNavLink);
    }

    public boolean isEventsNavigationDisplayed() {
        return isDisplayed(eventsNavLink);
    }

    public boolean isBookingsNavigationDisplayed() {
        return isDisplayed(bookingsNavLink);
    }
}
