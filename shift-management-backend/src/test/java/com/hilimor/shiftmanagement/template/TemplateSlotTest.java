package com.hilimor.shiftmanagement.template;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.time.LocalTime;

import org.junit.jupiter.api.Test;

import com.hilimor.shiftmanagement.staffing.StaffingRole;
import com.hilimor.shiftmanagement.team.SwapApprovalPolicy;
import com.hilimor.shiftmanagement.team.Team;

class TemplateSlotTest {

    @Test
    void newTemplateSlotStoresPatternDetails() {
        ShiftTemplate template = template();
        LocalTime startTime = LocalTime.of(8, 0);

        TemplateSlot slot = new TemplateSlot(template, 2, startTime, 480, "Morning shift", 2);

        assertThat(slot.getShiftTemplate()).isSameAs(template);
        assertThat(slot.getDayOffset()).isEqualTo(2);
        assertThat(slot.getStartTime()).isEqualTo(startTime);
        assertThat(slot.getDurationMinutes()).isEqualTo(480);
        assertThat(slot.getDescription()).isEqualTo("Morning shift");
        assertThat(slot.getRequiredWorkers()).isEqualTo(2);
        assertThat(slot.getRequiredStaffingRole()).isNull();
    }

    @Test
    void newTemplateSlotCanStoreRequiredStaffingRole() {
        ShiftTemplate template = template();
        StaffingRole role = new StaffingRole(template.getTeam(), "Shift Supervisor", null);

        TemplateSlot slot = new TemplateSlot(
                template,
                0,
                LocalTime.of(8, 0),
                480,
                "Supervisor shift",
                1,
                role
        );

        assertThat(slot.getRequiredStaffingRole()).isSameAs(role);
    }

    @Test
    void updateDetailsChangesSlotDetails() {
        TemplateSlot slot = new TemplateSlot(template(), 0, LocalTime.of(8, 0), 480, "Morning shift", 2);

        slot.updateDetails(1, LocalTime.of(16, 0), 360, "Evening shift", 3, null);

        assertThat(slot.getDayOffset()).isEqualTo(1);
        assertThat(slot.getStartTime()).isEqualTo(LocalTime.of(16, 0));
        assertThat(slot.getDurationMinutes()).isEqualTo(360);
        assertThat(slot.getDescription()).isEqualTo("Evening shift");
        assertThat(slot.getRequiredWorkers()).isEqualTo(3);
        assertThat(slot.getRequiredStaffingRole()).isNull();
    }

    @Test
    void shiftTemplateMustNotBeNull() {
        assertThatNullPointerException()
                .isThrownBy(() -> new TemplateSlot(null, 0, LocalTime.of(8, 0), 480, "Morning shift", 2));
    }

    @Test
    void startTimeMustNotBeNull() {
        assertThatNullPointerException()
                .isThrownBy(() -> new TemplateSlot(template(), 0, null, 480, "Morning shift", 2));
    }

    @Test
    void dayOffsetMustBeInsideTemplateCycle() {
        ShiftTemplate template = template();

        assertThatIllegalArgumentException()
                .isThrownBy(() -> new TemplateSlot(template, -1, LocalTime.of(8, 0), 480, "Morning shift", 2));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new TemplateSlot(template, 7, LocalTime.of(8, 0), 480, "Morning shift", 2));
    }

    @Test
    void durationMinutesMustBePositive() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new TemplateSlot(template(), 0, LocalTime.of(8, 0), 0, "Morning shift", 2));
    }

    @Test
    void requiredWorkersMustBePositive() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new TemplateSlot(template(), 0, LocalTime.of(8, 0), 480, "Morning shift", 0));
    }

    @Test
    void requiredStaffingRoleMustBelongToTemplateTeam() {
        ShiftTemplate template = template();
        StaffingRole roleFromOtherTeam = new StaffingRole(
                new Team("Support", SwapApprovalPolicy.MANAGER, 8, "Asia/Jerusalem"),
                "Shift Supervisor",
                null
        );

        assertThatIllegalArgumentException()
                .isThrownBy(() -> new TemplateSlot(
                        template,
                        0,
                        LocalTime.of(8, 0),
                        480,
                        "Supervisor shift",
                        1,
                        roleFromOtherTeam
                ));
    }

    private ShiftTemplate template() {
        return new ShiftTemplate(
                new Team("Operations", SwapApprovalPolicy.MANAGER, 8, "Asia/Jerusalem"),
                "Routine Week",
                null,
                7,
                8
        );
    }
}
