package com.hilimor.shiftmanagement.schedule;

import java.time.Instant;
import java.util.List;

import com.hilimor.shiftmanagement.assignment.Assignment;
import com.hilimor.shiftmanagement.shift.Shift;

public record PublishedShiftResponse(
        Long id,
        Long scheduleId,
        Instant startTime,
        Instant endTime,
        String description,
        int requiredWorkers,
        int minRestHours,
        Long requiredStaffingRoleId,
        String requiredStaffingRoleName,
        List<PublishedAssignmentResponse> assignments
) {

    static PublishedShiftResponse from(Shift shift, List<Assignment> assignments) {
        Long requiredStaffingRoleId = shift.getRequiredStaffingRole() == null
                ? null
                : shift.getRequiredStaffingRole().getId();
        String requiredStaffingRoleName = shift.getRequiredStaffingRole() == null
                ? null
                : shift.getRequiredStaffingRole().getName();

        return new PublishedShiftResponse(
                shift.getId(),
                shift.getSchedule().getId(),
                shift.getStartTime(),
                shift.getEndTime(),
                shift.getDescription(),
                shift.getRequiredWorkers(),
                shift.getMinRestHours(),
                requiredStaffingRoleId,
                requiredStaffingRoleName,
                assignments.stream()
                        .map(PublishedAssignmentResponse::from)
                        .toList()
        );
    }
}
