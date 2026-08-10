package com.hilimor.shiftmanagement.error;

import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

public record ApiErrorResponse(
        int status,
        String error,
        String code,
        String message,
        String path,
        Instant timestamp
) {

    public static ApiErrorResponse of(HttpStatusCode statusCode, String code, String message, String path) {
        HttpStatus httpStatus = HttpStatus.resolve(statusCode.value());
        String error = httpStatus == null ? "HTTP " + statusCode.value() : httpStatus.getReasonPhrase();
        return new ApiErrorResponse(statusCode.value(), error, code, message, path, Instant.now());
    }
}
