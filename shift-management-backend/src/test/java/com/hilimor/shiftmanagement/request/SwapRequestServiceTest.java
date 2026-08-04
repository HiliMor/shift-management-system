package com.hilimor.shiftmanagement.request;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import com.hilimor.shiftmanagement.assignment.Assignment;
import com.hilimor.shiftmanagement.assignment.AssignmentRepository;
import com.hilimor.shiftmanagement.schedule.Schedule;
import com.hilimor.shiftmanagement.schedule.ScheduleStatus;
import com.hilimor.shiftmanagement.shift.Shift;
import com.hilimor.shiftmanagement.team.SwapApprovalPolicy;
import com.hilimor.shiftmanagement.team.Team;
import com.hilimor.shiftmanagement.team.TeamMemberRepository;
import com.hilimor.shiftmanagement.user.ApplicationRole;
import com.hilimor.shiftmanagement.user.User;
import com.hilimor.shiftmanagement.user.UserRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class SwapRequestServiceTest {

    @Mock
    private SwapRequestRepository swapRequestRepository;

    @Mock
    private AssignmentRepository assignmentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TeamMemberRepository teamMemberRepository;

    @InjectMocks
    private SwapRequestService swapRequestService;

    @Test
    void createTransferRequestSavesPendingEmployeeRequest() {
        User requester = employee("employee1", 2L);
        User targetEmployee = employee("employee2", 3L);
        Assignment sourceAssignment = assignment(schedule(ScheduleStatus.PUBLISHED), requester, 30L);

        when(userRepository.findByUsername("employee1")).thenReturn(Optional.of(requester));
        when(assignmentRepository.findById(30L)).thenReturn(Optional.of(sourceAssignment));
        when(userRepository.findById(3L)).thenReturn(Optional.of(targetEmployee));
        when(teamMemberRepository.existsByUser_IdAndTeam_IdAndActiveTrue(3L, 1L)).thenReturn(true);
        when(swapRequestRepository.existsBySourceAssignment_IdAndStatusIn(
                30L,
                java.util.List.of(SwapRequestStatus.PENDING_EMPLOYEE, SwapRequestStatus.PENDING_MANAGER)
        )).thenReturn(false);
        when(swapRequestRepository.save(any(SwapRequest.class))).thenAnswer(invocation -> {
            SwapRequest request = invocation.getArgument(0);
            ReflectionTestUtils.setField(request, "id", 40L);
            return request;
        });

        SwapRequestResponse response = swapRequestService.createTransferRequest(
                "employee1",
                new CreateTransferRequest(30L, 3L)
        );

        assertThat(response.id()).isEqualTo(40L);
        assertThat(response.type()).isEqualTo(SwapRequestType.TRANSFER);
        assertThat(response.status()).isEqualTo(SwapRequestStatus.PENDING_EMPLOYEE);
        assertThat(response.requesterId()).isEqualTo(2L);
        assertThat(response.sourceAssignmentId()).isEqualTo(30L);
        assertThat(response.sourceShiftId()).isEqualTo(20L);
        assertThat(response.targetEmployeeId()).isEqualTo(3L);
        assertThat(response.targetAssignmentId()).isNull();
        assertThat(response.createdAt()).isNotNull();
        assertThat(response.updatedAt()).isNotNull();

        ArgumentCaptor<SwapRequest> captor = ArgumentCaptor.forClass(SwapRequest.class);
        verify(swapRequestRepository).save(captor.capture());
        assertThat(captor.getValue().getRequester()).isSameAs(requester);
        assertThat(captor.getValue().getSourceAssignment()).isSameAs(sourceAssignment);
        assertThat(captor.getValue().getTargetEmployee()).isSameAs(targetEmployee);
    }

    @Test
    void createTransferRequestRejectsManagerRequester() {
        User manager = user("manager1", 4L, ApplicationRole.MANAGER);

        when(userRepository.findByUsername("manager1")).thenReturn(Optional.of(manager));

        assertResponseStatus(
                () -> swapRequestService.createTransferRequest("manager1", new CreateTransferRequest(30L, 3L)),
                HttpStatus.FORBIDDEN
        );

        verifyNoInteractions(assignmentRepository);
        verify(swapRequestRepository, never()).save(any());
    }

    @Test
    void createTransferRequestHidesAssignmentsNotOwnedByRequester() {
        User requester = employee("employee1", 2L);
        User otherEmployee = employee("employee3", 4L);
        Assignment sourceAssignment = assignment(schedule(ScheduleStatus.PUBLISHED), otherEmployee, 30L);

        when(userRepository.findByUsername("employee1")).thenReturn(Optional.of(requester));
        when(assignmentRepository.findById(30L)).thenReturn(Optional.of(sourceAssignment));

        assertResponseStatus(
                () -> swapRequestService.createTransferRequest("employee1", new CreateTransferRequest(30L, 3L)),
                HttpStatus.NOT_FOUND
        );

        verify(swapRequestRepository, never()).save(any());
    }

    @Test
    void createTransferRequestRejectsDraftSchedule() {
        User requester = employee("employee1", 2L);
        Assignment sourceAssignment = assignment(schedule(ScheduleStatus.DRAFT), requester, 30L);

        when(userRepository.findByUsername("employee1")).thenReturn(Optional.of(requester));
        when(assignmentRepository.findById(30L)).thenReturn(Optional.of(sourceAssignment));

        assertResponseStatus(
                () -> swapRequestService.createTransferRequest("employee1", new CreateTransferRequest(30L, 3L)),
                HttpStatus.CONFLICT
        );

        verify(swapRequestRepository, never()).save(any());
    }

    @Test
    void createTransferRequestRejectsTargetOutsideTeam() {
        User requester = employee("employee1", 2L);
        User targetEmployee = employee("employee2", 3L);
        Assignment sourceAssignment = assignment(schedule(ScheduleStatus.PUBLISHED), requester, 30L);

        when(userRepository.findByUsername("employee1")).thenReturn(Optional.of(requester));
        when(assignmentRepository.findById(30L)).thenReturn(Optional.of(sourceAssignment));
        when(userRepository.findById(3L)).thenReturn(Optional.of(targetEmployee));
        when(teamMemberRepository.existsByUser_IdAndTeam_IdAndActiveTrue(3L, 1L)).thenReturn(false);

        assertResponseStatus(
                () -> swapRequestService.createTransferRequest("employee1", new CreateTransferRequest(30L, 3L)),
                HttpStatus.CONFLICT
        );

        verify(swapRequestRepository, never()).save(any());
    }

    @Test
    void createTransferRequestRejectsSameTargetEmployee() {
        User requester = employee("employee1", 2L);
        Assignment sourceAssignment = assignment(schedule(ScheduleStatus.PUBLISHED), requester, 30L);

        when(userRepository.findByUsername("employee1")).thenReturn(Optional.of(requester));
        when(assignmentRepository.findById(30L)).thenReturn(Optional.of(sourceAssignment));
        when(userRepository.findById(2L)).thenReturn(Optional.of(requester));

        assertResponseStatus(
                () -> swapRequestService.createTransferRequest("employee1", new CreateTransferRequest(30L, 2L)),
                HttpStatus.BAD_REQUEST
        );

        verify(swapRequestRepository, never()).save(any());
    }

    @Test
    void createTransferRequestRejectsAssignmentWithActiveRequest() {
        User requester = employee("employee1", 2L);
        User targetEmployee = employee("employee2", 3L);
        Assignment sourceAssignment = assignment(schedule(ScheduleStatus.PUBLISHED), requester, 30L);

        when(userRepository.findByUsername("employee1")).thenReturn(Optional.of(requester));
        when(assignmentRepository.findById(30L)).thenReturn(Optional.of(sourceAssignment));
        when(userRepository.findById(3L)).thenReturn(Optional.of(targetEmployee));
        when(teamMemberRepository.existsByUser_IdAndTeam_IdAndActiveTrue(3L, 1L)).thenReturn(true);
        when(swapRequestRepository.existsBySourceAssignment_IdAndStatusIn(
                30L,
                java.util.List.of(SwapRequestStatus.PENDING_EMPLOYEE, SwapRequestStatus.PENDING_MANAGER)
        )).thenReturn(true);

        assertResponseStatus(
                () -> swapRequestService.createTransferRequest("employee1", new CreateTransferRequest(30L, 3L)),
                HttpStatus.CONFLICT
        );

        verify(swapRequestRepository, never()).save(any());
    }

    private void assertResponseStatus(Runnable action, HttpStatus expectedStatus) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(expectedStatus));
    }

    private Assignment assignment(Schedule schedule, User employee, Long id) {
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
