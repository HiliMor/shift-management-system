package com.hilimor.shiftmanagement.assignment;

import jakarta.validation.constraints.NotNull;

public record CreateAssignmentRequest(
        @NotNull Long shiftId,
        @NotNull Long employeeId
) {
}
