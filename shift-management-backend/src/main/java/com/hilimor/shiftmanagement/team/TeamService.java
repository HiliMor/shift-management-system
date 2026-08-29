package com.hilimor.shiftmanagement.team;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.hilimor.shiftmanagement.staffing.TeamMemberStaffingRole;
import com.hilimor.shiftmanagement.staffing.TeamMemberStaffingRoleRepository;
import com.hilimor.shiftmanagement.user.User;
import com.hilimor.shiftmanagement.user.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TeamService {

    private final TeamManagerRepository teamManagerRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final TeamMemberStaffingRoleRepository teamMemberStaffingRoleRepository;
    private final UserRepository userRepository;

    public TeamService(
            TeamManagerRepository teamManagerRepository,
            TeamMemberRepository teamMemberRepository,
            TeamMemberStaffingRoleRepository teamMemberStaffingRoleRepository,
            UserRepository userRepository
    ) {
        this.teamManagerRepository = teamManagerRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.teamMemberStaffingRoleRepository = teamMemberStaffingRoleRepository;
        this.userRepository = userRepository;
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
    public List<TeamMembershipResponse> listMyMemberships(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        List<TeamMember> activeMembers = teamMemberRepository.findByUser_IdAndActiveTrue(user.getId());
        Map<Long, List<String>> roleNamesByMemberId = teamMemberStaffingRoleRepository
                .findByTeamMember_User_Id(user.getId())
                .stream()
                .filter(role -> role.getTeamMember().isActive())
                .collect(Collectors.groupingBy(
                        role -> role.getTeamMember().getId(),
                        Collectors.mapping(
                                role -> role.getStaffingRole().getName(),
                                Collectors.collectingAndThen(Collectors.toList(), roleNames -> roleNames.stream()
                                        .sorted()
                                        .toList())
                        )
                ));

        return activeMembers
                .stream()
                .sorted(Comparator.comparing(member -> member.getTeam().getName()))
                .map(member -> TeamMembershipResponse.from(
                        member,
                        roleNamesByMemberId.getOrDefault(member.getId(), List.of())
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TeamEmployeeResponse> listTeamEmployees(String managerUsername, Long teamId) {
        if (!teamManagerRepository.existsByManager_UsernameAndTeam_Id(managerUsername, teamId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only a team manager can view team employees");
        }

        List<TeamMember> activeMembers = teamMemberRepository.findByTeam_IdAndActiveTrue(teamId);
        Map<Long, List<TeamMemberStaffingRole>> rolesByMemberId = teamMemberStaffingRoleRepository
                .findByTeamMember_Team_Id(teamId)
                .stream()
                .collect(Collectors.groupingBy(
                        role -> role.getTeamMember().getId()
                ));

        return activeMembers
                .stream()
                .sorted(Comparator.comparing((TeamMember member) -> member.getUser().getFullName())
                        .thenComparing(member -> member.getUser().getUsername()))
                .map(member -> {
                    List<TeamMemberStaffingRole> roles = rolesByMemberId.getOrDefault(member.getId(), List.of())
                            .stream()
                            .sorted(Comparator.comparing(role -> role.getStaffingRole().getName()))
                            .toList();

                    return TeamEmployeeResponse.from(
                            member.getUser(),
                            roles.stream().map(role -> role.getStaffingRole().getId()).toList(),
                            roles.stream().map(role -> role.getStaffingRole().getName()).toList()
                    );
                })
                .toList();
    }
}
