package com.hilimor.shiftmanagement.schedule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.hilimor.shiftmanagement.team.SwapApprovalPolicy;
import com.hilimor.shiftmanagement.team.Team;

class ScheduleTest {

    @Test
    void newScheduleStartsAsDraft() {
        Team team = new Team("Operations", SwapApprovalPolicy.MANAGER, 8, "Asia/Jerusalem");
        LocalDate startDate = LocalDate.of(2026, 7, 1);
        LocalDate endDate = LocalDate.of(2026, 7, 7);

        Schedule schedule = new Schedule(team, startDate, endDate);

        assertThat(schedule.getTeam()).isSameAs(team);
        assertThat(schedule.getStartDate()).isEqualTo(startDate);
        assertThat(schedule.getEndDate()).isEqualTo(endDate);
        assertThat(schedule.getStatus()).isEqualTo(ScheduleStatus.DRAFT);
        assertThat(schedule.getPublicationNumber()).isZero();
        assertThat(schedule.getPublishedAt()).isNull();
    }

    @Test
    void endDateCannotBeBeforeStartDate() {
        Team team = new Team("Operations", SwapApprovalPolicy.MANAGER, 8, "Asia/Jerusalem");

        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Schedule(
                        team,
                        LocalDate.of(2026, 7, 7),
                        LocalDate.of(2026, 7, 1)));
    }
}
