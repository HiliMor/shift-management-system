package com.hilimor.shiftmanagement.schedule;

import java.time.Instant;
import java.time.LocalDate;

public record SchedulePublishedEvent(
        Long scheduleId,
        Long teamId,
        String teamName,
        LocalDate startDate,
        LocalDate endDate,
        int publicationNumber,
        Instant publishedAt
) {

    static SchedulePublishedEvent from(Schedule schedule) {
        return new SchedulePublishedEvent(
                schedule.getId(),
                schedule.getTeam().getId(),
                schedule.getTeam().getName(),
                schedule.getStartDate(),
                schedule.getEndDate(),
                schedule.getPublicationNumber(),
                schedule.getPublishedAt()
        );
    }
}
