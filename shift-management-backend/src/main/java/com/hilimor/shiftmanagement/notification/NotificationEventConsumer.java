package com.hilimor.shiftmanagement.notification;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hilimor.shiftmanagement.messaging.OutboxEventMessage;
import com.hilimor.shiftmanagement.request.SwapRequestCreatedEvent;
import com.hilimor.shiftmanagement.schedule.SchedulePublishedEvent;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        prefix = "app.messaging.notifications",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class NotificationEventConsumer {

    static final String SCHEDULE_PUBLISHED_EVENT_TYPE = "schedule.published";
    static final String REQUEST_CREATED_EVENT_TYPE = "request.created";

    private final ObjectMapper objectMapper;
    private final SchedulePublishedNotificationService schedulePublishedNotificationService;
    private final RequestCreatedNotificationService requestCreatedNotificationService;

    public NotificationEventConsumer(
            ObjectMapper objectMapper,
            SchedulePublishedNotificationService schedulePublishedNotificationService,
            RequestCreatedNotificationService requestCreatedNotificationService
    ) {
        this.objectMapper = objectMapper;
        this.schedulePublishedNotificationService = schedulePublishedNotificationService;
        this.requestCreatedNotificationService = requestCreatedNotificationService;
    }

    @JmsListener(destination = "${app.messaging.notifications.queue}")
    public void receive(String message) throws JsonProcessingException {
        OutboxEventMessage eventMessage = objectMapper.readValue(message, OutboxEventMessage.class);

        if (SCHEDULE_PUBLISHED_EVENT_TYPE.equals(eventMessage.eventType())) {
            SchedulePublishedEvent event = objectMapper.treeToValue(
                    eventMessage.payload(),
                    SchedulePublishedEvent.class
            );
            schedulePublishedNotificationService.createNotifications(eventMessage.eventId(), event);
            return;
        }

        if (REQUEST_CREATED_EVENT_TYPE.equals(eventMessage.eventType())) {
            SwapRequestCreatedEvent event = objectMapper.treeToValue(
                    eventMessage.payload(),
                    SwapRequestCreatedEvent.class
            );
            requestCreatedNotificationService.createNotifications(eventMessage.eventId(), event);
        }
    }
}
