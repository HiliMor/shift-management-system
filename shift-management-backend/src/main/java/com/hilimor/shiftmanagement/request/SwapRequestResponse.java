package com.hilimor.shiftmanagement.request;

import java.time.Instant;

public record SwapRequestResponse(
        Long id,
        SwapRequestType type,
        SwapRequestStatus status,
        Long requesterId,
        String requesterUsername,
        String requesterFullName,
        Long sourceAssignmentId,
        Long sourceShiftId,
        String sourceShiftDescription,
        Instant sourceShiftStartTime,
        Instant sourceShiftEndTime,
        Long targetEmployeeId,
        String targetEmployeeUsername,
        String targetEmployeeFullName,
        Long targetAssignmentId,
        Long targetShiftId,
        String targetShiftDescription,
        Instant targetShiftStartTime,
        Instant targetShiftEndTime,
        Instant employeeApprovedAt,
        Long managerApprovedById,
        Instant managerApprovedAt,
        Instant createdAt,
        Instant updatedAt
) {

    static SwapRequestResponse from(SwapRequest request) {
        return new SwapRequestResponse(
                request.getId(),
                request.getType(),
                request.getStatus(),
                request.getRequester().getId(),
                request.getRequester().getUsername(),
                request.getRequester().getFullName(),
                request.getSourceAssignment().getId(),
                request.getSourceAssignment().getShift().getId(),
                request.getSourceAssignment().getShift().getDescription(),
                request.getSourceAssignment().getShift().getStartTime(),
                request.getSourceAssignment().getShift().getEndTime(),
                request.getTargetEmployee().getId(),
                request.getTargetEmployee().getUsername(),
                request.getTargetEmployee().getFullName(),
                request.getTargetAssignment() == null ? null : request.getTargetAssignment().getId(),
                request.getTargetAssignment() == null ? null : request.getTargetAssignment().getShift().getId(),
                request.getTargetAssignment() == null ? null : request.getTargetAssignment().getShift().getDescription(),
                request.getTargetAssignment() == null ? null : request.getTargetAssignment().getShift().getStartTime(),
                request.getTargetAssignment() == null ? null : request.getTargetAssignment().getShift().getEndTime(),
                request.getEmployeeApprovedAt(),
                request.getManagerApprovedBy() == null ? null : request.getManagerApprovedBy().getId(),
                request.getManagerApprovedAt(),
                request.getCreatedAt(),
                request.getUpdatedAt()
        );
    }
}
