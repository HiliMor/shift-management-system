package com.hilimor.shiftmanagement.assignment;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;

import com.hilimor.shiftmanagement.schedule.Schedule;
import com.hilimor.shiftmanagement.shift.Shift;
import com.hilimor.shiftmanagement.team.SwapApprovalPolicy;
import com.hilimor.shiftmanagement.team.Team;
import com.hilimor.shiftmanagement.user.ApplicationRole;
import com.hilimor.shiftmanagement.user.User;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class AssignmentTest {

    @Test
    void transferToChangesEmployeeAndAssignedAt() {
        User originalEmployee = employee("employee1", 2L);
        User targetEmployee = employee("employee2", 3L);
        Assignment assignment = assignment(originalEmployee);
        Instant transferredAt = Instant.parse("2026-08-07T18:00:00Z");

        assignment.transferTo(targetEmployee, transferredAt);

        assertThat(assignment.getEmployee()).isSameAs(targetEmployee);
        assertThat(assignment.getAssignedAt()).isEqualTo(transferredAt);
    }

    private Assignment assignment(User employee) {
        Team team = new Team("Operations", SwapApprovalPolicy.EMPLOYEE, 8, "Asia/Jerusalem");
        ReflectionTestUtils.setField(team, "id", 1L);

        Schedule schedule = new Schedule(team, LocalDate.of(2026, 8, 7), LocalDate.of(2026, 8, 13));
        ReflectionTestUtils.setField(schedule, "id", 10L);

        Shift shift = new Shift(
                schedule,
                Instant.parse("2026-08-07T06:00:00Z"),
                Instant.parse("2026-08-07T14:00:00Z"),
                "Morning shift",
                1,
                8
        );
        ReflectionTestUtils.setField(shift, "id", 20L);

        Assignment assignment = new Assignment(shift, employee, Instant.parse("2026-08-06T10:00:00Z"));
        ReflectionTestUtils.setField(assignment, "id", 30L);
        return assignment;
    }

    private User employee(String username, Long id) {
        User user = new User(
                username,
                "password-hash",
                "Demo " + username,
                username + "@example.com",
                ApplicationRole.EMPLOYEE
        );
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
