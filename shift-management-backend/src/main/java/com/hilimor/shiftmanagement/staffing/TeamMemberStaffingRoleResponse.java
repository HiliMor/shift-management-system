package com.hilimor.shiftmanagement.staffing;

import java.time.Instant;

public record TeamMemberStaffingRoleResponse(
        Long id,
        Long teamId,
        Long teamMemberId,
        Long employeeId,
        String employeeUsername,
        String employeeFullName,
        Long staffingRoleId,
        String staffingRoleName,
        Instant assignedAt
) {

    public static TeamMemberStaffingRoleResponse from(TeamMemberStaffingRole teamMemberStaffingRole) {
        return new TeamMemberStaffingRoleResponse(
                teamMemberStaffingRole.getId(),
                teamMemberStaffingRole.getTeamMember().getTeam().getId(),
                teamMemberStaffingRole.getTeamMember().getId(),
                teamMemberStaffingRole.getTeamMember().getUser().getId(),
                teamMemberStaffingRole.getTeamMember().getUser().getUsername(),
                teamMemberStaffingRole.getTeamMember().getUser().getFullName(),
                teamMemberStaffingRole.getStaffingRole().getId(),
                teamMemberStaffingRole.getStaffingRole().getName(),
                teamMemberStaffingRole.getAssignedAt()
        );
    }
}
