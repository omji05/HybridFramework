package com.hybrid.framework.pages.eventhub;

import com.hybrid.framework.driver.DriverManager;
import com.hybrid.framework.pages.BasePage;
import com.hybrid.framework.pages.eventhub.components.EventHubNavBar;
import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;

import java.util.List;

/**
 * Page object for the EventHub sign-in page ({@code /login}).
 */
public class EventHubLoginPage extends BasePage {

    private final By pageHeading    = By.xpath("//h1[normalize-space()='Sign in to EventHub']");
    private final By emailField     = By.id("email");
    private final By passwordField  = By.id("password");
    private final By signInButton   = By.id("login-btn");
    private final By registerLink   = By.linkText("Register");
    private final By toastMessages  = By.cssSelector("[aria-live='polite'] p");

    @Step("Enter email address on sign in form")
    public EventHubLoginPage enterEmail(String email) {
        type(emailField, email);
        return this;
    }

    @Step("Enter password on sign in form")
    public EventHubLoginPage enterPassword(String password) {
        type(passwordField, password);
        return this;
    }

    @Step("Submit sign in form (expecting success)")
    public EventHubHomePage submitSignIn() {
        click(signInButton);
        waitForAuthenticatedLanding();
        return new EventHubHomePage();
    }

    @Step("Submit sign in form (expecting error)")
    public EventHubLoginPage submitSignInExpectingError() {
        click(signInButton);
        return this;
    }

    @Step("Click 'Register' to navigate to registration")
    public EventHubRegistrationPage navigateToRegistration() {
        click(registerLink);
        return new EventHubRegistrationPage();
    }

    @Step("Authenticate with email and password (expecting success)")
    public EventHubHomePage authenticateAs(String email, String password) {
        return enterEmail(email)
                .enterPassword(password)
                .submitSignIn();
    }

    @Step("Attempt authentication with email and password (expecting error)")
    public EventHubLoginPage attemptAuthenticationAs(String email, String password) {
        return enterEmail(email)
                .enterPassword(password)
                .submitSignInExpectingError();
    }

    public boolean isSignInPageDisplayed() {
        return isDisplayed(signInButton) && isDisplayed(pageHeading);
    }

    public boolean hasRegisterOption() {
        return isDisplayed(registerLink);
    }

    public boolean hasLogoutButton() {
        return isDisplayed(By.id("logout-btn"));
    }

    @Step("Read error toast messages from sign in form")
    public List<String> getErrorMessages() {
        waitForErrorToast();
        return findElements(toastMessages).stream()
                .map(el -> {
                    try {
                        return el.getText().trim();
                    } catch (Exception e) {
                        return "";
                    }
                })
                .filter(text -> !text.isEmpty())
                .toList();
    }

    public boolean hasErrorMessages() {
        return !getErrorMessages().isEmpty();
    }

    @Step("Read password field 'type' attribute")
    public String getPasswordFieldType() {
        return getDomAttribute(passwordField, "type");
    }

    public boolean isEmailFormatInvalid() {
        WebElement email = waitUtils.waitForVisible(emailField);
        Object valid = ((JavascriptExecutor) DriverManager.getDriver())
                .executeScript("return arguments[0].validity.valid;", email);
        Object message = ((JavascriptExecutor) DriverManager.getDriver())
                .executeScript("return arguments[0].validationMessage;", email);
        return Boolean.FALSE.equals(valid) && message != null && !message.toString().isBlank();
    }

    public EventHubNavBar navBar() {
        return new EventHubNavBar();
    }

    private void waitForAuthenticatedLanding() {
        waitUtils.fluentWait(driver -> {
            String url = driver.getCurrentUrl();
            return url != null && !url.contains("/login");
        });
        waitUtils.waitForPageLoad();
    }

    private void waitForErrorToast() {
        try {
            waitUtils.fluentWaitForElement(toastMessages, 5, 200);
        } catch (Exception ignored) {
            // HTML5 validation may block submission without showing a toast.
        }
    }
}
