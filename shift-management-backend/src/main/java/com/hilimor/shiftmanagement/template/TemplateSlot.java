package com.hilimor.shiftmanagement.template;

import java.time.LocalTime;
import java.util.Objects;

import com.hilimor.shiftmanagement.staffing.StaffingRole;
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
import jakarta.persistence.Version;

@Entity
@Table(name = "template_slots")
public class TemplateSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shift_template_id", nullable = false)
    private ShiftTemplate shiftTemplate;

    @Column(name = "day_offset", nullable = false)
    private int dayOffset;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes;

    @Column(length = 500)
    private String description;

    @Column(name = "required_workers", nullable = false)
    private int requiredWorkers;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "required_staffing_role_id")
    private StaffingRole requiredStaffingRole;

    @Version
    @Column(nullable = false)
    private Long version;

    protected TemplateSlot() {
    }

    public TemplateSlot(
            ShiftTemplate shiftTemplate,
            int dayOffset,
            LocalTime startTime,
            int durationMinutes,
            String description,
            int requiredWorkers
    ) {
        this(shiftTemplate, dayOffset, startTime, durationMinutes, description, requiredWorkers, null);
    }

    public TemplateSlot(
            ShiftTemplate shiftTemplate,
            int dayOffset,
            LocalTime startTime,
            int durationMinutes,
            String description,
            int requiredWorkers,
            StaffingRole requiredStaffingRole
    ) {
        this.shiftTemplate = Objects.requireNonNull(shiftTemplate, "shiftTemplate must not be null");
        updateDetails(dayOffset, startTime, durationMinutes, description, requiredWorkers, requiredStaffingRole);
    }

    public void updateDetails(
            int dayOffset,
            LocalTime startTime,
            int durationMinutes,
            String description,
            int requiredWorkers,
            StaffingRole requiredStaffingRole
    ) {
        Objects.requireNonNull(startTime, "startTime must not be null");

        if (dayOffset < 0 || dayOffset >= shiftTemplate.getCycleDays()) {
            throw new IllegalArgumentException("Template slot day offset must be inside the template cycle");
        }
        if (durationMinutes <= 0) {
            throw new IllegalArgumentException("Template slot duration minutes must be positive");
        }
        if (requiredWorkers <= 0) {
            throw new IllegalArgumentException("Template slot required workers must be positive");
        }
        if (requiredStaffingRole != null && !sameTeam(shiftTemplate.getTeam(), requiredStaffingRole.getTeam())) {
            throw new IllegalArgumentException("Template slot staffing role must belong to the template team");
        }

        this.dayOffset = dayOffset;
        this.startTime = startTime;
        this.durationMinutes = durationMinutes;
        this.description = description;
        this.requiredWorkers = requiredWorkers;
        this.requiredStaffingRole = requiredStaffingRole;
    }

    public Long getId() {
        return id;
    }

    public ShiftTemplate getShiftTemplate() {
        return shiftTemplate;
    }

    public int getDayOffset() {
        return dayOffset;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public String getDescription() {
        return description;
    }

    public int getRequiredWorkers() {
        return requiredWorkers;
    }

    public StaffingRole getRequiredStaffingRole() {
        return requiredStaffingRole;
    }

    public Long getVersion() {
        return version;
    }

    private boolean sameTeam(Team templateTeam, Team roleTeam) {
        if (templateTeam == roleTeam) {
            return true;
        }

        Long templateTeamId = templateTeam.getId();
        Long roleTeamId = roleTeam.getId();

        return templateTeamId != null && templateTeamId.equals(roleTeamId);
    }
}
