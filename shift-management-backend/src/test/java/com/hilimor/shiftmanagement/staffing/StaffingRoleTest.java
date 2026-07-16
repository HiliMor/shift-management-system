package com.hilimor.shiftmanagement.staffing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import com.hilimor.shiftmanagement.team.SwapApprovalPolicy;
import com.hilimor.shiftmanagement.team.Team;

import org.junit.jupiter.api.Test;

class StaffingRoleTest {

    @Test
    void newStaffingRoleStoresTeamNameAndDescription() {
        Team team = team();

        StaffingRole role = new StaffingRole(team, "Shift Supervisor", "Can supervise a shift");

        assertThat(role.getTeam()).isSameAs(team);
        assertThat(role.getName()).isEqualTo("Shift Supervisor");
        assertThat(role.getDescription()).isEqualTo("Can supervise a shift");
    }

    @Test
    void roleNameIsTrimmed() {
        StaffingRole role = new StaffingRole(team(), "  Entrance Guard  ", null);

        assertThat(role.getName()).isEqualTo("Entrance Guard");
        assertThat(role.getDescription()).isNull();
    }

    @Test
    void roleNameMustNotBeBlank() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new StaffingRole(team(), "   ", null));
    }

    @Test
    void teamMustNotBeNull() {
        assertThatNullPointerException()
                .isThrownBy(() -> new StaffingRole(null, "Shift Supervisor", null));
    }

    private Team team() {
        return new Team("Operations", SwapApprovalPolicy.MANAGER, 8, "Asia/Jerusalem");
    }
}
