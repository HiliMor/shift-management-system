package com.hilimor.shiftmanagement.availability;

import java.time.Instant;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateAvailabilityConstraintRequest(
        @NotNull Instant startTime,
        @NotNull Instant endTime,
        @Size(max = 500) String reason
) {
}
