package com.hilimor.shiftmanagement.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

class EventOutboxTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void newEventStartsAsPending() {
        UUID eventId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-07-31T18:00:00Z");
        JsonNode payload = objectMapper.createObjectNode().put("scheduleId", 10L);

        EventOutbox event = new EventOutbox(eventId, "schedule.published", payload, createdAt);

        assertThat(event.getEventId()).isEqualTo(eventId);
        assertThat(event.getEventType()).isEqualTo("schedule.published");
        assertThat(event.getPayload()).isSameAs(payload);
        assertThat(event.getCreatedAt()).isEqualTo(createdAt);
        assertThat(event.getSentAt()).isNull();
        assertThat(event.getAttemptCount()).isZero();
    }

    @Test
    void eventTypeMustNotBeBlank() {
        JsonNode payload = objectMapper.createObjectNode().put("scheduleId", 10L);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> new EventOutbox(UUID.randomUUID(), " ", payload, Instant.now()));
    }

    @Test
    void payloadMustNotBeNull() {
        assertThatNullPointerException()
                .isThrownBy(() -> new EventOutbox(UUID.randomUUID(), "schedule.published", null, Instant.now()));
    }

    @Test
    void markSentStoresSentTimestamp() {
        EventOutbox event = new EventOutbox(
                UUID.randomUUID(),
                "schedule.published",
                objectMapper.createObjectNode().put("scheduleId", 10L),
                Instant.parse("2026-07-31T18:00:00Z")
        );
        Instant sentAt = Instant.parse("2026-07-31T18:05:00Z");

        event.markSent(sentAt);

        assertThat(event.getSentAt()).isEqualTo(sentAt);
    }

    @Test
    void recordFailedAttemptIncrementsAttemptCount() {
        EventOutbox event = new EventOutbox(
                UUID.randomUUID(),
                "schedule.published",
                objectMapper.createObjectNode().put("scheduleId", 10L),
                Instant.parse("2026-07-31T18:00:00Z")
        );

        event.recordFailedAttempt();
        event.recordFailedAttempt();

        assertThat(event.getAttemptCount()).isEqualTo(2);
    }
}
