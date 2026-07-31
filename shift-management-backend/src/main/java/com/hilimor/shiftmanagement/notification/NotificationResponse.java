package com.hilimor.shiftmanagement.notification;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        Long id,
        UUID eventId,
        NotificationType type,
        String title,
        String message,
        String relatedEntityType,
        Long relatedEntityId,
        Instant createdAt,
        Instant readAt,
        boolean read
) {

    static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getEventId(),
                notification.getType(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getRelatedEntityType(),
                notification.getRelatedEntityId(),
                notification.getCreatedAt(),
                notification.getReadAt(),
                notification.isRead()
        );
    }
}
