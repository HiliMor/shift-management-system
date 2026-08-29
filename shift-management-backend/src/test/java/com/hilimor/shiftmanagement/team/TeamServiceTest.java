package com.hilimor.shiftmanagement.team;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import com.hilimor.shiftmanagement.user.ApplicationRole;
import com.hilimor.shiftmanagement.user.User;
import com.hilimor.shiftmanagement.staffing.StaffingRole;
import com.hilimor.shiftmanagement.staffing.TeamMemberStaffingRole;
import com.hilimor.shiftmanagement.staffing.TeamMemberStaffingRoleRepository;
import com.hilimor.shiftmanagement.user.UserRepository;

@ExtendWith(MockitoExtension.class)
class TeamServiceTest {

    @Mock
    private TeamManagerRepository teamManagerRepository;

    @Mock
    private TeamMemberRepository teamMemberRepository;

    @Mock
    private TeamMemberStaffingRoleRepository teamMemberStaffingRoleRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TeamService teamService;

    @Test
    void listManagedTeamsReturnsManagerTeamsSortedByName() {
        User manager = new User(
                "manager1",
                "hash",
                "Demo Manager",
                "manager1@example.com",
                ApplicationRole.MANAGER
        );
        Team warehouse = team(2L, "Warehouse");
        Team operations = team(1L, "Operations");

        when(teamManagerRepository.findByManager_Username("manager1"))
                .thenReturn(List.of(new TeamManager(manager, warehouse), new TeamManager(manager, operations)));

        List<TeamResponse> responses = teamService.listManagedTeams("manager1");

        assertThat(responses).hasSize(2);
        assertThat(responses).extracting(TeamResponse::id).containsExactly(1L, 2L);
        assertThat(responses).extracting(TeamResponse::name).containsExactly("Operations", "Warehouse");
        assertThat(responses.get(0).defaultMinRestHours()).isEqualTo(8);
        assertThat(responses.get(0).timeZone()).isEqualTo("Asia/Jerusalem");
    }

    @Test
    void listManagedTeamsReturnsEmptyListWhenUserManagesNoTeams() {
        when(teamManagerRepository.findByManager_Username("employee1")).thenReturn(List.of());

        List<TeamResponse> responses = teamService.listManagedTeams("employee1");

        assertThat(responses).isEmpty();
    }

    @Test
    void listMyMembershipsReturnsActiveTeamsAndRolesSortedByTeamName() {
        User employee = employee(10L, "employee1", "Demo Employee");
        Team warehouse = team(2L, "Warehouse");
        Team operations = team(1L, "Operations");
        TeamMember warehouseMember = teamMember(employee, warehouse);
        TeamMember operationsMember = teamMember(employee, operations);
        ReflectionTestUtils.setField(warehouseMember, "id", 22L);
        ReflectionTestUtils.setField(operationsMember, "id", 11L);

        StaffingRole backendRole = new StaffingRole(operations, "Backend Developer", "Backend work");
        StaffingRole frontendRole = new StaffingRole(operations, "Frontend Developer", "Frontend work");

        when(userRepository.findByUsername("employee1")).thenReturn(Optional.of(employee));
        when(teamMemberRepository.findByUser_IdAndActiveTrue(10L))
                .thenReturn(List.of(warehouseMember, operationsMember));
        when(teamMemberStaffingRoleRepository.findByTeamMember_User_Id(10L))
                .thenReturn(List.of(
                        new TeamMemberStaffingRole(operationsMember, frontendRole, Instant.parse("2026-07-01T09:00:00Z")),
                        new TeamMemberStaffingRole(operationsMember, backendRole, Instant.parse("2026-07-01T09:00:00Z"))
                ));

        List<TeamMembershipResponse> responses = teamService.listMyMemberships("employee1");

        assertThat(responses).hasSize(2);
        assertThat(responses).extracting(TeamMembershipResponse::teamName)
                .containsExactly("Operations", "Warehouse");
        assertThat(responses.get(0).staffingRoleNames())
                .containsExactly("Backend Developer", "Frontend Developer");
        assertThat(responses.get(1).staffingRoleNames()).isEmpty();
    }

    @Test
    void listTeamEmployeesReturnsActiveMembersForManagedTeamSortedByName() {
        Team team = team(1L, "Operations");
        User employeeTwo = employee(3L, "employee2", "Demo Employee Two");
        User employeeOne = employee(2L, "employee1", "Demo Employee One");
        TeamMember employeeTwoMember = teamMember(employeeTwo, team);
        TeamMember employeeOneMember = teamMember(employeeOne, team);
        ReflectionTestUtils.setField(employeeTwoMember, "id", 31L);
        ReflectionTestUtils.setField(employeeOneMember, "id", 32L);
        StaffingRole backendRole = new StaffingRole(team, "Backend Developer", "Backend work");
        StaffingRole frontendRole = new StaffingRole(team, "Frontend Developer", "Frontend work");
        ReflectionTestUtils.setField(backendRole, "id", 41L);
        ReflectionTestUtils.setField(frontendRole, "id", 42L);

        when(teamManagerRepository.existsByManager_UsernameAndTeam_Id("manager1", 1L)).thenReturn(true);
        when(teamMemberRepository.findByTeam_IdAndActiveTrue(1L))
                .thenReturn(List.of(employeeTwoMember, employeeOneMember));
        when(teamMemberStaffingRoleRepository.findByTeamMember_Team_Id(1L))
                .thenReturn(List.of(
                        new TeamMemberStaffingRole(employeeOneMember, backendRole, Instant.parse("2026-07-01T09:00:00Z")),
                        new TeamMemberStaffingRole(employeeTwoMember, frontendRole, Instant.parse("2026-07-01T09:00:00Z"))
                ));

        List<TeamEmployeeResponse> responses = teamService.listTeamEmployees("manager1", 1L);

        assertThat(responses).hasSize(2);
        assertThat(responses).extracting(TeamEmployeeResponse::id).containsExactly(2L, 3L);
        assertThat(responses).extracting(TeamEmployeeResponse::username).containsExactly("employee1", "employee2");
        assertThat(responses).extracting(TeamEmployeeResponse::fullName)
                .containsExactly("Demo Employee One", "Demo Employee Two");
        assertThat(responses).extracting(TeamEmployeeResponse::staffingRoleIds)
                .containsExactly(List.of(backendRole.getId()), List.of(frontendRole.getId()));
        assertThat(responses).extracting(TeamEmployeeResponse::staffingRoleNames)
                .containsExactly(List.of("Backend Developer"), List.of("Frontend Developer"));
    }

    @Test
    void listTeamEmployeesRejectsUnmanagedTeam() {
        when(teamManagerRepository.existsByManager_UsernameAndTeam_Id("manager2", 1L)).thenReturn(false);

        assertThatThrownBy(() -> teamService.listTeamEmployees("manager2", 1L))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
                    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(exception.getReason()).isEqualTo("Only a team manager can view team employees");
                });

        verifyNoInteractions(teamMemberRepository, teamMemberStaffingRoleRepository);
    }

    private Team team(Long id, String name) {
        Team team = new Team(name, SwapApprovalPolicy.MANAGER, 8, "Asia/Jerusalem");
        ReflectionTestUtils.setField(team, "id", id);
        return team;
    }

    private User employee(Long id, String username, String fullName) {
        User user = new User(username, "hash", fullName, username + "@example.com", ApplicationRole.EMPLOYEE);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private TeamMember teamMember(User user, Team team) {
        return new TeamMember(user, team, Instant.parse("2026-07-01T09:00:00Z"), true);
    }
}
