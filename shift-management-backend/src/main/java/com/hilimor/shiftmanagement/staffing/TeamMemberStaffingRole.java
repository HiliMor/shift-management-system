package com.hilimor.shiftmanagement.staffing;

import java.time.Instant;
import java.util.Objects;

import com.hilimor.shiftmanagement.team.Team;
import com.hilimor.shiftmanagement.team.TeamMember;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

@Entity
@Table(
        name = "team_member_staffing_roles",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_team_member_staffing_roles_member_role",
                columnNames = {"team_member_id", "staffing_role_id"}
        )
)
public class TeamMemberStaffingRole {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_member_id", nullable = false)
    private TeamMember teamMember;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "staffing_role_id", nullable = false)
    private StaffingRole staffingRole;

    @Column(name = "assigned_at", nullable = false)
    private Instant assignedAt;

    @Version
    @Column(nullable = false)
    private Long version;

    protected TeamMemberStaffingRole() {
    }

    public TeamMemberStaffingRole(TeamMember teamMember, StaffingRole staffingRole, Instant assignedAt) {
        this.teamMember = Objects.requireNonNull(teamMember, "teamMember must not be null");
        this.staffingRole = Objects.requireNonNull(staffingRole, "staffingRole must not be null");
        this.assignedAt = Objects.requireNonNull(assignedAt, "assignedAt must not be null");

        if (!sameTeam(teamMember.getTeam(), staffingRole.getTeam())) {
            throw new IllegalArgumentException("Team member and staffing role must belong to the same team");
        }
    }

    public Long getId() {
        return id;
    }

    public TeamMember getTeamMember() {
        return teamMember;
    }

    public StaffingRole getStaffingRole() {
        return staffingRole;
    }

    public Instant getAssignedAt() {
        return assignedAt;
    }

    public Long getVersion() {
        return version;
    }

    private boolean sameTeam(Team memberTeam, Team roleTeam) {
        if (memberTeam == roleTeam) {
            return true;
        }

        Long memberTeamId = memberTeam.getId();
        Long roleTeamId = roleTeam.getId();

        return memberTeamId != null && memberTeamId.equals(roleTeamId);
    }
}
