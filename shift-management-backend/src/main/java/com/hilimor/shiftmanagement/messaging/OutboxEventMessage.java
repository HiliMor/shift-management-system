package com.hilimor.shiftmanagement.messaging;

import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;

public record OutboxEventMessage(
        UUID eventId,
        String eventType,
        JsonNode payload
) {

    static OutboxEventMessage from(EventOutbox event) {
        return new OutboxEventMessage(
                event.getEventId(),
                event.getEventType(),
                event.getPayload()
        );
    }
}
