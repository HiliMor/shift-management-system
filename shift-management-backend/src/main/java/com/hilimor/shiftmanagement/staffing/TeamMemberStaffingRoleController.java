package com.hilimor.shiftmanagement.staffing;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/teams/{teamId}/employees/{employeeId}/staffing-roles")
public class TeamMemberStaffingRoleController {

    private final TeamMemberStaffingRoleService teamMemberStaffingRoleService;

    public TeamMemberStaffingRoleController(TeamMemberStaffingRoleService teamMemberStaffingRoleService) {
        this.teamMemberStaffingRoleService = teamMemberStaffingRoleService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TeamMemberStaffingRoleResponse assignRole(
            Authentication authentication,
            @PathVariable Long teamId,
            @PathVariable Long employeeId,
            @Valid @RequestBody AssignStaffingRoleRequest request
    ) {
        return teamMemberStaffingRoleService.assignRole(authentication.getName(), teamId, employeeId, request);
    }

    @GetMapping
    public List<TeamMemberStaffingRoleResponse> listEmployeeRoles(
            Authentication authentication,
            @PathVariable Long teamId,
            @PathVariable Long employeeId
    ) {
        return teamMemberStaffingRoleService.listEmployeeRoles(authentication.getName(), teamId, employeeId);
    }
}
