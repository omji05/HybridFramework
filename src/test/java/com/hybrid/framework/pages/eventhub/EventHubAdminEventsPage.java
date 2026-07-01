package com.hybrid.framework.pages.eventhub;

import com.hybrid.framework.pages.BasePage;
import com.hybrid.framework.pages.eventhub.components.EventHubNavBar;
import com.hybrid.framework.testdata.EventHubEventData;
import io.qameta.allure.Step;
import org.openqa.selenium.By;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Admin event management page ({@code /admin/events}).
 */
public class EventHubAdminEventsPage extends BasePage {

    private final By newEventHeading   = By.xpath("//*[contains(normalize-space(),'New Event') or contains(normalize-space(),'Edit Event')]");
    private final By eventForm         = By.id("admin-event-form");
    private final By titleField        = By.id("event-title-input");
    private final By descriptionField  = By.cssSelector("#admin-event-form textarea[placeholder='Describe the event…']");
    private final By categorySelect    = By.cssSelector("#admin-event-form select");
    private final By cityField         = By.cssSelector("input[placeholder='e.g. Bangalore']");
    private final By venueField        = By.cssSelector("input[placeholder='Venue name & address']");
    private final By eventDateField    = By.cssSelector("#admin-event-form input[type='datetime-local']");
    private final By priceField        = By.cssSelector("input[placeholder='0.00']");
    private final By totalSeatsField   = By.cssSelector("input[placeholder='e.g. 500']");
    private final By addEventButton    = By.id("add-event-btn");
    private final By toastMessages     = By.cssSelector("[aria-live='polite'] p");

    @Step("Verify admin events page is displayed")
    public boolean isAdminEventsPageDisplayed() {
        return getCurrentUrl().contains("/admin/events") && isDisplayed(eventForm);
    }

    @Step("Verify new event form is available for data entry")
    public boolean isNewEventFormDisplayed() {
        return isDisplayed(newEventHeading)
                && isDisplayed(titleField)
                && isDisplayed(addEventButton);
    }

    @Step("Fill new event details on the create form")
    public EventHubAdminEventsPage fillNewEventDetails(EventHubEventData eventData) {
        return fillNewEventDetails(
                eventData.title(),
                eventData.description(),
                eventData.category(),
                eventData.city(),
                eventData.venue(),
                eventData.price(),
                eventData.totalSeats()
        );
    }

    @Step("Fill new event details on the create form")
    public EventHubAdminEventsPage fillNewEventDetails(String title, String description, String category,
                                                       String city, String venue, int price, int totalSeats) {
        type(titleField, title);
        type(descriptionField, description);
        dropdown().selectByVisibleText(categorySelect, category);
        type(cityField, city);
        type(venueField, venue);
        type(eventDateField, futureEventDateTime());
        type(priceField, String.valueOf(price));
        type(totalSeatsField, String.valueOf(totalSeats));
        return this;
    }

    @Step("Submit new event form")
    public EventHubAdminEventsPage submitNewEvent() {
        click(addEventButton);
        waitUtils.waitForPageLoad();
        return this;
    }

    public boolean hasSuccessToast() {
        try {
            waitUtils.fluentWaitForElement(toastMessages, 5, 200);
        } catch (Exception ignored) {
            return false;
        }
        return findElements(toastMessages).stream()
                .anyMatch(el -> {
                    try {
                        String text = el.getText().trim();
                        return text.contains("Event created") || text.contains("Event updated");
                    } catch (Exception e) {
                        return false;
                    }
                });
    }

    public EventHubNavBar navBar() {
        return new EventHubNavBar();
    }

    private String futureEventDateTime() {
        LocalDateTime future = LocalDateTime.now().plusMonths(2).withHour(18).withMinute(0).withSecond(0);
        return future.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));
    }
}
