package com.hilimor.shiftmanagement.schedule;

import java.util.List;

public record SchedulePublicationReadinessResponse(
        ScheduleResponse schedule,
        boolean readyToPublish,
        int totalShifts,
        int totalRequiredWorkers,
        int totalAssignedWorkers,
        int totalOpenSlots,
        List<SchedulePublicationReadinessShiftResponse> unfilledShifts
) {

    static SchedulePublicationReadinessResponse from(
            Schedule schedule,
            List<SchedulePublicationReadinessShiftResponse> shifts
    ) {
        int totalRequiredWorkers = shifts.stream()
                .mapToInt(SchedulePublicationReadinessShiftResponse::requiredWorkers)
                .sum();
        int totalAssignedWorkers = shifts.stream()
                .mapToInt(SchedulePublicationReadinessShiftResponse::assignedWorkers)
                .sum();
        int totalOpenSlots = shifts.stream()
                .mapToInt(SchedulePublicationReadinessShiftResponse::openSlots)
                .sum();
        List<SchedulePublicationReadinessShiftResponse> unfilledShifts = shifts.stream()
                .filter(shift -> !shift.filled())
                .toList();

        return new SchedulePublicationReadinessResponse(
                ScheduleResponse.from(schedule),
                !shifts.isEmpty() && totalOpenSlots == 0,
                shifts.size(),
                totalRequiredWorkers,
                totalAssignedWorkers,
                totalOpenSlots,
                unfilledShifts
        );
    }
}
