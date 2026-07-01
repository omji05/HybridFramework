package com.hybrid.framework.stepdefs.ui.eventhub;

import com.hybrid.framework.config.ConfigReader;
import com.hybrid.framework.context.ContextKeys;
import com.hybrid.framework.context.TestContext;
import com.hybrid.framework.pages.eventhub.EventHubBookingsPage;
import com.hybrid.framework.pages.eventhub.EventHubEventsPage;
import com.hybrid.framework.pages.eventhub.EventHubHomePage;
import com.hybrid.framework.pages.eventhub.EventHubLoginPage;
import com.hybrid.framework.pages.eventhub.components.EventHubNavBar;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.testng.Assert;

/**
 * Step definitions for EventHub home page scenarios.
 */
public class EventHubHomeSteps {

    private final TestContext testContext;
    private final ConfigReader config;

    private EventHubHomePage homePage;
    private EventHubEventsPage eventsPage;
    private EventHubBookingsPage bookingsPage;
    private EventHubLoginPage loginPage;

    public EventHubHomeSteps(TestContext testContext) {
        this.testContext = testContext;
        this.config = ConfigReader.getInstance();
    }

    @When("the user is on the EventHub home page")
    public void theUserIsOnTheEventHubHomePage() {
        homePage = resolveHomePage();
        homePage = homePage.navBar().goToHome();
        testContext.put(ContextKeys.UI_HOME_PAGE, homePage);
    }

    @When("the user opens Events from the home page")
    public void theUserOpensEventsFromTheHomePage() {
        homePage = resolveHomePage();
        eventsPage = homePage.openEventsFromHome();
    }

    @When("the user opens My Bookings from the home page")
    public void theUserOpensMyBookingsFromTheHomePage() {
        homePage = resolveHomePage();
        bookingsPage = homePage.openBookingsFromHome();
    }

    @When("the user signs out from EventHub")
    public void theUserSignsOutFromEventHub() {
        homePage = resolveHomePage();
        loginPage = homePage.navBar().logout();
    }

    @Then("the EventHub home page should display the discover hero section")
    public void theEventHubHomePageShouldDisplayTheDiscoverHeroSection() {
        homePage = resolveHomePage();
        Assert.assertTrue(
                homePage.isHeroSectionDisplayed(),
                "Home page should display the discover hero section"
        );
    }

    @And("the EventHub home page should display featured events")
    public void theEventHubHomePageShouldDisplayFeaturedEvents() {
        homePage = resolveHomePage();
        Assert.assertTrue(
                homePage.isFeaturedEventsSectionDisplayed(),
                "Home page should display the featured events section"
        );
    }

    @And("the EventHub home page should offer browse and bookings shortcuts")
    public void theEventHubHomePageShouldOfferBrowseAndBookingsShortcuts() {
        homePage = resolveHomePage();
        Assert.assertTrue(homePage.isBrowseEventsCtaDisplayed(), "Browse Events CTA should be visible on home");
        Assert.assertTrue(homePage.isMyBookingsCtaDisplayed(), "My Bookings CTA should be visible on home");
    }

    @Then("the user should be on the events listing page")
    public void theUserShouldBeOnTheEventsListingPage() {
        if (eventsPage == null) {
            eventsPage = new EventHubEventsPage();
        }
        Assert.assertTrue(eventsPage.isEventsPageDisplayed(), "Events listing page should be displayed");
    }

    @Then("the user should be on the My Bookings page")
    public void theUserShouldBeOnTheMyBookingsPage() {
        if (bookingsPage == null) {
            bookingsPage = new EventHubBookingsPage();
        }
        Assert.assertTrue(bookingsPage.isBookingsPageDisplayed(), "My Bookings page should be displayed");
    }

    @Then("the user should be returned to the sign in page")
    public void theUserShouldBeReturnedToTheSignInPage() {
        if (loginPage == null) {
            loginPage = new EventHubLoginPage();
        }
        Assert.assertTrue(loginPage.isSignInPageDisplayed(), "User should be on the sign in page after logout");
    }

    @And("the navigation bar should no longer show an authenticated user")
    public void theNavigationBarShouldNoLongerShowAnAuthenticatedUser() {
        if (loginPage == null) {
            loginPage = new EventHubLoginPage();
        }
        Assert.assertFalse(loginPage.hasLogoutButton(), "Logout button should not be visible after sign out");
    }

    @And("the navigation bar should show home, events, and bookings links")
    public void theNavigationBarShouldShowHomeEventsAndBookingsLinks() {
        EventHubNavBar navBar = resolveHomePage().navBar();
        Assert.assertTrue(navBar.isHomeNavigationDisplayed(), "Home nav link should be visible");
        Assert.assertTrue(navBar.isEventsNavigationDisplayed(), "Events nav link should be visible");
        Assert.assertTrue(navBar.isBookingsNavigationDisplayed(), "Bookings nav link should be visible");
    }

    private EventHubHomePage resolveHomePage() {
        if (homePage != null) {
            return homePage;
        }
        EventHubHomePage fromContext = testContext.get(ContextKeys.UI_HOME_PAGE, EventHubHomePage.class);
        if (fromContext != null) {
            homePage = fromContext;
            return homePage;
        }
        homePage = new EventHubHomePage();
        return homePage;
    }
}
