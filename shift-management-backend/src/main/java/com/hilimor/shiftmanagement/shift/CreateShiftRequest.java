package com.hilimor.shiftmanagement.shift;

import java.time.Instant;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateShiftRequest(
        @NotNull Instant startTime,
        @NotNull Instant endTime,
        @Size(max = 500) String description,
        @Positive int requiredWorkers,
        @Min(0) int minRestHours
) {
}
