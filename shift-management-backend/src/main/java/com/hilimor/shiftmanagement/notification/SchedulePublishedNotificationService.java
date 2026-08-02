package com.hilimor.shiftmanagement.notification;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.hilimor.shiftmanagement.schedule.SchedulePublishedEvent;
import com.hilimor.shiftmanagement.team.TeamMember;
import com.hilimor.shiftmanagement.team.TeamMemberRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SchedulePublishedNotificationService {

    private static final String RELATED_ENTITY_TYPE = "SCHEDULE";

    private final TeamMemberRepository teamMemberRepository;
    private final NotificationService notificationService;

    public SchedulePublishedNotificationService(
            TeamMemberRepository teamMemberRepository,
            NotificationService notificationService
    ) {
        this.teamMemberRepository = teamMemberRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public void createNotifications(UUID eventId, SchedulePublishedEvent event) {
        List<TeamMember> activeTeamMembers = teamMemberRepository.findByTeam_IdAndActiveTrue(event.teamId());
        Instant createdAt = event.publishedAt() != null ? event.publishedAt() : Instant.now();

        for (TeamMember teamMember : activeTeamMembers) {
            notificationService.createNotification(
                    teamMember.getUser(),
                    eventId,
                    NotificationType.SCHEDULE_PUBLISHED,
                    "Schedule published",
                    publishedScheduleMessage(event),
                    RELATED_ENTITY_TYPE,
                    event.scheduleId(),
                    createdAt
            );
        }
    }

    private String publishedScheduleMessage(SchedulePublishedEvent event) {
        return event.teamName()
                + " schedule for "
                + event.startDate()
                + " to "
                + event.endDate()
                + " was published.";
    }
}
