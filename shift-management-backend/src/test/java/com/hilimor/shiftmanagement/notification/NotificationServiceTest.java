package com.hilimor.shiftmanagement.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.hilimor.shiftmanagement.user.ApplicationRole;
import com.hilimor.shiftmanagement.user.User;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    void listMyNotificationsReturnsNotificationsForAuthenticatedUser() {
        Notification notification = notification(employee(), 100L, UUID.randomUUID());

        when(notificationRepository.findByRecipient_UsernameOrderByCreatedAtDesc("employee1"))
                .thenReturn(List.of(notification));

        List<NotificationResponse> responses = notificationService.listMyNotifications("employee1");

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).id()).isEqualTo(100L);
        assertThat(responses.get(0).type()).isEqualTo(NotificationType.SCHEDULE_PUBLISHED);
        assertThat(responses.get(0).title()).isEqualTo("Schedule published");
        assertThat(responses.get(0).read()).isFalse();
    }

    @Test
    void countMyUnreadNotificationsReturnsRepositoryCount() {
        when(notificationRepository.countByRecipient_UsernameAndReadAtIsNull("employee1")).thenReturn(3L);

        UnreadNotificationCountResponse response = notificationService.countMyUnreadNotifications("employee1");

        assertThat(response.unreadCount()).isEqualTo(3L);
    }

    @Test
    void markMyNotificationReadMarksOnlyOwnedNotification() {
        Notification notification = notification(employee(), 100L, UUID.randomUUID());

        when(notificationRepository.findByIdAndRecipient_Username(100L, "employee1"))
                .thenReturn(Optional.of(notification));

        NotificationResponse response = notificationService.markMyNotificationRead("employee1", 100L);

        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.read()).isTrue();
        assertThat(response.readAt()).isNotNull();
        assertThat(notification.isRead()).isTrue();
    }

    @Test
    void markMyNotificationReadRejectsMissingOrUnownedNotification() {
        when(notificationRepository.findByIdAndRecipient_Username(100L, "employee1"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.markMyNotificationRead("employee1", 100L))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void createNotificationReturnsExistingNotificationForSameEventAndRecipient() {
        UUID eventId = UUID.randomUUID();
        User employee = employee();
        Notification existingNotification = notification(employee, 100L, eventId);

        when(notificationRepository.findByEventIdAndRecipient_Id(eventId, 2L))
                .thenReturn(Optional.of(existingNotification));

        NotificationResponse response = notificationService.createNotification(
                employee,
                eventId,
                NotificationType.SCHEDULE_PUBLISHED,
                "Schedule published",
                "The Operations schedule was published.",
                "SCHEDULE",
                10L,
                Instant.parse("2026-07-31T18:00:00Z")
        );

        assertThat(response.id()).isEqualTo(100L);
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void createNotificationSavesNewNotificationWhenEventWasNotHandledForRecipient() {
        UUID eventId = UUID.randomUUID();
        User employee = employee();

        when(notificationRepository.findByEventIdAndRecipient_Id(eventId, 2L)).thenReturn(Optional.empty());
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification notification = invocation.getArgument(0);
            ReflectionTestUtils.setField(notification, "id", 101L);
            return notification;
        });

        NotificationResponse response = notificationService.createNotification(
                employee,
                eventId,
                NotificationType.SCHEDULE_PUBLISHED,
                "Schedule published",
                "The Operations schedule was published.",
                "SCHEDULE",
                10L,
                Instant.parse("2026-07-31T18:00:00Z")
        );

        assertThat(response.id()).isEqualTo(101L);
        assertThat(response.eventId()).isEqualTo(eventId);
        assertThat(response.relatedEntityType()).isEqualTo("SCHEDULE");
        assertThat(response.relatedEntityId()).isEqualTo(10L);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertThat(captor.getValue().getRecipient()).isSameAs(employee);
        assertThat(captor.getValue().getType()).isEqualTo(NotificationType.SCHEDULE_PUBLISHED);
    }

    private Notification notification(User employee, Long id, UUID eventId) {
        Notification notification = new Notification(
                eventId,
                employee,
                NotificationType.SCHEDULE_PUBLISHED,
                "Schedule published",
                "The Operations schedule was published.",
                "SCHEDULE",
                10L,
                Instant.parse("2026-07-31T18:00:00Z")
        );
        ReflectionTestUtils.setField(notification, "id", id);
        return notification;
    }

    private User employee() {
        User user = new User(
                "employee1",
                "password-hash",
                "Demo Employee",
                "employee1@example.com",
                ApplicationRole.EMPLOYEE
        );
        ReflectionTestUtils.setField(user, "id", 2L);
        return user;
    }
}
