package com.hilimor.shiftmanagement.template;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateShiftTemplateRequest(
        @NotBlank @Size(max = 150) String name,
        @Size(max = 500) String description,
        @Positive int cycleDays,
        @Min(0) int defaultMinRestHours
) {
}
