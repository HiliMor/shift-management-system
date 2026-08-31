package com.hilimor.shiftmanagement.team;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/teams/{teamId}/employees")
public class TeamEmployeeController {
    private final TeamEmployeeService teamEmployeeService;

    public TeamEmployeeController(TeamEmployeeService teamEmployeeService) {
        this.teamEmployeeService = teamEmployeeService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TeamEmployeeResponse createEmployee(Authentication authentication, @PathVariable Long teamId,
            @Valid @RequestBody CreateTeamEmployeeRequest request) {
        return teamEmployeeService.createEmployee(authentication.getName(), teamId, request);
    }
}
