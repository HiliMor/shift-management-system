package com.hilimor.shiftmanagement.notification;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.hilimor.shiftmanagement.user.User;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> listMyNotifications(String username) {
        return notificationRepository.findByRecipient_UsernameOrderByCreatedAtDesc(username)
                .stream()
                .map(NotificationResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public UnreadNotificationCountResponse countMyUnreadNotifications(String username) {
        long unreadCount = notificationRepository.countByRecipient_UsernameAndReadAtIsNull(username);
        return new UnreadNotificationCountResponse(unreadCount);
    }

    @Transactional
    public NotificationResponse markMyNotificationRead(String username, Long notificationId) {
        Notification notification = notificationRepository.findByIdAndRecipient_Username(notificationId, username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found"));

        notification.markRead(Instant.now());
        return NotificationResponse.from(notification);
    }

    @Transactional
    public NotificationResponse createNotification(
            User recipient,
            UUID eventId,
            NotificationType type,
            String title,
            String message,
            String relatedEntityType,
            Long relatedEntityId,
            Instant createdAt
    ) {
        return notificationRepository.findByEventIdAndRecipient_Id(eventId, recipient.getId())
                .map(NotificationResponse::from)
                .orElseGet(() -> NotificationResponse.from(notificationRepository.save(new Notification(
                        eventId,
                        recipient,
                        type,
                        title,
                        message,
                        relatedEntityType,
                        relatedEntityId,
                        createdAt
                ))));
    }
}
