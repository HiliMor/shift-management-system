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
@RequestMapping("/api/teams/{teamId}/staffing-roles")
public class StaffingRoleController {

    private final StaffingRoleService staffingRoleService;

    public StaffingRoleController(StaffingRoleService staffingRoleService) {
        this.staffingRoleService = staffingRoleService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StaffingRoleResponse createRole(
            Authentication authentication,
            @PathVariable Long teamId,
            @Valid @RequestBody CreateStaffingRoleRequest request
    ) {
        return staffingRoleService.createRole(authentication.getName(), teamId, request);
    }

    @GetMapping
    public List<StaffingRoleResponse> listRoles(Authentication authentication, @PathVariable Long teamId) {
        return staffingRoleService.listRoles(authentication.getName(), teamId);
    }
}
