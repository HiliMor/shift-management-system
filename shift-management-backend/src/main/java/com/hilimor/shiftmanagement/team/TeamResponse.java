package com.hilimor.shiftmanagement.team;

public record TeamResponse(
        Long id,
        String name,
        SwapApprovalPolicy swapApprovalPolicy,
        int defaultMinRestHours,
        String timeZone
) {

    static TeamResponse from(Team team) {
        return new TeamResponse(
                team.getId(),
                team.getName(),
                team.getSwapApprovalPolicy(),
                team.getDefaultMinRestHours(),
                team.getTimeZone()
        );
    }
}
