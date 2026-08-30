package com.hilimor.shiftmanagement.request;

import java.time.Instant;

import com.hilimor.shiftmanagement.assignment.Assignment;
import com.hilimor.shiftmanagement.schedule.Schedule;
import com.hilimor.shiftmanagement.shift.Shift;

public record SwapRequestCreatedEvent(
        Long requestId,
        SwapRequestType requestType,
        Long teamId,
        String teamName,
        Long requesterId,
        String requesterFullName,
        Long targetEmployeeId,
        String targetEmployeeFullName,
        Long sourceAssignmentId,
        Long sourceShiftId,
        String sourceShiftDescription,
        Instant sourceShiftStartTime,
        Instant sourceShiftEndTime,
        Long targetAssignmentId,
        Long targetShiftId,
        String targetShiftDescription,
        Instant targetShiftStartTime,
        Instant targetShiftEndTime,
        Instant createdAt
) {

    static SwapRequestCreatedEvent from(SwapRequest request) {
        Assignment sourceAssignment = request.getSourceAssignment();
        Shift sourceShift = sourceAssignment.getShift();
        Schedule schedule = sourceShift.getSchedule();
        Assignment targetAssignment = request.getTargetAssignment();
        Shift targetShift = targetAssignment == null ? null : targetAssignment.getShift();

        return new SwapRequestCreatedEvent(
                request.getId(),
                request.getType(),
                schedule.getTeam().getId(),
                schedule.getTeam().getName(),
                request.getRequester().getId(),
                request.getRequester().getFullName(),
                request.getTargetEmployee().getId(),
                request.getTargetEmployee().getFullName(),
                sourceAssignment.getId(),
                sourceShift.getId(),
                sourceShift.getDescription(),
                sourceShift.getStartTime(),
                sourceShift.getEndTime(),
                targetAssignment == null ? null : targetAssignment.getId(),
                targetShift == null ? null : targetShift.getId(),
                targetShift == null ? null : targetShift.getDescription(),
                targetShift == null ? null : targetShift.getStartTime(),
                targetShift == null ? null : targetShift.getEndTime(),
                request.getCreatedAt()
        );
    }
}
