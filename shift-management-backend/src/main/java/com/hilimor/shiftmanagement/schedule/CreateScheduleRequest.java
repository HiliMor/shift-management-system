package com.hilimor.shiftmanagement.schedule;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;

public record CreateScheduleRequest(
        @NotNull Long teamId,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate
) {
}
