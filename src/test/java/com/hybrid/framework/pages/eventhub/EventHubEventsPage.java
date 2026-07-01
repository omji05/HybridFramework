package com.hybrid.framework.pages.eventhub;

import com.hybrid.framework.pages.BasePage;
import com.hybrid.framework.pages.eventhub.components.EventHubNavBar;
import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.util.List;

/**
 * Public events listing page ({@code /events}).
 */
public class EventHubEventsPage extends BasePage {

    private final By pageHeading    = By.xpath("//h1[contains(normalize-space(),'Upcoming Events')]");
    private final By pageSubheading = By.xpath("//*[contains(normalize-space(),'Find your next unforgettable experience')]");
    private final By searchField    = By.cssSelector("input[placeholder='Search events, venues…']");
    private final By eventCards     = By.cssSelector("[data-testid='event-card']");
    private final By addNewEventLink = By.xpath("//a[contains(@href,'/admin/events') and contains(.,'Add New Event')]");

    @Step("Verify events listing page is displayed")
    public boolean isEventsPageDisplayed() {
        return getCurrentUrl().contains("/events") && isDisplayed(pageHeading);
    }

    public boolean isPageSubheadingDisplayed() {
        return isDisplayed(pageSubheading);
    }

    public boolean hasEventCards() {
        return !findElements(eventCards).isEmpty();
    }

    @Step("Search events by keyword: {keyword}")
    public EventHubEventsPage searchEvents(String keyword) {
        type(searchField, keyword);
        waitUtils.waitForPageLoad();
        return this;
    }

    @Step("Open admin events page via Add New Event link")
    public EventHubAdminEventsPage openAddNewEvent() {
        scrollToElement(addNewEventLink);
        WebElement link = waitUtils.waitForClickable(addNewEventLink);
        jsClick(link);
        waitUtils.waitForPageLoad();
        return new EventHubAdminEventsPage();
    }

    @Step("Open admin events page via navigation")
    public EventHubAdminEventsPage openAdminEventsViaNav() {
        navigateTo(getCurrentUrl().split("/events")[0] + "/admin/events");
        waitUtils.waitForPageLoad();
        return new EventHubAdminEventsPage();
    }

    @Step("Book the first available event from the listing")
    public EventHubEventDetailPage bookFirstAvailableEvent() {
        WebElement bookButton = waitUtils.waitForClickable(findFirstBookNowButton());
        new org.openqa.selenium.interactions.Actions(getDriver())
                .scrollToElement(bookButton)
                .perform();
        bookButton.click();
        waitUtils.waitForUrlContains("/events/");
        return new EventHubEventDetailPage();
    }

    public boolean isAddNewEventLinkDisplayed() {
        return isDisplayed(addNewEventLink);
    }

    public EventHubNavBar navBar() {
        return new EventHubNavBar();
    }

    private WebElement findFirstBookNowButton() {
        List<WebElement> buttons = findElements(By.cssSelector("#book-now-btn, [data-testid='book-now-btn']"));
        return buttons.stream()
                .filter(btn -> {
                    try {
                        return btn.isDisplayed() && "Book Now".equalsIgnoreCase(btn.getText().trim());
                    } catch (Exception e) {
                        return false;
                    }
                })
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No available 'Book Now' button found on events page"));
    }
}
