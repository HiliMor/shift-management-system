package com.hilimor.shiftmanagement.template;

import java.util.List;

import com.hilimor.shiftmanagement.shift.ShiftResponse;

public record GenerateTemplateShiftsResponse(
        Long templateId,
        Long scheduleId,
        int shiftsCreated,
        int skippedExistingShifts,
        int skippedOutsideSchedule,
        List<ShiftResponse> shifts
) {
}
