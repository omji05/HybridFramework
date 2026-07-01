package com.hybrid.framework.testdata;

/**
 * Immutable event payload for EventHub admin create-form UI tests.
 */
public record EventHubEventData(
        String title,
        String description,
        String category,
        String city,
        String venue,
        int price,
        int totalSeats
) {
}
