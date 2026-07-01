package com.hybrid.framework.testdata;

/**
 * Immutable attendee payload for EventHub booking form UI tests.
 */
public record EventHubAttendeeData(
        String name,
        String email,
        String phone
) {
}
