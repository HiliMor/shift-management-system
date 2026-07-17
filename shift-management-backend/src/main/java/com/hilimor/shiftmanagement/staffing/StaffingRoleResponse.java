package com.hilimor.shiftmanagement.staffing;

public record StaffingRoleResponse(
        Long id,
        Long teamId,
        String name,
        String description
) {

    public static StaffingRoleResponse from(StaffingRole staffingRole) {
        return new StaffingRoleResponse(
                staffingRole.getId(),
                staffingRole.getTeam().getId(),
                staffingRole.getName(),
                staffingRole.getDescription()
        );
    }
}
