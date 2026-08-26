package com.hilimor.shiftmanagement.assignment;

import java.util.List;

public record AutoAssignmentReportResponse(
        Long scheduleId,
        int totalShifts,
        int assignmentsCreated,
        int totalOpenSlotsBefore,
        int totalOpenSlotsAfter,
        List<AutoAssignmentShiftResultResponse> shifts
) {

    static AutoAssignmentReportResponse from(Long scheduleId, List<AutoAssignmentShiftResultResponse> shifts) {
        int assignmentsCreated = shifts.stream()
                .mapToInt(AutoAssignmentShiftResultResponse::assignmentsCreated)
                .sum();
        int totalOpenSlotsBefore = shifts.stream()
                .mapToInt(AutoAssignmentShiftResultResponse::openSlotsBefore)
                .sum();
        int totalOpenSlotsAfter = shifts.stream()
                .mapToInt(AutoAssignmentShiftResultResponse::openSlotsAfter)
                .sum();

        return new AutoAssignmentReportResponse(
                scheduleId,
                shifts.size(),
                assignmentsCreated,
                totalOpenSlotsBefore,
                totalOpenSlotsAfter,
                shifts
        );
    }
}
