package com.hilimor.shiftmanagement.team;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.hilimor.shiftmanagement.staffing.TeamMemberStaffingRoleRepository;
import com.hilimor.shiftmanagement.user.User;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TeamService {

    private final TeamManagerRepository teamManagerRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final TeamMemberStaffingRoleRepository teamMemberStaffingRoleRepository;

    public TeamService(
            TeamManagerRepository teamManagerRepository,
            TeamMemberRepository teamMemberRepository,
            TeamMemberStaffingRoleRepository teamMemberStaffingRoleRepository
    ) {
        this.teamManagerRepository = teamManagerRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.teamMemberStaffingRoleRepository = teamMemberStaffingRoleRepository;
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

        List<TeamMember> activeMembers = teamMemberRepository.findByTeam_IdAndActiveTrue(teamId);
        Map<Long, List<String>> roleNamesByMemberId = teamMemberStaffingRoleRepository
                .findByTeamMember_Team_Id(teamId)
                .stream()
                .collect(Collectors.groupingBy(
                        role -> role.getTeamMember().getId(),
                        Collectors.mapping(
                                role -> role.getStaffingRole().getName(),
                                Collectors.toList()
                        )
                ));

        return activeMembers
                .stream()
                .sorted(Comparator.comparing((TeamMember member) -> member.getUser().getFullName())
                        .thenComparing(member -> member.getUser().getUsername()))
                .map(member -> TeamEmployeeResponse.from(
                        member.getUser(),
                        roleNamesByMemberId.getOrDefault(member.getId(), List.of())
                ))
                .toList();
    }
}
