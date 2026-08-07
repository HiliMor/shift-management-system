package com.hilimor.shiftmanagement.assignment;

import java.time.Instant;
import java.util.Objects;

import com.hilimor.shiftmanagement.shift.Shift;
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
import jakarta.persistence.Version;

@Entity
@Table(
        name = "assignments",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_assignments_shift_employee",
                columnNames = {"shift_id", "employee_id"}
        )
)
public class Assignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shift_id", nullable = false)
    private Shift shift;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private User employee;

    @Column(name = "assigned_at", nullable = false)
    private Instant assignedAt;

    @Version
    @Column(nullable = false)
    private Long version;

    protected Assignment() {
    }

    public Assignment(Shift shift, User employee, Instant assignedAt) {
        this.shift = Objects.requireNonNull(shift, "shift must not be null");
        this.employee = Objects.requireNonNull(employee, "employee must not be null");
        this.assignedAt = Objects.requireNonNull(assignedAt, "assignedAt must not be null");
    }

    public void transferTo(User employee, Instant assignedAt) {
        this.employee = Objects.requireNonNull(employee, "employee must not be null");
        this.assignedAt = Objects.requireNonNull(assignedAt, "assignedAt must not be null");
    }

    public Long getId() {
        return id;
    }

    public Shift getShift() {
        return shift;
    }

    public User getEmployee() {
        return employee;
    }

    public Instant getAssignedAt() {
        return assignedAt;
    }

    public Long getVersion() {
        return version;
    }
}
