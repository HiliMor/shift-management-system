package com.hilimor.shiftmanagement.messaging;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jms.JmsException;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@ConditionalOnProperty(
        prefix = "app.messaging.outbox-dispatch",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class OutboxEventDispatcher {

    private static final Logger log = LoggerFactory.getLogger(OutboxEventDispatcher.class);

    private final EventOutboxRepository eventOutboxRepository;
    private final JmsTemplate jmsTemplate;
    private final ObjectMapper objectMapper;
    private final String notificationQueue;

    public OutboxEventDispatcher(
            EventOutboxRepository eventOutboxRepository,
            JmsTemplate jmsTemplate,
            ObjectMapper objectMapper,
            @Value("${app.messaging.notifications.queue}") String notificationQueue
    ) {
        this.eventOutboxRepository = eventOutboxRepository;
        this.jmsTemplate = jmsTemplate;
        this.objectMapper = objectMapper;
        this.notificationQueue = notificationQueue;
    }

    @Scheduled(fixedDelayString = "${app.messaging.outbox-dispatch.fixed-delay-ms:5000}")
    @Transactional
    public void dispatchPendingEvents() {
        List<EventOutbox> pendingEvents = eventOutboxRepository.findTop50BySentAtIsNullOrderByCreatedAtAsc();

        for (EventOutbox event : pendingEvents) {
            try {
                String message = objectMapper.writeValueAsString(OutboxEventMessage.from(event));
                jmsTemplate.convertAndSend(notificationQueue, message);
                event.markSent(Instant.now());
                log.info(
                        "Dispatched outbox event {} of type {} to queue {}",
                        event.getEventId(),
                        event.getEventType(),
                        notificationQueue
                );
            } catch (JsonProcessingException | JmsException exception) {
                event.recordFailedAttempt();
                log.warn("Failed to dispatch outbox event {}: {}", event.getEventId(), exception.getMessage());
            }
        }
    }
}
