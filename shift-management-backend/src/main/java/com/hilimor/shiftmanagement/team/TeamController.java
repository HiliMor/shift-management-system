package com.hilimor.shiftmanagement.team;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/teams")
public class TeamController {

    private final TeamService teamService;

    public TeamController(TeamService teamService) {
        this.teamService = teamService;
    }

    @GetMapping("/me/managed")
    public List<TeamResponse> listMyManagedTeams(Authentication authentication) {
        return teamService.listManagedTeams(authentication.getName());
    }

    @GetMapping("/me/memberships")
    public List<TeamMembershipResponse> listMyMemberships(Authentication authentication) {
        return teamService.listMyMemberships(authentication.getName());
    }

    @GetMapping("/{teamId}/employees")
    public List<TeamEmployeeResponse> listTeamEmployees(
            Authentication authentication,
            @PathVariable Long teamId
    ) {
        return teamService.listTeamEmployees(authentication.getName(), teamId);
    }
}
