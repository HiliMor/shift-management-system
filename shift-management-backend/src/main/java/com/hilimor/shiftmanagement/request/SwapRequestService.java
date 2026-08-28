package com.hilimor.shiftmanagement.request;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

import com.hilimor.shiftmanagement.assignment.Assignment;
import com.hilimor.shiftmanagement.assignment.AssignmentRepository;
import com.hilimor.shiftmanagement.schedule.Schedule;
import com.hilimor.shiftmanagement.schedule.ScheduleStatus;
import com.hilimor.shiftmanagement.team.TeamManagerRepository;
import com.hilimor.shiftmanagement.team.TeamMemberRepository;
import com.hilimor.shiftmanagement.user.ApplicationRole;
import com.hilimor.shiftmanagement.user.User;
import com.hilimor.shiftmanagement.user.UserRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SwapRequestService {

    private static final Logger log = LoggerFactory.getLogger(SwapRequestService.class);

    private static final List<SwapRequestStatus> ACTIVE_REQUEST_STATUSES = List.of(
            SwapRequestStatus.PENDING_EMPLOYEE,
            SwapRequestStatus.PENDING_MANAGER
    );

    private final SwapRequestRepository swapRequestRepository;
    private final AssignmentRepository assignmentRepository;
    private final SwapRequestExecutor swapRequestExecutor;
    private final UserRepository userRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final TeamManagerRepository teamManagerRepository;

    public SwapRequestService(
            SwapRequestRepository swapRequestRepository,
            AssignmentRepository assignmentRepository,
            SwapRequestExecutor swapRequestExecutor,
            UserRepository userRepository,
            TeamMemberRepository teamMemberRepository,
            TeamManagerRepository teamManagerRepository
    ) {
        this.swapRequestRepository = swapRequestRepository;
        this.assignmentRepository = assignmentRepository;
        this.swapRequestExecutor = swapRequestExecutor;
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
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Cannot transfer an assignment to the same employee"
            );
        }

        Long teamId = schedule.getTeam().getId();
        if (!teamMemberRepository.existsByUser_IdAndTeam_IdAndActiveTrue(targetEmployee.getId(), teamId)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Target employee must be an active member of the shift team"
            );
        }

        if (hasActiveRequestForAssignment(sourceAssignment.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "An active request already exists for this assignment"
            );
        }

        SwapRequest swapRequest = SwapRequest.createTransfer(
                requester,
                sourceAssignment,
                targetEmployee,
                Instant.now()
        );
        SwapRequest savedRequest = swapRequestRepository.save(swapRequest);
        log.info(
                "Transfer request {} created for assignment {} from employee {} to employee {}",
                savedRequest.getId(),
                sourceAssignment.getId(),
                requester.getId(),
                targetEmployee.getId()
        );

        return SwapRequestResponse.from(savedRequest);
    }

    @Transactional
    public SwapRequestResponse createSwapRequest(String username, CreateSwapRequest request) {
        User requester = currentUser(username);
        requireEmployee(requester, "Only employees can create swap requests");

        Assignment sourceAssignment = assignmentRepository.findById(request.sourceAssignmentId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Source assignment not found"));

        if (!Objects.equals(sourceAssignment.getEmployee().getId(), requester.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Source assignment not found");
        }

        Assignment targetAssignment = assignmentRepository.findById(request.targetAssignmentId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Target assignment not found"));

        if (Objects.equals(sourceAssignment.getId(), targetAssignment.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot swap an assignment with itself");
        }

        User targetEmployee = targetAssignment.getEmployee();
        requireEmployee(targetEmployee, "Target assignment must belong to an employee");

        if (Objects.equals(targetEmployee.getId(), requester.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Cannot swap assignments owned by the same employee"
            );
        }

        Schedule sourceSchedule = sourceAssignment.getShift().getSchedule();
        Schedule targetSchedule = targetAssignment.getShift().getSchedule();
        if (sourceSchedule.getStatus() != ScheduleStatus.PUBLISHED
                || targetSchedule.getStatus() != ScheduleStatus.PUBLISHED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Swap requests can be created only for published schedules"
            );
        }

        Long sourceTeamId = sourceSchedule.getTeam().getId();
        Long targetTeamId = targetSchedule.getTeam().getId();
        if (!Objects.equals(sourceTeamId, targetTeamId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Swap assignments must belong to the same team");
        }

        if (!teamMemberRepository.existsByUser_IdAndTeam_IdAndActiveTrue(targetEmployee.getId(), sourceTeamId)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Target employee must be an active member of the shift team"
            );
        }

        if (hasActiveRequestForAssignment(sourceAssignment.getId())
                || hasActiveRequestForAssignment(targetAssignment.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "An active request already exists for one of these assignments"
            );
        }

        SwapRequest swapRequest = SwapRequest.createSwap(
                requester,
                sourceAssignment,
                targetAssignment,
                Instant.now()
        );
        SwapRequest savedRequest = swapRequestRepository.save(swapRequest);
        log.info(
                "Swap request {} created between assignments {} and {} by employee {}",
                savedRequest.getId(),
                sourceAssignment.getId(),
                targetAssignment.getId(),
                requester.getId()
        );

        return SwapRequestResponse.from(savedRequest);
    }

    @Transactional(readOnly = true)
    public List<SwapRequestResponse> listMyOutgoingRequests(String username) {
        User requester = currentUser(username);
        requireEmployee(requester, "Only employees can view outgoing requests");

        return swapRequestRepository.findByRequester_UsernameOrderByCreatedAtDesc(username)
                .stream()
                .map(SwapRequestResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SwapRequestResponse> listMyIncomingRequests(String username) {
        User targetEmployee = currentUser(username);
        requireEmployee(targetEmployee, "Only employees can view incoming requests");

        return swapRequestRepository.findByTargetEmployee_UsernameOrderByCreatedAtDesc(username)
                .stream()
                .map(SwapRequestResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SwapRequestResponse> listPendingManagerRequests(String username) {
        User manager = currentUser(username);
        requireManager(manager, "Only managers can view pending manager requests");

        List<Long> teamIds = teamManagerRepository.findByManager_Username(username)
                .stream()
                .map(teamManager -> teamManager.getTeam().getId())
                .toList();

        if (teamIds.isEmpty()) {
            return List.of();
        }

        return swapRequestRepository.findByStatusAndSourceAssignment_Shift_Schedule_Team_IdInOrderByCreatedAtDesc(
                SwapRequestStatus.PENDING_MANAGER,
                teamIds
        )
                .stream()
                .map(SwapRequestResponse::from)
                .toList();
    }

    @Transactional
    public SwapRequestResponse approveByTargetEmployee(String username, Long requestId) {
        User targetEmployee = currentUser(username);
        requireEmployee(targetEmployee, "Only employees can approve incoming requests");

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

        swapRequestExecutor.executeIfReady(request, approvedAt);
        log.info(
                "{} request {} approved by target employee {}; status is {}",
                request.getType(),
                request.getId(),
                targetEmployee.getId(),
                request.getStatus()
        );

        return SwapRequestResponse.from(request);
    }

    @Transactional
    public SwapRequestResponse approveByManager(String username, Long requestId) {
        User manager = currentUser(username);
        requireManager(manager, "Only managers can approve requests");

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

        swapRequestExecutor.executeIfReady(request, approvedAt);
        log.info(
                "{} request {} approved by manager {}; status is {}",
                request.getType(),
                request.getId(),
                manager.getId(),
                request.getStatus()
        );

        return SwapRequestResponse.from(request);
    }

    @Transactional
    public SwapRequestResponse rejectByTargetEmployee(String username, Long requestId) {
        User targetEmployee = currentUser(username);
        requireEmployee(targetEmployee, "Only employees can reject incoming requests");

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
        log.info(
                "{} request {} rejected by target employee {}",
                request.getType(),
                request.getId(),
                targetEmployee.getId()
        );

        return SwapRequestResponse.from(request);
    }

    @Transactional
    public SwapRequestResponse cancelByRequester(String username, Long requestId) {
        User requester = currentUser(username);
        requireEmployee(requester, "Only employees can cancel requests");

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
        log.info("{} request {} cancelled by requester {}", request.getType(), request.getId(), requester.getId());

        return SwapRequestResponse.from(request);
    }

    private boolean hasActiveRequestForAssignment(Long assignmentId) {
        return swapRequestRepository.existsBySourceAssignment_IdAndStatusIn(
                assignmentId,
                ACTIVE_REQUEST_STATUSES
        ) || swapRequestRepository.existsByTargetAssignment_IdAndStatusIn(
                assignmentId,
                ACTIVE_REQUEST_STATUSES
        );
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
