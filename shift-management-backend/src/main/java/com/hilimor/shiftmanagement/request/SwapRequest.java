package com.hilimor.shiftmanagement.request;

import java.time.Instant;
import java.util.Objects;

import com.hilimor.shiftmanagement.assignment.Assignment;
import com.hilimor.shiftmanagement.team.SwapApprovalPolicy;
import com.hilimor.shiftmanagement.user.User;

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
@Table(name = "swap_requests")
public class SwapRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SwapRequestType type;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_assignment_id", nullable = false)
    private Assignment sourceAssignment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "target_employee_id", nullable = false)
    private User targetEmployee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_assignment_id")
    private Assignment targetAssignment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SwapRequestStatus status;

    @Column(name = "employee_approved_at")
    private Instant employeeApprovedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_approved_by")
    private User managerApprovedBy;

    @Column(name = "manager_approved_at")
    private Instant managerApprovedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private Long version;

    protected SwapRequest() {
    }

    private SwapRequest(
            SwapRequestType type,
            User requester,
            Assignment sourceAssignment,
            User targetEmployee,
            Assignment targetAssignment,
            SwapRequestStatus status,
            Instant createdAt
    ) {
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.requester = Objects.requireNonNull(requester, "requester must not be null");
        this.sourceAssignment = Objects.requireNonNull(sourceAssignment, "sourceAssignment must not be null");
        this.targetEmployee = Objects.requireNonNull(targetEmployee, "targetEmployee must not be null");
        this.targetAssignment = targetAssignment;
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.updatedAt = createdAt;

        if (type == SwapRequestType.TRANSFER && targetAssignment != null) {
            throw new IllegalArgumentException("Transfer request must not have a target assignment");
        }
        if (type == SwapRequestType.SWAP && targetAssignment == null) {
            throw new IllegalArgumentException("Swap request must have a target assignment");
        }
    }

    public static SwapRequest createTransfer(
            User requester,
            Assignment sourceAssignment,
            User targetEmployee,
            Instant createdAt
    ) {
        return new SwapRequest(
                SwapRequestType.TRANSFER,
                requester,
                sourceAssignment,
                targetEmployee,
                null,
                SwapRequestStatus.PENDING_EMPLOYEE,
                createdAt
        );
    }

    public void approveByTargetEmployee(Instant approvedAt, SwapApprovalPolicy approvalPolicy) {
        Objects.requireNonNull(approvedAt, "approvedAt must not be null");
        Objects.requireNonNull(approvalPolicy, "approvalPolicy must not be null");

        if (status != SwapRequestStatus.PENDING_EMPLOYEE) {
            throw new IllegalStateException("Only requests pending employee approval can be approved by the target employee");
        }

        employeeApprovedAt = approvedAt;
        updatedAt = approvedAt;
        status = approvalPolicy == SwapApprovalPolicy.MANAGER
                ? SwapRequestStatus.PENDING_MANAGER
                : SwapRequestStatus.APPROVED;
    }

    public void invalidate(Instant invalidatedAt) {
        Objects.requireNonNull(invalidatedAt, "invalidatedAt must not be null");

        status = SwapRequestStatus.INVALIDATED;
        updatedAt = invalidatedAt;
    }

    public Long getId() {
        return id;
    }

    public SwapRequestType getType() {
        return type;
    }

    public User getRequester() {
        return requester;
    }

    public Assignment getSourceAssignment() {
        return sourceAssignment;
    }

    public User getTargetEmployee() {
        return targetEmployee;
    }

    public Assignment getTargetAssignment() {
        return targetAssignment;
    }

    public SwapRequestStatus getStatus() {
        return status;
    }

    public Instant getEmployeeApprovedAt() {
        return employeeApprovedAt;
    }

    public User getManagerApprovedBy() {
        return managerApprovedBy;
    }

    public Instant getManagerApprovedAt() {
        return managerApprovedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Long getVersion() {
        return version;
    }
}
