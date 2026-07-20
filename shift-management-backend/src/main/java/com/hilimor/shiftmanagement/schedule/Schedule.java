package com.hilimor.shiftmanagement.schedule;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

import com.hilimor.shiftmanagement.team.Team;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "schedules")
public class Schedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ScheduleStatus status;

    @Column(name = "publication_number", nullable = false)
    private int publicationNumber;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Version
    @Column(nullable = false)
    private Long version;

    protected Schedule() {
    }

    public Schedule(Team team, LocalDate startDate, LocalDate endDate) {
        Objects.requireNonNull(team, "team must not be null");
        Objects.requireNonNull(startDate, "startDate must not be null");
        Objects.requireNonNull(endDate, "endDate must not be null");

        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("Schedule end date must not be before start date");
        }

        this.team = team;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = ScheduleStatus.DRAFT;
        this.publicationNumber = 0;
    }

    public void publish(Instant publishedAt) {
        Objects.requireNonNull(publishedAt, "publishedAt must not be null");

        if (status != ScheduleStatus.DRAFT) {
            throw new IllegalStateException("Only draft schedules can be published");
        }

        status = ScheduleStatus.PUBLISHED;
        publicationNumber++;
        this.publishedAt = publishedAt;
    }

    public Long getId() {
        return id;
    }

    public Team getTeam() {
        return team;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public ScheduleStatus getStatus() {
        return status;
    }

    public int getPublicationNumber() {
        return publicationNumber;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public Long getVersion() {
        return version;
    }
}
