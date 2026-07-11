package com.hilimor.shiftmanagement.assignment;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = AssignmentController.class)
public class AssignmentExceptionHandler {

    @ExceptionHandler(AssignmentValidationException.class)
    ResponseEntity<AssignmentErrorResponse> handleAssignmentValidation(AssignmentValidationException exception) {
        AssignmentErrorResponse response = new AssignmentErrorResponse(exception.getCode(), exception.getMessage());
        return ResponseEntity.status(exception.getStatus()).body(response);
    }
}
