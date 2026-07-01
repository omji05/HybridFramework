@api @eventhub @epic-EH-100
Feature: EventHub API - Authentication
  As an EventHub platform user
  I want to register and authenticate via the REST API
  So that I can obtain a JWT for protected EventHub operations

  @smoke @registration @EH-API-001 @P0
  Scenario: Successfully register a new user
    When I register a new EventHub user using the "eventhub_register.json" payload
    Then the response status code should be 201
    And the response body should match the "eventhub-auth-response.json" schema
    And the EventHub response should contain a valid JWT token

  # @smoke @auth @EH-API-002
  # Scenario: Successfully log in with valid credentials
  #   When I log in to EventHub using the "eventhub_login.json" payload
  #   Then the response status code should be 200
  #   And the response body should match the "eventhub-auth-response.json" schema
  #   And the EventHub response should contain a valid JWT token
  #   And the EventHub auth token should be stored for subsequent requests
