@ui @home
Feature: EventHub Home Page
  In order to discover events quickly
  As a signed-in EventHub user
  I want to use the home page to explore the application

  Background:
    Given the user is signed in to EventHub

  @smoke @HOME-001
  Scenario: Home page displays hero content and featured events
    When the user is on the EventHub home page
    Then the EventHub home page should display the discover hero section
    And the EventHub home page should display featured events
    And the EventHub home page should offer browse and bookings shortcuts

  @smoke @HOME-002
  Scenario: User can navigate to Events from the home page
    When the user opens Events from the home page
    Then the user should be on the events listing page

  @regression @navigation @HOME-003
  Scenario: User can navigate to My Bookings from the home page
    When the user opens My Bookings from the home page
    Then the user should be on the My Bookings page

  @smoke @HOME-004
  Scenario: Navigation bar exposes primary application sections
    When the user is on the EventHub home page
    Then the EventHub home page should display the discover hero section
    And the navigation bar should show home, events, and bookings links

  @smoke @LOGOUT-001
  Scenario: A signed-in user can sign out and return to the login page
    When the user is on the EventHub home page
    And the user signs out from EventHub
    Then the user should be returned to the sign in page
    And the navigation bar should no longer show an authenticated user
