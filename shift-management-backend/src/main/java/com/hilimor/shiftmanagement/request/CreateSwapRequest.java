package com.hilimor.shiftmanagement.request;

import jakarta.validation.constraints.NotNull;

public record CreateSwapRequest(
        @NotNull Long sourceAssignmentId,
        @NotNull Long targetAssignmentId
) {
}
