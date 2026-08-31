package com.hilimor.shiftmanagement.team;

import java.util.List;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateTeamEmployeeRequest(
        @NotBlank @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9._-]{2,99}",
                message = "must be 3-100 letters, digits, dots, underscores or hyphens, starting with a letter or digit") String username,
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotBlank @Size(max = 200) String fullName,
        @Email @Size(max = 255) String email,
        @Size(max = 20) List<@NotNull @Positive Long> staffingRoleIds
) {
    public CreateTeamEmployeeRequest {
        username = username == null ? null : username.trim();
        fullName = fullName == null ? null : fullName.trim();
        email = email == null || email.isBlank() ? null : email.trim();
        staffingRoleIds = staffingRoleIds == null ? List.of() : staffingRoleIds;
    }

    @Override
    public String toString() {
        return "CreateTeamEmployeeRequest[redacted]";
    }
}
