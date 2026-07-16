package com.hilimor.shiftmanagement.staffing;

import java.util.Objects;

import com.hilimor.shiftmanagement.team.Team;

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
        name = "staffing_roles",
        uniqueConstraints = @UniqueConstraint(name = "uk_staffing_roles_team_name", columnNames = {"team_id", "name"})
)
public class StaffingRole {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Version
    @Column(nullable = false)
    private Long version;

    protected StaffingRole() {
    }

    public StaffingRole(Team team, String name, String description) {
        this.team = Objects.requireNonNull(team, "team must not be null");
        this.name = normalizeName(name);
        this.description = description;
    }

    public Long getId() {
        return id;
    }

    public Team getTeam() {
        return team;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Long getVersion() {
        return version;
    }

    private String normalizeName(String name) {
        String normalizedName = Objects.requireNonNull(name, "name must not be null").trim();

        if (normalizedName.isBlank()) {
            throw new IllegalArgumentException("Staffing role name must not be blank");
        }

        return normalizedName;
    }
}
