@api @eventhub @epic-EH-100
Feature: EventHub API - Bookings
  As an authenticated EventHub user
  I want to list and create bookings via the REST API
  So that I can validate ticket purchase and retrieval flows

  Background:
    Given I am authenticated on EventHub using the "eventhub_login.json" payload

  @regression @bookings @EH-API-005
  Scenario: Retrieve all bookings
    When I fetch all EventHub bookings
    Then the response status code should be 200
    And the response body should match the "eventhub-bookings-list-response.json" schema

  @regression @bookings @EH-API-006
  Scenario: Create a booking for an event
    When I create a new EventHub event using the "eventhub_create_event.json" payload
    Then the response status code should be 201
    And the event id should be stored for subsequent requests
    When I create an EventHub booking using the "eventhub_create_booking.json" payload
    Then the response status code should be 201
    And the response body should match the "eventhub-booking-response.json" schema
    And the booking id should be stored for subsequent requests
    When I fetch the EventHub booking using the stored booking id
    Then the response status code should be 200
    And the response body should match the "eventhub-booking-response.json" schema
