package com.hilimor.shiftmanagement.staffing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import com.hilimor.shiftmanagement.team.SwapApprovalPolicy;
import com.hilimor.shiftmanagement.team.Team;
import com.hilimor.shiftmanagement.team.TeamManagerRepository;
import com.hilimor.shiftmanagement.team.TeamRepository;

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
class StaffingRoleServiceTest {

    @Mock
    private StaffingRoleRepository staffingRoleRepository;

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private TeamManagerRepository teamManagerRepository;

    @InjectMocks
    private StaffingRoleService staffingRoleService;

    @Test
    void createRoleSavesRoleForManagedTeam() {
        Team team = team();
        CreateStaffingRoleRequest request = new CreateStaffingRoleRequest(
                "  Shift Supervisor  ",
                "Can supervise a shift"
        );

        when(teamRepository.findById(1L)).thenReturn(Optional.of(team));
        when(teamManagerRepository.existsByManager_UsernameAndTeam_Id("manager1", 1L)).thenReturn(true);
        when(staffingRoleRepository.existsByTeam_IdAndName(1L, "Shift Supervisor")).thenReturn(false);
        when(staffingRoleRepository.save(any(StaffingRole.class))).thenAnswer(invocation -> {
            StaffingRole staffingRole = invocation.getArgument(0);
            ReflectionTestUtils.setField(staffingRole, "id", 20L);
            return staffingRole;
        });

        StaffingRoleResponse response = staffingRoleService.createRole("manager1", 1L, request);

        assertThat(response.id()).isEqualTo(20L);
        assertThat(response.teamId()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Shift Supervisor");
        assertThat(response.description()).isEqualTo("Can supervise a shift");

        ArgumentCaptor<StaffingRole> captor = ArgumentCaptor.forClass(StaffingRole.class);
        verify(staffingRoleRepository).save(captor.capture());
        assertThat(captor.getValue().getTeam()).isSameAs(team);
        assertThat(captor.getValue().getName()).isEqualTo("Shift Supervisor");
    }

    @Test
    void createRoleRejectsUnmanagedTeam() {
        CreateStaffingRoleRequest request = new CreateStaffingRoleRequest("Shift Supervisor", null);

        when(teamRepository.findById(1L)).thenReturn(Optional.of(team()));
        when(teamManagerRepository.existsByManager_UsernameAndTeam_Id("employee1", 1L)).thenReturn(false);

        assertThatThrownBy(() -> staffingRoleService.createRole("employee1", 1L, request))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));

        verify(staffingRoleRepository, never()).save(any());
    }

    @Test
    void createRoleRejectsMissingTeam() {
        CreateStaffingRoleRequest request = new CreateStaffingRoleRequest("Shift Supervisor", null);

        when(teamRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> staffingRoleService.createRole("manager1", 99L, request))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));

        verify(staffingRoleRepository, never()).save(any());
    }

    @Test
    void createRoleRejectsDuplicateNameInSameTeam() {
        CreateStaffingRoleRequest request = new CreateStaffingRoleRequest("Shift Supervisor", null);

        when(teamRepository.findById(1L)).thenReturn(Optional.of(team()));
        when(teamManagerRepository.existsByManager_UsernameAndTeam_Id("manager1", 1L)).thenReturn(true);
        when(staffingRoleRepository.existsByTeam_IdAndName(1L, "Shift Supervisor")).thenReturn(true);

        assertThatThrownBy(() -> staffingRoleService.createRole("manager1", 1L, request))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));

        verify(staffingRoleRepository, never()).save(any());
    }

    @Test
    void listRolesReturnsRolesForManagedTeam() {
        Team team = team();
        StaffingRole supervisor = staffingRole(team, 20L, "Shift Supervisor");
        StaffingRole entranceGuard = staffingRole(team, 21L, "Entrance Guard");

        when(teamRepository.findById(1L)).thenReturn(Optional.of(team));
        when(teamManagerRepository.existsByManager_UsernameAndTeam_Id("manager1", 1L)).thenReturn(true);
        when(staffingRoleRepository.findByTeam_IdOrderByName(1L)).thenReturn(List.of(entranceGuard, supervisor));

        List<StaffingRoleResponse> responses = staffingRoleService.listRoles("manager1", 1L);

        assertThat(responses).extracting(StaffingRoleResponse::id).containsExactly(21L, 20L);
        assertThat(responses).extracting(StaffingRoleResponse::name).containsExactly("Entrance Guard", "Shift Supervisor");
    }

    @Test
    void listRolesRejectsUnmanagedTeam() {
        when(teamRepository.findById(1L)).thenReturn(Optional.of(team()));
        when(teamManagerRepository.existsByManager_UsernameAndTeam_Id("employee1", 1L)).thenReturn(false);

        assertThatThrownBy(() -> staffingRoleService.listRoles("employee1", 1L))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));

        verify(staffingRoleRepository, never()).findByTeam_IdOrderByName(1L);
    }

    private StaffingRole staffingRole(Team team, Long id, String name) {
        StaffingRole staffingRole = new StaffingRole(team, name, null);
        ReflectionTestUtils.setField(staffingRole, "id", id);
        return staffingRole;
    }

    private Team team() {
        Team team = new Team("Operations", SwapApprovalPolicy.MANAGER, 8, "Asia/Jerusalem");
        ReflectionTestUtils.setField(team, "id", 1L);
        return team;
    }
}
