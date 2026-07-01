@ui @bookings
Feature: EventHub My Bookings
  In order to review ticket purchases
  As a signed-in EventHub user
  I want to view and manage my bookings

  Background:
    Given the user is signed in to EventHub

  @smoke @BOOKINGS-001 @P0
  Scenario: My Bookings page loads for a signed-in user
    When the user navigates to the My Bookings page
    Then the My Bookings page should be displayed
    And the bookings page should show either existing bookings or an empty state

  @regression @BOOKINGS-002 @P1
  Scenario: Bookings page provides a path to browse events
    When the user navigates to the My Bookings page
    Then the My Bookings page should be displayed
    And the bookings page should offer a way to browse events

  @regression @BOOKINGS-003 @P2
  Scenario: User can open events from the bookings page
    When the user navigates to the My Bookings page
    And the user opens Browse Events from the bookings page
    Then the user should land on the events listing page from bookings

  @regression @BOOKINGS-004 @P2
  Scenario: Existing bookings expose management controls
    When the user navigates to the My Bookings page
    Then the My Bookings page should be displayed
    And the clear all bookings control should be available when bookings exist
