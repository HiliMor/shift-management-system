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

    @Test
    void approveByTargetEmployeeApprovesRequestWhenPolicyIsEmployee() {
        SwapRequest request = transferRequest();
        Instant approvedAt = Instant.parse("2026-08-04T19:00:00Z");

        request.approveByTargetEmployee(approvedAt, SwapApprovalPolicy.EMPLOYEE);

        assertThat(request.getStatus()).isEqualTo(SwapRequestStatus.APPROVED);
        assertThat(request.getEmployeeApprovedAt()).isEqualTo(approvedAt);
        assertThat(request.getUpdatedAt()).isEqualTo(approvedAt);
    }

    @Test
    void approveByTargetEmployeeMovesRequestToManagerApprovalWhenPolicyIsManager() {
        SwapRequest request = transferRequest();
        Instant approvedAt = Instant.parse("2026-08-04T19:00:00Z");

        request.approveByTargetEmployee(approvedAt, SwapApprovalPolicy.MANAGER);

        assertThat(request.getStatus()).isEqualTo(SwapRequestStatus.PENDING_MANAGER);
        assertThat(request.getEmployeeApprovedAt()).isEqualTo(approvedAt);
        assertThat(request.getUpdatedAt()).isEqualTo(approvedAt);
    }

    @Test
    void approveByTargetEmployeeRejectsRequestThatIsNotPendingEmployeeApproval() {
        SwapRequest request = transferRequest();
        request.approveByTargetEmployee(Instant.parse("2026-08-04T19:00:00Z"), SwapApprovalPolicy.MANAGER);

        assertThatThrownBy(() -> request.approveByTargetEmployee(
                Instant.parse("2026-08-04T20:00:00Z"),
                SwapApprovalPolicy.MANAGER
        )).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void approveByManagerApprovesRequestThatIsPendingManagerApproval() {
        SwapRequest request = transferRequest();
        User manager = user("manager1", 4L, ApplicationRole.MANAGER);
        Instant employeeApprovedAt = Instant.parse("2026-08-04T19:00:00Z");
        Instant managerApprovedAt = Instant.parse("2026-08-04T20:00:00Z");
        request.approveByTargetEmployee(employeeApprovedAt, SwapApprovalPolicy.MANAGER);

        request.approveByManager(manager, managerApprovedAt);

        assertThat(request.getStatus()).isEqualTo(SwapRequestStatus.APPROVED);
        assertThat(request.getManagerApprovedBy()).isSameAs(manager);
        assertThat(request.getManagerApprovedAt()).isEqualTo(managerApprovedAt);
        assertThat(request.getUpdatedAt()).isEqualTo(managerApprovedAt);
    }

    @Test
    void approveByManagerRejectsRequestThatIsNotPendingManagerApproval() {
        SwapRequest request = transferRequest();
        User manager = user("manager1", 4L, ApplicationRole.MANAGER);

        assertThatThrownBy(() -> request.approveByManager(
                manager,
                Instant.parse("2026-08-04T20:00:00Z")
        )).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rejectByTargetEmployeeRejectsPendingEmployeeRequest() {
        SwapRequest request = transferRequest();
        Instant rejectedAt = Instant.parse("2026-08-04T19:00:00Z");

        request.rejectByTargetEmployee(rejectedAt);

        assertThat(request.getStatus()).isEqualTo(SwapRequestStatus.REJECTED);
        assertThat(request.getUpdatedAt()).isEqualTo(rejectedAt);
    }

    @Test
    void rejectByTargetEmployeeRejectsRequestThatIsNotPendingEmployeeApproval() {
        SwapRequest request = transferRequest();
        request.approveByTargetEmployee(Instant.parse("2026-08-04T19:00:00Z"), SwapApprovalPolicy.MANAGER);

        assertThatThrownBy(() -> request.rejectByTargetEmployee(
                Instant.parse("2026-08-04T20:00:00Z")
        )).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void cancelByRequesterCancelsPendingEmployeeRequest() {
        SwapRequest request = transferRequest();
        Instant cancelledAt = Instant.parse("2026-08-04T19:00:00Z");

        request.cancelByRequester(cancelledAt);

        assertThat(request.getStatus()).isEqualTo(SwapRequestStatus.CANCELLED);
        assertThat(request.getUpdatedAt()).isEqualTo(cancelledAt);
    }

    @Test
    void cancelByRequesterCancelsPendingManagerRequest() {
        SwapRequest request = transferRequest();
        request.approveByTargetEmployee(Instant.parse("2026-08-04T19:00:00Z"), SwapApprovalPolicy.MANAGER);
        Instant cancelledAt = Instant.parse("2026-08-04T20:00:00Z");

        request.cancelByRequester(cancelledAt);

        assertThat(request.getStatus()).isEqualTo(SwapRequestStatus.CANCELLED);
        assertThat(request.getUpdatedAt()).isEqualTo(cancelledAt);
    }

    @Test
    void cancelByRequesterRejectsCompletedRequest() {
        SwapRequest request = transferRequest();
        request.approveByTargetEmployee(Instant.parse("2026-08-04T19:00:00Z"), SwapApprovalPolicy.EMPLOYEE);

        assertThatThrownBy(() -> request.cancelByRequester(
                Instant.parse("2026-08-04T20:00:00Z")
        )).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void invalidateMovesRequestToInvalidated() {
        SwapRequest request = transferRequest();
        Instant invalidatedAt = Instant.parse("2026-08-04T20:00:00Z");

        request.invalidate(invalidatedAt);

        assertThat(request.getStatus()).isEqualTo(SwapRequestStatus.INVALIDATED);
        assertThat(request.getUpdatedAt()).isEqualTo(invalidatedAt);
    }

    private SwapRequest transferRequest() {
        User requester = user("employee1", 2L);
        User targetEmployee = user("employee2", 3L);
        return SwapRequest.createTransfer(
                requester,
                assignment(requester),
                targetEmployee,
                Instant.parse("2026-08-04T18:00:00Z")
        );
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
        return user(username, id, ApplicationRole.EMPLOYEE);
    }

    private User user(String username, Long id, ApplicationRole role) {
        User user = new User(
                username,
                "password-hash",
                "Demo " + username,
                username + "@example.com",
                role
        );
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
