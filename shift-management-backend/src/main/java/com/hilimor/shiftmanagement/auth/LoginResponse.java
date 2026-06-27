package com.hilimor.shiftmanagement.auth;

import com.hilimor.shiftmanagement.user.ApplicationRole;

public record LoginResponse(
        String tokenType,
        String accessToken,
        long expiresInSeconds,
        AuthenticatedUser user
) {

    public record AuthenticatedUser(
            Long id,
            String username,
            String fullName,
            ApplicationRole applicationRole
    ) {
    }
}
