package com.hilimor.shiftmanagement.assignment;

import com.hilimor.shiftmanagement.shift.ShiftResponse;

public record AssignmentDeletionPreviewResponse(AssignmentResponse assignment, ShiftResponse shift, String revision) {
}
