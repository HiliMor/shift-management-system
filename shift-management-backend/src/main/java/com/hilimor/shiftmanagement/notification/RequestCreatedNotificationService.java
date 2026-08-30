package com.hilimor.shiftmanagement.notification;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.hilimor.shiftmanagement.request.SwapRequestCreatedEvent;
import com.hilimor.shiftmanagement.request.SwapRequestType;
import com.hilimor.shiftmanagement.team.TeamManager;
import com.hilimor.shiftmanagement.team.TeamManagerRepository;
import com.hilimor.shiftmanagement.user.User;
import com.hilimor.shiftmanagement.user.UserRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RequestCreatedNotificationService {

    private static final Logger log = LoggerFactory.getLogger(RequestCreatedNotificationService.class);

    private static final String RELATED_ENTITY_TYPE = "REQUEST";

    private final UserRepository userRepository;
    private final TeamManagerRepository teamManagerRepository;
    private final NotificationService notificationService;

    public RequestCreatedNotificationService(
            UserRepository userRepository,
            TeamManagerRepository teamManagerRepository,
            NotificationService notificationService
    ) {
        this.userRepository = userRepository;
        this.teamManagerRepository = teamManagerRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public void createNotifications(UUID eventId, SwapRequestCreatedEvent event) {
        User targetEmployee = userRepository.findById(event.targetEmployeeId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Target employee not found"));
        Instant createdAt = event.createdAt() != null ? event.createdAt() : Instant.now();

        notificationService.createNotification(
                targetEmployee,
                eventId,
                NotificationType.REQUEST_CREATED,
                requestTitle(event.requestType()),
                targetEmployeeMessage(event),
                RELATED_ENTITY_TYPE,
                event.requestId(),
                createdAt
        );

        List<TeamManager> teamManagers = teamManagerRepository.findByTeam_Id(event.teamId());
        for (TeamManager teamManager : teamManagers) {
            notificationService.createNotification(
                    teamManager.getManager(),
                    eventId,
                    NotificationType.REQUEST_CREATED,
                    requestTitle(event.requestType()),
                    managerMessage(event),
                    RELATED_ENTITY_TYPE,
                    event.requestId(),
                    createdAt
            );
        }

        log.info(
                "Created request notifications for request {} and {} managers",
                event.requestId(),
                teamManagers.size()
        );
    }

    private String requestTitle(SwapRequestType requestType) {
        return requestType == SwapRequestType.TRANSFER
                ? "New transfer request"
                : "New swap request";
    }

    private String targetEmployeeMessage(SwapRequestCreatedEvent event) {
        String sourceShift = shiftLabel(event.sourceShiftDescription(), event.sourceShiftId());

        if (event.requestType() == SwapRequestType.TRANSFER) {
            return event.requesterFullName() + " requested to transfer " + sourceShift + " to you.";
        }

        String targetShift = shiftLabel(event.targetShiftDescription(), event.targetShiftId());
        return event.requesterFullName() + " requested to swap " + sourceShift + " with your " + targetShift + ".";
    }

    private String managerMessage(SwapRequestCreatedEvent event) {
        return event.requesterFullName()
                + " created a "
                + event.requestType().name().toLowerCase()
                + " request for the "
                + event.teamName()
                + " team, targeting "
                + event.targetEmployeeFullName()
                + ".";
    }

    private String shiftLabel(String description, Long shiftId) {
        return description == null || description.isBlank()
                ? "shift #" + shiftId
                : "shift " + description;
    }
}
