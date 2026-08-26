package com.hilimor.shiftmanagement.assignment;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AssignmentController {

    private final AssignmentService assignmentService;

    public AssignmentController(AssignmentService assignmentService) {
        this.assignmentService = assignmentService;
    }

    @PostMapping("/api/assignments")
    @ResponseStatus(HttpStatus.CREATED)
    public AssignmentResponse createAssignment(
            Authentication authentication,
            @Valid @RequestBody CreateAssignmentRequest request
    ) {
        return assignmentService.createAssignment(authentication.getName(), request);
    }

    @GetMapping("/api/schedules/{scheduleId}/assignments")
    public List<AssignmentResponse> listScheduleAssignments(
            Authentication authentication,
            @PathVariable Long scheduleId
    ) {
        return assignmentService.listScheduleAssignments(authentication.getName(), scheduleId);
    }

    @PostMapping("/api/schedules/{scheduleId}/auto-assign")
    public AutoAssignmentReportResponse autoAssignSchedule(
            Authentication authentication,
            @PathVariable Long scheduleId
    ) {
        return assignmentService.autoAssignSchedule(authentication.getName(), scheduleId);
    }

    @DeleteMapping("/api/assignments/{assignmentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAssignment(
            Authentication authentication,
            @PathVariable Long assignmentId
    ) {
        assignmentService.deleteAssignment(authentication.getName(), assignmentId);
    }
}
