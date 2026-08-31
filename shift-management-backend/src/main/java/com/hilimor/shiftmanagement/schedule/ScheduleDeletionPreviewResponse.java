package com.hilimor.shiftmanagement.schedule;

public record ScheduleDeletionPreviewResponse(
        ScheduleResponse schedule,
        int shiftCount,
        int assignmentCount,
        String revision
) {
}
