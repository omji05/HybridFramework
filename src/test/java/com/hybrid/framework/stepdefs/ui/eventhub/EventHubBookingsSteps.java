package com.hybrid.framework.stepdefs.ui.eventhub;

import com.hybrid.framework.context.ContextKeys;
import com.hybrid.framework.context.TestContext;
import com.hybrid.framework.pages.eventhub.EventHubBookingsPage;
import com.hybrid.framework.pages.eventhub.EventHubEventsPage;
import com.hybrid.framework.pages.eventhub.EventHubHomePage;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.testng.Assert;

/**
 * Step definitions for EventHub My Bookings scenarios.
 */
public class EventHubBookingsSteps {

    private final TestContext testContext;

    private EventHubBookingsPage bookingsPage;
    private EventHubEventsPage eventsPage;

    public EventHubBookingsSteps(TestContext testContext) {
        this.testContext = testContext;
    }

    @When("the user navigates to the My Bookings page")
    public void theUserNavigatesToTheMyBookingsPage() {
        EventHubHomePage homePage = testContext.get(ContextKeys.UI_HOME_PAGE, EventHubHomePage.class);
        if (homePage == null) {
            homePage = new EventHubHomePage();
        }
        bookingsPage = homePage.navBar().goToBookings();
    }

    @When("the user opens Browse Events from the bookings page")
    public void theUserOpensBrowseEventsFromTheBookingsPage() {
        bookingsPage = resolveBookingsPage();
        if (bookingsPage.isBrowseEventsLinkDisplayed()) {
            eventsPage = bookingsPage.browseEventsFromEmptyState();
        } else {
            eventsPage = bookingsPage.navBar().goToEvents();
        }
    }

    @Then("the My Bookings page should be displayed")
    public void theMyBookingsPageShouldBeDisplayed() {
        bookingsPage = resolveBookingsPage();
        Assert.assertTrue(bookingsPage.isBookingsPageDisplayed(), "My Bookings page should be displayed");
        Assert.assertTrue(bookingsPage.isPageSubheadingDisplayed(), "Bookings page subheading should be visible");
    }

    @And("the bookings page should show either existing bookings or an empty state")
    public void theBookingsPageShouldShowEitherExistingBookingsOrAnEmptyState() {
        bookingsPage = resolveBookingsPage();
        boolean hasContent = bookingsPage.hasBookingCards() || bookingsPage.isEmptyStateDisplayed();
        Assert.assertTrue(
                hasContent,
                "Bookings page should display booking cards or an empty-state message"
        );
    }

    @And("the bookings page should offer a way to browse events")
    public void theBookingsPageShouldOfferAWayToBrowseEvents() {
        bookingsPage = resolveBookingsPage();
        Assert.assertTrue(
                bookingsPage.isBrowseEventsLinkDisplayed() || bookingsPage.hasBookingCards(),
                "Bookings page should link to events or show existing bookings"
        );
    }

    @And("the clear all bookings control should be available when bookings exist")
    public void theClearAllBookingsControlShouldBeAvailableWhenBookingsExist() {
        bookingsPage = resolveBookingsPage();
        if (bookingsPage.hasBookingCards()) {
            Assert.assertTrue(
                    bookingsPage.isClearAllBookingsButtonDisplayed(),
                    "Clear all bookings button should be visible when bookings exist"
            );
        }
    }

    @Then("the user should land on the events listing page from bookings")
    public void theUserShouldLandOnTheEventsListingPageFromBookings() {
        if (eventsPage == null) {
            eventsPage = new EventHubEventsPage();
        }
        Assert.assertTrue(eventsPage.isEventsPageDisplayed(), "Events listing should open from bookings page");
    }

    private EventHubBookingsPage resolveBookingsPage() {
        if (bookingsPage != null) {
            return bookingsPage;
        }
        bookingsPage = new EventHubBookingsPage();
        return bookingsPage;
    }
}
