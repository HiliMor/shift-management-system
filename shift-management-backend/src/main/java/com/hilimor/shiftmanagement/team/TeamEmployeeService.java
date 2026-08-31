package com.hilimor.shiftmanagement.team;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

import com.hilimor.shiftmanagement.schedule.ScheduleWriteLock;
import com.hilimor.shiftmanagement.staffing.StaffingRole;
import com.hilimor.shiftmanagement.staffing.StaffingRoleRepository;
import com.hilimor.shiftmanagement.staffing.TeamMemberStaffingRole;
import com.hilimor.shiftmanagement.staffing.TeamMemberStaffingRoleRepository;
import com.hilimor.shiftmanagement.user.ApplicationRole;
import com.hilimor.shiftmanagement.user.User;
import com.hilimor.shiftmanagement.user.UserRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TeamEmployeeService {
    private static final Logger log = LoggerFactory.getLogger(TeamEmployeeService.class);

    private final UserRepository users;
    private final TeamRepository teams;
    private final TeamManagerRepository managers;
    private final TeamMemberRepository members;
    private final StaffingRoleRepository roles;
    private final TeamMemberStaffingRoleRepository memberRoles;
    private final PasswordEncoder passwordEncoder;
    private final ScheduleWriteLock writeLock;

    public TeamEmployeeService(UserRepository users, TeamRepository teams, TeamManagerRepository managers,
            TeamMemberRepository members, StaffingRoleRepository roles,
            TeamMemberStaffingRoleRepository memberRoles, PasswordEncoder passwordEncoder, ScheduleWriteLock writeLock) {
        this.users = users;
        this.teams = teams;
        this.managers = managers;
        this.members = members;
        this.roles = roles;
        this.memberRoles = memberRoles;
        this.passwordEncoder = passwordEncoder;
        this.writeLock = writeLock;
    }

    @Transactional
    public TeamEmployeeResponse createEmployee(String managerUsername, Long teamId, CreateTeamEmployeeRequest request) {
        User manager = users.findByUsername(managerUsername)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Only a team manager can create employees"));
        if (manager.getApplicationRole() != ApplicationRole.MANAGER
                || !managers.existsByManager_UsernameAndTeam_Id(managerUsername, teamId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only a team manager can create employees");
        }
        // BCrypt has a byte limit, not a Unicode character limit.
        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must not exceed 72 UTF-8 bytes");
        }
        if (users.existsByUsername(request.username())) throw duplicateUsername();
        String passwordHash = passwordEncoder.encode(request.password());
        Team team = teams.findById(teamId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Team not found"));
        writeLock.lockTeam(team);

        List<StaffingRole> selectedRoles = request.staffingRoleIds().stream().distinct().sorted()
                .map(id -> roles.findById(id)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Staffing role is not available for this team")))
                .toList();
        if (selectedRoles.stream().anyMatch(role -> !Objects.equals(role.getTeam().getId(), teamId))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Staffing role is not available for this team");
        }

        User employee;
        try {
            // The unique database key also rejects same-name requests from different teams.
            employee = users.saveAndFlush(new User(request.username(), passwordHash, request.fullName(),
                    request.email(), ApplicationRole.EMPLOYEE));
        } catch (DataIntegrityViolationException exception) {
            throw duplicateUsername();
        }
        Instant now = Instant.now();
        TeamMember member = members.save(new TeamMember(employee, team, now, true));
        memberRoles.saveAllAndFlush(selectedRoles.stream().map(role -> new TeamMemberStaffingRole(member, role, now)).toList());
        log.info("Employee {} created in team {} by manager {}", employee.getId(), teamId, manager.getId());
        return TeamEmployeeResponse.from(employee, selectedRoles.stream().map(StaffingRole::getId).toList(),
                selectedRoles.stream().map(StaffingRole::getName).toList());
    }

    private ResponseStatusException duplicateUsername() {
        return new ResponseStatusException(HttpStatus.CONFLICT, "Username is already in use");
    }
}
