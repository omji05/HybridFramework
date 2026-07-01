package com.hybrid.framework.pages.eventhub;

import com.hybrid.framework.pages.BasePage;
import com.hybrid.framework.pages.eventhub.components.EventHubNavBar;
import io.qameta.allure.Step;
import org.openqa.selenium.By;

/**
 * My Bookings page ({@code /bookings}).
 */
public class EventHubBookingsPage extends BasePage {

    private final By pageHeading       = By.xpath("//h1[contains(normalize-space(),'My Bookings')]");
    private final By pageSubheading    = By.xpath("//*[contains(normalize-space(),'View and manage all your ticket bookings')]");
    private final By emptyStateTitle   = By.xpath("//*[contains(normalize-space(),'No bookings yet')]");
    private final By browseEventsLink  = By.xpath("//a[contains(@href,'/events') and contains(.,'Browse Events')]");
    private final By bookingCards      = By.cssSelector("[data-testid='booking-card']");
    private final By clearAllButton    = By.xpath("//button[contains(.,'Clear all bookings')]");

    @Step("Verify My Bookings page is displayed")
    public boolean isBookingsPageDisplayed() {
        return getCurrentUrl().contains("/bookings") && isDisplayed(pageHeading);
    }

    public boolean isPageSubheadingDisplayed() {
        return isDisplayed(pageSubheading);
    }

    public boolean hasBookingCards() {
        return !findElements(bookingCards).isEmpty();
    }

    public boolean isEmptyStateDisplayed() {
        return isDisplayed(emptyStateTitle);
    }

    public boolean isBrowseEventsLinkDisplayed() {
        return isDisplayed(browseEventsLink);
    }

    @Step("Open Events page from bookings empty state")
    public EventHubEventsPage browseEventsFromEmptyState() {
        scrollToElement(browseEventsLink);
        click(browseEventsLink);
        waitUtils.waitForPageLoad();
        return new EventHubEventsPage();
    }

    public boolean isClearAllBookingsButtonDisplayed() {
        return isDisplayed(clearAllButton);
    }

    public EventHubNavBar navBar() {
        return new EventHubNavBar();
    }
}
