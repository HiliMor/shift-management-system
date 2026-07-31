package com.hilimor.shiftmanagement.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.Instant;
import java.util.UUID;

import com.hilimor.shiftmanagement.user.ApplicationRole;
import com.hilimor.shiftmanagement.user.User;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class NotificationTest {

    @Test
    void newNotificationStartsUnread() {
        UUID eventId = UUID.randomUUID();
        User recipient = employee();
        Instant createdAt = Instant.parse("2026-07-31T18:00:00Z");

        Notification notification = new Notification(
                eventId,
                recipient,
                NotificationType.SCHEDULE_PUBLISHED,
                "Schedule published",
                "The Operations schedule was published.",
                "SCHEDULE",
                10L,
                createdAt
        );

        assertThat(notification.getEventId()).isEqualTo(eventId);
        assertThat(notification.getRecipient()).isSameAs(recipient);
        assertThat(notification.getType()).isEqualTo(NotificationType.SCHEDULE_PUBLISHED);
        assertThat(notification.getTitle()).isEqualTo("Schedule published");
        assertThat(notification.getMessage()).isEqualTo("The Operations schedule was published.");
        assertThat(notification.getRelatedEntityType()).isEqualTo("SCHEDULE");
        assertThat(notification.getRelatedEntityId()).isEqualTo(10L);
        assertThat(notification.getCreatedAt()).isEqualTo(createdAt);
        assertThat(notification.getReadAt()).isNull();
        assertThat(notification.isRead()).isFalse();
    }

    @Test
    void titleMustNotBeBlank() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Notification(
                        UUID.randomUUID(),
                        employee(),
                        NotificationType.SCHEDULE_PUBLISHED,
                        " ",
                        "The Operations schedule was published.",
                        "SCHEDULE",
                        10L,
                        Instant.now()
                ));
    }

    @Test
    void messageMustNotBeBlank() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Notification(
                        UUID.randomUUID(),
                        employee(),
                        NotificationType.SCHEDULE_PUBLISHED,
                        "Schedule published",
                        " ",
                        "SCHEDULE",
                        10L,
                        Instant.now()
                ));
    }

    @Test
    void markReadStoresTimestamp() {
        Notification notification = notification();
        Instant readAt = Instant.parse("2026-07-31T18:05:00Z");

        notification.markRead(readAt);

        assertThat(notification.getReadAt()).isEqualTo(readAt);
        assertThat(notification.isRead()).isTrue();
    }

    @Test
    void markReadDoesNotOverwriteExistingReadTimestamp() {
        Notification notification = notification();
        Instant firstReadAt = Instant.parse("2026-07-31T18:05:00Z");
        Instant secondReadAt = Instant.parse("2026-07-31T18:10:00Z");

        notification.markRead(firstReadAt);
        notification.markRead(secondReadAt);

        assertThat(notification.getReadAt()).isEqualTo(firstReadAt);
    }

    private Notification notification() {
        return new Notification(
                UUID.randomUUID(),
                employee(),
                NotificationType.SCHEDULE_PUBLISHED,
                "Schedule published",
                "The Operations schedule was published.",
                "SCHEDULE",
                10L,
                Instant.parse("2026-07-31T18:00:00Z")
        );
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
