package com.hilimor.shiftmanagement.request;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;

import com.hilimor.shiftmanagement.assignment.Assignment;
import com.hilimor.shiftmanagement.schedule.Schedule;
import com.hilimor.shiftmanagement.shift.Shift;
import com.hilimor.shiftmanagement.team.SwapApprovalPolicy;
import com.hilimor.shiftmanagement.team.Team;
import com.hilimor.shiftmanagement.user.ApplicationRole;
import com.hilimor.shiftmanagement.user.User;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class SwapRequestTest {

    @Test
    void createTransferInitializesPendingEmployeeRequest() {
        User requester = user("employee1", 2L);
        User targetEmployee = user("employee2", 3L);
        Assignment sourceAssignment = assignment(requester);
        Instant createdAt = Instant.parse("2026-08-04T18:00:00Z");

        SwapRequest request = SwapRequest.createTransfer(requester, sourceAssignment, targetEmployee, createdAt);

        assertThat(request.getType()).isEqualTo(SwapRequestType.TRANSFER);
        assertThat(request.getStatus()).isEqualTo(SwapRequestStatus.PENDING_EMPLOYEE);
        assertThat(request.getRequester()).isSameAs(requester);
        assertThat(request.getSourceAssignment()).isSameAs(sourceAssignment);
        assertThat(request.getTargetEmployee()).isSameAs(targetEmployee);
        assertThat(request.getTargetAssignment()).isNull();
        assertThat(request.getCreatedAt()).isEqualTo(createdAt);
        assertThat(request.getUpdatedAt()).isEqualTo(createdAt);
    }

    @Test
    void createTransferRejectsMissingTargetEmployee() {
        User requester = user("employee1", 2L);
        Assignment sourceAssignment = assignment(requester);

        assertThatThrownBy(() -> SwapRequest.createTransfer(
                requester,
                sourceAssignment,
                null,
                Instant.parse("2026-08-04T18:00:00Z")
        )).isInstanceOf(NullPointerException.class);
    }

    private Assignment assignment(User employee) {
        Team team = new Team("Operations", SwapApprovalPolicy.MANAGER, 8, "Asia/Jerusalem");
        ReflectionTestUtils.setField(team, "id", 1L);

        Schedule schedule = new Schedule(team, LocalDate.of(2026, 8, 4), LocalDate.of(2026, 8, 10));
        ReflectionTestUtils.setField(schedule, "id", 10L);

        Shift shift = new Shift(
                schedule,
                Instant.parse("2026-08-04T06:00:00Z"),
                Instant.parse("2026-08-04T14:00:00Z"),
                "Morning shift",
                1,
                8
        );
        ReflectionTestUtils.setField(shift, "id", 20L);

        Assignment assignment = new Assignment(shift, employee, Instant.parse("2026-08-03T10:00:00Z"));
        ReflectionTestUtils.setField(assignment, "id", 30L);
        return assignment;
    }

    private User user(String username, Long id) {
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
