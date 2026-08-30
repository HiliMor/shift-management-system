package com.hilimor.shiftmanagement.assignment;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.hilimor.shiftmanagement.schedule.Schedule;
import com.hilimor.shiftmanagement.schedule.ScheduleRepository;
import com.hilimor.shiftmanagement.schedule.ScheduleStatus;
import com.hilimor.shiftmanagement.shift.Shift;
import com.hilimor.shiftmanagement.shift.ShiftRepository;
import com.hilimor.shiftmanagement.team.TeamManagerRepository;
import com.hilimor.shiftmanagement.team.TeamMember;
import com.hilimor.shiftmanagement.team.TeamMemberRepository;
import com.hilimor.shiftmanagement.user.ApplicationRole;
import com.hilimor.shiftmanagement.user.User;
import com.hilimor.shiftmanagement.user.UserRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AssignmentService {

    private static final Logger log = LoggerFactory.getLogger(AssignmentService.class);

    private final AssignmentRepository assignmentRepository;
    private final ScheduleRepository scheduleRepository;
    private final ShiftRepository shiftRepository;
    private final UserRepository userRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final TeamManagerRepository teamManagerRepository;
    private final AssignmentValidator assignmentValidator;

    public AssignmentService(
            AssignmentRepository assignmentRepository,
            ScheduleRepository scheduleRepository,
            ShiftRepository shiftRepository,
            UserRepository userRepository,
            TeamMemberRepository teamMemberRepository,
            TeamManagerRepository teamManagerRepository,
            AssignmentValidator assignmentValidator
    ) {
        this.assignmentRepository = assignmentRepository;
        this.scheduleRepository = scheduleRepository;
        this.shiftRepository = shiftRepository;
        this.userRepository = userRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.teamManagerRepository = teamManagerRepository;
        this.assignmentValidator = assignmentValidator;
    }

    @Transactional
    public AssignmentResponse createAssignment(String managerUsername, CreateAssignmentRequest request) {
        Shift shift = shiftRepository.findByIdForUpdate(request.shiftId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Shift not found"));

        Schedule schedule = shift.getSchedule();
        Long teamId = requireManagedSchedule(managerUsername, schedule, "Only a team manager can assign employees to this shift");

        if (schedule.getStatus() != ScheduleStatus.DRAFT) {
            throw conflict("SCHEDULE_NOT_DRAFT", "Employees can be assigned only while the schedule is a draft");
        }

        User employee = lockEmployee(request.employeeId());

        Assignment savedAssignment = createValidatedAssignment(managerUsername, shift, employee, teamId);

        return AssignmentResponse.from(savedAssignment);
    }

    @Transactional
    public AutoAssignmentReportResponse autoAssignSchedule(String managerUsername, Long scheduleId) {
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Schedule not found"));

        Long teamId = requireManagedSchedule(
                managerUsername,
                schedule,
                "Only a team manager can auto-assign this schedule"
        );

        if (schedule.getStatus() != ScheduleStatus.DRAFT) {
            throw conflict("SCHEDULE_NOT_DRAFT", "Automatic assignment can run only while the schedule is a draft");
        }

        List<Shift> shifts = shiftRepository.findBySchedule_IdOrderByStartTime(scheduleId);
        // Lock all shifts, then all employees, in ID order before validating or writing assignments.
        List<Shift> lockedShifts = shifts.stream()
                .sorted(Comparator.comparing(Shift::getId))
                .map(this::lockShift)
                .sorted(Comparator.comparing(Shift::getStartTime).thenComparing(Shift::getId))
                .toList();
        List<User> candidates = teamMemberRepository.findByTeam_IdAndActiveTrue(teamId)
                .stream()
                .map(TeamMember::getUser)
                .filter(user -> user.getApplicationRole() == ApplicationRole.EMPLOYEE)
                .sorted(Comparator.comparing(User::getId))
                .map(user -> lockEmployee(user.getId()))
                .toList();
        List<Assignment> existingAssignments = assignmentRepository
                .findByShift_Schedule_IdOrderByShift_StartTimeAscEmployee_FullNameAsc(scheduleId);

        Map<Long, List<Assignment>> assignmentsByShiftId = existingAssignments
                .stream()
                .collect(Collectors.groupingBy(assignment -> assignment.getShift().getId()));
        Map<Long, Long> assignedMinutesByEmployeeId = assignedMinutesByEmployeeId(existingAssignments);
        List<AutoAssignmentShiftResultResponse> shiftResults = new ArrayList<>();

        for (Shift shift : lockedShifts) {
            List<Assignment> shiftAssignments = new ArrayList<>(
                    assignmentsByShiftId.getOrDefault(shift.getId(), List.of())
            );
            Set<Long> assignedEmployeeIds = shiftAssignments.stream()
                    .map(assignment -> assignment.getEmployee().getId())
                    .collect(Collectors.toCollection(HashSet::new));
            int assignedWorkersBefore = shiftAssignments.size();
            int openSlotsBefore = Math.max(0, shift.getRequiredWorkers() - assignedWorkersBefore);
            List<AssignmentResponse> createdAssignments = new ArrayList<>();

            while (createdAssignments.size() < openSlotsBefore) {
                Assignment nextAssignment = assignNextEligibleEmployee(
                        managerUsername,
                        teamId,
                        shift,
                        candidates,
                        assignedEmployeeIds,
                        assignedMinutesByEmployeeId
                );

                if (nextAssignment == null) {
                    break;
                }

                shiftAssignments.add(nextAssignment);
                assignedEmployeeIds.add(nextAssignment.getEmployee().getId());
                assignedMinutesByEmployeeId.merge(
                        nextAssignment.getEmployee().getId(),
                        durationMinutes(shift),
                        Long::sum
                );
                createdAssignments.add(AssignmentResponse.from(nextAssignment));
            }

            int openSlotsAfter = Math.max(
                    0,
                    shift.getRequiredWorkers() - assignedWorkersBefore - createdAssignments.size()
            );
            shiftResults.add(AutoAssignmentShiftResultResponse.from(
                    shift,
                    assignedWorkersBefore,
                    openSlotsBefore,
                    openSlotsAfter,
                    createdAssignments
            ));
        }

        AutoAssignmentReportResponse report = AutoAssignmentReportResponse.from(scheduleId, shiftResults);
        log.info(
                "Automatic assignment completed for schedule {} by manager {} with {} created assignments and {} remaining open slots",
                scheduleId,
                managerUsername,
                report.assignmentsCreated(),
                report.totalOpenSlotsAfter()
        );

        return report;
    }

    @Transactional(readOnly = true)
    public List<AssignmentResponse> listScheduleAssignments(String managerUsername, Long scheduleId) {
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Schedule not found"));

        requireManagedSchedule(managerUsername, schedule, "Only a team manager can view assignments for this schedule");

        return assignmentRepository.findByShift_Schedule_IdOrderByShift_StartTimeAscEmployee_FullNameAsc(scheduleId)
                .stream()
                .map(AssignmentResponse::from)
                .toList();
    }

    @Transactional
    public void deleteAssignment(String managerUsername, Long assignmentId) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment not found"));

        Schedule schedule = assignment.getShift().getSchedule();
        requireManagedSchedule(managerUsername, schedule, "Only a team manager can delete this assignment");

        if (schedule.getStatus() != ScheduleStatus.DRAFT) {
            throw conflict("SCHEDULE_NOT_DRAFT", "Assignments can be deleted only while the schedule is a draft");
        }

        assignmentRepository.delete(assignment);
        log.info(
                "Assignment {} deleted from shift {} by manager {}",
                assignment.getId(),
                assignment.getShift().getId(),
                managerUsername
        );
    }

    @Transactional(readOnly = true)
    public void validateEmployeeCanReceiveTransferredAssignment(Shift shift, User employee) {
        assignmentValidator.validateEmployeeCanReceiveTransferredAssignment(shift, employee);
    }

    @Transactional(readOnly = true)
    public void validateEmployeeCanReceiveSwappedAssignment(
            Shift shift,
            User employee,
            Assignment assignmentToReplace
    ) {
        assignmentValidator.validateEmployeeCanReceiveSwappedAssignment(shift, employee, assignmentToReplace);
    }

    private Assignment assignNextEligibleEmployee(
            String managerUsername,
            Long teamId,
            Shift shift,
            List<User> candidates,
            Set<Long> assignedEmployeeIds,
            Map<Long, Long> assignedMinutesByEmployeeId
    ) {
        List<User> rankedCandidates = candidates.stream()
                .filter(candidate -> !assignedEmployeeIds.contains(candidate.getId()))
                .sorted(Comparator
                        .comparingLong((User candidate) ->
                                assignedMinutesByEmployeeId.getOrDefault(candidate.getId(), 0L))
                        .thenComparing(User::getFullName)
                        .thenComparing(User::getUsername)
                        .thenComparing(User::getId))
                .toList();

        for (User candidate : rankedCandidates) {
            try {
                return createValidatedAssignment(managerUsername, shift, candidate, teamId);
            } catch (AssignmentValidationException exception) {
                log.debug(
                        "Automatic assignment skipped employee {} for shift {} because {}",
                        candidate.getId(),
                        shift.getId(),
                        exception.getCode()
                );
            }
        }

        return null;
    }

    private Shift lockShift(Shift shift) {
        return shiftRepository.findByIdForUpdate(shift.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Shift not found"));
    }

    private User lockEmployee(Long employeeId) {
        return userRepository.findByIdForUpdate(employeeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found"));
    }

    private Assignment createValidatedAssignment(
            String managerUsername,
            Shift shift,
            User employee,
            Long teamId
    ) {
        assignmentValidator.validateEmployeeCanBeAssignedToShift(shift, employee, teamId);

        Assignment assignment = new Assignment(shift, employee, Instant.now());
        Assignment savedAssignment = assignmentRepository.save(assignment);
        log.info(
                "Assignment {} created for shift {} and employee {} by manager {}",
                savedAssignment.getId(),
                shift.getId(),
                employee.getId(),
                managerUsername
        );

        return savedAssignment;
    }

    private Long requireManagedSchedule(String managerUsername, Schedule schedule, String errorMessage) {
        Long teamId = schedule.getTeam().getId();
        if (!teamManagerRepository.existsByManager_UsernameAndTeam_Id(managerUsername, teamId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, errorMessage);
        }

        return teamId;
    }

    private Map<Long, Long> assignedMinutesByEmployeeId(List<Assignment> assignments) {
        Map<Long, Long> assignedMinutesByEmployeeId = new HashMap<>();

        for (Assignment assignment : assignments) {
            assignedMinutesByEmployeeId.merge(
                    assignment.getEmployee().getId(),
                    durationMinutes(assignment.getShift()),
                    Long::sum
            );
        }

        return assignedMinutesByEmployeeId;
    }

    private long durationMinutes(Shift shift) {
        return Duration.between(shift.getStartTime(), shift.getEndTime()).toMinutes();
    }

    private AssignmentValidationException conflict(String code, String message) {
        return new AssignmentValidationException(HttpStatus.CONFLICT, code, message);
    }
}
