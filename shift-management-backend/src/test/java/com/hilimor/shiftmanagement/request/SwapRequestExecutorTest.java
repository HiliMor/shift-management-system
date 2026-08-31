package com.hilimor.shiftmanagement.request;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Instant;
import java.time.LocalDate;

import com.hilimor.shiftmanagement.assignment.Assignment;
import com.hilimor.shiftmanagement.assignment.AssignmentValidator;
import com.hilimor.shiftmanagement.assignment.AssignmentValidationException;
import com.hilimor.shiftmanagement.schedule.Schedule;
import com.hilimor.shiftmanagement.schedule.ScheduleStatus;
import com.hilimor.shiftmanagement.shift.Shift;
import com.hilimor.shiftmanagement.team.SwapApprovalPolicy;
import com.hilimor.shiftmanagement.team.Team;
import com.hilimor.shiftmanagement.user.ApplicationRole;
import com.hilimor.shiftmanagement.user.User;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class SwapRequestExecutorTest {

    private static final Instant CREATED_AT = Instant.parse("2026-08-04T18:00:00Z");
    private static final Instant EXECUTED_AT = Instant.parse("2026-08-04T19:00:00Z");

    @Mock
    private AssignmentValidator assignmentValidator;

    @Mock
    private SwapRequestLock requestLock;

    @Mock
    private SwapRequestRepository swapRequestRepository;

    @InjectMocks
    private SwapRequestExecutor swapRequestExecutor;

    @Test
    void executeIfReadyDoesNothingForPendingRequest() {
        User requester = employee("employee1", 2L);
        User targetEmployee = employee("employee2", 3L);
        SwapRequest request = transferRequest(
                requester,
                targetEmployee,
                schedule(ScheduleStatus.PUBLISHED)
        );

        swapRequestExecutor.executeIfReady(request, EXECUTED_AT);

        assertThat(request.getStatus()).isEqualTo(SwapRequestStatus.PENDING_EMPLOYEE);
        assertThat(request.getSourceAssignment().getEmployee()).isSameAs(requester);
        verifyNoInteractions(assignmentValidator, requestLock, swapRequestRepository);
    }

    @Test
    void executeIfReadyTransfersApprovedTransferRequest() {
        User requester = employee("employee1", 2L);
        User targetEmployee = employee("employee2", 3L);
        SwapRequest request = transferRequest(
                requester,
                targetEmployee,
                schedule(ScheduleStatus.PUBLISHED)
        );
        request.approveByTargetEmployee(EXECUTED_AT, SwapApprovalPolicy.EMPLOYEE);

        swapRequestExecutor.executeIfReady(request, EXECUTED_AT);

        assertThat(request.getStatus()).isEqualTo(SwapRequestStatus.APPROVED);
        assertThat(request.getSourceAssignment().getEmployee()).isSameAs(targetEmployee);
        assertThat(request.getSourceAssignment().getAssignedAt()).isEqualTo(EXECUTED_AT);
        verify(requestLock).lockExecution(request);
        verify(assignmentValidator).validateEmployeeCanReceiveTransferredAssignment(
                request.getSourceAssignment().getShift(),
                targetEmployee
        );
    }

    @Test
    void executeIfReadyInvalidatesTransferWhenSourceOwnerChanged() {
        User requester = employee("employee1", 2L);
        User targetEmployee = employee("employee2", 3L);
        User otherEmployee = employee("employee3", 4L);
        SwapRequest request = transferRequest(
                requester,
                targetEmployee,
                schedule(ScheduleStatus.PUBLISHED)
        );
        request.approveByTargetEmployee(EXECUTED_AT, SwapApprovalPolicy.EMPLOYEE);
        request.getSourceAssignment().transferTo(otherEmployee, CREATED_AT);

        swapRequestExecutor.executeIfReady(request, EXECUTED_AT);

        assertThat(request.getStatus()).isEqualTo(SwapRequestStatus.INVALIDATED);
        assertThat(request.getUpdatedAt()).isEqualTo(EXECUTED_AT);
        assertThat(request.getSourceAssignment().getEmployee()).isSameAs(otherEmployee);
        verifyNoInteractions(assignmentValidator, swapRequestRepository);
    }

    @Test
    void executeIfReadyInvalidatesTransferWhenScheduleIsNoLongerPublished() {
        User requester = employee("employee1", 2L);
        User targetEmployee = employee("employee2", 3L);
        SwapRequest request = transferRequest(
                requester,
                targetEmployee,
                schedule(ScheduleStatus.DRAFT)
        );
        request.approveByTargetEmployee(EXECUTED_AT, SwapApprovalPolicy.EMPLOYEE);

        swapRequestExecutor.executeIfReady(request, EXECUTED_AT);

        assertThat(request.getStatus()).isEqualTo(SwapRequestStatus.INVALIDATED);
        assertThat(request.getSourceAssignment().getEmployee()).isSameAs(requester);
        verifyNoInteractions(assignmentValidator, swapRequestRepository);
    }

    @Test
    void executeIfReadyInvalidatesTransferWhenAssignmentValidationFails() {
        User requester = employee("employee1", 2L);
        User targetEmployee = employee("employee2", 3L);
        SwapRequest request = transferRequest(
                requester,
                targetEmployee,
                schedule(ScheduleStatus.PUBLISHED)
        );
        request.approveByTargetEmployee(EXECUTED_AT, SwapApprovalPolicy.EMPLOYEE);
        doThrow(validationException()).when(assignmentValidator)
                .validateEmployeeCanReceiveTransferredAssignment(
                        request.getSourceAssignment().getShift(),
                        targetEmployee
                );

        swapRequestExecutor.executeIfReady(request, EXECUTED_AT);

        assertThat(request.getStatus()).isEqualTo(SwapRequestStatus.INVALIDATED);
        assertThat(request.getUpdatedAt()).isEqualTo(EXECUTED_AT);
        assertThat(request.getSourceAssignment().getEmployee()).isSameAs(requester);
    }

    @Test
    void executeIfReadySwapsApprovedSwapRequest() {
        User requester = employee("employee1", 2L);
        User targetEmployee = employee("employee2", 3L);
        SwapRequest request = swapRequest(
                requester,
                targetEmployee,
                schedule(ScheduleStatus.PUBLISHED)
        );
        Assignment sourceAssignment = request.getSourceAssignment();
        Assignment targetAssignment = request.getTargetAssignment();
        request.approveByTargetEmployee(EXECUTED_AT, SwapApprovalPolicy.EMPLOYEE);

        swapRequestExecutor.executeIfReady(request, EXECUTED_AT);

        assertThat(request.getStatus()).isEqualTo(SwapRequestStatus.APPROVED);
        assertThat(sourceAssignment.getEmployee()).isSameAs(targetEmployee);
        assertThat(targetAssignment.getEmployee()).isSameAs(requester);
        assertThat(sourceAssignment.getAssignedAt()).isEqualTo(EXECUTED_AT);
        assertThat(targetAssignment.getAssignedAt()).isEqualTo(EXECUTED_AT);
        verify(assignmentValidator).validateEmployeeCanReceiveSwappedAssignment(
                sourceAssignment.getShift(),
                targetEmployee,
                targetAssignment
        );
        verify(assignmentValidator).validateEmployeeCanReceiveSwappedAssignment(
                targetAssignment.getShift(),
                requester,
                sourceAssignment
        );
    }

    @Test
    void executeIfReadyInvalidatesSwapWhenOwnerChanged() {
        User requester = employee("employee1", 2L);
        User targetEmployee = employee("employee2", 3L);
        User otherEmployee = employee("employee3", 4L);
        SwapRequest request = swapRequest(
                requester,
                targetEmployee,
                schedule(ScheduleStatus.PUBLISHED)
        );
        request.approveByTargetEmployee(EXECUTED_AT, SwapApprovalPolicy.EMPLOYEE);
        request.getTargetAssignment().transferTo(otherEmployee, CREATED_AT);

        swapRequestExecutor.executeIfReady(request, EXECUTED_AT);

        assertThat(request.getStatus()).isEqualTo(SwapRequestStatus.INVALIDATED);
        assertThat(request.getSourceAssignment().getEmployee()).isSameAs(requester);
        assertThat(request.getTargetAssignment().getEmployee()).isSameAs(otherEmployee);
        verifyNoInteractions(assignmentValidator, swapRequestRepository);
    }

    @Test
    void executeIfReadyInvalidatesSwapWhenScheduleIsNoLongerPublished() {
        User requester = employee("employee1", 2L);
        User targetEmployee = employee("employee2", 3L);
        SwapRequest request = swapRequest(
                requester,
                targetEmployee,
                schedule(ScheduleStatus.DRAFT)
        );
        request.approveByTargetEmployee(EXECUTED_AT, SwapApprovalPolicy.EMPLOYEE);

        swapRequestExecutor.executeIfReady(request, EXECUTED_AT);

        assertThat(request.getStatus()).isEqualTo(SwapRequestStatus.INVALIDATED);
        assertThat(request.getSourceAssignment().getEmployee()).isSameAs(requester);
        assertThat(request.getTargetAssignment().getEmployee()).isSameAs(targetEmployee);
        verifyNoInteractions(assignmentValidator, swapRequestRepository);
    }

    @Test
    void executeIfReadyInvalidatesSwapWhenAssignmentValidationFails() {
        User requester = employee("employee1", 2L);
        User targetEmployee = employee("employee2", 3L);
        SwapRequest request = swapRequest(
                requester,
                targetEmployee,
                schedule(ScheduleStatus.PUBLISHED)
        );
        Assignment sourceAssignment = request.getSourceAssignment();
        Assignment targetAssignment = request.getTargetAssignment();
        request.approveByTargetEmployee(EXECUTED_AT, SwapApprovalPolicy.EMPLOYEE);
        doThrow(validationException()).when(assignmentValidator)
                .validateEmployeeCanReceiveSwappedAssignment(
                        sourceAssignment.getShift(),
                        targetEmployee,
                        targetAssignment
                );

        swapRequestExecutor.executeIfReady(request, EXECUTED_AT);

        assertThat(request.getStatus()).isEqualTo(SwapRequestStatus.INVALIDATED);
        assertThat(sourceAssignment.getEmployee()).isSameAs(requester);
        assertThat(targetAssignment.getEmployee()).isSameAs(targetEmployee);
    }

    private AssignmentValidationException validationException() {
        return new AssignmentValidationException(
                HttpStatus.CONFLICT,
                "SHIFT_OVERLAP",
                "Employee already has an overlapping assignment"
        );
    }

    private SwapRequest transferRequest(User requester, User targetEmployee, Schedule schedule) {
        SwapRequest request = SwapRequest.createTransfer(
                requester,
                assignment(schedule, requester, 30L, 20L),
                targetEmployee,
                CREATED_AT
        );
        ReflectionTestUtils.setField(request, "id", 40L);
        return request;
    }

    private SwapRequest swapRequest(User requester, User targetEmployee, Schedule schedule) {
        SwapRequest request = SwapRequest.createSwap(
                requester,
                assignment(schedule, requester, 30L, 20L),
                assignment(schedule, targetEmployee, 31L, 21L),
                CREATED_AT
        );
        ReflectionTestUtils.setField(request, "id", 40L);
        return request;
    }

    private Assignment assignment(Schedule schedule, User employee, Long id, Long shiftId) {
        Shift shift = new Shift(
                schedule,
                Instant.parse("2026-08-04T06:00:00Z"),
                Instant.parse("2026-08-04T14:00:00Z"),
                "Morning shift",
                1,
                8
        );
        ReflectionTestUtils.setField(shift, "id", shiftId);

        Assignment assignment = new Assignment(shift, employee, Instant.parse("2026-08-03T10:00:00Z"));
        ReflectionTestUtils.setField(assignment, "id", id);
        return assignment;
    }

    private Schedule schedule(ScheduleStatus status) {
        Team team = new Team("Operations", SwapApprovalPolicy.MANAGER, 8, "Asia/Jerusalem");
        ReflectionTestUtils.setField(team, "id", 1L);

        Schedule schedule = new Schedule(team, LocalDate.of(2026, 8, 4), LocalDate.of(2026, 8, 10));
        ReflectionTestUtils.setField(schedule, "id", 10L);
        ReflectionTestUtils.setField(schedule, "status", status);
        return schedule;
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
