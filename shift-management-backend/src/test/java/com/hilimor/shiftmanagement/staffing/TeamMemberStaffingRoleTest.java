package com.hilimor.shiftmanagement.staffing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.time.Instant;

import com.hilimor.shiftmanagement.team.SwapApprovalPolicy;
import com.hilimor.shiftmanagement.team.Team;
import com.hilimor.shiftmanagement.team.TeamMember;
import com.hilimor.shiftmanagement.user.ApplicationRole;
import com.hilimor.shiftmanagement.user.User;

import org.junit.jupiter.api.Test;

class TeamMemberStaffingRoleTest {

    @Test
    void newTeamMemberStaffingRoleStoresMemberRoleAndAssignedAt() {
        Team team = team("Operations");
        TeamMember teamMember = teamMember(team);
        StaffingRole staffingRole = new StaffingRole(team, "Shift Supervisor", null);
        Instant assignedAt = Instant.parse("2026-07-16T19:00:00Z");

        TeamMemberStaffingRole memberRole = new TeamMemberStaffingRole(teamMember, staffingRole, assignedAt);

        assertThat(memberRole.getTeamMember()).isSameAs(teamMember);
        assertThat(memberRole.getStaffingRole()).isSameAs(staffingRole);
        assertThat(memberRole.getAssignedAt()).isEqualTo(assignedAt);
    }

    @Test
    void teamMemberAndStaffingRoleMustBelongToSameTeam() {
        TeamMember teamMember = teamMember(team("Operations"));
        StaffingRole staffingRole = new StaffingRole(team("Support"), "Shift Supervisor", null);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> new TeamMemberStaffingRole(
                        teamMember,
                        staffingRole,
                        Instant.parse("2026-07-16T19:00:00Z")));
    }

    @Test
    void teamMemberMustNotBeNull() {
        StaffingRole staffingRole = new StaffingRole(team("Operations"), "Shift Supervisor", null);

        assertThatNullPointerException()
                .isThrownBy(() -> new TeamMemberStaffingRole(
                        null,
                        staffingRole,
                        Instant.parse("2026-07-16T19:00:00Z")));
    }

    @Test
    void staffingRoleMustNotBeNull() {
        TeamMember teamMember = teamMember(team("Operations"));

        assertThatNullPointerException()
                .isThrownBy(() -> new TeamMemberStaffingRole(
                        teamMember,
                        null,
                        Instant.parse("2026-07-16T19:00:00Z")));
    }

    @Test
    void assignedAtMustNotBeNull() {
        Team team = team("Operations");
        TeamMember teamMember = teamMember(team);
        StaffingRole staffingRole = new StaffingRole(team, "Shift Supervisor", null);

        assertThatNullPointerException()
                .isThrownBy(() -> new TeamMemberStaffingRole(teamMember, staffingRole, null));
    }

    private TeamMember teamMember(Team team) {
        return new TeamMember(
                employee(),
                team,
                Instant.parse("2026-07-01T08:00:00Z"),
                true
        );
    }

    private Team team(String name) {
        return new Team(name, SwapApprovalPolicy.MANAGER, 8, "Asia/Jerusalem");
    }

    private User employee() {
        return new User(
                "employee1",
                "password-hash",
                "Demo Employee",
                "employee1@example.com",
                ApplicationRole.EMPLOYEE
        );
    }
}
