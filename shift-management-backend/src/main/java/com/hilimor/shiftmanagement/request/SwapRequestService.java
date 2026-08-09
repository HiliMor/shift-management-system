package com.hilimor.shiftmanagement.request;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

import com.hilimor.shiftmanagement.assignment.Assignment;
import com.hilimor.shiftmanagement.assignment.AssignmentRepository;
import com.hilimor.shiftmanagement.assignment.AssignmentService;
import com.hilimor.shiftmanagement.assignment.AssignmentValidationException;
import com.hilimor.shiftmanagement.schedule.Schedule;
import com.hilimor.shiftmanagement.schedule.ScheduleStatus;
import com.hilimor.shiftmanagement.team.TeamManagerRepository;
import com.hilimor.shiftmanagement.team.TeamMemberRepository;
import com.hilimor.shiftmanagement.user.ApplicationRole;
import com.hilimor.shiftmanagement.user.User;
import com.hilimor.shiftmanagement.user.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SwapRequestService {

    private static final List<SwapRequestStatus> ACTIVE_REQUEST_STATUSES = List.of(
            SwapRequestStatus.PENDING_EMPLOYEE,
            SwapRequestStatus.PENDING_MANAGER
    );

    private final SwapRequestRepository swapRequestRepository;
    private final AssignmentRepository assignmentRepository;
    private final AssignmentService assignmentService;
    private final UserRepository userRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final TeamManagerRepository teamManagerRepository;

    public SwapRequestService(
            SwapRequestRepository swapRequestRepository,
            AssignmentRepository assignmentRepository,
            AssignmentService assignmentService,
            UserRepository userRepository,
            TeamMemberRepository teamMemberRepository,
            TeamManagerRepository teamManagerRepository
    ) {
        this.swapRequestRepository = swapRequestRepository;
        this.assignmentRepository = assignmentRepository;
        this.assignmentService = assignmentService;
        this.userRepository = userRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.teamManagerRepository = teamManagerRepository;
    }

    @Transactional
    public SwapRequestResponse createTransferRequest(String username, CreateTransferRequest request) {
        User requester = currentUser(username);
        requireEmployee(requester, "Only employees can create transfer requests");

        Assignment sourceAssignment = assignmentRepository.findById(request.sourceAssignmentId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment not found"));

        if (!Objects.equals(sourceAssignment.getEmployee().getId(), requester.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment not found");
        }

        Schedule schedule = sourceAssignment.getShift().getSchedule();
        if (schedule.getStatus() != ScheduleStatus.PUBLISHED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Transfer requests can be created only for published schedules"
            );
        }

        User targetEmployee = userRepository.findById(request.targetEmployeeId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Target employee not found"));
        requireEmployee(targetEmployee, "Target user must be an employee");

        if (Objects.equals(targetEmployee.getId(), requester.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot transfer an assignment to the same employee");
        }

        Long teamId = schedule.getTeam().getId();
        if (!teamMemberRepository.existsByUser_IdAndTeam_IdAndActiveTrue(targetEmployee.getId(), teamId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Target employee must be an active member of the shift team");
        }

        if (swapRequestRepository.existsBySourceAssignment_IdAndStatusIn(
                sourceAssignment.getId(),
                ACTIVE_REQUEST_STATUSES
        )) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An active request already exists for this assignment");
        }

        SwapRequest swapRequest = SwapRequest.createTransfer(
                requester,
                sourceAssignment,
                targetEmployee,
                Instant.now()
        );
        return SwapRequestResponse.from(swapRequestRepository.save(swapRequest));
    }

    @Transactional
    public SwapRequestResponse approveByTargetEmployee(String username, Long requestId) {
        User targetEmployee = currentUser(username);
        requireEmployee(targetEmployee, "Only employees can approve incoming transfer requests");

        SwapRequest request = swapRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Request not found"));

        if (!Objects.equals(request.getTargetEmployee().getId(), targetEmployee.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Request not found");
        }

        Instant approvedAt = Instant.now();
        try {
            request.approveByTargetEmployee(
                    approvedAt,
                    request.getSourceAssignment().getShift().getSchedule().getTeam().getSwapApprovalPolicy()
            );
        } catch (IllegalStateException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, exception.getMessage(), exception);
        }

        executeApprovedTransferIfReady(request, approvedAt);

        return SwapRequestResponse.from(request);
    }

    @Transactional
    public SwapRequestResponse approveByManager(String username, Long requestId) {
        User manager = currentUser(username);
        requireManager(manager, "Only managers can approve transfer requests");

        SwapRequest request = swapRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Request not found"));

        Long teamId = request.getSourceAssignment().getShift().getSchedule().getTeam().getId();
        if (!teamManagerRepository.existsByManager_UsernameAndTeam_Id(username, teamId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only a manager of the source shift team can approve this request"
            );
        }

        Instant approvedAt = Instant.now();
        try {
            request.approveByManager(manager, approvedAt);
        } catch (IllegalStateException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, exception.getMessage(), exception);
        }

        executeApprovedTransferIfReady(request, approvedAt);

        return SwapRequestResponse.from(request);
    }

    @Transactional
    public SwapRequestResponse rejectByTargetEmployee(String username, Long requestId) {
        User targetEmployee = currentUser(username);
        requireEmployee(targetEmployee, "Only employees can reject incoming transfer requests");

        SwapRequest request = swapRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Request not found"));

        if (!Objects.equals(request.getTargetEmployee().getId(), targetEmployee.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Request not found");
        }

        try {
            request.rejectByTargetEmployee(Instant.now());
        } catch (IllegalStateException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, exception.getMessage(), exception);
        }

        return SwapRequestResponse.from(request);
    }

    @Transactional
    public SwapRequestResponse cancelByRequester(String username, Long requestId) {
        User requester = currentUser(username);
        requireEmployee(requester, "Only employees can cancel transfer requests");

        SwapRequest request = swapRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Request not found"));

        if (!Objects.equals(request.getRequester().getId(), requester.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Request not found");
        }

        try {
            request.cancelByRequester(Instant.now());
        } catch (IllegalStateException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, exception.getMessage(), exception);
        }

        return SwapRequestResponse.from(request);
    }

    private void executeApprovedTransferIfReady(SwapRequest request, Instant executedAt) {
        if (request.getType() != SwapRequestType.TRANSFER || request.getStatus() != SwapRequestStatus.APPROVED) {
            return;
        }

        Assignment sourceAssignment = request.getSourceAssignment();
        User targetEmployee = request.getTargetEmployee();

        try {
            assignmentService.validateEmployeeCanReceiveTransferredAssignment(sourceAssignment.getShift(), targetEmployee);
        } catch (AssignmentValidationException exception) {
            request.invalidate(executedAt);
            return;
        }

        sourceAssignment.transferTo(targetEmployee, executedAt);
    }

    private User currentUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Authenticated user not found"));
    }

    private void requireEmployee(User user, String message) {
        if (user.getApplicationRole() != ApplicationRole.EMPLOYEE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, message);
        }
    }

    private void requireManager(User user, String message) {
        if (user.getApplicationRole() != ApplicationRole.MANAGER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, message);
        }
    }
}
