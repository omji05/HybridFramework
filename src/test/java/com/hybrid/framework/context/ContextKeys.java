package com.hybrid.framework.context;

/**
 * Well-known {@link TestContext} map keys for hybrid API / UI / DB handoff.
 * Domain services (e.g. {@link com.hybrid.framework.services.RealWorldApiService}) use
 * {@link TestContext#put} / {@link TestContext#get} with these constants.
 */
public final class ContextKeys {

    public static final String USER_ID = "userId";
    public static final String USER_NAME = "userName";
    public static final String ACCESS_TOKEN = "accessToken";

    /** Slug of the most recently created RealWorld article, used for chained fetch/update/delete. */
    public static final String ARTICLE_SLUG = "articleSlug";

    /** Request body for the next POST/PUT in {@link com.hybrid.framework.services.ApiHttpService}. */
    public static final String API_PAYLOAD = "apiPayload";

    /** Request body for order API calls. */
    public static final String ORDER_PAYLOAD = "orderPayload";
    /** Request body for summary API calls. */
    public static final String SUMMARY_PAYLOAD = "summaryPayload";

    // ── EventHub UI keys ───────────────────────────────────────────────────

    /** Email of the currently authenticated EventHub user (UI session). */
    public static final String TEST_USER_EMAIL = "testUserEmail";

    /**
     * Holds a reference to the {@link com.hybrid.framework.pages.eventhub.EventHubHomePage}
     * instance after a successful sign-in.
     */
    public static final String UI_HOME_PAGE = "uiHomePage";

    /** Title of the most recently created EventHub event in UI tests. */
    public static final String EVENT_TITLE = "eventTitle";

    /** Full faker-generated payload for the most recent EventHub event form fill. */
    public static final String EVENT_DATA = "eventData";

    /** Faker-generated attendee details for the most recent booking form fill. */
    public static final String ATTENDEE_DATA = "attendeeData";

    // ── EventHub API keys ──────────────────────────────────────────────────

    /** Numeric ID of the most recently created EventHub event (API). */
    public static final String EVENT_ID = "eventId";

    /** Numeric ID of the most recently created EventHub booking (API). */
    public static final String BOOKING_ID = "bookingId";

    private ContextKeys() {
    }
}
