package com.hilimor.shiftmanagement.team;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.hilimor.shiftmanagement.user.ApplicationRole;
import com.hilimor.shiftmanagement.user.User;

@ExtendWith(MockitoExtension.class)
class TeamServiceTest {

    @Mock
    private TeamManagerRepository teamManagerRepository;

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

    private Team team(Long id, String name) {
        Team team = new Team(name, SwapApprovalPolicy.MANAGER, 8, "Asia/Jerusalem");
        ReflectionTestUtils.setField(team, "id", id);
        return team;
    }
}
