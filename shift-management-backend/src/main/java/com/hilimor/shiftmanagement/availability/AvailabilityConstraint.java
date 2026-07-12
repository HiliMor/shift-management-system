package com.hilimor.shiftmanagement.availability;

import java.time.Instant;
import java.util.Objects;

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
import jakarta.persistence.Version;

@Entity
@Table(name = "availability_constraints")
public class AvailabilityConstraint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private User employee;

    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @Column(name = "end_time", nullable = false)
    private Instant endTime;

    @Column(length = 500)
    private String reason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Version
    @Column(nullable = false)
    private Long version;

    protected AvailabilityConstraint() {
    }

    public AvailabilityConstraint(
            User employee,
            Instant startTime,
            Instant endTime,
            String reason,
            Instant createdAt
    ) {
        this.employee = Objects.requireNonNull(employee, "employee must not be null");
        this.startTime = Objects.requireNonNull(startTime, "startTime must not be null");
        this.endTime = Objects.requireNonNull(endTime, "endTime must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.reason = reason;

        if (!endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("Availability constraint end time must be after start time");
        }
    }

    public Long getId() {
        return id;
    }

    public User getEmployee() {
        return employee;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public Instant getEndTime() {
        return endTime;
    }

    public String getReason() {
        return reason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Long getVersion() {
        return version;
    }
}
