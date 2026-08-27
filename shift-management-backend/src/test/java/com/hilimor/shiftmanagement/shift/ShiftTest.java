package com.hilimor.shiftmanagement.shift;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;

import com.hilimor.shiftmanagement.schedule.Schedule;
import com.hilimor.shiftmanagement.staffing.StaffingRole;
import com.hilimor.shiftmanagement.template.ShiftTemplate;
import com.hilimor.shiftmanagement.template.TemplateSlot;
import com.hilimor.shiftmanagement.team.SwapApprovalPolicy;
import com.hilimor.shiftmanagement.team.Team;

class ShiftTest {

    @Test
    void newShiftStoresScheduleTimesAndRequirements() {
        Schedule schedule = schedule();
        Instant startTime = Instant.parse("2026-07-06T06:00:00Z");
        Instant endTime = Instant.parse("2026-07-06T14:00:00Z");

        Shift shift = new Shift(schedule, startTime, endTime, "Morning shift", 2, 8);

        assertThat(shift.getSchedule()).isSameAs(schedule);
        assertThat(shift.getStartTime()).isEqualTo(startTime);
        assertThat(shift.getEndTime()).isEqualTo(endTime);
        assertThat(shift.getDescription()).isEqualTo("Morning shift");
        assertThat(shift.getRequiredWorkers()).isEqualTo(2);
        assertThat(shift.getMinRestHours()).isEqualTo(8);
        assertThat(shift.getRequiredStaffingRole()).isNull();
        assertThat(shift.getTemplateSlot()).isNull();
    }

    @Test
    void newShiftCanStoreRequiredStaffingRole() {
        Schedule schedule = schedule();
        StaffingRole staffingRole = new StaffingRole(schedule.getTeam(), "Shift Supervisor", null);

        Shift shift = new Shift(
                schedule,
                Instant.parse("2026-07-06T06:00:00Z"),
                Instant.parse("2026-07-06T14:00:00Z"),
                "Supervisor shift",
                1,
                8,
                staffingRole
        );

        assertThat(shift.getRequiredStaffingRole()).isSameAs(staffingRole);
    }

    @Test
    void newShiftCanStoreSourceTemplateSlot() {
        Schedule schedule = schedule();
        ShiftTemplate template = new ShiftTemplate(schedule.getTeam(), "Routine Week", null, 7, 8);
        TemplateSlot templateSlot = new TemplateSlot(
                template,
                0,
                LocalTime.of(6, 0),
                480,
                "Morning shift",
                2
        );

        Shift shift = new Shift(
                schedule,
                Instant.parse("2026-07-06T06:00:00Z"),
                Instant.parse("2026-07-06T14:00:00Z"),
                "Morning shift",
                2,
                8,
                null,
                templateSlot
        );

        assertThat(shift.getTemplateSlot()).isSameAs(templateSlot);
    }

    @Test
    void sourceTemplateSlotMustBelongToScheduleTeam() {
        Schedule schedule = schedule();
        ShiftTemplate otherTemplate = new ShiftTemplate(
                new Team("Support", SwapApprovalPolicy.MANAGER, 8, "Asia/Jerusalem"),
                "Support Week",
                null,
                7,
                8
        );
        TemplateSlot otherTemplateSlot = new TemplateSlot(
                otherTemplate,
                0,
                LocalTime.of(6, 0),
                480,
                "Morning shift",
                2
        );

        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Shift(
                        schedule,
                        Instant.parse("2026-07-06T06:00:00Z"),
                        Instant.parse("2026-07-06T14:00:00Z"),
                        "Morning shift",
                        2,
                        8,
                        null,
                        otherTemplateSlot
                ));
    }

    @Test
    void endTimeMustBeAfterStartTime() {
        Instant startTime = Instant.parse("2026-07-06T06:00:00Z");

        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Shift(
                        schedule(),
                        startTime,
                        startTime,
                        "Invalid shift",
                        1,
                        8));
    }

    @Test
    void requiredWorkersMustBePositive() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Shift(
                        schedule(),
                        Instant.parse("2026-07-06T06:00:00Z"),
                        Instant.parse("2026-07-06T14:00:00Z"),
                        "Invalid shift",
                        0,
                        8));
    }

    @Test
    void minimumRestHoursMustNotBeNegative() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Shift(
                        schedule(),
                        Instant.parse("2026-07-06T06:00:00Z"),
                        Instant.parse("2026-07-06T14:00:00Z"),
                        "Invalid shift",
                        1,
                        -1));
    }

    @Test
    void updateDetailsChangesEditableFields() {
        Shift shift = new Shift(
                schedule(),
                Instant.parse("2026-07-06T06:00:00Z"),
                Instant.parse("2026-07-06T14:00:00Z"),
                "Morning shift",
                2,
                8
        );

        shift.updateDetails(
                Instant.parse("2026-07-06T14:00:00Z"),
                Instant.parse("2026-07-06T22:00:00Z"),
                "Evening shift",
                3,
                10
        );

        assertThat(shift.getStartTime()).isEqualTo(Instant.parse("2026-07-06T14:00:00Z"));
        assertThat(shift.getEndTime()).isEqualTo(Instant.parse("2026-07-06T22:00:00Z"));
        assertThat(shift.getDescription()).isEqualTo("Evening shift");
        assertThat(shift.getRequiredWorkers()).isEqualTo(3);
        assertThat(shift.getMinRestHours()).isEqualTo(10);
        assertThat(shift.getRequiredStaffingRole()).isNull();
    }

    private Schedule schedule() {
        Team team = new Team("Operations", SwapApprovalPolicy.MANAGER, 8, "Asia/Jerusalem");
        return new Schedule(team, LocalDate.of(2026, 7, 6), LocalDate.of(2026, 7, 12));
    }
}
