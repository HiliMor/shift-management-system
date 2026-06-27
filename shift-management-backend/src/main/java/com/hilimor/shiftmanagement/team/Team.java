package com.hilimor.shiftmanagement.team;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "teams")
public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "swap_approval_policy", nullable = false, length = 30)
    private SwapApprovalPolicy swapApprovalPolicy;

    @Column(name = "default_min_rest_hours", nullable = false)
    private int defaultMinRestHours;

    @Column(name = "time_zone", nullable = false, length = 100)
    private String timeZone;

    protected Team() {
    }

    public Team(String name, SwapApprovalPolicy swapApprovalPolicy, int defaultMinRestHours, String timeZone) {
        this.name = name;
        this.swapApprovalPolicy = swapApprovalPolicy;
        this.defaultMinRestHours = defaultMinRestHours;
        this.timeZone = timeZone;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public SwapApprovalPolicy getSwapApprovalPolicy() {
        return swapApprovalPolicy;
    }

    public int getDefaultMinRestHours() {
        return defaultMinRestHours;
    }

    public String getTimeZone() {
        return timeZone;
    }
}
