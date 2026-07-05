package com.hilimor.shiftmanagement.schedule;

import java.time.Instant;
import java.time.LocalDate;

public record ScheduleResponse(
        Long id,
        Long teamId,
        String teamName,
        LocalDate startDate,
        LocalDate endDate,
        ScheduleStatus status,
        int publicationNumber,
        Instant publishedAt
) {

    static ScheduleResponse from(Schedule schedule) {
        return new ScheduleResponse(
                schedule.getId(),
                schedule.getTeam().getId(),
                schedule.getTeam().getName(),
                schedule.getStartDate(),
                schedule.getEndDate(),
                schedule.getStatus(),
                schedule.getPublicationNumber(),
                schedule.getPublishedAt()
        );
    }
}
