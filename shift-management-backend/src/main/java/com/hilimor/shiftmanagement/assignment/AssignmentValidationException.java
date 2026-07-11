package com.hilimor.shiftmanagement.assignment;

import org.springframework.http.HttpStatus;

public class AssignmentValidationException extends RuntimeException {

    private final String code;
    private final HttpStatus status;

    public AssignmentValidationException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
