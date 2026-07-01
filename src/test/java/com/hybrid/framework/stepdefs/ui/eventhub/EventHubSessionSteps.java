package com.hybrid.framework.stepdefs.ui.eventhub;

import com.hybrid.framework.config.ConfigReader;
import com.hybrid.framework.context.ContextKeys;
import com.hybrid.framework.context.TestContext;
import com.hybrid.framework.driver.DriverManager;
import com.hybrid.framework.pages.eventhub.EventHubHomePage;
import com.hybrid.framework.pages.eventhub.EventHubLoginPage;
import io.cucumber.java.en.Given;
import org.testng.Assert;

/**
 * Shared EventHub session steps used as Background preconditions across UI features.
 */
public class EventHubSessionSteps {

    private final TestContext testContext;
    private final ConfigReader config;

    public EventHubSessionSteps(TestContext testContext) {
        this.testContext = testContext;
        this.config = ConfigReader.getInstance();
    }

    @Given("the user is signed in to EventHub")
    public void theUserIsSignedInToEventHub() {
        String signInUrl = config.getBaseUrl() + "/login";
        DriverManager.getDriver().get(signInUrl);
        EventHubLoginPage loginPage = new EventHubLoginPage();
        Assert.assertTrue(loginPage.isSignInPageDisplayed(), "Sign in page should load at " + signInUrl);

        String email = config.getProperty("test.email");
        String password = config.getProperty("test.password");
        EventHubHomePage homePage = loginPage.authenticateAs(email, password);

        testContext.put(ContextKeys.UI_HOME_PAGE, homePage);
        testContext.put(ContextKeys.TEST_USER_EMAIL, email);
        Assert.assertTrue(homePage.isHomePageDisplayed(), "User should be signed in and on the home page");
    }
}
