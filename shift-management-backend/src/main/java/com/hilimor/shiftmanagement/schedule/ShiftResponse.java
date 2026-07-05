package com.hilimor.shiftmanagement.schedule;

import java.time.Instant;

public record ShiftResponse(
        Long id,
        Long scheduleId,
        Instant startTime,
        Instant endTime,
        String description,
        int requiredWorkers,
        int minRestHours
) {

    static ShiftResponse from(Shift shift) {
        return new ShiftResponse(
                shift.getId(),
                shift.getSchedule().getId(),
                shift.getStartTime(),
                shift.getEndTime(),
                shift.getDescription(),
                shift.getRequiredWorkers(),
                shift.getMinRestHours()
        );
    }
}
