package com.hybrid.framework.pages.eventhub;

import com.hybrid.framework.pages.BasePage;
import com.hybrid.framework.pages.eventhub.components.EventHubNavBar;
import io.qameta.allure.Step;
import org.openqa.selenium.By;

/**
 * EventHub home page ({@code /}).
 */
public class EventHubHomePage extends BasePage {

    private final By heroHeading       = By.xpath("//*[contains(normalize-space(),'Discover & Book')]");
    private final By heroSubheading      = By.xpath("//*[contains(normalize-space(),'Amazing Events')]");
    private final By featuredSection     = By.xpath("//*[contains(normalize-space(),'Featured Events')]");
    private final By browseEventsCta   = By.xpath("//a[contains(@href,'/events') and contains(.,'Browse Events')]");
    private final By myBookingsCta       = By.xpath("//a[contains(@href,'/bookings') and contains(.,'My Bookings')]");

    @Step("Verify EventHub home page is displayed")
    public boolean isHomePageDisplayed() {
        String currentUrl = getCurrentUrl();
        boolean onAuthenticatedRoute = currentUrl != null && !currentUrl.contains("/login");
        return onAuthenticatedRoute && navBar().isUserAuthenticated();
    }

    @Step("Verify home hero section is displayed")
    public boolean isHeroSectionDisplayed() {
        return isDisplayed(heroHeading) && isDisplayed(heroSubheading);
    }

    @Step("Verify featured events section is displayed")
    public boolean isFeaturedEventsSectionDisplayed() {
        return isDisplayed(featuredSection);
    }

    @Step("Open Events page from home Browse Events CTA")
    public EventHubEventsPage openEventsFromHome() {
        click(browseEventsCta);
        waitUtils.waitForPageLoad();
        return new EventHubEventsPage();
    }

    @Step("Open My Bookings from home CTA")
    public EventHubBookingsPage openBookingsFromHome() {
        click(myBookingsCta);
        waitUtils.waitForPageLoad();
        return new EventHubBookingsPage();
    }

    public boolean isBrowseEventsCtaDisplayed() {
        return isDisplayed(browseEventsCta);
    }

    public boolean isMyBookingsCtaDisplayed() {
        return isDisplayed(myBookingsCta);
    }

    public EventHubNavBar navBar() {
        return new EventHubNavBar();
    }
}
