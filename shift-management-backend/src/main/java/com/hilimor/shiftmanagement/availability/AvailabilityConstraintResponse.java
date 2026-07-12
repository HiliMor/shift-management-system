package com.hilimor.shiftmanagement.availability;

import java.time.Instant;

public record AvailabilityConstraintResponse(
        Long id,
        Long employeeId,
        Instant startTime,
        Instant endTime,
        String reason,
        Instant createdAt
) {

    static AvailabilityConstraintResponse from(AvailabilityConstraint constraint) {
        return new AvailabilityConstraintResponse(
                constraint.getId(),
                constraint.getEmployee().getId(),
                constraint.getStartTime(),
                constraint.getEndTime(),
                constraint.getReason(),
                constraint.getCreatedAt()
        );
    }
}
