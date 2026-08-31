package com.hilimor.shiftmanagement.assignment;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

import com.hilimor.shiftmanagement.availability.AvailabilityConstraintRepository;
import com.hilimor.shiftmanagement.shift.Shift;
import com.hilimor.shiftmanagement.staffing.TeamMemberStaffingRoleRepository;
import com.hilimor.shiftmanagement.team.TeamMemberRepository;
import com.hilimor.shiftmanagement.user.User;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class AssignmentValidator {

    private final AssignmentRepository assignmentRepository;
    private final AvailabilityConstraintRepository availabilityConstraintRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final TeamMemberStaffingRoleRepository teamMemberStaffingRoleRepository;

    public AssignmentValidator(
            AssignmentRepository assignmentRepository,
            AvailabilityConstraintRepository availabilityConstraintRepository,
            TeamMemberRepository teamMemberRepository,
            TeamMemberStaffingRoleRepository teamMemberStaffingRoleRepository
    ) {
        this.assignmentRepository = assignmentRepository;
        this.availabilityConstraintRepository = availabilityConstraintRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.teamMemberStaffingRoleRepository = teamMemberStaffingRoleRepository;
    }

    public void validateEmployeeCanBeAssignedToShift(Shift shift, User employee, Long teamId) {
        validateTeamMembership(employee.getId(), teamId);
        validateRequiredStaffingRole(shift, employee.getId(), teamId);
        validateNotAlreadyAssigned(shift.getId(), employee.getId());
        validateCapacity(shift);
        validateAvailability(shift, employee.getId());
        validateNoOverlap(shift, employee.getId());
        validateMinimumRest(shift, employee.getId());
    }

    public void validateEmployeeCanReceiveTransferredAssignment(Shift shift, User employee) {
        Long teamId = shift.getSchedule().getTeam().getId();
        Long employeeId = employee.getId();

        validateTeamMembership(employeeId, teamId);
        validateRequiredStaffingRole(shift, employeeId, teamId);
        validateNotAlreadyAssigned(shift.getId(), employeeId);
        validateAvailability(shift, employeeId);
        validateNoOverlap(shift, employeeId);
        validateMinimumRest(shift, employeeId);
    }

    public void validateEmployeeCanReceiveSwappedAssignment(
            Shift shift,
            User employee,
            Assignment assignmentToReplace
    ) {
        Objects.requireNonNull(assignmentToReplace, "assignmentToReplace must not be null");

        Long teamId = shift.getSchedule().getTeam().getId();
        Long employeeId = employee.getId();
        Long ignoredAssignmentId = assignmentToReplace.getId();

        validateTeamMembership(employeeId, teamId);
        validateRequiredStaffingRole(shift, employeeId, teamId);
        validateNotAlreadyAssigned(shift.getId(), employeeId);
        validateAvailability(shift, employeeId);
        validateNoOverlap(shift, employeeId, ignoredAssignmentId);
        validateMinimumRest(shift, employeeId, ignoredAssignmentId);
    }

    public void validateExistingAssignments(Shift shift, List<Assignment> assignments) {
        // Full capacity is valid here: these employees are already assigned, not new candidates.
        if (assignments.size() > shift.getRequiredWorkers()) {
            throw conflict("SHIFT_CAPACITY", "Shift #" + shift.getId()
                    + " has more assigned employees than required workers");
        }

        Long teamId = shift.getSchedule().getTeam().getId();
        for (Assignment assignment : assignments) {
            Long employeeId = assignment.getEmployee().getId();
            try {
                validateTeamMembership(employeeId, teamId);
                validateRequiredStaffingRole(shift, employeeId, teamId);
                validateAvailability(shift, employeeId);
                validateNoOverlap(shift, employeeId, assignment.getId());
                validateMinimumRest(shift, employeeId, assignment.getId());
            } catch (AssignmentValidationException exception) {
                throw new AssignmentValidationException(exception.getStatus(), exception.getCode(),
                        "Shift #" + shift.getId() + ", employee #" + employeeId + ": " + exception.getMessage());
            }
        }
    }

    private void validateTeamMembership(Long employeeId, Long teamId) {
        if (!teamMemberRepository.existsByUser_IdAndTeam_IdAndActiveTrue(employeeId, teamId)) {
            throw conflict("TEAM_MEMBERSHIP", "Employee must be an active member of the shift team");
        }
    }

    private void validateRequiredStaffingRole(Shift shift, Long employeeId, Long teamId) {
        if (shift.getRequiredStaffingRole() == null) {
            return;
        }

        Long staffingRoleId = shift.getRequiredStaffingRole().getId();
        boolean hasRequiredRole = teamMemberStaffingRoleRepository
                .existsByTeamMember_User_IdAndTeamMember_Team_IdAndStaffingRole_Id(
                        employeeId,
                        teamId,
                        staffingRoleId
                );

        if (!hasRequiredRole) {
            throw conflict(
                    "STAFFING_ROLE_REQUIRED",
                    "Employee does not have the staffing role required for this shift"
            );
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

    private void validateAvailability(Shift shift, Long employeeId) {
        boolean hasUnavailableOverlap = !availabilityConstraintRepository
                .findByEmployee_IdAndStartTimeLessThanAndEndTimeGreaterThan(
                        employeeId,
                        shift.getEndTime(),
                        shift.getStartTime()
                )
                .isEmpty();

        if (hasUnavailableOverlap) {
            throw conflict("AVAILABILITY_CONFLICT", "Employee is unavailable during this shift");
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

    private void validateNoOverlap(Shift shift, Long employeeId, Long ignoredAssignmentId) {
        if (ignoredAssignmentId == null) {
            validateNoOverlap(shift, employeeId);
            return;
        }

        boolean hasOverlap = !assignmentRepository
                .findByEmployee_IdAndIdNotAndShift_StartTimeLessThanAndShift_EndTimeGreaterThan(
                        employeeId,
                        ignoredAssignmentId,
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

    private void validateMinimumRest(Shift shift, Long employeeId, Long ignoredAssignmentId) {
        if (ignoredAssignmentId == null) {
            validateMinimumRest(shift, employeeId);
            return;
        }

        assignmentRepository
                .findTopByEmployee_IdAndIdNotAndShift_EndTimeLessThanEqualOrderByShift_EndTimeDesc(
                        employeeId,
                        ignoredAssignmentId,
                        shift.getStartTime()
                )
                .ifPresent(previousAssignment -> validateRestAfterPreviousShift(previousAssignment.getShift(), shift));

        assignmentRepository
                .findTopByEmployee_IdAndIdNotAndShift_StartTimeGreaterThanEqualOrderByShift_StartTimeAsc(
                        employeeId,
                        ignoredAssignmentId,
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
