package com.hilimor.shiftmanagement.staffing;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

import com.hilimor.shiftmanagement.team.Team;
import com.hilimor.shiftmanagement.team.TeamManagerRepository;
import com.hilimor.shiftmanagement.team.TeamMember;
import com.hilimor.shiftmanagement.team.TeamMemberRepository;
import com.hilimor.shiftmanagement.team.TeamRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TeamMemberStaffingRoleService {

    private final TeamRepository teamRepository;
    private final TeamManagerRepository teamManagerRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final StaffingRoleRepository staffingRoleRepository;
    private final TeamMemberStaffingRoleRepository teamMemberStaffingRoleRepository;

    public TeamMemberStaffingRoleService(
            TeamRepository teamRepository,
            TeamManagerRepository teamManagerRepository,
            TeamMemberRepository teamMemberRepository,
            StaffingRoleRepository staffingRoleRepository,
            TeamMemberStaffingRoleRepository teamMemberStaffingRoleRepository
    ) {
        this.teamRepository = teamRepository;
        this.teamManagerRepository = teamManagerRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.staffingRoleRepository = staffingRoleRepository;
        this.teamMemberStaffingRoleRepository = teamMemberStaffingRoleRepository;
    }

    @Transactional
    public TeamMemberStaffingRoleResponse assignRole(
            String username,
            Long teamId,
            Long employeeId,
            AssignStaffingRoleRequest request
    ) {
        managedTeam(username, teamId);
        TeamMember teamMember = activeTeamMember(employeeId, teamId);
        StaffingRole staffingRole = staffingRoleForTeam(request.staffingRoleId(), teamId);

        if (teamMemberStaffingRoleRepository.existsByTeamMember_IdAndStaffingRole_Id(
                teamMember.getId(),
                staffingRole.getId()
        )) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Employee already has this staffing role");
        }

        TeamMemberStaffingRole teamMemberStaffingRole = new TeamMemberStaffingRole(
                teamMember,
                staffingRole,
                Instant.now()
        );
        TeamMemberStaffingRole savedRole = teamMemberStaffingRoleRepository.save(teamMemberStaffingRole);

        return TeamMemberStaffingRoleResponse.from(savedRole);
    }

    @Transactional(readOnly = true)
    public List<TeamMemberStaffingRoleResponse> listEmployeeRoles(String username, Long teamId, Long employeeId) {
        managedTeam(username, teamId);
        TeamMember teamMember = activeTeamMember(employeeId, teamId);

        return teamMemberStaffingRoleRepository.findByTeamMember_Id(teamMember.getId())
                .stream()
                .map(TeamMemberStaffingRoleResponse::from)
                .toList();
    }

    private Team managedTeam(String username, Long teamId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Team not found"));

        if (!teamManagerRepository.existsByManager_UsernameAndTeam_Id(username, teamId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only a team manager can manage employee staffing roles for this team");
        }

        return team;
    }

    private TeamMember activeTeamMember(Long employeeId, Long teamId) {
        return teamMemberRepository.findByUser_IdAndTeam_IdAndActiveTrue(employeeId, teamId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Active team member not found"));
    }

    private StaffingRole staffingRoleForTeam(Long staffingRoleId, Long teamId) {
        StaffingRole staffingRole = staffingRoleRepository.findById(staffingRoleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Staffing role not found"));

        if (!Objects.equals(staffingRole.getTeam().getId(), teamId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Staffing role must belong to the requested team");
        }

        return staffingRole;
    }
}
