@ui @login
Feature: EventHub User Sign In
  In order to discover and book events
  As a registered EventHub user
  I want to securely sign in to my account

  Background:
    Given the user has navigated to the sign in page

  # ── Happy Path ───────────────────────────────────────────────────────────

  @smoke @LOGIN-001 @P0
  Scenario: A registered user can sign in with valid credentials
    When the user authenticates with valid registered credentials
    Then the user should land on the home page in an authenticated state
    And the navigation bar should reflect the signed-in user's email

  # ── Negative — Credential Validation ─────────────────────────────────────

  @regression @negative @LOGIN-002 @P0
  Scenario: Sign in is denied for an unregistered email address
    When the user attempts to authenticate with email "ghost@notregistered.io" and password "WrongPass1"
    Then the sign in form should display an authentication failure message
    And the user should remain on the sign in page

  @regression @negative @LOGIN-003 @P1
  Scenario: Sign in is denied when the correct email is paired with a wrong password
    When the user attempts to authenticate with the registered email and an incorrect password
    Then the sign in form should display an authentication failure message
    And the user should remain on the sign in page

  @regression @negative @LOGIN-004 @P2
  Scenario Outline: Sign in requires both email and password to be present
    When the user attempts to authenticate with email "<email>" and password "<password>"
    Then the user should not be granted access to the home page

    Examples:
      | email | password  |
      |       | Passw0rd! |
      |       |           |

  @regression @negative @LOGIN-004b @P1
  Scenario: Sign in is denied when the registered email is submitted without a password
    When the user attempts to authenticate with the registered email and a blank password
    Then the user should not be granted access to the home page

  @regression @negative @LOGIN-005
  Scenario: A syntactically invalid email address is not accepted
    When the user attempts to authenticate with email "not-a-valid-format" and password "WrongPass1"
    Then the sign in form should indicate the email address is not in a valid format

  # ── Security ─────────────────────────────────────────────────────────────

  @regression @security @LOGIN-006
  Scenario: The password field masks input to prevent credential exposure
    When the user enters their password on the sign in form
    Then the password input field should not reveal the entered characters

  # ── Navigation ───────────────────────────────────────────────────────────

  @regression @navigation @LOGIN-007
  Scenario: A visitor without an account can navigate to registration from the sign in page
    When the user selects the registration link on the sign in page
    Then the user should be directed to the registration page

  # ── Session Integrity ────────────────────────────────────────────────────

  @smoke @negative @LOGIN-008
  Scenario: A failed authentication attempt does not establish a session
    When the user attempts to authenticate with email "ghost@notregistered.io" and password "WrongPass1"
    Then the navigation bar should not display an authenticated user
    And the register option should remain visible on the sign in page
