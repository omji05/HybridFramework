@api @eventhub @epic-EH-100
Feature: EventHub API - Events
  As an authenticated EventHub user
  I want to list and create events via the REST API
  So that I can manage the event catalogue in my sandbox

  Background:
    Given I am authenticated on EventHub using the "eventhub_login.json" payload

  @smoke @events @EH-API-003 @P0
  Scenario: Retrieve all events
    When I fetch all EventHub events
    Then the response status code should be 200
    And the response body should match the "eventhub-events-list-response.json" schema

  @regression @events @EH-API-004
  Scenario: Create a new event
    When I create a new EventHub event using the "eventhub_create_event.json" payload
    Then the response status code should be 201
    And the response body should match the "eventhub-event-response.json" schema
    And the event id should be stored for subsequent requests
