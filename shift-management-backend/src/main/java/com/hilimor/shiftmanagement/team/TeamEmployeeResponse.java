package com.hilimor.shiftmanagement.team;

import com.hilimor.shiftmanagement.user.User;

public record TeamEmployeeResponse(
        Long id,
        String username,
        String fullName
) {

    static TeamEmployeeResponse from(User user) {
        return new TeamEmployeeResponse(
                user.getId(),
                user.getUsername(),
                user.getFullName()
        );
    }
}
