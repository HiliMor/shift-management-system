package com.hilimor.shiftmanagement.template;

import java.time.LocalTime;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateTemplateSlotRequest(
        @NotNull @Min(0) Integer dayOffset,
        @NotNull LocalTime startTime,
        @NotNull @Positive Integer durationMinutes,
        @Size(max = 500) String description,
        @NotNull @Positive Integer requiredWorkers,
        Long requiredStaffingRoleId
) {
}
