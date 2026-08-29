package com.hilimor.shiftmanagement.team;

import java.util.List;

public record TeamMembershipResponse(
        Long teamId,
        String teamName,
        List<String> staffingRoleNames
) {

    static TeamMembershipResponse from(TeamMember teamMember, List<String> staffingRoleNames) {
        return new TeamMembershipResponse(
                teamMember.getTeam().getId(),
                teamMember.getTeam().getName(),
                staffingRoleNames
        );
    }
}
