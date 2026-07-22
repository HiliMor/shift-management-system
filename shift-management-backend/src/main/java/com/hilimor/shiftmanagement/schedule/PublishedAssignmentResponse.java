package com.hilimor.shiftmanagement.schedule;

import java.time.Instant;

import com.hilimor.shiftmanagement.assignment.Assignment;
import com.hilimor.shiftmanagement.user.User;

public record PublishedAssignmentResponse(
        Long id,
        Long shiftId,
        Long employeeId,
        String employeeUsername,
        String employeeFullName,
        Instant assignedAt
) {

    static PublishedAssignmentResponse from(Assignment assignment) {
        User employee = assignment.getEmployee();

        return new PublishedAssignmentResponse(
                assignment.getId(),
                assignment.getShift().getId(),
                employee.getId(),
                employee.getUsername(),
                employee.getFullName(),
                assignment.getAssignedAt()
        );
    }
}
