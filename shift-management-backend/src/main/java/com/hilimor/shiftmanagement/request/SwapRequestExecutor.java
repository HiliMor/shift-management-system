package com.hilimor.shiftmanagement.request;

import java.time.Instant;
import java.util.Objects;

import com.hilimor.shiftmanagement.assignment.Assignment;
import com.hilimor.shiftmanagement.assignment.AssignmentService;
import com.hilimor.shiftmanagement.assignment.AssignmentValidationException;
import com.hilimor.shiftmanagement.schedule.ScheduleStatus;
import com.hilimor.shiftmanagement.user.User;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class SwapRequestExecutor {

    private static final Logger log = LoggerFactory.getLogger(SwapRequestExecutor.class);

    private final AssignmentService assignmentService;

    public SwapRequestExecutor(AssignmentService assignmentService) {
        this.assignmentService = assignmentService;
    }

    @Transactional
    public void executeIfReady(SwapRequest request, Instant executedAt) {
        if (request.getStatus() != SwapRequestStatus.APPROVED) {
            return;
        }

        if (request.getType() == SwapRequestType.TRANSFER) {
            executeTransfer(request, executedAt);
            return;
        }

        executeSwap(request, executedAt);
    }

    private void executeTransfer(SwapRequest request, Instant executedAt) {
        Assignment sourceAssignment = request.getSourceAssignment();
        User targetEmployee = request.getTargetEmployee();

        if (!Objects.equals(sourceAssignment.getEmployee().getId(), request.getRequester().getId())) {
            invalidateRequest(request, executedAt, "Source assignment owner changed before transfer execution");
            return;
        }

        if (sourceAssignment.getShift().getSchedule().getStatus() != ScheduleStatus.PUBLISHED) {
            invalidateRequest(request, executedAt, "Schedule is no longer published before transfer execution");
            return;
        }

        try {
            assignmentService.validateEmployeeCanReceiveTransferredAssignment(
                    sourceAssignment.getShift(),
                    targetEmployee
            );
        } catch (AssignmentValidationException exception) {
            request.invalidate(executedAt);
            log.warn(
                    "Transfer request {} invalidated before assignment transfer; assignment={}, targetEmployee={}, reason={}",
                    request.getId(),
                    sourceAssignment.getId(),
                    targetEmployee.getId(),
                    exception.getCode()
            );
            return;
        }

        Long previousEmployeeId = sourceAssignment.getEmployee().getId();
        sourceAssignment.transferTo(targetEmployee, executedAt);
        log.info(
                "Assignment {} transferred from employee {} to employee {} through transfer request {}",
                sourceAssignment.getId(),
                previousEmployeeId,
                targetEmployee.getId(),
                request.getId()
        );
    }

    private void executeSwap(SwapRequest request, Instant executedAt) {
        Assignment sourceAssignment = request.getSourceAssignment();
        Assignment targetAssignment = request.getTargetAssignment();
        User requester = request.getRequester();
        User targetEmployee = request.getTargetEmployee();

        if (targetAssignment == null) {
            invalidateRequest(request, executedAt, "Swap target assignment is missing before execution");
            return;
        }

        if (!Objects.equals(sourceAssignment.getEmployee().getId(), requester.getId())
                || !Objects.equals(targetAssignment.getEmployee().getId(), targetEmployee.getId())) {
            invalidateRequest(request, executedAt, "Assignment owner changed before swap execution");
            return;
        }

        if (sourceAssignment.getShift().getSchedule().getStatus() != ScheduleStatus.PUBLISHED
                || targetAssignment.getShift().getSchedule().getStatus() != ScheduleStatus.PUBLISHED) {
            invalidateRequest(request, executedAt, "Schedule is no longer published before swap execution");
            return;
        }

        try {
            assignmentService.validateEmployeeCanReceiveSwappedAssignment(
                    sourceAssignment.getShift(),
                    targetEmployee,
                    targetAssignment
            );
            assignmentService.validateEmployeeCanReceiveSwappedAssignment(
                    targetAssignment.getShift(),
                    requester,
                    sourceAssignment
            );
        } catch (AssignmentValidationException exception) {
            request.invalidate(executedAt);
            log.warn(
                    "Swap request {} invalidated before assignment exchange; sourceAssignment={}, targetAssignment={}, reason={}",
                    request.getId(),
                    sourceAssignment.getId(),
                    targetAssignment.getId(),
                    exception.getCode()
            );
            return;
        }

        sourceAssignment.transferTo(targetEmployee, executedAt);
        targetAssignment.transferTo(requester, executedAt);
        log.info(
                "Assignments {} and {} swapped between employees {} and {} through request {}",
                sourceAssignment.getId(),
                targetAssignment.getId(),
                requester.getId(),
                targetEmployee.getId(),
                request.getId()
        );
    }

    private void invalidateRequest(SwapRequest request, Instant invalidatedAt, String reason) {
        request.invalidate(invalidatedAt);
        log.warn("{} request {} invalidated: {}", request.getType(), request.getId(), reason);
    }
}
