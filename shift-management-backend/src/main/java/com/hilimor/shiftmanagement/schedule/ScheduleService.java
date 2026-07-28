package com.hilimor.shiftmanagement.schedule;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.hilimor.shiftmanagement.assignment.Assignment;
import com.hilimor.shiftmanagement.assignment.AssignmentRepository;
import com.hilimor.shiftmanagement.shift.ShiftRepository;
import com.hilimor.shiftmanagement.team.Team;
import com.hilimor.shiftmanagement.team.TeamManagerRepository;
import com.hilimor.shiftmanagement.team.TeamMemberRepository;
import com.hilimor.shiftmanagement.team.TeamRepository;
import com.hilimor.shiftmanagement.user.User;
import com.hilimor.shiftmanagement.user.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final TeamRepository teamRepository;
    private final TeamManagerRepository teamManagerRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final UserRepository userRepository;
    private final ShiftRepository shiftRepository;
    private final AssignmentRepository assignmentRepository;

    public ScheduleService(
            ScheduleRepository scheduleRepository,
            TeamRepository teamRepository,
            TeamManagerRepository teamManagerRepository,
            TeamMemberRepository teamMemberRepository,
            UserRepository userRepository,
            ShiftRepository shiftRepository,
            AssignmentRepository assignmentRepository
    ) {
        this.scheduleRepository = scheduleRepository;
        this.teamRepository = teamRepository;
        this.teamManagerRepository = teamManagerRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.userRepository = userRepository;
        this.shiftRepository = shiftRepository;
        this.assignmentRepository = assignmentRepository;
    }

    @Transactional
    public ScheduleResponse createDraftSchedule(String username, CreateScheduleRequest request) {
        if (request.endDate().isBefore(request.startDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Schedule end date must not be before start date");
        }

        Team team = teamRepository.findById(request.teamId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Team not found"));

        if (!teamManagerRepository.existsByManager_UsernameAndTeam_Id(username, request.teamId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only a team manager can create schedules for this team");
        }

        Schedule schedule = new Schedule(team, request.startDate(), request.endDate());
        Schedule savedSchedule = scheduleRepository.save(schedule);

        return ScheduleResponse.from(savedSchedule);
    }

    @Transactional
    public ScheduleResponse publishSchedule(String username, Long scheduleId, boolean confirmUnfilled) {
        Schedule schedule = managedSchedule(
                username,
                scheduleId,
                "Only a team manager can publish this schedule"
        );

        if (schedule.getStatus() != ScheduleStatus.DRAFT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only draft schedules can be published");
        }

        if (!confirmUnfilled) {
            SchedulePublicationReadinessResponse readiness = publicationReadiness(schedule, scheduleId);
            if (!readiness.readyToPublish()) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Schedule is not fully assigned; confirm publication with unfilled shifts"
                );
            }
        }

        try {
            schedule.publish(Instant.now());
        } catch (IllegalStateException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, exception.getMessage());
        }

        return ScheduleResponse.from(schedule);
    }

    @Transactional
    public ScheduleResponse reopenSchedule(String username, Long scheduleId) {
        Schedule schedule = managedSchedule(
                username,
                scheduleId,
                "Only a team manager can reopen this schedule"
        );

        try {
            schedule.reopen();
        } catch (IllegalStateException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, exception.getMessage());
        }

        return ScheduleResponse.from(schedule);
    }

    @Transactional(readOnly = true)
    public List<ScheduleResponse> listPublishedSchedulesForUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        List<Long> teamIds = teamMemberRepository.findByUser_IdAndActiveTrue(user.getId())
                .stream()
                .map(teamMember -> teamMember.getTeam().getId())
                .toList();

        if (teamIds.isEmpty()) {
            return List.of();
        }

        return scheduleRepository.findByTeam_IdInAndStatusOrderByStartDateDesc(teamIds, ScheduleStatus.PUBLISHED)
                .stream()
                .map(ScheduleResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ScheduleResponse> listManagedDraftSchedules(String username) {
        List<Long> teamIds = teamManagerRepository.findByManager_Username(username)
                .stream()
                .map(teamManager -> teamManager.getTeam().getId())
                .toList();

        if (teamIds.isEmpty()) {
            return List.of();
        }

        return scheduleRepository.findByTeam_IdInAndStatusOrderByStartDateDesc(teamIds, ScheduleStatus.DRAFT)
                .stream()
                .map(ScheduleResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public PublishedScheduleDetailsResponse getPublishedScheduleDetailsForUser(String username, Long scheduleId) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Schedule not found"));

        if (schedule.getStatus() != ScheduleStatus.PUBLISHED) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Schedule not found");
        }

        Long teamId = schedule.getTeam().getId();
        if (teamMemberRepository.findByUser_IdAndTeam_IdAndActiveTrue(user.getId(), teamId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Schedule not found");
        }

        Map<Long, List<Assignment>> assignmentsByShiftId = assignmentsByShiftId(scheduleId);

        List<PublishedShiftResponse> shifts = shiftRepository.findBySchedule_IdOrderByStartTime(scheduleId)
                .stream()
                .map(shift -> PublishedShiftResponse.from(
                        shift,
                        assignmentsByShiftId.getOrDefault(shift.getId(), List.of())
                ))
                .toList();

        return PublishedScheduleDetailsResponse.from(schedule, shifts);
    }

    @Transactional(readOnly = true)
    public SchedulePublicationReadinessResponse getPublicationReadiness(String username, Long scheduleId) {
        Schedule schedule = managedSchedule(
                username,
                scheduleId,
                "Only a team manager can view publication readiness for this schedule"
        );

        return publicationReadiness(schedule, scheduleId);
    }

    private Schedule managedSchedule(String username, Long scheduleId, String errorMessage) {
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Schedule not found"));

        Long teamId = schedule.getTeam().getId();
        if (!teamManagerRepository.existsByManager_UsernameAndTeam_Id(username, teamId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, errorMessage);
        }

        return schedule;
    }

    private SchedulePublicationReadinessResponse publicationReadiness(Schedule schedule, Long scheduleId) {
        Map<Long, List<Assignment>> assignmentsByShiftId = assignmentsByShiftId(scheduleId);
        List<SchedulePublicationReadinessShiftResponse> shifts = shiftRepository.findBySchedule_IdOrderByStartTime(scheduleId)
                .stream()
                .map(shift -> SchedulePublicationReadinessShiftResponse.from(
                        shift,
                        assignmentsByShiftId.getOrDefault(shift.getId(), List.of()).size()
                ))
                .toList();

        return SchedulePublicationReadinessResponse.from(schedule, shifts);
    }

    private Map<Long, List<Assignment>> assignmentsByShiftId(Long scheduleId) {
        return assignmentRepository.findByShift_Schedule_IdOrderByShift_StartTimeAscEmployee_FullNameAsc(scheduleId)
                .stream()
                .collect(Collectors.groupingBy(assignment -> assignment.getShift().getId()));
    }
}
