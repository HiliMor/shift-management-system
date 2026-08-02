package com.hilimor.shiftmanagement.notification;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.hilimor.shiftmanagement.schedule.SchedulePublishedEvent;
import com.hilimor.shiftmanagement.team.SwapApprovalPolicy;
import com.hilimor.shiftmanagement.team.Team;
import com.hilimor.shiftmanagement.team.TeamMember;
import com.hilimor.shiftmanagement.team.TeamMemberRepository;
import com.hilimor.shiftmanagement.user.ApplicationRole;
import com.hilimor.shiftmanagement.user.User;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class SchedulePublishedNotificationServiceTest {

    @Mock
    private TeamMemberRepository teamMemberRepository;

    @Mock
    private NotificationService notificationService;

    @Test
    void createNotificationsCreatesNotificationForEachActiveTeamMember() {
        UUID eventId = UUID.randomUUID();
        SchedulePublishedEvent event = schedulePublishedEvent();
        User employeeOne = employee("employee1", 2L, "Demo Employee One");
        User employeeTwo = employee("employee2", 3L, "Demo Employee Two");

        when(teamMemberRepository.findByTeam_IdAndActiveTrue(1L))
                .thenReturn(List.of(teamMember(employeeOne), teamMember(employeeTwo)));

        SchedulePublishedNotificationService service = new SchedulePublishedNotificationService(
                teamMemberRepository,
                notificationService
        );

        service.createNotifications(eventId, event);

        verify(notificationService).createNotification(
                eq(employeeOne),
                eq(eventId),
                eq(NotificationType.SCHEDULE_PUBLISHED),
                eq("Schedule published"),
                eq("Operations schedule for 2026-08-02 to 2026-08-08 was published."),
                eq("SCHEDULE"),
                eq(10L),
                eq(Instant.parse("2026-08-02T18:00:00Z"))
        );
        verify(notificationService).createNotification(
                eq(employeeTwo),
                eq(eventId),
                eq(NotificationType.SCHEDULE_PUBLISHED),
                eq("Schedule published"),
                eq("Operations schedule for 2026-08-02 to 2026-08-08 was published."),
                eq("SCHEDULE"),
                eq(10L),
                eq(Instant.parse("2026-08-02T18:00:00Z"))
        );
    }

    @Test
    void createNotificationsDoesNothingWhenTeamHasNoActiveMembers() {
        UUID eventId = UUID.randomUUID();
        SchedulePublishedEvent event = schedulePublishedEvent();

        when(teamMemberRepository.findByTeam_IdAndActiveTrue(1L)).thenReturn(List.of());

        SchedulePublishedNotificationService service = new SchedulePublishedNotificationService(
                teamMemberRepository,
                notificationService
        );

        service.createNotifications(eventId, event);

        verify(notificationService, never()).createNotification(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
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

    private TeamMember teamMember(User user) {
        Team team = new Team("Operations", SwapApprovalPolicy.MANAGER, 8, "Asia/Jerusalem");
        ReflectionTestUtils.setField(team, "id", 1L);
        return new TeamMember(user, team, Instant.parse("2026-07-01T08:00:00Z"), true);
    }

    private User employee(String username, Long id, String fullName) {
        User user = new User(
                username,
                "password-hash",
                fullName,
                username + "@example.com",
                ApplicationRole.EMPLOYEE
        );
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
