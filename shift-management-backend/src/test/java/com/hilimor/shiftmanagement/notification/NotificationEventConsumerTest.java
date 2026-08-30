package com.hilimor.shiftmanagement.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hilimor.shiftmanagement.messaging.OutboxEventMessage;
import com.hilimor.shiftmanagement.request.SwapRequestCreatedEvent;
import com.hilimor.shiftmanagement.request.SwapRequestType;
import com.hilimor.shiftmanagement.schedule.SchedulePublishedEvent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationEventConsumerTest {

    @Mock
    private SchedulePublishedNotificationService schedulePublishedNotificationService;

    @Mock
    private RequestCreatedNotificationService requestCreatedNotificationService;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private NotificationEventConsumer notificationEventConsumer;

    @BeforeEach
    void setUp() {
        notificationEventConsumer = new NotificationEventConsumer(
                objectMapper,
                schedulePublishedNotificationService,
                requestCreatedNotificationService
        );
    }

    @Test
    void receiveHandlesSchedulePublishedEvent() throws Exception {
        UUID eventId = UUID.randomUUID();
        SchedulePublishedEvent event = schedulePublishedEvent();
        String message = objectMapper.writeValueAsString(new OutboxEventMessage(
                eventId,
                "schedule.published",
                objectMapper.valueToTree(event)
        ));

        notificationEventConsumer.receive(message);

        ArgumentCaptor<SchedulePublishedEvent> eventCaptor = ArgumentCaptor.forClass(SchedulePublishedEvent.class);
        verify(schedulePublishedNotificationService).createNotifications(eq(eventId), eventCaptor.capture());
        assertThat(eventCaptor.getValue().scheduleId()).isEqualTo(10L);
        assertThat(eventCaptor.getValue().teamId()).isEqualTo(1L);
        assertThat(eventCaptor.getValue().teamName()).isEqualTo("Operations");
    }

    @Test
    void receiveIgnoresUnknownEventTypes() throws Exception {
        UUID eventId = UUID.randomUUID();
        String message = objectMapper.writeValueAsString(new OutboxEventMessage(
                eventId,
                "unknown.event",
                objectMapper.valueToTree(schedulePublishedEvent())
        ));

        notificationEventConsumer.receive(message);

        verify(schedulePublishedNotificationService, never())
                .createNotifications(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(requestCreatedNotificationService, never())
                .createNotifications(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void receiveHandlesRequestCreatedEvent() throws Exception {
        UUID eventId = UUID.randomUUID();
        SwapRequestCreatedEvent event = requestCreatedEvent();
        String message = objectMapper.writeValueAsString(new OutboxEventMessage(
                eventId,
                "request.created",
                objectMapper.valueToTree(event)
        ));

        notificationEventConsumer.receive(message);

        ArgumentCaptor<SwapRequestCreatedEvent> eventCaptor = ArgumentCaptor.forClass(SwapRequestCreatedEvent.class);
        verify(requestCreatedNotificationService).createNotifications(eq(eventId), eventCaptor.capture());
        assertThat(eventCaptor.getValue().requestId()).isEqualTo(40L);
        assertThat(eventCaptor.getValue().requestType()).isEqualTo(SwapRequestType.TRANSFER);
        assertThat(eventCaptor.getValue().targetEmployeeId()).isEqualTo(3L);
    }

    private SchedulePublishedEvent schedulePublishedEvent() {
        return new SchedulePublishedEvent(
                10L,
                1L,
                "Operations",
                LocalDate.of(2026, 8, 2),
                LocalDate.of(2026, 8, 8),
                1,
                Instant.parse("2026-08-02T18:00:00Z")
        );
    }

    private SwapRequestCreatedEvent requestCreatedEvent() {
        return new SwapRequestCreatedEvent(
                40L,
                SwapRequestType.TRANSFER,
                1L,
                "Operations",
                2L,
                "Employee One",
                3L,
                "Employee Two",
                30L,
                20L,
                "Morning shift",
                Instant.parse("2026-08-04T06:00:00Z"),
                Instant.parse("2026-08-04T14:00:00Z"),
                null,
                null,
                null,
                null,
                null,
                Instant.parse("2026-08-04T05:00:00Z")
        );
    }
}
