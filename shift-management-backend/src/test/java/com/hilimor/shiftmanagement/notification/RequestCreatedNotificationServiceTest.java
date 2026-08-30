package com.hilimor.shiftmanagement.notification;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.hilimor.shiftmanagement.request.SwapRequestCreatedEvent;
import com.hilimor.shiftmanagement.request.SwapRequestType;
import com.hilimor.shiftmanagement.team.TeamManager;
import com.hilimor.shiftmanagement.team.TeamManagerRepository;
import com.hilimor.shiftmanagement.user.ApplicationRole;
import com.hilimor.shiftmanagement.user.User;
import com.hilimor.shiftmanagement.user.UserRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class RequestCreatedNotificationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TeamManagerRepository teamManagerRepository;

    @Mock
    private NotificationService notificationService;

    @Test
    void createNotificationsNotifiesTargetEmployeeAndTeamManagers() {
        UUID eventId = UUID.randomUUID();
        User targetEmployee = user("employee2", 3L, "Employee Two", ApplicationRole.EMPLOYEE);
        User manager = user("manager1", 4L, "Manager One", ApplicationRole.MANAGER);
        TeamManager teamManager = org.mockito.Mockito.mock(TeamManager.class);

        when(userRepository.findById(3L)).thenReturn(Optional.of(targetEmployee));
        when(teamManagerRepository.findByTeam_Id(1L)).thenReturn(List.of(teamManager));
        when(teamManager.getManager()).thenReturn(manager);

        RequestCreatedNotificationService service = new RequestCreatedNotificationService(
                userRepository,
                teamManagerRepository,
                notificationService
        );

        service.createNotifications(eventId, requestCreatedEvent());

        verify(notificationService).createNotification(
                eq(targetEmployee),
                eq(eventId),
                eq(NotificationType.REQUEST_CREATED),
                eq("New transfer request"),
                eq("Employee One requested to transfer shift Morning shift to you."),
                eq("REQUEST"),
                eq(40L),
                eq(Instant.parse("2026-08-04T05:00:00Z"))
        );
        verify(notificationService).createNotification(
                eq(manager),
                eq(eventId),
                eq(NotificationType.REQUEST_CREATED),
                eq("New transfer request"),
                eq("Employee One created a transfer request for the Operations team, targeting Employee Two."),
                eq("REQUEST"),
                eq(40L),
                eq(Instant.parse("2026-08-04T05:00:00Z"))
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

    private User user(String username, Long id, String fullName, ApplicationRole role) {
        User user = new User(username, "password-hash", fullName, username + "@example.com", role);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
