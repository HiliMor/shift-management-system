package com.hilimor.shiftmanagement.messaging;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "event_outbox")
public class EventOutbox {

    @Id
    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private JsonNode payload;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    protected EventOutbox() {
    }

    public EventOutbox(UUID eventId, String eventType, JsonNode payload, Instant createdAt) {
        this.eventId = Objects.requireNonNull(eventId, "eventId must not be null");
        this.eventType = requireText(eventType, "eventType");
        this.payload = Objects.requireNonNull(payload, "payload must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.attemptCount = 0;
    }

    public UUID getEventId() {
        return eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public JsonNode getPayload() {
        return payload;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public void markSent(Instant sentAt) {
        this.sentAt = Objects.requireNonNull(sentAt, "sentAt must not be null");
    }

    public void recordFailedAttempt() {
        attemptCount += 1;
    }

    private String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }

        return value;
    }
}
