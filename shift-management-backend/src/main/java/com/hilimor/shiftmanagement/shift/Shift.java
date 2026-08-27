package com.hilimor.shiftmanagement.shift;

import java.time.Instant;
import java.util.Objects;

import com.hilimor.shiftmanagement.schedule.Schedule;
import com.hilimor.shiftmanagement.staffing.StaffingRole;
import com.hilimor.shiftmanagement.template.TemplateSlot;
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
@Table(name = "shifts")
public class Shift {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "schedule_id", nullable = false)
    private Schedule schedule;

    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @Column(name = "end_time", nullable = false)
    private Instant endTime;

    @Column(length = 500)
    private String description;

    @Column(name = "required_workers", nullable = false)
    private int requiredWorkers;

    @Column(name = "min_rest_hours", nullable = false)
    private int minRestHours;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "required_staffing_role_id")
    private StaffingRole requiredStaffingRole;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_slot_id")
    private TemplateSlot templateSlot;

    @Version
    @Column(nullable = false)
    private Long version;

    protected Shift() {
    }

    public Shift(
            Schedule schedule,
            Instant startTime,
            Instant endTime,
            String description,
            int requiredWorkers,
            int minRestHours
    ) {
        this(schedule, startTime, endTime, description, requiredWorkers, minRestHours, null);
    }

    public Shift(
            Schedule schedule,
            Instant startTime,
            Instant endTime,
            String description,
            int requiredWorkers,
            int minRestHours,
            StaffingRole requiredStaffingRole
    ) {
        this(schedule, startTime, endTime, description, requiredWorkers, minRestHours, requiredStaffingRole, null);
    }

    public Shift(
            Schedule schedule,
            Instant startTime,
            Instant endTime,
            String description,
            int requiredWorkers,
            int minRestHours,
            StaffingRole requiredStaffingRole,
            TemplateSlot templateSlot
    ) {
        Objects.requireNonNull(schedule, "schedule must not be null");
        if (templateSlot != null && !sameTeam(schedule.getTeam(), templateSlot.getShiftTemplate().getTeam())) {
            throw new IllegalArgumentException("Shift template slot must belong to the schedule team");
        }

        this.schedule = schedule;
        updateDetails(startTime, endTime, description, requiredWorkers, minRestHours, requiredStaffingRole);
        this.templateSlot = templateSlot;
    }

    public void updateDetails(
            Instant startTime,
            Instant endTime,
            String description,
            int requiredWorkers,
            int minRestHours
    ) {
        updateDetails(startTime, endTime, description, requiredWorkers, minRestHours, null);
    }

    public void updateDetails(
            Instant startTime,
            Instant endTime,
            String description,
            int requiredWorkers,
            int minRestHours,
            StaffingRole requiredStaffingRole
    ) {
        Objects.requireNonNull(startTime, "startTime must not be null");
        Objects.requireNonNull(endTime, "endTime must not be null");

        if (!endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("Shift end time must be after start time");
        }
        if (requiredWorkers <= 0) {
            throw new IllegalArgumentException("Shift required workers must be positive");
        }
        if (minRestHours < 0) {
            throw new IllegalArgumentException("Shift minimum rest hours must not be negative");
        }

        this.startTime = startTime;
        this.endTime = endTime;
        this.description = description;
        this.requiredWorkers = requiredWorkers;
        this.minRestHours = minRestHours;
        this.requiredStaffingRole = requiredStaffingRole;
    }

    public Long getId() {
        return id;
    }

    public Schedule getSchedule() {
        return schedule;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public Instant getEndTime() {
        return endTime;
    }

    public String getDescription() {
        return description;
    }

    public int getRequiredWorkers() {
        return requiredWorkers;
    }

    public int getMinRestHours() {
        return minRestHours;
    }

    public StaffingRole getRequiredStaffingRole() {
        return requiredStaffingRole;
    }

    public TemplateSlot getTemplateSlot() {
        return templateSlot;
    }

    public Long getVersion() {
        return version;
    }

    private boolean sameTeam(Team scheduleTeam, Team templateTeam) {
        if (scheduleTeam == templateTeam) {
            return true;
        }

        Long scheduleTeamId = scheduleTeam.getId();
        Long templateTeamId = templateTeam.getId();

        return scheduleTeamId != null && scheduleTeamId.equals(templateTeamId);
    }
}
