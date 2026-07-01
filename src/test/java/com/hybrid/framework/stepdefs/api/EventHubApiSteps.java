package com.hybrid.framework.stepdefs.api;

import com.hybrid.framework.services.EventHubApiService;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

/**
 * Cucumber step definitions for EventHub REST API scenarios.
 */
public class EventHubApiSteps {

    private final EventHubApiService eventHubApiService;

    public EventHubApiSteps(EventHubApiService eventHubApiService) {
        this.eventHubApiService = eventHubApiService;
    }

    @When("I register a new EventHub user using the {string} payload")
    public void iRegisterANewEventHubUserUsingThePayload(String fileName) {
        eventHubApiService.registerUser(fileName);
    }

    @When("I log in to EventHub using the {string} payload")
    public void iLogInToEventHubUsingThePayload(String fileName) {
        eventHubApiService.loginUser(fileName);
    }

    @Given("I am authenticated on EventHub using the {string} payload")
    public void iAmAuthenticatedOnEventHubUsingThePayload(String fileName) {
        eventHubApiService.authenticate(fileName);
    }

    @When("I fetch all EventHub events")
    public void iFetchAllEventHubEvents() {
        eventHubApiService.getEvents();
    }

    @When("I create a new EventHub event using the {string} payload")
    public void iCreateANewEventHubEventUsingThePayload(String fileName) {
        eventHubApiService.createEvent(fileName);
    }

    @When("I fetch all EventHub bookings")
    public void iFetchAllEventHubBookings() {
        eventHubApiService.getBookings();
    }

    @When("I fetch the EventHub booking using the stored booking id")
    public void iFetchTheEventHubBookingUsingTheStoredBookingId() {
        eventHubApiService.getBookingById();
    }

    @When("I create an EventHub booking using the {string} payload")
    public void iCreateAnEventHubBookingUsingThePayload(String fileName) {
        eventHubApiService.createBooking(fileName);
    }

    @Then("the EventHub response should contain a valid JWT token")
    public void theEventHubResponseShouldContainAValidJwtToken() {
        eventHubApiService.assertJwtTokenPresent();
    }

    @And("the EventHub auth token should be stored for subsequent requests")
    public void theEventHubAuthTokenShouldBeStoredForSubsequentRequests() {
        eventHubApiService.storeAuthToken();
    }

    @And("the event id should be stored for subsequent requests")
    public void theEventIdShouldBeStoredForSubsequentRequests() {
        eventHubApiService.storeEventId();
    }

    @And("the booking id should be stored for subsequent requests")
    public void theBookingIdShouldBeStoredForSubsequentRequests() {
        eventHubApiService.storeBookingId();
    }
}
