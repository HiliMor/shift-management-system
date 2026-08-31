package com.hilimor.shiftmanagement.shift;

public record ShiftDeletionPreviewResponse(ShiftResponse shift, int assignmentCount, String revision) {
}
