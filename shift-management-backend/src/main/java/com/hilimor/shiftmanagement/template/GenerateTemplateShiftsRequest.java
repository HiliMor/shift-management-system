package com.hilimor.shiftmanagement.template;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record GenerateTemplateShiftsRequest(
        @NotNull
        @Positive
        Long scheduleId
) {
}
