@api @wiremock
Feature: Login via WireMock file-based stub
  As a test engineer
  I want to stub the login API in-process
  So that tests run reliably in CI without hitting the real backend

  Scenario: Login returns stubbed token
    When I log in using the "login_user.json" payload
    Then the response status code should be 200
    And the response body should match the "user-response.json" schema
    And the response should contain a valid auth token

  @api @wiremock @regression
  Scenario: Invalid login returns 401 file based stub
    When I attempt to log in using the "login_user.json" payload with email "bad@example.com" and password "wrongpass"
    Then the response status code should be 401
    And the API error response should be present
