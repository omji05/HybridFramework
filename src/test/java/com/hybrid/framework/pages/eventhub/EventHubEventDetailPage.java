package com.hybrid.framework.pages.eventhub;

import com.hybrid.framework.pages.BasePage;
import com.hybrid.framework.testdata.EventHubAttendeeData;
import io.qameta.allure.Step;
import org.openqa.selenium.By;

/**
 * Event detail and ticket booking page ({@code /events/{id}}).
 */
public class EventHubEventDetailPage extends BasePage {

    private final By bookTicketsHeading = By.xpath("//*[contains(normalize-space(),'Book Tickets')]");
    private final By customerNameField  = By.id("customerName");
    private final By customerEmailField = By.id("customer-email");
    private final By phoneField         = By.id("phone");
    private final By ticketCountField   = By.id("ticket-count");
    private final By confirmBookingBtn  = By.id("confirm-booking");
    private final By bookingConfirmed   = By.xpath("//*[contains(normalize-space(),'Booking Confirmed')]");

    @Step("Verify booking details form is displayed")
    public boolean isBookingFormDisplayed() {
        return getCurrentUrl().contains("/events/")
                && isDisplayed(bookTicketsHeading)
                && isDisplayed(customerNameField)
                && isDisplayed(confirmBookingBtn);
    }

    @Step("Fill attendee booking details")
    public EventHubEventDetailPage fillBookingDetails(EventHubAttendeeData attendee, int tickets) {
        return fillBookingDetails(attendee.name(), attendee.email(), attendee.phone(), tickets);
    }

    @Step("Fill attendee booking details")
    public EventHubEventDetailPage fillBookingDetails(String name, String email, String phone, int tickets) {
        type(customerNameField, name);
        type(customerEmailField, email);
        type(phoneField, phone);
        adjustTicketCount(tickets);
        return this;
    }

    @Step("Submit booking confirmation")
    public EventHubEventDetailPage confirmBooking() {
        click(confirmBookingBtn);
        waitUtils.waitForPageLoad();
        return this;
    }

    public boolean isBookingConfirmed() {
        return isDisplayed(bookingConfirmed);
    }

    public String getCurrentEventUrl() {
        return getCurrentUrl();
    }

    private void adjustTicketCount(int target) {
        int current = Integer.parseInt(getText(ticketCountField).trim());
        By incrementButton = By.xpath("//span[@id='ticket-count']/following-sibling::button[1]");
        By decrementButton = By.xpath("//span[@id='ticket-count']/preceding-sibling::button[1]");
        while (current < target) {
            click(incrementButton);
            current++;
        }
        while (current > target) {
            click(decrementButton);
            current--;
        }
    }
}
