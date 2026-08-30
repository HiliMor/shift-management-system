package com.hilimor.shiftmanagement.request;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.hilimor.shiftmanagement.assignment.Assignment;
import com.hilimor.shiftmanagement.assignment.AssignmentRepository;
import com.hilimor.shiftmanagement.messaging.EventOutboxService;
import com.hilimor.shiftmanagement.schedule.Schedule;
import com.hilimor.shiftmanagement.schedule.ScheduleStatus;
import com.hilimor.shiftmanagement.shift.Shift;
import com.hilimor.shiftmanagement.team.SwapApprovalPolicy;
import com.hilimor.shiftmanagement.team.Team;
import com.hilimor.shiftmanagement.team.TeamManager;
import com.hilimor.shiftmanagement.team.TeamManagerRepository;
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
    private SwapRequestExecutor swapRequestExecutor;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TeamMemberRepository teamMemberRepository;

    @Mock
    private TeamManagerRepository teamManagerRepository;

    @Mock
    private EventOutboxService eventOutboxService;

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
                List.of(SwapRequestStatus.PENDING_EMPLOYEE, SwapRequestStatus.PENDING_MANAGER)
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
        assertThat(response.sourceShiftDescription()).isEqualTo("Morning shift");
        assertThat(response.sourceShiftStartTime()).isEqualTo(Instant.parse("2026-08-04T06:00:00Z"));
        assertThat(response.sourceShiftEndTime()).isEqualTo(Instant.parse("2026-08-04T14:00:00Z"));
        assertThat(response.targetEmployeeId()).isEqualTo(3L);
        assertThat(response.targetAssignmentId()).isNull();
        assertThat(response.targetShiftDescription()).isNull();
        assertThat(response.targetShiftStartTime()).isNull();
        assertThat(response.targetShiftEndTime()).isNull();
        assertThat(response.createdAt()).isNotNull();
        assertThat(response.updatedAt()).isNotNull();

        ArgumentCaptor<SwapRequest> captor = ArgumentCaptor.forClass(SwapRequest.class);
        verify(swapRequestRepository).save(captor.capture());
        assertThat(captor.getValue().getRequester()).isSameAs(requester);
        assertThat(captor.getValue().getSourceAssignment()).isSameAs(sourceAssignment);
        assertThat(captor.getValue().getTargetEmployee()).isSameAs(targetEmployee);

        ArgumentCaptor<SwapRequestCreatedEvent> eventCaptor = ArgumentCaptor.forClass(SwapRequestCreatedEvent.class);
        verify(eventOutboxService).createEvent(eq("request.created"), eventCaptor.capture());
        assertThat(eventCaptor.getValue().requestId()).isEqualTo(40L);
        assertThat(eventCaptor.getValue().requestType()).isEqualTo(SwapRequestType.TRANSFER);
        assertThat(eventCaptor.getValue().targetEmployeeId()).isEqualTo(3L);
        assertThat(eventCaptor.getValue().sourceShiftId()).isEqualTo(20L);
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
                List.of(SwapRequestStatus.PENDING_EMPLOYEE, SwapRequestStatus.PENDING_MANAGER)
        )).thenReturn(true);

        assertResponseStatus(
                () -> swapRequestService.createTransferRequest("employee1", new CreateTransferRequest(30L, 3L)),
                HttpStatus.CONFLICT
        );

        verify(swapRequestRepository, never()).save(any());
    }

    @Test
    void createSwapRequestSavesPendingEmployeeRequest() {
        User requester = employee("employee1", 2L);
        User targetEmployee = employee("employee2", 3L);
        Schedule schedule = schedule(ScheduleStatus.PUBLISHED, SwapApprovalPolicy.EMPLOYEE);
        Assignment sourceAssignment = assignment(
                schedule,
                requester,
                30L,
                20L,
                Instant.parse("2026-08-04T06:00:00Z"),
                Instant.parse("2026-08-04T14:00:00Z"),
                "Morning shift"
        );
        Assignment targetAssignment = assignment(
                schedule,
                targetEmployee,
                31L,
                21L,
                Instant.parse("2026-08-05T06:00:00Z"),
                Instant.parse("2026-08-05T14:00:00Z"),
                "Next morning shift"
        );

        when(userRepository.findByUsername("employee1")).thenReturn(Optional.of(requester));
        when(assignmentRepository.findById(30L)).thenReturn(Optional.of(sourceAssignment));
        when(assignmentRepository.findById(31L)).thenReturn(Optional.of(targetAssignment));
        when(teamMemberRepository.existsByUser_IdAndTeam_IdAndActiveTrue(3L, 1L)).thenReturn(true);
        when(swapRequestRepository.save(any(SwapRequest.class))).thenAnswer(invocation -> {
            SwapRequest request = invocation.getArgument(0);
            ReflectionTestUtils.setField(request, "id", 40L);
            return request;
        });

        SwapRequestResponse response = swapRequestService.createSwapRequest(
                "employee1",
                new CreateSwapRequest(30L, 31L)
        );

        assertThat(response.id()).isEqualTo(40L);
        assertThat(response.type()).isEqualTo(SwapRequestType.SWAP);
        assertThat(response.status()).isEqualTo(SwapRequestStatus.PENDING_EMPLOYEE);
        assertThat(response.requesterId()).isEqualTo(2L);
        assertThat(response.sourceAssignmentId()).isEqualTo(30L);
        assertThat(response.sourceShiftId()).isEqualTo(20L);
        assertThat(response.sourceShiftDescription()).isEqualTo("Morning shift");
        assertThat(response.sourceShiftStartTime()).isEqualTo(Instant.parse("2026-08-04T06:00:00Z"));
        assertThat(response.sourceShiftEndTime()).isEqualTo(Instant.parse("2026-08-04T14:00:00Z"));
        assertThat(response.targetEmployeeId()).isEqualTo(3L);
        assertThat(response.targetAssignmentId()).isEqualTo(31L);
        assertThat(response.targetShiftId()).isEqualTo(21L);
        assertThat(response.targetShiftDescription()).isEqualTo("Next morning shift");
        assertThat(response.targetShiftStartTime()).isEqualTo(Instant.parse("2026-08-05T06:00:00Z"));
        assertThat(response.targetShiftEndTime()).isEqualTo(Instant.parse("2026-08-05T14:00:00Z"));

        ArgumentCaptor<SwapRequest> captor = ArgumentCaptor.forClass(SwapRequest.class);
        verify(swapRequestRepository).save(captor.capture());
        assertThat(captor.getValue().getRequester()).isSameAs(requester);
        assertThat(captor.getValue().getSourceAssignment()).isSameAs(sourceAssignment);
        assertThat(captor.getValue().getTargetEmployee()).isSameAs(targetEmployee);
        assertThat(captor.getValue().getTargetAssignment()).isSameAs(targetAssignment);
    }

    @Test
    void createSwapRequestRejectsAssignmentsFromDifferentTeams() {
        User requester = employee("employee1", 2L);
        User targetEmployee = employee("employee2", 3L);
        Assignment sourceAssignment = assignment(
                schedule(ScheduleStatus.PUBLISHED, SwapApprovalPolicy.MANAGER, 1L),
                requester,
                30L
        );
        Assignment targetAssignment = assignment(
                schedule(ScheduleStatus.PUBLISHED, SwapApprovalPolicy.MANAGER, 2L),
                targetEmployee,
                31L
        );

        when(userRepository.findByUsername("employee1")).thenReturn(Optional.of(requester));
        when(assignmentRepository.findById(30L)).thenReturn(Optional.of(sourceAssignment));
        when(assignmentRepository.findById(31L)).thenReturn(Optional.of(targetAssignment));

        assertResponseStatus(
                () -> swapRequestService.createSwapRequest("employee1", new CreateSwapRequest(30L, 31L)),
                HttpStatus.BAD_REQUEST
        );

        verify(swapRequestRepository, never()).save(any());
    }

    @Test
    void createSwapRequestRejectsAssignmentAlreadyInActiveRequest() {
        User requester = employee("employee1", 2L);
        User targetEmployee = employee("employee2", 3L);
        Schedule schedule = schedule(ScheduleStatus.PUBLISHED, SwapApprovalPolicy.MANAGER);
        Assignment sourceAssignment = assignment(schedule, requester, 30L);
        Assignment targetAssignment = assignment(schedule, targetEmployee, 31L, 21L);

        when(userRepository.findByUsername("employee1")).thenReturn(Optional.of(requester));
        when(assignmentRepository.findById(30L)).thenReturn(Optional.of(sourceAssignment));
        when(assignmentRepository.findById(31L)).thenReturn(Optional.of(targetAssignment));
        when(teamMemberRepository.existsByUser_IdAndTeam_IdAndActiveTrue(3L, 1L)).thenReturn(true);
        when(swapRequestRepository.existsBySourceAssignment_IdAndStatusIn(
                30L,
                List.of(SwapRequestStatus.PENDING_EMPLOYEE, SwapRequestStatus.PENDING_MANAGER)
        )).thenReturn(false);
        when(swapRequestRepository.existsByTargetAssignment_IdAndStatusIn(
                30L,
                List.of(SwapRequestStatus.PENDING_EMPLOYEE, SwapRequestStatus.PENDING_MANAGER)
        )).thenReturn(false);
        when(swapRequestRepository.existsBySourceAssignment_IdAndStatusIn(
                31L,
                List.of(SwapRequestStatus.PENDING_EMPLOYEE, SwapRequestStatus.PENDING_MANAGER)
        )).thenReturn(true);

        assertResponseStatus(
                () -> swapRequestService.createSwapRequest("employee1", new CreateSwapRequest(30L, 31L)),
                HttpStatus.CONFLICT
        );

        verify(swapRequestRepository, never()).save(any());
    }

    @Test
    void listMyOutgoingRequestsReturnsRequestsCreatedByCurrentEmployee() {
        User requester = employee("employee1", 2L);
        User targetEmployee = employee("employee2", 3L);
        SwapRequest request = transferRequest(
                requester,
                targetEmployee,
                schedule(ScheduleStatus.PUBLISHED, SwapApprovalPolicy.MANAGER)
        );

        when(userRepository.findByUsername("employee1")).thenReturn(Optional.of(requester));
        when(swapRequestRepository.findByRequester_UsernameOrderByCreatedAtDesc("employee1")).thenReturn(List.of(request));

        List<SwapRequestResponse> responses = swapRequestService.listMyOutgoingRequests("employee1");

        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().id()).isEqualTo(40L);
        assertThat(responses.getFirst().requesterUsername()).isEqualTo("employee1");
        assertThat(responses.getFirst().targetEmployeeUsername()).isEqualTo("employee2");
    }

    @Test
    void listMyIncomingRequestsReturnsRequestsTargetingCurrentEmployee() {
        User requester = employee("employee1", 2L);
        User targetEmployee = employee("employee2", 3L);
        SwapRequest request = transferRequest(
                requester,
                targetEmployee,
                schedule(ScheduleStatus.PUBLISHED, SwapApprovalPolicy.MANAGER)
        );

        when(userRepository.findByUsername("employee2")).thenReturn(Optional.of(targetEmployee));
        when(swapRequestRepository.findByTargetEmployee_UsernameOrderByCreatedAtDesc("employee2")).thenReturn(List.of(request));

        List<SwapRequestResponse> responses = swapRequestService.listMyIncomingRequests("employee2");

        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().id()).isEqualTo(40L);
        assertThat(responses.getFirst().requesterUsername()).isEqualTo("employee1");
        assertThat(responses.getFirst().targetEmployeeUsername()).isEqualTo("employee2");
    }

    @Test
    void listPendingManagerRequestsReturnsRequestsWaitingForManagedTeamApproval() {
        User requester = employee("employee1", 2L);
        User targetEmployee = employee("employee2", 3L);
        User manager = user("manager1", 4L, ApplicationRole.MANAGER);
        SwapRequest request = transferRequest(
                requester,
                targetEmployee,
                schedule(ScheduleStatus.PUBLISHED, SwapApprovalPolicy.MANAGER)
        );
        request.approveByTargetEmployee(Instant.parse("2026-08-04T19:00:00Z"), SwapApprovalPolicy.MANAGER);
        TeamManager teamManager = new TeamManager(manager, request.getSourceAssignment().getShift().getSchedule().getTeam());

        when(userRepository.findByUsername("manager1")).thenReturn(Optional.of(manager));
        when(teamManagerRepository.findByManager_Username("manager1")).thenReturn(List.of(teamManager));
        when(swapRequestRepository.findByStatusAndSourceAssignment_Shift_Schedule_Team_IdInOrderByCreatedAtDesc(
                SwapRequestStatus.PENDING_MANAGER,
                List.of(1L)
        ))
                .thenReturn(List.of(request));

        List<SwapRequestResponse> responses = swapRequestService.listPendingManagerRequests("manager1");

        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().id()).isEqualTo(40L);
        assertThat(responses.getFirst().status()).isEqualTo(SwapRequestStatus.PENDING_MANAGER);
        assertThat(responses.getFirst().requesterUsername()).isEqualTo("employee1");
        assertThat(responses.getFirst().targetEmployeeUsername()).isEqualTo("employee2");
    }

    @Test
    void listPendingManagerRequestsReturnsEmptyListWhenManagerHasNoTeams() {
        User manager = user("manager1", 4L, ApplicationRole.MANAGER);

        when(userRepository.findByUsername("manager1")).thenReturn(Optional.of(manager));
        when(teamManagerRepository.findByManager_Username("manager1")).thenReturn(List.of());

        List<SwapRequestResponse> responses = swapRequestService.listPendingManagerRequests("manager1");

        assertThat(responses).isEmpty();
        verify(swapRequestRepository, never())
                .findByStatusAndSourceAssignment_Shift_Schedule_Team_IdInOrderByCreatedAtDesc(any(), any());
    }

    @Test
    void listManagerRequestsIncludesRequestsWaitingForEmployeeAndManagerApproval() {
        User requester = employee("employee1", 2L);
        User targetEmployee = employee("employee2", 3L);
        User manager = user("manager1", 4L, ApplicationRole.MANAGER);
        SwapRequest request = transferRequest(
                requester,
                targetEmployee,
                schedule(ScheduleStatus.PUBLISHED, SwapApprovalPolicy.MANAGER)
        );
        TeamManager teamManager = new TeamManager(manager, request.getSourceAssignment().getShift().getSchedule().getTeam());

        when(userRepository.findByUsername("manager1")).thenReturn(Optional.of(manager));
        when(teamManagerRepository.findByManager_Username("manager1")).thenReturn(List.of(teamManager));
        when(swapRequestRepository.findByStatusInAndSourceAssignment_Shift_Schedule_Team_IdInOrderByCreatedAtDesc(
                List.of(SwapRequestStatus.PENDING_EMPLOYEE, SwapRequestStatus.PENDING_MANAGER),
                List.of(1L)
        )).thenReturn(List.of(request));

        List<SwapRequestResponse> responses = swapRequestService.listManagerRequests("manager1");

        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().status()).isEqualTo(SwapRequestStatus.PENDING_EMPLOYEE);
        assertThat(responses.getFirst().targetEmployeeUsername()).isEqualTo("employee2");
    }

    @Test
    void listManagerRequestsReturnsEmptyListWhenManagerHasNoTeams() {
        User manager = user("manager1", 4L, ApplicationRole.MANAGER);

        when(userRepository.findByUsername("manager1")).thenReturn(Optional.of(manager));
        when(teamManagerRepository.findByManager_Username("manager1")).thenReturn(List.of());

        List<SwapRequestResponse> responses = swapRequestService.listManagerRequests("manager1");

        assertThat(responses).isEmpty();
        verify(swapRequestRepository, never())
                .findByStatusInAndSourceAssignment_Shift_Schedule_Team_IdInOrderByCreatedAtDesc(any(), any());
    }

    @Test
    void approveByTargetEmployeeMovesRequestToPendingManagerWhenTeamPolicyRequiresManager() {
        User requester = employee("employee1", 2L);
        User targetEmployee = employee("employee2", 3L);
        SwapRequest request = transferRequest(
                requester,
                targetEmployee,
                schedule(ScheduleStatus.PUBLISHED, SwapApprovalPolicy.MANAGER)
        );

        when(userRepository.findByUsername("employee2")).thenReturn(Optional.of(targetEmployee));
        when(swapRequestRepository.findById(40L)).thenReturn(Optional.of(request));

        SwapRequestResponse response = swapRequestService.approveByTargetEmployee("employee2", 40L);

        assertThat(response.status()).isEqualTo(SwapRequestStatus.PENDING_MANAGER);
        assertThat(response.employeeApprovedAt()).isNotNull();
        assertThat(response.updatedAt()).isEqualTo(response.employeeApprovedAt());
        assertThat(request.getSourceAssignment().getEmployee()).isSameAs(requester);
        verify(swapRequestExecutor).executeIfReady(request, response.employeeApprovedAt());
    }

    @Test
    void approveByTargetEmployeeDelegatesExecutionWhenTeamPolicyIsEmployeeOnly() {
        User requester = employee("employee1", 2L);
        User targetEmployee = employee("employee2", 3L);
        SwapRequest request = transferRequest(
                requester,
                targetEmployee,
                schedule(ScheduleStatus.PUBLISHED, SwapApprovalPolicy.EMPLOYEE)
        );

        when(userRepository.findByUsername("employee2")).thenReturn(Optional.of(targetEmployee));
        when(swapRequestRepository.findById(40L)).thenReturn(Optional.of(request));

        SwapRequestResponse response = swapRequestService.approveByTargetEmployee("employee2", 40L);

        assertThat(response.status()).isEqualTo(SwapRequestStatus.APPROVED);
        assertThat(response.employeeApprovedAt()).isNotNull();
        assertThat(response.updatedAt()).isEqualTo(response.employeeApprovedAt());
        assertThat(request.getSourceAssignment().getEmployee()).isSameAs(requester);
        verify(swapRequestExecutor).executeIfReady(request, response.employeeApprovedAt());
    }

    @Test
    void approveByTargetEmployeeReturnsRequestStatusUpdatedByExecutor() {
        User requester = employee("employee1", 2L);
        User targetEmployee = employee("employee2", 3L);
        SwapRequest request = transferRequest(
                requester,
                targetEmployee,
                schedule(ScheduleStatus.PUBLISHED, SwapApprovalPolicy.EMPLOYEE)
        );

        when(userRepository.findByUsername("employee2")).thenReturn(Optional.of(targetEmployee));
        when(swapRequestRepository.findById(40L)).thenReturn(Optional.of(request));
        doAnswer(invocation -> {
            request.invalidate(invocation.getArgument(1));
            return null;
        }).when(swapRequestExecutor).executeIfReady(any(SwapRequest.class), any(Instant.class));

        SwapRequestResponse response = swapRequestService.approveByTargetEmployee("employee2", 40L);

        assertThat(response.status()).isEqualTo(SwapRequestStatus.INVALIDATED);
        assertThat(response.employeeApprovedAt()).isNotNull();
        assertThat(response.updatedAt()).isEqualTo(response.employeeApprovedAt());
        assertThat(request.getSourceAssignment().getEmployee()).isSameAs(requester);
    }

    @Test
    void approveByTargetEmployeeDelegatesSwapExecutionWhenTeamPolicyIsEmployeeOnly() {
        User requester = employee("employee1", 2L);
        User targetEmployee = employee("employee2", 3L);
        SwapRequest request = swapRequest(
                requester,
                targetEmployee,
                schedule(ScheduleStatus.PUBLISHED, SwapApprovalPolicy.EMPLOYEE)
        );
        Assignment sourceAssignment = request.getSourceAssignment();
        Assignment targetAssignment = request.getTargetAssignment();

        when(userRepository.findByUsername("employee2")).thenReturn(Optional.of(targetEmployee));
        when(swapRequestRepository.findById(40L)).thenReturn(Optional.of(request));

        SwapRequestResponse response = swapRequestService.approveByTargetEmployee("employee2", 40L);

        assertThat(response.status()).isEqualTo(SwapRequestStatus.APPROVED);
        assertThat(response.employeeApprovedAt()).isNotNull();
        assertThat(sourceAssignment.getEmployee()).isSameAs(requester);
        assertThat(targetAssignment.getEmployee()).isSameAs(targetEmployee);
        verify(swapRequestExecutor).executeIfReady(request, response.employeeApprovedAt());
    }

    @Test
    void approveByTargetEmployeeRejectsRequesterApproval() {
        User requester = employee("employee1", 2L);
        User targetEmployee = employee("employee2", 3L);
        SwapRequest request = transferRequest(
                requester,
                targetEmployee,
                schedule(ScheduleStatus.PUBLISHED, SwapApprovalPolicy.MANAGER)
        );

        when(userRepository.findByUsername("employee1")).thenReturn(Optional.of(requester));
        when(swapRequestRepository.findById(40L)).thenReturn(Optional.of(request));

        assertResponseStatus(
                () -> swapRequestService.approveByTargetEmployee("employee1", 40L),
                HttpStatus.NOT_FOUND
        );

        verifyNoInteractions(swapRequestExecutor);
    }

    @Test
    void approveByTargetEmployeeRejectsEmployeeWhoIsNotTarget() {
        User requester = employee("employee1", 2L);
        User targetEmployee = employee("employee2", 3L);
        User otherEmployee = employee("employee3", 4L);
        SwapRequest request = transferRequest(
                requester,
                targetEmployee,
                schedule(ScheduleStatus.PUBLISHED, SwapApprovalPolicy.MANAGER)
        );

        when(userRepository.findByUsername("employee3")).thenReturn(Optional.of(otherEmployee));
        when(swapRequestRepository.findById(40L)).thenReturn(Optional.of(request));

        assertResponseStatus(
                () -> swapRequestService.approveByTargetEmployee("employee3", 40L),
                HttpStatus.NOT_FOUND
        );

        verifyNoInteractions(swapRequestExecutor);
    }

    @Test
    void approveByTargetEmployeeRejectsRequestThatIsNotPendingEmployeeApproval() {
        User requester = employee("employee1", 2L);
        User targetEmployee = employee("employee2", 3L);
        SwapRequest request = transferRequest(
                requester,
                targetEmployee,
                schedule(ScheduleStatus.PUBLISHED, SwapApprovalPolicy.MANAGER)
        );
        request.approveByTargetEmployee(Instant.parse("2026-08-04T19:00:00Z"), SwapApprovalPolicy.MANAGER);

        when(userRepository.findByUsername("employee2")).thenReturn(Optional.of(targetEmployee));
        when(swapRequestRepository.findById(40L)).thenReturn(Optional.of(request));

        assertResponseStatus(
                () -> swapRequestService.approveByTargetEmployee("employee2", 40L),
                HttpStatus.CONFLICT
        );

        verifyNoInteractions(swapRequestExecutor);
    }

    @Test
    void approveByManagerDelegatesExecutionWhenManagerApprovalCompletes() {
        User requester = employee("employee1", 2L);
        User targetEmployee = employee("employee2", 3L);
        User manager = user("manager1", 4L, ApplicationRole.MANAGER);
        SwapRequest request = transferRequest(
                requester,
                targetEmployee,
                schedule(ScheduleStatus.PUBLISHED, SwapApprovalPolicy.MANAGER)
        );
        request.approveByTargetEmployee(Instant.parse("2026-08-04T19:00:00Z"), SwapApprovalPolicy.MANAGER);

        when(userRepository.findByUsername("manager1")).thenReturn(Optional.of(manager));
        when(swapRequestRepository.findById(40L)).thenReturn(Optional.of(request));
        when(teamManagerRepository.existsByManager_UsernameAndTeam_Id("manager1", 1L)).thenReturn(true);

        SwapRequestResponse response = swapRequestService.approveByManager("manager1", 40L);

        assertThat(response.status()).isEqualTo(SwapRequestStatus.APPROVED);
        assertThat(response.managerApprovedById()).isEqualTo(4L);
        assertThat(response.managerApprovedAt()).isNotNull();
        assertThat(response.updatedAt()).isEqualTo(response.managerApprovedAt());
        assertThat(request.getSourceAssignment().getEmployee()).isSameAs(requester);
        verify(swapRequestExecutor).executeIfReady(request, response.managerApprovedAt());
    }

    @Test
    void approveByManagerReturnsRequestStatusUpdatedByExecutor() {
        User requester = employee("employee1", 2L);
        User targetEmployee = employee("employee2", 3L);
        User manager = user("manager1", 4L, ApplicationRole.MANAGER);
        SwapRequest request = transferRequest(
                requester,
                targetEmployee,
                schedule(ScheduleStatus.PUBLISHED, SwapApprovalPolicy.MANAGER)
        );
        request.approveByTargetEmployee(Instant.parse("2026-08-04T19:00:00Z"), SwapApprovalPolicy.MANAGER);

        when(userRepository.findByUsername("manager1")).thenReturn(Optional.of(manager));
        when(swapRequestRepository.findById(40L)).thenReturn(Optional.of(request));
        when(teamManagerRepository.existsByManager_UsernameAndTeam_Id("manager1", 1L)).thenReturn(true);
        doAnswer(invocation -> {
            request.invalidate(invocation.getArgument(1));
            return null;
        }).when(swapRequestExecutor).executeIfReady(any(SwapRequest.class), any(Instant.class));

        SwapRequestResponse response = swapRequestService.approveByManager("manager1", 40L);

        assertThat(response.status()).isEqualTo(SwapRequestStatus.INVALIDATED);
        assertThat(response.managerApprovedById()).isEqualTo(4L);
        assertThat(response.managerApprovedAt()).isNotNull();
        assertThat(response.updatedAt()).isEqualTo(response.managerApprovedAt());
        assertThat(request.getSourceAssignment().getEmployee()).isSameAs(requester);
    }

    @Test
    void approveByManagerDelegatesSwapExecutionWhenManagerApprovalCompletes() {
        User requester = employee("employee1", 2L);
        User targetEmployee = employee("employee2", 3L);
        User manager = user("manager1", 4L, ApplicationRole.MANAGER);
        SwapRequest request = swapRequest(
                requester,
                targetEmployee,
                schedule(ScheduleStatus.PUBLISHED, SwapApprovalPolicy.MANAGER)
        );
        request.approveByTargetEmployee(Instant.parse("2026-08-04T19:00:00Z"), SwapApprovalPolicy.MANAGER);
        Assignment sourceAssignment = request.getSourceAssignment();
        Assignment targetAssignment = request.getTargetAssignment();

        when(userRepository.findByUsername("manager1")).thenReturn(Optional.of(manager));
        when(swapRequestRepository.findById(40L)).thenReturn(Optional.of(request));
        when(teamManagerRepository.existsByManager_UsernameAndTeam_Id("manager1", 1L)).thenReturn(true);

        SwapRequestResponse response = swapRequestService.approveByManager("manager1", 40L);

        assertThat(response.status()).isEqualTo(SwapRequestStatus.APPROVED);
        assertThat(response.managerApprovedById()).isEqualTo(4L);
        assertThat(response.managerApprovedAt()).isNotNull();
        assertThat(sourceAssignment.getEmployee()).isSameAs(requester);
        assertThat(targetAssignment.getEmployee()).isSameAs(targetEmployee);
        verify(swapRequestExecutor).executeIfReady(request, response.managerApprovedAt());
    }

    @Test
    void approveByManagerRejectsEmployeeUser() {
        User targetEmployee = employee("employee2", 3L);

        when(userRepository.findByUsername("employee2")).thenReturn(Optional.of(targetEmployee));

        assertResponseStatus(
                () -> swapRequestService.approveByManager("employee2", 40L),
                HttpStatus.FORBIDDEN
        );

        verify(swapRequestRepository, never()).findById(any());
        verifyNoInteractions(swapRequestExecutor);
    }

    @Test
    void approveByManagerRejectsManagerOutsideSourceTeam() {
        User requester = employee("employee1", 2L);
        User targetEmployee = employee("employee2", 3L);
        User manager = user("manager1", 4L, ApplicationRole.MANAGER);
        SwapRequest request = transferRequest(
                requester,
                targetEmployee,
                schedule(ScheduleStatus.PUBLISHED, SwapApprovalPolicy.MANAGER)
        );
        request.approveByTargetEmployee(Instant.parse("2026-08-04T19:00:00Z"), SwapApprovalPolicy.MANAGER);

        when(userRepository.findByUsername("manager1")).thenReturn(Optional.of(manager));
        when(swapRequestRepository.findById(40L)).thenReturn(Optional.of(request));
        when(teamManagerRepository.existsByManager_UsernameAndTeam_Id("manager1", 1L)).thenReturn(false);

        assertResponseStatus(
                () -> swapRequestService.approveByManager("manager1", 40L),
                HttpStatus.FORBIDDEN
        );

        assertThat(request.getSourceAssignment().getEmployee()).isSameAs(requester);
        verifyNoInteractions(swapRequestExecutor);
    }

    @Test
    void approveByManagerRejectsRequestThatIsNotPendingManagerApproval() {
        User requester = employee("employee1", 2L);
        User targetEmployee = employee("employee2", 3L);
        User manager = user("manager1", 4L, ApplicationRole.MANAGER);
        SwapRequest request = transferRequest(
                requester,
                targetEmployee,
                schedule(ScheduleStatus.PUBLISHED, SwapApprovalPolicy.MANAGER)
        );

        when(userRepository.findByUsername("manager1")).thenReturn(Optional.of(manager));
        when(swapRequestRepository.findById(40L)).thenReturn(Optional.of(request));
        when(teamManagerRepository.existsByManager_UsernameAndTeam_Id("manager1", 1L)).thenReturn(true);

        assertResponseStatus(
                () -> swapRequestService.approveByManager("manager1", 40L),
                HttpStatus.CONFLICT
        );

        assertThat(request.getSourceAssignment().getEmployee()).isSameAs(requester);
        verifyNoInteractions(swapRequestExecutor);
    }

    @Test
    void rejectByTargetEmployeeRejectsPendingRequest() {
        User requester = employee("employee1", 2L);
        User targetEmployee = employee("employee2", 3L);
        SwapRequest request = transferRequest(
                requester,
                targetEmployee,
                schedule(ScheduleStatus.PUBLISHED, SwapApprovalPolicy.MANAGER)
        );

        when(userRepository.findByUsername("employee2")).thenReturn(Optional.of(targetEmployee));
        when(swapRequestRepository.findById(40L)).thenReturn(Optional.of(request));

        SwapRequestResponse response = swapRequestService.rejectByTargetEmployee("employee2", 40L);

        assertThat(response.status()).isEqualTo(SwapRequestStatus.REJECTED);
        assertThat(response.updatedAt()).isNotEqualTo(response.createdAt());
        assertThat(request.getSourceAssignment().getEmployee()).isSameAs(requester);
        verifyNoInteractions(swapRequestExecutor);
    }

    @Test
    void rejectByTargetEmployeeRejectsEmployeeWhoIsNotTarget() {
        User requester = employee("employee1", 2L);
        User targetEmployee = employee("employee2", 3L);
        User otherEmployee = employee("employee3", 4L);
        SwapRequest request = transferRequest(
                requester,
                targetEmployee,
                schedule(ScheduleStatus.PUBLISHED, SwapApprovalPolicy.MANAGER)
        );

        when(userRepository.findByUsername("employee3")).thenReturn(Optional.of(otherEmployee));
        when(swapRequestRepository.findById(40L)).thenReturn(Optional.of(request));

        assertResponseStatus(
                () -> swapRequestService.rejectByTargetEmployee("employee3", 40L),
                HttpStatus.NOT_FOUND
        );

        assertThat(request.getStatus()).isEqualTo(SwapRequestStatus.PENDING_EMPLOYEE);
        verifyNoInteractions(swapRequestExecutor);
    }

    @Test
    void rejectByTargetEmployeeRejectsRequestThatIsNotPendingEmployeeApproval() {
        User requester = employee("employee1", 2L);
        User targetEmployee = employee("employee2", 3L);
        SwapRequest request = transferRequest(
                requester,
                targetEmployee,
                schedule(ScheduleStatus.PUBLISHED, SwapApprovalPolicy.MANAGER)
        );
        request.approveByTargetEmployee(Instant.parse("2026-08-04T19:00:00Z"), SwapApprovalPolicy.MANAGER);

        when(userRepository.findByUsername("employee2")).thenReturn(Optional.of(targetEmployee));
        when(swapRequestRepository.findById(40L)).thenReturn(Optional.of(request));

        assertResponseStatus(
                () -> swapRequestService.rejectByTargetEmployee("employee2", 40L),
                HttpStatus.CONFLICT
        );

        assertThat(request.getStatus()).isEqualTo(SwapRequestStatus.PENDING_MANAGER);
        verifyNoInteractions(swapRequestExecutor);
    }

    @Test
    void cancelByRequesterCancelsPendingEmployeeRequest() {
        User requester = employee("employee1", 2L);
        User targetEmployee = employee("employee2", 3L);
        SwapRequest request = transferRequest(
                requester,
                targetEmployee,
                schedule(ScheduleStatus.PUBLISHED, SwapApprovalPolicy.MANAGER)
        );

        when(userRepository.findByUsername("employee1")).thenReturn(Optional.of(requester));
        when(swapRequestRepository.findById(40L)).thenReturn(Optional.of(request));

        SwapRequestResponse response = swapRequestService.cancelByRequester("employee1", 40L);

        assertThat(response.status()).isEqualTo(SwapRequestStatus.CANCELLED);
        assertThat(response.updatedAt()).isNotEqualTo(response.createdAt());
        assertThat(request.getSourceAssignment().getEmployee()).isSameAs(requester);
        verifyNoInteractions(swapRequestExecutor);
    }

    @Test
    void cancelByRequesterCancelsPendingManagerRequest() {
        User requester = employee("employee1", 2L);
        User targetEmployee = employee("employee2", 3L);
        SwapRequest request = transferRequest(
                requester,
                targetEmployee,
                schedule(ScheduleStatus.PUBLISHED, SwapApprovalPolicy.MANAGER)
        );
        request.approveByTargetEmployee(Instant.parse("2026-08-04T19:00:00Z"), SwapApprovalPolicy.MANAGER);

        when(userRepository.findByUsername("employee1")).thenReturn(Optional.of(requester));
        when(swapRequestRepository.findById(40L)).thenReturn(Optional.of(request));

        SwapRequestResponse response = swapRequestService.cancelByRequester("employee1", 40L);

        assertThat(response.status()).isEqualTo(SwapRequestStatus.CANCELLED);
        assertThat(response.updatedAt()).isNotEqualTo(response.createdAt());
        assertThat(request.getSourceAssignment().getEmployee()).isSameAs(requester);
        verifyNoInteractions(swapRequestExecutor);
    }

    @Test
    void cancelByRequesterRejectsEmployeeWhoIsNotRequester() {
        User requester = employee("employee1", 2L);
        User targetEmployee = employee("employee2", 3L);
        SwapRequest request = transferRequest(
                requester,
                targetEmployee,
                schedule(ScheduleStatus.PUBLISHED, SwapApprovalPolicy.MANAGER)
        );

        when(userRepository.findByUsername("employee2")).thenReturn(Optional.of(targetEmployee));
        when(swapRequestRepository.findById(40L)).thenReturn(Optional.of(request));

        assertResponseStatus(
                () -> swapRequestService.cancelByRequester("employee2", 40L),
                HttpStatus.NOT_FOUND
        );

        assertThat(request.getStatus()).isEqualTo(SwapRequestStatus.PENDING_EMPLOYEE);
        verifyNoInteractions(swapRequestExecutor);
    }

    @Test
    void cancelByRequesterRejectsCompletedRequest() {
        User requester = employee("employee1", 2L);
        User targetEmployee = employee("employee2", 3L);
        SwapRequest request = transferRequest(
                requester,
                targetEmployee,
                schedule(ScheduleStatus.PUBLISHED, SwapApprovalPolicy.EMPLOYEE)
        );
        request.approveByTargetEmployee(Instant.parse("2026-08-04T19:00:00Z"), SwapApprovalPolicy.EMPLOYEE);

        when(userRepository.findByUsername("employee1")).thenReturn(Optional.of(requester));
        when(swapRequestRepository.findById(40L)).thenReturn(Optional.of(request));

        assertResponseStatus(
                () -> swapRequestService.cancelByRequester("employee1", 40L),
                HttpStatus.CONFLICT
        );

        assertThat(request.getStatus()).isEqualTo(SwapRequestStatus.APPROVED);
        verifyNoInteractions(swapRequestExecutor);
    }

    private void assertResponseStatus(Runnable action, HttpStatus expectedStatus) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(expectedStatus));
    }

    private Assignment assignment(Schedule schedule, User employee, Long id) {
        return assignment(schedule, employee, id, 20L);
    }

    private Assignment assignment(Schedule schedule, User employee, Long id, Long shiftId) {
        return assignment(
                schedule,
                employee,
                id,
                shiftId,
                Instant.parse("2026-08-04T06:00:00Z"),
                Instant.parse("2026-08-04T14:00:00Z"),
                "Morning shift"
        );
    }

    private Assignment assignment(
            Schedule schedule,
            User employee,
            Long id,
            Long shiftId,
            Instant startTime,
            Instant endTime,
            String description
    ) {
        Shift shift = new Shift(
                schedule,
                startTime,
                endTime,
                description,
                1,
                8
        );
        ReflectionTestUtils.setField(shift, "id", shiftId);

        Assignment assignment = new Assignment(shift, employee, Instant.parse("2026-08-03T10:00:00Z"));
        ReflectionTestUtils.setField(assignment, "id", id);
        return assignment;
    }

    private Schedule schedule(ScheduleStatus status) {
        return schedule(status, SwapApprovalPolicy.MANAGER);
    }

    private Schedule schedule(ScheduleStatus status, SwapApprovalPolicy approvalPolicy) {
        return schedule(status, approvalPolicy, 1L);
    }

    private Schedule schedule(ScheduleStatus status, SwapApprovalPolicy approvalPolicy, Long teamId) {
        Team team = new Team("Operations", approvalPolicy, 8, "Asia/Jerusalem");
        ReflectionTestUtils.setField(team, "id", teamId);

        Schedule schedule = new Schedule(team, LocalDate.of(2026, 8, 4), LocalDate.of(2026, 8, 10));
        ReflectionTestUtils.setField(schedule, "id", 10L);
        ReflectionTestUtils.setField(schedule, "status", status);
        return schedule;
    }

    private SwapRequest transferRequest(User requester, User targetEmployee, Schedule schedule) {
        SwapRequest request = SwapRequest.createTransfer(
                requester,
                assignment(schedule, requester, 30L),
                targetEmployee,
                Instant.parse("2026-08-04T18:00:00Z")
        );
        ReflectionTestUtils.setField(request, "id", 40L);
        return request;
    }

    private SwapRequest swapRequest(User requester, User targetEmployee, Schedule schedule) {
        SwapRequest request = SwapRequest.createSwap(
                requester,
                assignment(schedule, requester, 30L, 20L),
                assignment(schedule, targetEmployee, 31L, 21L),
                Instant.parse("2026-08-04T18:00:00Z")
        );
        ReflectionTestUtils.setField(request, "id", 40L);
        return request;
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
