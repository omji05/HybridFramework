package com.hybrid.framework.testdata;

import net.datafaker.Faker;

import java.util.List;

/**
 * Generates realistic, unique EventHub test data via Datafaker.
 */
public final class EventHubTestDataFactory {

    private static final Faker FAKER = new Faker();
    private static final List<String> CATEGORIES = List.of(
            "Conference", "Concert", "Sports", "Workshop", "Festival"
    );

    private EventHubTestDataFactory() {
    }

    public static EventHubEventData randomEvent() {
        return new EventHubEventData(
                uniqueTitle(),
                FAKER.lorem().paragraph(2),
                FAKER.options().nextElement(CATEGORIES),
                FAKER.address().city(),
                FAKER.company().name() + ", " + FAKER.address().streetAddress(),
                FAKER.number().numberBetween(99, 999),
                FAKER.number().numberBetween(50, 500)
        );
    }

    public static EventHubEventData randomEventWithTitle(String title) {
        EventHubEventData base = randomEvent();
        return new EventHubEventData(
                title,
                base.description(),
                base.category(),
                base.city(),
                base.venue(),
                base.price(),
                base.totalSeats()
        );
    }

    private static String uniqueTitle() {
        return FAKER.artist().name() + " Live — " + FAKER.number().digits(4);
    }

    public static EventHubAttendeeData randomAttendee() {
        return new EventHubAttendeeData(
                FAKER.name().fullName(),
                FAKER.internet().emailAddress(),
                "+91 " + FAKER.numerify("9#########")
        );
    }
}
