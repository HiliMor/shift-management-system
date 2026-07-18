package com.hilimor.shiftmanagement.staffing;

import jakarta.validation.constraints.NotNull;

public record AssignStaffingRoleRequest(
        @NotNull Long staffingRoleId
) {
}
