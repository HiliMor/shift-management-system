package com.hilimor.shiftmanagement.template;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import org.junit.jupiter.api.Test;

import com.hilimor.shiftmanagement.team.SwapApprovalPolicy;
import com.hilimor.shiftmanagement.team.Team;

class ShiftTemplateTest {

    @Test
    void newShiftTemplateStoresTeamDetailsAndDefaultsToInactive() {
        Team team = team();

        ShiftTemplate template = new ShiftTemplate(team, "Emergency Week", "Three shifts per day", 7, 8);

        assertThat(template.getTeam()).isSameAs(team);
        assertThat(template.getName()).isEqualTo("Emergency Week");
        assertThat(template.getDescription()).isEqualTo("Three shifts per day");
        assertThat(template.getCycleDays()).isEqualTo(7);
        assertThat(template.getDefaultMinRestHours()).isEqualTo(8);
        assertThat(template.isActive()).isFalse();
    }

    @Test
    void templateNameIsTrimmed() {
        ShiftTemplate template = new ShiftTemplate(team(), "  Routine Week  ", null, 7, 8);

        assertThat(template.getName()).isEqualTo("Routine Week");
        assertThat(template.getDescription()).isNull();
    }

    @Test
    void activateAndDeactivateUpdateActiveStatus() {
        ShiftTemplate template = new ShiftTemplate(team(), "Routine Week", null, 7, 8);

        template.activate();
        assertThat(template.isActive()).isTrue();

        template.deactivate();
        assertThat(template.isActive()).isFalse();
    }

    @Test
    void updateDetailsChangesTemplateMetadata() {
        ShiftTemplate template = new ShiftTemplate(team(), "Routine Week", null, 7, 8);

        template.updateDetails("Emergency Week", "Updated pattern", 14, 10);

        assertThat(template.getName()).isEqualTo("Emergency Week");
        assertThat(template.getDescription()).isEqualTo("Updated pattern");
        assertThat(template.getCycleDays()).isEqualTo(14);
        assertThat(template.getDefaultMinRestHours()).isEqualTo(10);
    }

    @Test
    void teamMustNotBeNull() {
        assertThatNullPointerException()
                .isThrownBy(() -> new ShiftTemplate(null, "Routine Week", null, 7, 8));
    }

    @Test
    void templateNameMustNotBeBlank() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new ShiftTemplate(team(), "   ", null, 7, 8));
    }

    @Test
    void cycleDaysMustBePositive() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new ShiftTemplate(team(), "Routine Week", null, 0, 8));
    }

    @Test
    void defaultMinRestHoursMustNotBeNegative() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new ShiftTemplate(team(), "Routine Week", null, 7, -1));
    }

    private Team team() {
        return new Team("Operations", SwapApprovalPolicy.MANAGER, 8, "Asia/Jerusalem");
    }
}
