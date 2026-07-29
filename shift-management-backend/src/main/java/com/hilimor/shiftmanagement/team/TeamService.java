package com.hilimor.shiftmanagement.team;

import java.util.Comparator;
import java.util.List;

import com.hilimor.shiftmanagement.user.User;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TeamService {

    private final TeamManagerRepository teamManagerRepository;
    private final TeamMemberRepository teamMemberRepository;

    public TeamService(TeamManagerRepository teamManagerRepository, TeamMemberRepository teamMemberRepository) {
        this.teamManagerRepository = teamManagerRepository;
        this.teamMemberRepository = teamMemberRepository;
    }

    @Transactional(readOnly = true)
    public List<TeamResponse> listManagedTeams(String username) {
        return teamManagerRepository.findByManager_Username(username)
                .stream()
                .map(TeamManager::getTeam)
                .sorted(Comparator.comparing(Team::getName))
                .map(TeamResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TeamEmployeeResponse> listTeamEmployees(String managerUsername, Long teamId) {
        if (!teamManagerRepository.existsByManager_UsernameAndTeam_Id(managerUsername, teamId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only a team manager can view team employees");
        }

        return teamMemberRepository.findByTeam_IdAndActiveTrue(teamId)
                .stream()
                .map(TeamMember::getUser)
                .sorted(Comparator.comparing(User::getFullName).thenComparing(User::getUsername))
                .map(TeamEmployeeResponse::from)
                .toList();
    }
}
