package com.hilimor.shiftmanagement.assignment;

import java.time.Instant;

import com.hilimor.shiftmanagement.shift.Shift;
import com.hilimor.shiftmanagement.user.User;

public record AssignmentResponse(
        Long id,
        Long shiftId,
        Long employeeId,
        String employeeUsername,
        String employeeFullName,
        Instant assignedAt
) {

    static AssignmentResponse from(Assignment assignment) {
        Shift shift = assignment.getShift();
        User employee = assignment.getEmployee();

        return new AssignmentResponse(
                assignment.getId(),
                shift.getId(),
                employee.getId(),
                employee.getUsername(),
                employee.getFullName(),
                assignment.getAssignedAt()
        );
    }
}
