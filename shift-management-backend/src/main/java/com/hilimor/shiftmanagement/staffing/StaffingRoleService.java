package com.hilimor.shiftmanagement.staffing;

import java.util.List;

import com.hilimor.shiftmanagement.team.Team;
import com.hilimor.shiftmanagement.team.TeamManagerRepository;
import com.hilimor.shiftmanagement.team.TeamRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class StaffingRoleService {

    private final StaffingRoleRepository staffingRoleRepository;
    private final TeamRepository teamRepository;
    private final TeamManagerRepository teamManagerRepository;

    public StaffingRoleService(
            StaffingRoleRepository staffingRoleRepository,
            TeamRepository teamRepository,
            TeamManagerRepository teamManagerRepository
    ) {
        this.staffingRoleRepository = staffingRoleRepository;
        this.teamRepository = teamRepository;
        this.teamManagerRepository = teamManagerRepository;
    }

    @Transactional
    public StaffingRoleResponse createRole(String username, Long teamId, CreateStaffingRoleRequest request) {
        Team team = managedTeam(username, teamId);
        StaffingRole staffingRole = new StaffingRole(team, request.name(), request.description());

        if (staffingRoleRepository.existsByTeam_IdAndName(teamId, staffingRole.getName())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Staffing role name already exists for this team");
        }

        StaffingRole savedRole = staffingRoleRepository.save(staffingRole);

        return StaffingRoleResponse.from(savedRole);
    }

    @Transactional(readOnly = true)
    public List<StaffingRoleResponse> listRoles(String username, Long teamId) {
        managedTeam(username, teamId);

        return staffingRoleRepository.findByTeam_IdOrderByName(teamId)
                .stream()
                .map(StaffingRoleResponse::from)
                .toList();
    }

    private Team managedTeam(String username, Long teamId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Team not found"));

        if (!teamManagerRepository.existsByManager_UsernameAndTeam_Id(username, teamId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only a team manager can manage staffing roles for this team");
        }

        return team;
    }
}
