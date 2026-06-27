package com.hilimor.shiftmanagement.team;

import com.hilimor.shiftmanagement.user.User;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "team_managers",
        uniqueConstraints = @UniqueConstraint(name = "uk_team_managers_manager_team", columnNames = {"manager_id", "team_id"})
)
public class TeamManager {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "manager_id", nullable = false)
    private User manager;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    protected TeamManager() {
    }

    public TeamManager(User manager, Team team) {
        this.manager = manager;
        this.team = team;
    }

    public Long getId() {
        return id;
    }

    public User getManager() {
        return manager;
    }

    public Team getTeam() {
        return team;
    }
}
