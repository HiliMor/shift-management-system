package com.hilimor.shiftmanagement.request;

import jakarta.validation.constraints.NotNull;

public record CreateTransferRequest(
        @NotNull Long sourceAssignmentId,
        @NotNull Long targetEmployeeId
) {
}
