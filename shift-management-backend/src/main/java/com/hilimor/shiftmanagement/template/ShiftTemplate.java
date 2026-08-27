package com.hilimor.shiftmanagement.template;

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
        name = "shift_templates",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_shift_templates_team_name",
                columnNames = {"team_id", "name"}
        )
)
public class ShiftTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(name = "cycle_days", nullable = false)
    private int cycleDays;

    @Column(name = "default_min_rest_hours", nullable = false)
    private int defaultMinRestHours;

    @Column(nullable = false)
    private boolean active;

    @Version
    @Column(nullable = false)
    private Long version;

    protected ShiftTemplate() {
    }

    public ShiftTemplate(
            Team team,
            String name,
            String description,
            int cycleDays,
            int defaultMinRestHours
    ) {
        this.team = Objects.requireNonNull(team, "team must not be null");
        updateDetails(name, description, cycleDays, defaultMinRestHours);
        this.active = false;
    }

    public void updateDetails(String name, String description, int cycleDays, int defaultMinRestHours) {
        if (cycleDays <= 0) {
            throw new IllegalArgumentException("Template cycle days must be positive");
        }
        if (defaultMinRestHours < 0) {
            throw new IllegalArgumentException("Template default minimum rest hours must not be negative");
        }

        this.name = normalizeName(name);
        this.description = description;
        this.cycleDays = cycleDays;
        this.defaultMinRestHours = defaultMinRestHours;
    }

    public void activate() {
        active = true;
    }

    public void deactivate() {
        active = false;
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

    public int getCycleDays() {
        return cycleDays;
    }

    public int getDefaultMinRestHours() {
        return defaultMinRestHours;
    }

    public boolean isActive() {
        return active;
    }

    public Long getVersion() {
        return version;
    }

    private String normalizeName(String name) {
        String normalizedName = Objects.requireNonNull(name, "name must not be null").trim();

        if (normalizedName.isBlank()) {
            throw new IllegalArgumentException("Template name must not be blank");
        }

        return normalizedName;
    }
}
