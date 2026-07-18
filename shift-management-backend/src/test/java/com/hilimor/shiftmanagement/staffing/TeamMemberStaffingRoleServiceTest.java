package com.hilimor.shiftmanagement.staffing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.hilimor.shiftmanagement.team.SwapApprovalPolicy;
import com.hilimor.shiftmanagement.team.Team;
import com.hilimor.shiftmanagement.team.TeamManagerRepository;
import com.hilimor.shiftmanagement.team.TeamMember;
import com.hilimor.shiftmanagement.team.TeamMemberRepository;
import com.hilimor.shiftmanagement.team.TeamRepository;
import com.hilimor.shiftmanagement.user.ApplicationRole;
import com.hilimor.shiftmanagement.user.User;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class TeamMemberStaffingRoleServiceTest {

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private TeamManagerRepository teamManagerRepository;

    @Mock
    private TeamMemberRepository teamMemberRepository;

    @Mock
    private StaffingRoleRepository staffingRoleRepository;

    @Mock
    private TeamMemberStaffingRoleRepository teamMemberStaffingRoleRepository;

    @InjectMocks
    private TeamMemberStaffingRoleService teamMemberStaffingRoleService;

    @Test
    void assignRoleSavesRoleForActiveTeamMemberInManagedTeam() {
        Team team = team(1L, "Operations");
        TeamMember teamMember = teamMember(team, employee());
        StaffingRole staffingRole = staffingRole(team, 20L, "Shift Supervisor");
        AssignStaffingRoleRequest request = new AssignStaffingRoleRequest(20L);

        when(teamRepository.findById(1L)).thenReturn(Optional.of(team));
        when(teamManagerRepository.existsByManager_UsernameAndTeam_Id("manager1", 1L)).thenReturn(true);
        when(teamMemberRepository.findByUser_IdAndTeam_IdAndActiveTrue(2L, 1L)).thenReturn(Optional.of(teamMember));
        when(staffingRoleRepository.findById(20L)).thenReturn(Optional.of(staffingRole));
        when(teamMemberStaffingRoleRepository.existsByTeamMember_IdAndStaffingRole_Id(10L, 20L)).thenReturn(false);
        when(teamMemberStaffingRoleRepository.save(any(TeamMemberStaffingRole.class))).thenAnswer(invocation -> {
            TeamMemberStaffingRole roleAssignment = invocation.getArgument(0);
            ReflectionTestUtils.setField(roleAssignment, "id", 30L);
            return roleAssignment;
        });

        TeamMemberStaffingRoleResponse response = teamMemberStaffingRoleService.assignRole(
                "manager1",
                1L,
                2L,
                request
        );

        assertThat(response.id()).isEqualTo(30L);
        assertThat(response.teamId()).isEqualTo(1L);
        assertThat(response.teamMemberId()).isEqualTo(10L);
        assertThat(response.employeeId()).isEqualTo(2L);
        assertThat(response.employeeUsername()).isEqualTo("employee1");
        assertThat(response.employeeFullName()).isEqualTo("Demo Employee");
        assertThat(response.staffingRoleId()).isEqualTo(20L);
        assertThat(response.staffingRoleName()).isEqualTo("Shift Supervisor");
        assertThat(response.assignedAt()).isNotNull();

        ArgumentCaptor<TeamMemberStaffingRole> captor = ArgumentCaptor.forClass(TeamMemberStaffingRole.class);
        verify(teamMemberStaffingRoleRepository).save(captor.capture());
        assertThat(captor.getValue().getTeamMember()).isSameAs(teamMember);
        assertThat(captor.getValue().getStaffingRole()).isSameAs(staffingRole);
    }

    @Test
    void assignRoleRejectsUnmanagedTeam() {
        AssignStaffingRoleRequest request = new AssignStaffingRoleRequest(20L);

        when(teamRepository.findById(1L)).thenReturn(Optional.of(team(1L, "Operations")));
        when(teamManagerRepository.existsByManager_UsernameAndTeam_Id("employee1", 1L)).thenReturn(false);

        assertThatThrownBy(() -> teamMemberStaffingRoleService.assignRole("employee1", 1L, 2L, request))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));

        verify(teamMemberStaffingRoleRepository, never()).save(any());
    }

    @Test
    void assignRoleRejectsMissingActiveTeamMember() {
        AssignStaffingRoleRequest request = new AssignStaffingRoleRequest(20L);

        when(teamRepository.findById(1L)).thenReturn(Optional.of(team(1L, "Operations")));
        when(teamManagerRepository.existsByManager_UsernameAndTeam_Id("manager1", 1L)).thenReturn(true);
        when(teamMemberRepository.findByUser_IdAndTeam_IdAndActiveTrue(2L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> teamMemberStaffingRoleService.assignRole("manager1", 1L, 2L, request))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));

        verify(teamMemberStaffingRoleRepository, never()).save(any());
    }

    @Test
    void assignRoleRejectsStaffingRoleFromAnotherTeam() {
        Team team = team(1L, "Operations");
        Team otherTeam = team(2L, "Support");
        TeamMember teamMember = teamMember(team, employee());
        StaffingRole staffingRole = staffingRole(otherTeam, 20L, "Shift Supervisor");
        AssignStaffingRoleRequest request = new AssignStaffingRoleRequest(20L);

        when(teamRepository.findById(1L)).thenReturn(Optional.of(team));
        when(teamManagerRepository.existsByManager_UsernameAndTeam_Id("manager1", 1L)).thenReturn(true);
        when(teamMemberRepository.findByUser_IdAndTeam_IdAndActiveTrue(2L, 1L)).thenReturn(Optional.of(teamMember));
        when(staffingRoleRepository.findById(20L)).thenReturn(Optional.of(staffingRole));

        assertThatThrownBy(() -> teamMemberStaffingRoleService.assignRole("manager1", 1L, 2L, request))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));

        verify(teamMemberStaffingRoleRepository, never()).save(any());
    }

    @Test
    void assignRoleRejectsDuplicateRoleForTeamMember() {
        Team team = team(1L, "Operations");
        TeamMember teamMember = teamMember(team, employee());
        StaffingRole staffingRole = staffingRole(team, 20L, "Shift Supervisor");
        AssignStaffingRoleRequest request = new AssignStaffingRoleRequest(20L);

        when(teamRepository.findById(1L)).thenReturn(Optional.of(team));
        when(teamManagerRepository.existsByManager_UsernameAndTeam_Id("manager1", 1L)).thenReturn(true);
        when(teamMemberRepository.findByUser_IdAndTeam_IdAndActiveTrue(2L, 1L)).thenReturn(Optional.of(teamMember));
        when(staffingRoleRepository.findById(20L)).thenReturn(Optional.of(staffingRole));
        when(teamMemberStaffingRoleRepository.existsByTeamMember_IdAndStaffingRole_Id(10L, 20L)).thenReturn(true);

        assertThatThrownBy(() -> teamMemberStaffingRoleService.assignRole("manager1", 1L, 2L, request))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));

        verify(teamMemberStaffingRoleRepository, never()).save(any());
    }

    @Test
    void listEmployeeRolesReturnsRolesForActiveTeamMemberInManagedTeam() {
        Team team = team(1L, "Operations");
        TeamMember teamMember = teamMember(team, employee());
        TeamMemberStaffingRole roleAssignment = teamMemberStaffingRole(
                30L,
                teamMember,
                staffingRole(team, 20L, "Shift Supervisor")
        );

        when(teamRepository.findById(1L)).thenReturn(Optional.of(team));
        when(teamManagerRepository.existsByManager_UsernameAndTeam_Id("manager1", 1L)).thenReturn(true);
        when(teamMemberRepository.findByUser_IdAndTeam_IdAndActiveTrue(2L, 1L)).thenReturn(Optional.of(teamMember));
        when(teamMemberStaffingRoleRepository.findByTeamMember_Id(10L)).thenReturn(List.of(roleAssignment));

        List<TeamMemberStaffingRoleResponse> responses = teamMemberStaffingRoleService.listEmployeeRoles(
                "manager1",
                1L,
                2L
        );

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).id()).isEqualTo(30L);
        assertThat(responses.get(0).employeeId()).isEqualTo(2L);
        assertThat(responses.get(0).staffingRoleId()).isEqualTo(20L);
        assertThat(responses.get(0).staffingRoleName()).isEqualTo("Shift Supervisor");
    }

    @Test
    void listEmployeeRolesRejectsUnmanagedTeam() {
        when(teamRepository.findById(1L)).thenReturn(Optional.of(team(1L, "Operations")));
        when(teamManagerRepository.existsByManager_UsernameAndTeam_Id("employee1", 1L)).thenReturn(false);

        assertThatThrownBy(() -> teamMemberStaffingRoleService.listEmployeeRoles("employee1", 1L, 2L))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));

        verify(teamMemberStaffingRoleRepository, never()).findByTeamMember_Id(10L);
    }

    private TeamMemberStaffingRole teamMemberStaffingRole(Long id, TeamMember teamMember, StaffingRole staffingRole) {
        TeamMemberStaffingRole roleAssignment = new TeamMemberStaffingRole(
                teamMember,
                staffingRole,
                Instant.parse("2026-07-18T08:00:00Z")
        );
        ReflectionTestUtils.setField(roleAssignment, "id", id);
        return roleAssignment;
    }

    private StaffingRole staffingRole(Team team, Long id, String name) {
        StaffingRole staffingRole = new StaffingRole(team, name, null);
        ReflectionTestUtils.setField(staffingRole, "id", id);
        return staffingRole;
    }

    private TeamMember teamMember(Team team, User employee) {
        TeamMember teamMember = new TeamMember(
                employee,
                team,
                Instant.parse("2026-07-01T08:00:00Z"),
                true
        );
        ReflectionTestUtils.setField(teamMember, "id", 10L);
        return teamMember;
    }

    private Team team(Long id, String name) {
        Team team = new Team(name, SwapApprovalPolicy.MANAGER, 8, "Asia/Jerusalem");
        ReflectionTestUtils.setField(team, "id", id);
        return team;
    }

    private User employee() {
        User employee = new User(
                "employee1",
                "password-hash",
                "Demo Employee",
                "employee1@example.com",
                ApplicationRole.EMPLOYEE
        );
        ReflectionTestUtils.setField(employee, "id", 2L);
        return employee;
    }
}
