package com.hilimor.shiftmanagement.assignment;

import java.time.Instant;
import java.util.List;

import com.hilimor.shiftmanagement.shift.Shift;

public record AutoAssignmentShiftResultResponse(
        Long shiftId,
        Instant startTime,
        Instant endTime,
        String description,
        int requiredWorkers,
        int assignedWorkersBefore,
        int assignmentsCreated,
        int openSlotsBefore,
        int openSlotsAfter,
        String message,
        List<AssignmentResponse> createdAssignments
) {

    static AutoAssignmentShiftResultResponse from(
            Shift shift,
            int assignedWorkersBefore,
            int openSlotsBefore,
            int openSlotsAfter,
            List<AssignmentResponse> createdAssignments
    ) {
        return new AutoAssignmentShiftResultResponse(
                shift.getId(),
                shift.getStartTime(),
                shift.getEndTime(),
                shift.getDescription(),
                shift.getRequiredWorkers(),
                assignedWorkersBefore,
                createdAssignments.size(),
                openSlotsBefore,
                openSlotsAfter,
                resultMessage(openSlotsBefore, openSlotsAfter, createdAssignments.size()),
                createdAssignments
        );
    }

    private static String resultMessage(int openSlotsBefore, int openSlotsAfter, int assignmentsCreated) {
        if (openSlotsBefore == 0) {
            return "Shift is already fully assigned";
        }

        if (openSlotsAfter == 0) {
            return "All open slots were assigned";
        }

        if (assignmentsCreated == 0) {
            return "No eligible employees were available for the open slots";
        }

        return "Some open slots were assigned, but the shift is still not full";
    }
}
