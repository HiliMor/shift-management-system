package com.hilimor.shiftmanagement.team;

import java.util.List;

import com.hilimor.shiftmanagement.user.User;

public record TeamEmployeeResponse(
        Long id,
        String username,
        String fullName,
        List<Long> staffingRoleIds,
        List<String> staffingRoleNames
) {

    static TeamEmployeeResponse from(User user, List<Long> staffingRoleIds, List<String> staffingRoleNames) {
        return new TeamEmployeeResponse(
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                staffingRoleIds,
                staffingRoleNames
        );
    }
}
