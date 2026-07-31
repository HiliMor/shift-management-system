package com.hilimor.shiftmanagement.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class EventOutboxServiceTest {

    private final EventOutboxRepository eventOutboxRepository = org.mockito.Mockito.mock(EventOutboxRepository.class);
    private final EventOutboxService eventOutboxService = new EventOutboxService(
            eventOutboxRepository,
            new ObjectMapper().findAndRegisterModules()
    );

    @Test
    void createEventSavesPendingEventWithJsonPayload() {
        SchedulePublishedPayload payload = new SchedulePublishedPayload(
                10L,
                1L,
                "Operations",
                LocalDate.of(2026, 8, 2)
        );

        UUID eventId = eventOutboxService.createEvent("schedule.published", payload);

        ArgumentCaptor<EventOutbox> captor = ArgumentCaptor.forClass(EventOutbox.class);
        verify(eventOutboxRepository).save(captor.capture());

        EventOutbox savedEvent = captor.getValue();
        assertThat(savedEvent.getEventId()).isEqualTo(eventId);
        assertThat(savedEvent.getEventType()).isEqualTo("schedule.published");
        assertThat(savedEvent.getPayload().get("scheduleId").asLong()).isEqualTo(10L);
        assertThat(savedEvent.getPayload().get("teamName").asText()).isEqualTo("Operations");
        assertThat(savedEvent.getCreatedAt()).isNotNull();
        assertThat(savedEvent.getSentAt()).isNull();
        assertThat(savedEvent.getAttemptCount()).isZero();
    }

    private record SchedulePublishedPayload(
            Long scheduleId,
            Long teamId,
            String teamName,
            LocalDate startDate
    ) {
    }
}
