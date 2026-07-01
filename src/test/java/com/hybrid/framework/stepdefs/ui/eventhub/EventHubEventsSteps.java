package com.hybrid.framework.stepdefs.ui.eventhub;

import com.hybrid.framework.context.ContextKeys;
import com.hybrid.framework.context.TestContext;
import com.hybrid.framework.pages.eventhub.EventHubAdminEventsPage;
import com.hybrid.framework.pages.eventhub.EventHubEventDetailPage;
import com.hybrid.framework.pages.eventhub.EventHubEventsPage;
import com.hybrid.framework.pages.eventhub.EventHubHomePage;
import com.hybrid.framework.testdata.EventHubAttendeeData;
import com.hybrid.framework.testdata.EventHubEventData;
import com.hybrid.framework.testdata.EventHubTestDataFactory;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.testng.Assert;

/**
 * Step definitions for EventHub events and admin event creation scenarios.
 */
public class EventHubEventsSteps {

    private final TestContext testContext;

    private EventHubEventsPage eventsPage;
    private EventHubEventDetailPage eventDetailPage;
    private EventHubAdminEventsPage adminEventsPage;

    public EventHubEventsSteps(TestContext testContext) {
        this.testContext = testContext;
    }

    @When("the user navigates to the events listing page")
    public void theUserNavigatesToTheEventsListingPage() {
        EventHubHomePage homePage = testContext.get(ContextKeys.UI_HOME_PAGE, EventHubHomePage.class);
        if (homePage == null) {
            homePage = new EventHubHomePage();
        }
        eventsPage = homePage.navBar().goToEvents();
    }

    @When("the user searches events for {string}")
    public void theUserSearchesEventsFor(String keyword) {
        eventsPage = resolveEventsPage();
        eventsPage.searchEvents(keyword);
    }

    @When("the user selects Book Now on the first available event")
    public void theUserSelectsBookNowOnTheFirstAvailableEvent() {
        eventsPage = resolveEventsPage();
        eventDetailPage = eventsPage.bookFirstAvailableEvent();
    }

    @When("the user selects Add New Event from the events page")
    public void theUserSelectsAddNewEventFromTheEventsPage() {
        eventsPage = resolveEventsPage();
        adminEventsPage = eventsPage.openAddNewEvent();
    }

    @When("the user fills the event booking details form")
    public void theUserFillsTheEventBookingDetailsForm() {
        eventDetailPage = resolveEventDetailPage();
        EventHubAttendeeData attendee = EventHubTestDataFactory.randomAttendee();
        testContext.put(ContextKeys.ATTENDEE_DATA, attendee);
        eventDetailPage.fillBookingDetails(attendee, 1);
    }

    @When("the user fills a new event with generated details")
    public void theUserFillsANewEventWithGeneratedDetails() {
        adminEventsPage = resolveAdminEventsPage();
        EventHubEventData eventData = EventHubTestDataFactory.randomEvent();
        fillAndStoreEventData(eventData);
    }

    @When("the user fills a new event with title {string}")
    public void theUserFillsANewEventWithTitle(String title) {
        adminEventsPage = resolveAdminEventsPage();
        EventHubEventData eventData = EventHubTestDataFactory.randomEventWithTitle(title);
        fillAndStoreEventData(eventData);
    }

    private void fillAndStoreEventData(EventHubEventData eventData) {
        testContext.put(ContextKeys.EVENT_TITLE, eventData.title());
        testContext.put(ContextKeys.EVENT_DATA, eventData);
        adminEventsPage.fillNewEventDetails(eventData);
    }

    @Then("the events listing page should be displayed")
    public void theEventsListingPageShouldBeDisplayed() {
        eventsPage = resolveEventsPage();
        Assert.assertTrue(eventsPage.isEventsPageDisplayed(), "Events listing page should be displayed");
        Assert.assertTrue(eventsPage.isPageSubheadingDisplayed(), "Events page subheading should be visible");
    }

    @And("the events listing should show available event cards")
    public void theEventsListingShouldShowAvailableEventCards() {
        eventsPage = resolveEventsPage();
        Assert.assertTrue(eventsPage.hasEventCards(), "At least one event card should be listed");
    }

    @Then("the event booking details form should be displayed")
    public void theEventBookingDetailsFormShouldBeDisplayed() {
        eventDetailPage = resolveEventDetailPage();
        Assert.assertTrue(
                eventDetailPage.isBookingFormDisplayed(),
                "Booking details form should be displayed on the event page"
        );
    }

    @Then("the admin events page should be displayed")
    public void theAdminEventsPageShouldBeDisplayed() {
        adminEventsPage = resolveAdminEventsPage();
        Assert.assertTrue(
                adminEventsPage.isAdminEventsPageDisplayed(),
                "Admin events page should be displayed"
        );
    }

    @And("the new event form should be ready for data entry")
    public void theNewEventFormShouldBeReadyForDataEntry() {
        adminEventsPage = resolveAdminEventsPage();
        Assert.assertTrue(
                adminEventsPage.isNewEventFormDisplayed(),
                "New event form fields should be visible for data entry"
        );
    }

    @And("the Add New Event option should be visible on the events page")
    public void theAddNewEventOptionShouldBeVisibleOnTheEventsPage() {
        eventsPage = resolveEventsPage();
        Assert.assertTrue(
                eventsPage.isAddNewEventLinkDisplayed(),
                "Add New Event link should be visible on the events listing page"
        );
    }

    private EventHubEventsPage resolveEventsPage() {
        if (eventsPage != null) {
            return eventsPage;
        }
        eventsPage = new EventHubEventsPage();
        return eventsPage;
    }

    private EventHubEventDetailPage resolveEventDetailPage() {
        if (eventDetailPage != null) {
            return eventDetailPage;
        }
        eventDetailPage = new EventHubEventDetailPage();
        return eventDetailPage;
    }

    private EventHubAdminEventsPage resolveAdminEventsPage() {
        if (adminEventsPage != null) {
            return adminEventsPage;
        }
        adminEventsPage = new EventHubAdminEventsPage();
        return adminEventsPage;
    }
}
