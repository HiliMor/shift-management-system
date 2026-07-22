package com.hilimor.shiftmanagement.schedule;

import java.util.List;

public record PublishedScheduleDetailsResponse(
        ScheduleResponse schedule,
        List<PublishedShiftResponse> shifts
) {

    static PublishedScheduleDetailsResponse from(Schedule schedule, List<PublishedShiftResponse> shifts) {
        return new PublishedScheduleDetailsResponse(
                ScheduleResponse.from(schedule),
                shifts
        );
    }
}
