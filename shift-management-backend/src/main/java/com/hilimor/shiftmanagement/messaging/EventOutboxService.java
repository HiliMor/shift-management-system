package com.hilimor.shiftmanagement.messaging;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EventOutboxService {

    private final EventOutboxRepository eventOutboxRepository;
    private final ObjectMapper objectMapper;

    public EventOutboxService(EventOutboxRepository eventOutboxRepository, ObjectMapper objectMapper) {
        this.eventOutboxRepository = eventOutboxRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public UUID createEvent(String eventType, Object payload) {
        JsonNode payloadJson = objectMapper.valueToTree(Objects.requireNonNull(payload, "payload must not be null"));
        EventOutbox event = new EventOutbox(UUID.randomUUID(), eventType, payloadJson, Instant.now());
        eventOutboxRepository.save(event);

        return event.getEventId();
    }
}
