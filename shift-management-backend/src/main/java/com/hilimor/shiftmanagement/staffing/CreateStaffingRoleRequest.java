package com.hilimor.shiftmanagement.staffing;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateStaffingRoleRequest(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 500) String description
) {
}
