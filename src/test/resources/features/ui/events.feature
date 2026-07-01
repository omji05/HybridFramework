@ui @events
Feature: EventHub Events Discovery and Creation
  In order to find and manage events
  As a signed-in EventHub user
  I want to browse events, start bookings, and create new events

  Background:
    Given the user is signed in to EventHub

  @smoke @EVENTS-001
  Scenario: Events listing page displays upcoming events
    When the user navigates to the events listing page
    Then the events listing page should be displayed
    And the events listing should show available event cards
    And the Add New Event option should be visible on the events page

  @regression @EVENTS-002
  Scenario: User can search the events listing
    When the user navigates to the events listing page
    And the user searches events for "Conference"
    Then the events listing page should be displayed

  @smoke @EVENTS-003
  Scenario: Book Now opens the event booking details form
    When the user navigates to the events listing page
    And the user selects Book Now on the first available event
    Then the event booking details form should be displayed

  @smoke @EVENTS-004
  Scenario: User can move from events page to the new event form
    When the user navigates to the events listing page
    And the user selects Add New Event from the events page
    Then the admin events page should be displayed
    And the new event form should be ready for data entry

  @regression @EVENTS-005
  Scenario: User can fill new event details on the admin form
    When the user navigates to the events listing page
    And the user selects Add New Event from the events page
    And the user fills a new event with generated details
    Then the admin events page should be displayed
    And the new event form should be ready for data entry

  @regression @EVENTS-006
  Scenario: User can fill attendee details on the booking form
    When the user navigates to the events listing page
    And the user selects Book Now on the first available event
    And the user fills the event booking details form
    Then the event booking details form should be displayed
