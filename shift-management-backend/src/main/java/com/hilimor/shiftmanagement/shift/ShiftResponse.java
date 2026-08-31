package com.hilimor.shiftmanagement.shift;

import java.time.Instant;

public record ShiftResponse(
        Long id,
        Long scheduleId,
        Instant startTime,
        Instant endTime,
        String description,
        int requiredWorkers,
        int minRestHours,
        Long requiredStaffingRoleId,
        String requiredStaffingRoleName,
        Long templateSlotId,
        Long version
) {

    public static ShiftResponse from(Shift shift) {
        Long requiredStaffingRoleId = shift.getRequiredStaffingRole() == null
                ? null
                : shift.getRequiredStaffingRole().getId();
        String requiredStaffingRoleName = shift.getRequiredStaffingRole() == null
                ? null
                : shift.getRequiredStaffingRole().getName();
        Long templateSlotId = shift.getTemplateSlot() == null
                ? null
                : shift.getTemplateSlot().getId();

        return new ShiftResponse(
                shift.getId(),
                shift.getSchedule().getId(),
                shift.getStartTime(),
                shift.getEndTime(),
                shift.getDescription(),
                shift.getRequiredWorkers(),
                shift.getMinRestHours(),
                requiredStaffingRoleId,
                requiredStaffingRoleName,
                templateSlotId,
                shift.getVersion()
        );
    }
}
