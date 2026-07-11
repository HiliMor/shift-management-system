package com.hilimor.shiftmanagement.assignment;

import java.time.Duration;
import java.time.Instant;

import com.hilimor.shiftmanagement.schedule.Schedule;
import com.hilimor.shiftmanagement.schedule.ScheduleStatus;
import com.hilimor.shiftmanagement.shift.Shift;
import com.hilimor.shiftmanagement.shift.ShiftRepository;
import com.hilimor.shiftmanagement.team.TeamManagerRepository;
import com.hilimor.shiftmanagement.team.TeamMemberRepository;
import com.hilimor.shiftmanagement.user.User;
import com.hilimor.shiftmanagement.user.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final ShiftRepository shiftRepository;
    private final UserRepository userRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final TeamManagerRepository teamManagerRepository;

    public AssignmentService(
            AssignmentRepository assignmentRepository,
            ShiftRepository shiftRepository,
            UserRepository userRepository,
            TeamMemberRepository teamMemberRepository,
            TeamManagerRepository teamManagerRepository
    ) {
        this.assignmentRepository = assignmentRepository;
        this.shiftRepository = shiftRepository;
        this.userRepository = userRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.teamManagerRepository = teamManagerRepository;
    }

    @Transactional
    public AssignmentResponse createAssignment(String managerUsername, CreateAssignmentRequest request) {
        Shift shift = shiftRepository.findById(request.shiftId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Shift not found"));

        Schedule schedule = shift.getSchedule();
        Long teamId = schedule.getTeam().getId();

        if (!teamManagerRepository.existsByManager_UsernameAndTeam_Id(managerUsername, teamId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only a team manager can assign employees to this shift");
        }

        if (schedule.getStatus() != ScheduleStatus.DRAFT) {
            throw conflict("SCHEDULE_NOT_DRAFT", "Employees can be assigned only while the schedule is a draft");
        }

        User employee = userRepository.findById(request.employeeId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found"));

        validateTeamMembership(employee.getId(), teamId);
        validateNotAlreadyAssigned(shift.getId(), employee.getId());
        validateCapacity(shift);
        validateNoOverlap(shift, employee.getId());
        validateMinimumRest(shift, employee.getId());

        Assignment assignment = new Assignment(shift, employee, Instant.now());
        Assignment savedAssignment = assignmentRepository.save(assignment);

        return AssignmentResponse.from(savedAssignment);
    }

    private void validateTeamMembership(Long employeeId, Long teamId) {
        if (!teamMemberRepository.existsByUser_IdAndTeam_IdAndActiveTrue(employeeId, teamId)) {
            throw conflict("TEAM_MEMBERSHIP", "Employee must be an active member of the shift team");
        }
    }

    private void validateNotAlreadyAssigned(Long shiftId, Long employeeId) {
        if (assignmentRepository.existsByShift_IdAndEmployee_Id(shiftId, employeeId)) {
            throw conflict("DUPLICATE_ASSIGNMENT", "Employee is already assigned to this shift");
        }
    }

    private void validateCapacity(Shift shift) {
        long currentAssignments = assignmentRepository.countByShift_Id(shift.getId());
        if (currentAssignments >= shift.getRequiredWorkers()) {
            throw conflict("SHIFT_CAPACITY", "Shift has no available assignment slots");
        }
    }

    private void validateNoOverlap(Shift shift, Long employeeId) {
        boolean hasOverlap = !assignmentRepository
                .findByEmployee_IdAndShift_StartTimeLessThanAndShift_EndTimeGreaterThan(
                        employeeId,
                        shift.getEndTime(),
                        shift.getStartTime()
                )
                .isEmpty();

        if (hasOverlap) {
            throw conflict("SHIFT_OVERLAP", "Employee already has an overlapping assignment");
        }
    }

    private void validateMinimumRest(Shift shift, Long employeeId) {
        assignmentRepository
                .findTopByEmployee_IdAndShift_EndTimeLessThanEqualOrderByShift_EndTimeDesc(
                        employeeId,
                        shift.getStartTime()
                )
                .ifPresent(previousAssignment -> validateRestAfterPreviousShift(previousAssignment.getShift(), shift));

        assignmentRepository
                .findTopByEmployee_IdAndShift_StartTimeGreaterThanEqualOrderByShift_StartTimeAsc(
                        employeeId,
                        shift.getEndTime()
                )
                .ifPresent(nextAssignment -> validateRestBeforeNextShift(shift, nextAssignment.getShift()));
    }

    private void validateRestAfterPreviousShift(Shift previousShift, Shift newShift) {
        int requiredRestHours = Math.max(previousShift.getMinRestHours(), newShift.getMinRestHours());
        Instant earliestAllowedStart = previousShift.getEndTime().plus(Duration.ofHours(requiredRestHours));

        if (earliestAllowedStart.isAfter(newShift.getStartTime())) {
            throw conflict("MINIMUM_REST", "Employee does not have enough rest before this shift");
        }
    }

    private void validateRestBeforeNextShift(Shift newShift, Shift nextShift) {
        int requiredRestHours = Math.max(newShift.getMinRestHours(), nextShift.getMinRestHours());
        Instant earliestAllowedNextStart = newShift.getEndTime().plus(Duration.ofHours(requiredRestHours));

        if (earliestAllowedNextStart.isAfter(nextShift.getStartTime())) {
            throw conflict("MINIMUM_REST", "Employee does not have enough rest after this shift");
        }
    }

    private AssignmentValidationException conflict(String code, String message) {
        return new AssignmentValidationException(HttpStatus.CONFLICT, code, message);
    }
}
