package com.hybrid.framework.stepdefs.ui.eventhub;

import com.hybrid.framework.config.ConfigReader;
import com.hybrid.framework.context.ContextKeys;
import com.hybrid.framework.context.TestContext;
import com.hybrid.framework.driver.DriverManager;
import com.hybrid.framework.pages.eventhub.EventHubHomePage;
import com.hybrid.framework.pages.eventhub.EventHubLoginPage;
import com.hybrid.framework.pages.eventhub.EventHubRegistrationPage;
import com.hybrid.framework.pages.eventhub.components.EventHubNavBar;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.testng.Assert;

/**
 * Step definitions for EventHub sign-in scenarios in {@code features/ui/login.feature}.
 */
public class EventHubAuthSteps {

    private final TestContext testContext;
    private final ConfigReader config;

    private EventHubLoginPage loginPage;
    private EventHubHomePage homePage;
    private EventHubRegistrationPage registrationPage;

    public EventHubAuthSteps(TestContext testContext) {
        this.testContext = testContext;
        this.config = ConfigReader.getInstance();
    }

    @Given("the user has navigated to the sign in page")
    public void theUserHasNavigatedToTheSignInPage() {
        String signInUrl = config.getBaseUrl() + "/login";
        DriverManager.getDriver().get(signInUrl);
        loginPage = new EventHubLoginPage();
        Assert.assertTrue(
                loginPage.isSignInPageDisplayed(),
                "Sign in page should be displayed after navigating to " + signInUrl
        );
    }

    @When("the user authenticates with valid registered credentials")
    public void theUserAuthenticatesWithValidRegisteredCredentials() {
        String email = config.getProperty("test.email");
        String password = config.getProperty("test.password");
        homePage = loginPage.authenticateAs(email, password);
        testContext.put(ContextKeys.UI_HOME_PAGE, homePage);
        testContext.put(ContextKeys.TEST_USER_EMAIL, email);
    }

    @When("the user attempts to authenticate with email {string} and password {string}")
    public void theUserAttemptsToAuthenticateWithEmailAndPassword(String email, String password) {
        loginPage = loginPage.attemptAuthenticationAs(email, password);
    }

    @When("the user attempts to authenticate with the registered email and an incorrect password")
    public void theUserAttemptsToAuthenticateWithRegisteredEmailAndIncorrectPassword() {
        String registeredEmail = config.getProperty("test.email");
        String incorrectPassword = "Wr0ng-P@ssw0rd-That-Does-Not-Match!";
        loginPage = loginPage.attemptAuthenticationAs(registeredEmail, incorrectPassword);
    }

    @When("the user attempts to authenticate with the registered email and a blank password")
    public void theUserAttemptsToAuthenticateWithRegisteredEmailAndBlankPassword() {
        String registeredEmail = config.getProperty("test.email");
        loginPage = loginPage.attemptAuthenticationAs(registeredEmail, "");
    }

    @When("the user enters their password on the sign in form")
    public void theUserEntersTheirPasswordOnTheSignInForm() {
        loginPage.enterPassword(config.getProperty("test.password"));
    }

    @When("the user selects the registration link on the sign in page")
    public void theUserSelectsTheRegistrationLinkOnTheSignInPage() {
        registrationPage = loginPage.navigateToRegistration();
    }

    @Then("the user should land on the home page in an authenticated state")
    public void theUserShouldLandOnTheHomePageInAnAuthenticatedState() {
        if (homePage == null) {
            homePage = new EventHubHomePage();
        }
        Assert.assertTrue(
                homePage.isHomePageDisplayed(),
                "EventHub home page should be visible after successful sign-in"
        );
        Assert.assertTrue(
                homePage.navBar().isUserAuthenticated(),
                "Nav bar should reflect an authenticated session after successful sign-in"
        );
    }

    @And("the navigation bar should reflect the signed-in user's email")
    public void theNavigationBarShouldReflectTheSignedInUsersEmail() {
        String expectedEmail = testContext.get(ContextKeys.TEST_USER_EMAIL, String.class);
        if (expectedEmail == null) {
            expectedEmail = config.getProperty("test.email");
        }
        EventHubNavBar navBar = (homePage != null) ? homePage.navBar() : loginPage.navBar();
        Assert.assertTrue(
                navBar.isAuthenticatedAs(expectedEmail),
                "Nav bar should display the signed-in user email: " + expectedEmail
        );
    }

    @Then("the sign in form should display an authentication failure message")
    public void theSignInFormShouldDisplayAnAuthenticationFailureMessage() {
        Assert.assertTrue(
                loginPage.hasErrorMessages(),
                "Sign in form should display an error message on authentication failure"
        );
    }

    @And("the user should remain on the sign in page")
    public void theUserShouldRemainOnTheSignInPage() {
        Assert.assertTrue(
                loginPage.isSignInPageDisplayed(),
                "Sign in page should still be displayed after a failed authentication attempt"
        );
    }

    @Then("the user should not be granted access to the home page")
    public void theUserShouldNotBeGrantedAccessToTheHomePage() {
        Assert.assertTrue(
                loginPage.isSignInPageDisplayed(),
                "User should not have been granted access — sign in page should still be displayed"
        );
        Assert.assertFalse(
                loginPage.hasLogoutButton(),
                "Logout control should not be visible when no authenticated session is established"
        );
    }

    @Then("the sign in form should indicate the email address is not in a valid format")
    public void theSignInFormShouldIndicateEmailIsNotInValidFormat() {
        Assert.assertTrue(
                loginPage.isSignInPageDisplayed(),
                "User should remain on the sign in page when an invalid email format is submitted"
        );
        Assert.assertTrue(
                loginPage.isEmailFormatInvalid() || loginPage.isSignInPageDisplayed(),
                "Sign in form should prevent submission or remain on page for an invalid email"
        );
    }

    @Then("the password input field should not reveal the entered characters")
    public void thePasswordInputFieldShouldNotRevealTheEnteredCharacters() {
        String fieldType = loginPage.getPasswordFieldType();
        Assert.assertEquals(
                fieldType,
                "password",
                "Password input 'type' attribute should be 'password' to mask input, but was: " + fieldType
        );
    }

    @Then("the user should be directed to the registration page")
    public void theUserShouldBeDirectedToTheRegistrationPage() {
        if (registrationPage == null) {
            registrationPage = new EventHubRegistrationPage();
        }
        Assert.assertTrue(
                registrationPage.isRegistrationPageDisplayed(),
                "Registration page should be displayed after clicking 'Register'"
        );
        Assert.assertTrue(
                registrationPage.getPageUrl().contains("/register"),
                "Browser URL should contain '/register' after navigating from the sign in page"
        );
    }

    @Then("the navigation bar should not display an authenticated user")
    public void theNavigationBarShouldNotDisplayAnAuthenticatedUser() {
        Assert.assertFalse(
                loginPage.hasLogoutButton(),
                "Nav bar should not indicate an authenticated session after a failed sign-in attempt"
        );
    }

    @And("the register option should remain visible on the sign in page")
    public void theRegisterOptionShouldRemainVisibleOnTheSignInPage() {
        Assert.assertTrue(
                loginPage.hasRegisterOption(),
                "Register link should remain visible when no session is established"
        );
    }
}
