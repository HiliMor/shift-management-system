package com.hilimor.shiftmanagement.error;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.stream.Stream;

import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.LockTimeoutException;
import jakarta.persistence.PessimisticLockException;

import com.hilimor.shiftmanagement.assignment.AssignmentValidationException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.core.MethodParameter;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.orm.jpa.JpaObjectRetrievalFailureException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @ParameterizedTest
    @MethodSource("missingRecords")
    void missingPersistenceRecordMapsTo404WithoutLeakingDetails(RuntimeException exception) throws Exception {
        var mvc = MockMvcBuilders.standaloneSetup(new MissingRecordController(exception))
                .setControllerAdvice(handler).build();
        mvc.perform(get("/missing-record"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("This record no longer exists. Reload the current data."));
    }

    static Stream<RuntimeException> missingRecords() {
        return Stream.of(new EntityNotFoundException("Internal entity ID"),
                new JpaObjectRetrievalFailureException(new EntityNotFoundException("Internal entity ID")));
    }

    @RestController
    static class MissingRecordController {
        private final RuntimeException exception;

        MissingRecordController(RuntimeException exception) {
            this.exception = exception;
        }

        @GetMapping("/missing-record")
        public void missingRecord() {
            throw exception;
        }
    }

    @ParameterizedTest
    @MethodSource("optimisticConflicts")
    void staleVersionReturnsConflictWithoutExposingPersistenceDetails(Exception exception) {
        ResponseEntity<ApiErrorResponse> response = handler.handleStaleVersion(exception,
                request("PUT", "/api/schedules/1/shifts/2"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("STALE_VERSION");
        assertThat(response.getBody().message())
                .isEqualTo("This record has changed. Reload it and review your changes before saving again.");
        assertThat(response.getBody().path()).isEqualTo("/api/schedules/1/shifts/2");
    }

    static Stream<Exception> optimisticConflicts() {
        return Stream.of(new ObjectOptimisticLockingFailureException("Internal entity", 2L),
                new OptimisticLockException("Internal persistence details"));
    }

    @ParameterizedTest
    @MethodSource("lockConflicts")
    void lockFailureReturnsConflictWithoutExposingDatabaseDetails(Exception exception) {
        ResponseEntity<ApiErrorResponse> response = handler.handleConcurrentModification(exception,
                request("DELETE", "/api/templates/2"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("CONCURRENT_MODIFICATION");
        assertThat(response.getBody().message()).isEqualTo("Another operation is using this data. Reload it and try again.");
        assertThat(response.getBody().path()).isEqualTo("/api/templates/2");
    }

    static Stream<Exception> lockConflicts() {
        return Stream.of(new CannotAcquireLockException("Internal SQL"),
                new PessimisticLockingFailureException("Internal SQL"),
                new PessimisticLockException("Internal entity"),
                new LockTimeoutException("Internal entity"));
    }

    @Test
    void handleAssignmentValidationReturnsBusinessCode() {
        AssignmentValidationException exception = new AssignmentValidationException(
                HttpStatus.CONFLICT,
                "SHIFT_OVERLAP",
                "Employee already has an overlapping assignment"
        );

        ResponseEntity<ApiErrorResponse> response = handler.handleAssignmentValidation(
                exception,
                request("POST", "/api/assignments")
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(409);
        assertThat(response.getBody().error()).isEqualTo("Conflict");
        assertThat(response.getBody().code()).isEqualTo("SHIFT_OVERLAP");
        assertThat(response.getBody().message()).isEqualTo("Employee already has an overlapping assignment");
        assertThat(response.getBody().path()).isEqualTo("/api/assignments");
        assertThat(response.getBody().timestamp()).isNotNull();
    }

    @Test
    void handleResponseStatusReturnsReasonAndStatusCodeName() {
        ResponseStatusException exception = new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Schedule not found"
        );

        ResponseEntity<ApiErrorResponse> response = handler.handleResponseStatus(
                exception,
                request("GET", "/api/schedules/99")
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(404);
        assertThat(response.getBody().error()).isEqualTo("Not Found");
        assertThat(response.getBody().code()).isEqualTo("NOT_FOUND");
        assertThat(response.getBody().message()).isEqualTo("Schedule not found");
        assertThat(response.getBody().path()).isEqualTo("/api/schedules/99");
    }

    @Test
    void handleMethodArgumentNotValidReturnsFieldMessages() throws NoSuchMethodException {
        Method method = getClass().getDeclaredMethod("validationTarget", ValidationFixture.class);
        MethodParameter methodParameter = new MethodParameter(method, 0);
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(
                new ValidationFixture(""),
                "request"
        );
        bindingResult.addError(new FieldError("request", "name", "must not be blank"));
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(
                methodParameter,
                bindingResult
        );

        ResponseEntity<ApiErrorResponse> response = handler.handleMethodArgumentNotValid(
                exception,
                request("POST", "/api/schedules")
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("VALIDATION_ERROR");
        assertThat(response.getBody().message()).isEqualTo("name: must not be blank");
        assertThat(response.getBody().path()).isEqualTo("/api/schedules");
    }

    @Test
    void handleHttpMessageNotReadableReturnsMalformedRequest() {
        HttpMessageNotReadableException exception = new HttpMessageNotReadableException(
                "Invalid JSON",
                new MockHttpInputMessage(new byte[0])
        );

        ResponseEntity<ApiErrorResponse> response = handler.handleHttpMessageNotReadable(
                exception,
                request("POST", "/api/auth/login")
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("MALFORMED_REQUEST");
        assertThat(response.getBody().message()).isEqualTo("Request body is missing or invalid");
    }

    @Test
    void handleUnexpectedReturnsGenericServerError() {
        RuntimeException exception = new RuntimeException("Sensitive internal detail");

        ResponseEntity<ApiErrorResponse> response = handler.handleUnexpected(
                exception,
                request("GET", "/api/schedules")
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("INTERNAL_SERVER_ERROR");
        assertThat(response.getBody().message()).isEqualTo("Unexpected server error");
    }

    private MockHttpServletRequest request(String method, String path) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setRequestURI(path);
        return request;
    }

    @SuppressWarnings("unused")
    private void validationTarget(ValidationFixture request) {
    }

    private record ValidationFixture(String name) {
    }
}
