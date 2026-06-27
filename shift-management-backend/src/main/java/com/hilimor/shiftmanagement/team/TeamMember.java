package com.hilimor.shiftmanagement.team;

import java.time.Instant;

import com.hilimor.shiftmanagement.user.User;

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

@Entity
@Table(
        name = "team_members",
        uniqueConstraints = @UniqueConstraint(name = "uk_team_members_user_team", columnNames = {"user_id", "team_id"})
)
public class TeamMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt;

    @Column(nullable = false)
    private boolean active;

    protected TeamMember() {
    }

    public TeamMember(User user, Team team, Instant joinedAt, boolean active) {
        this.user = user;
        this.team = team;
        this.joinedAt = joinedAt;
        this.active = active;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public Team getTeam() {
        return team;
    }

    public Instant getJoinedAt() {
        return joinedAt;
    }

    public boolean isActive() {
        return active;
    }
}
