package com.hilimor.shiftmanagement.schedule;

import java.time.Instant;

import com.hilimor.shiftmanagement.shift.Shift;

public record SchedulePublicationReadinessShiftResponse(
        Long shiftId,
        Instant startTime,
        Instant endTime,
        String description,
        int requiredWorkers,
        int assignedWorkers,
        int openSlots,
        boolean filled
) {

    static SchedulePublicationReadinessShiftResponse from(Shift shift, int assignedWorkers) {
        int openSlots = Math.max(shift.getRequiredWorkers() - assignedWorkers, 0);

        return new SchedulePublicationReadinessShiftResponse(
                shift.getId(),
                shift.getStartTime(),
                shift.getEndTime(),
                shift.getDescription(),
                shift.getRequiredWorkers(),
                assignedWorkers,
                openSlots,
                openSlots == 0
        );
    }
}
