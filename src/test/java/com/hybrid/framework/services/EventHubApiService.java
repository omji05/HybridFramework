package com.hybrid.framework.services;

import com.hybrid.framework.api.ApiAuthManager;
import com.hybrid.framework.api.ApiResponseUtils;
import com.hybrid.framework.config.ConfigReader;
import com.hybrid.framework.context.ContextKeys;
import com.hybrid.framework.context.TestContext;
import com.hybrid.framework.testdata.EventHubAttendeeData;
import com.hybrid.framework.testdata.EventHubEventData;
import com.hybrid.framework.testdata.EventHubTestDataFactory;
import io.restassured.response.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.Assert;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

/**
 * EventHub API domain orchestration — auth, events, and bookings against
 * {@code https://api.eventhub.rahulshettyacademy.com/api}.
 */
public class EventHubApiService {

    private static final Logger LOG = LogManager.getLogger(EventHubApiService.class);

    private final TestContext testContext;
    private final ApiHttpService apiHttp;
    private final ConfigReader config;

    public EventHubApiService(TestContext testContext, ApiHttpService apiHttp) {
        this.testContext = testContext;
        this.apiHttp = apiHttp;
        this.config = ConfigReader.getInstance();
    }

    public void registerUser(String fileName) {
        String uniqueEmail = "api_" + UUID.randomUUID().toString().substring(0, 8) + "@gmail.com";
        LOG.info("Registering EventHub user with email '{}'", uniqueEmail);
        apiHttp.setPayloadFromFile(fileName, Map.of("email", uniqueEmail));
        apiHttp.post("/auth/register");
        testContext.put(ContextKeys.TEST_USER_EMAIL, uniqueEmail);
    }

    public void loginUser(String fileName) {
        String email = config.getProperty("test.email");
        String password = config.getProperty("test.password");
        LOG.info("Logging in to EventHub as '{}'", email);
        apiHttp.setPayloadFromFile(fileName, Map.of(
                "email", email,
                "password", password
        ));
        apiHttp.post("/auth/login");
    }

    public void authenticate(String fileName) {
        loginUser(fileName);
        storeAuthToken();
    }

    public void getEvents() {
        LOG.info("Fetching all EventHub events");
        apiHttp.get("/events");
    }

    public void createEvent(String fileName) {
        EventHubEventData eventData = EventHubTestDataFactory.randomEvent();
        String eventDate = Instant.now().plus(180, ChronoUnit.DAYS).toString();
        LOG.info("Creating EventHub event '{}'", eventData.title());
        apiHttp.setPayloadFromFile(fileName, Map.of(
                "title", eventData.title(),
                "description", eventData.description(),
                "category", eventData.category(),
                "venue", eventData.venue(),
                "city", eventData.city(),
                "eventDate", eventDate,
                "price", String.valueOf(eventData.price()),
                "totalSeats", String.valueOf(eventData.totalSeats())
        ));
        apiHttp.post("/events");
        testContext.put(ContextKeys.EVENT_TITLE, eventData.title());
    }

    public void getBookings() {
        LOG.info("Fetching all EventHub bookings");
        apiHttp.get("/bookings");
    }

    public void getBookingById() {
        Integer bookingId = testContext.get(ContextKeys.BOOKING_ID, Integer.class);
        Assert.assertNotNull(bookingId, "Booking id must be stored in TestContext before fetching");
        LOG.info("Fetching EventHub booking with id '{}'", bookingId);
        apiHttp.getWithPathParam("/bookings/{id}", "id", String.valueOf(bookingId));
    }

    public void createBooking(String fileName) {
        Integer eventId = testContext.get(ContextKeys.EVENT_ID, Integer.class);
        Assert.assertNotNull(eventId, "Event id must be stored in TestContext before creating a booking");
        EventHubAttendeeData attendee = EventHubTestDataFactory.randomAttendee();
        LOG.info("Creating EventHub booking for event id '{}'", eventId);
        apiHttp.setPayloadFromFile(fileName, Map.of(
                "eventId", String.valueOf(eventId),
                "customerName", attendee.name(),
                "customerEmail", attendee.email(),
                "customerPhone", attendee.phone()
        ));
        apiHttp.post("/bookings");
        testContext.put(ContextKeys.ATTENDEE_DATA, attendee);
    }

    public void assertJwtTokenPresent() {
        Response response = requireLastResponse();
        ApiResponseUtils.assertJsonFieldNotNull(response, "token");
        String token = ApiResponseUtils.requireString(response, "token", "JWT token should be present");
        Assert.assertFalse(token.isBlank(), "JWT token should not be blank");
        LOG.info("JWT token is present in response");
    }

    public void storeAuthToken() {
        Response response = requireLastResponse();
        String token = ApiResponseUtils.requireString(response, "token", "JWT token should be present");
        testContext.put(ContextKeys.ACCESS_TOKEN, token);
        ApiAuthManager.setAccessToken(token);
        LOG.info("JWT token stored in TestContext and ApiAuthManager");
    }

    public void storeEventId() {
        Response response = requireLastResponse();
        int eventId = ApiResponseUtils.getInt(response, "data.id");
        testContext.put(ContextKeys.EVENT_ID, eventId);
        LOG.info("Event id '{}' stored in TestContext", eventId);
    }

    public void storeBookingId() {
        Response response = requireLastResponse();
        int bookingId = ApiResponseUtils.getInt(response, "data.id");
        testContext.put(ContextKeys.BOOKING_ID, bookingId);
        LOG.info("Booking id '{}' stored in TestContext", bookingId);
    }

    private Response requireLastResponse() {
        Response response = testContext.getLastResponse();
        Assert.assertNotNull(response, "No API response available - send a request first");
        return response;
    }
}
