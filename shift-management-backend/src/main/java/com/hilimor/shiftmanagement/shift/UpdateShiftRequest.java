package com.hilimor.shiftmanagement.shift;

import java.time.Instant;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record UpdateShiftRequest(
        @NotNull Instant startTime,
        @NotNull Instant endTime,
        @Size(max = 500) String description,
        @NotNull @Positive Integer requiredWorkers,
        @NotNull @Min(0) Integer minRestHours,
        Long requiredStaffingRoleId,
        @NotNull @Min(0) Long version
) {
}
