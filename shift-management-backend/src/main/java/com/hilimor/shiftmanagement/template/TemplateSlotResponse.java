package com.hilimor.shiftmanagement.template;

import java.time.LocalTime;

import com.hilimor.shiftmanagement.staffing.StaffingRole;

public record TemplateSlotResponse(
        Long id,
        Long shiftTemplateId,
        int dayOffset,
        LocalTime startTime,
        int durationMinutes,
        String description,
        int requiredWorkers,
        Long requiredStaffingRoleId,
        String requiredStaffingRoleName
) {

    public static TemplateSlotResponse from(TemplateSlot templateSlot) {
        StaffingRole requiredStaffingRole = templateSlot.getRequiredStaffingRole();

        return new TemplateSlotResponse(
                templateSlot.getId(),
                templateSlot.getShiftTemplate().getId(),
                templateSlot.getDayOffset(),
                templateSlot.getStartTime(),
                templateSlot.getDurationMinutes(),
                templateSlot.getDescription(),
                templateSlot.getRequiredWorkers(),
                requiredStaffingRole == null ? null : requiredStaffingRole.getId(),
                requiredStaffingRole == null ? null : requiredStaffingRole.getName()
        );
    }
}
