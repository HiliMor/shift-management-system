package com.hilimor.shiftmanagement.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jms.JmsException;
import org.springframework.jms.core.JmsTemplate;

@ExtendWith(MockitoExtension.class)
class OutboxEventDispatcherTest {

    @Mock
    private EventOutboxRepository eventOutboxRepository;

    @Mock
    private JmsTemplate jmsTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private OutboxEventDispatcher outboxEventDispatcher;

    @BeforeEach
    void setUp() {
        outboxEventDispatcher = new OutboxEventDispatcher(
                eventOutboxRepository,
                jmsTemplate,
                objectMapper,
                "notification.events"
        );
    }

    @Test
    void dispatchPendingEventsSendsEventToNotificationQueueAndMarksItSent() throws Exception {
        UUID eventId = UUID.randomUUID();
        EventOutbox event = event(eventId);

        when(eventOutboxRepository.findTop50BySentAtIsNullOrderByCreatedAtAsc()).thenReturn(List.of(event));

        outboxEventDispatcher.dispatchPendingEvents();

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(jmsTemplate).convertAndSend(eq("notification.events"), messageCaptor.capture());

        JsonNode message = objectMapper.readTree(messageCaptor.getValue());
        assertThat(message.get("eventId").asText()).isEqualTo(eventId.toString());
        assertThat(message.get("eventType").asText()).isEqualTo("schedule.published");
        assertThat(message.get("payload").get("scheduleId").asLong()).isEqualTo(10L);
        assertThat(event.getSentAt()).isNotNull();
        assertThat(event.getAttemptCount()).isZero();
    }

    @Test
    void dispatchPendingEventsRecordsFailedAttemptWhenJmsSendFails() {
        EventOutbox event = event(UUID.randomUUID());

        when(eventOutboxRepository.findTop50BySentAtIsNullOrderByCreatedAtAsc()).thenReturn(List.of(event));
        doThrow(new JmsException("broker unavailable") {
        }).when(jmsTemplate).convertAndSend(eq("notification.events"), anyString());

        outboxEventDispatcher.dispatchPendingEvents();

        assertThat(event.getSentAt()).isNull();
        assertThat(event.getAttemptCount()).isEqualTo(1);
    }

    @Test
    void dispatchPendingEventsDoesNothingWhenThereAreNoPendingEvents() {
        when(eventOutboxRepository.findTop50BySentAtIsNullOrderByCreatedAtAsc()).thenReturn(List.of());

        outboxEventDispatcher.dispatchPendingEvents();

        verify(jmsTemplate, never()).convertAndSend(anyString(), anyString());
    }

    private EventOutbox event(UUID eventId) {
        return new EventOutbox(
                eventId,
                "schedule.published",
                objectMapper.createObjectNode().put("scheduleId", 10L),
                Instant.parse("2026-08-02T18:00:00Z")
        );
    }
}
