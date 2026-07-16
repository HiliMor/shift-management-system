package com.hilimor.shiftmanagement.availability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.hilimor.shiftmanagement.assignment.Assignment;
import com.hilimor.shiftmanagement.assignment.AssignmentRepository;
import com.hilimor.shiftmanagement.user.ApplicationRole;
import com.hilimor.shiftmanagement.user.User;
import com.hilimor.shiftmanagement.user.UserRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class AvailabilityConstraintServiceTest {

    @Mock
    private AvailabilityConstraintRepository availabilityConstraintRepository;

    @Mock
    private AssignmentRepository assignmentRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AvailabilityConstraintService availabilityConstraintService;

    @Test
    void createConstraintSavesConstraintForCurrentUser() {
        User employee = employee();
        CreateAvailabilityConstraintRequest request = validRequest();

        when(userRepository.findByUsername("employee1")).thenReturn(Optional.of(employee));
        when(assignmentRepository.findByEmployee_IdAndShift_StartTimeLessThanAndShift_EndTimeGreaterThan(
                2L,
                request.endTime(),
                request.startTime()
        )).thenReturn(List.of());
        when(availabilityConstraintRepository.save(any(AvailabilityConstraint.class))).thenAnswer(invocation -> {
            AvailabilityConstraint constraint = invocation.getArgument(0);
            ReflectionTestUtils.setField(constraint, "id", 40L);
            return constraint;
        });

        AvailabilityConstraintResponse response = availabilityConstraintService.createConstraint("employee1", request);

        assertThat(response.id()).isEqualTo(40L);
        assertThat(response.employeeId()).isEqualTo(2L);
        assertThat(response.startTime()).isEqualTo(Instant.parse("2026-07-13T06:00:00Z"));
        assertThat(response.endTime()).isEqualTo(Instant.parse("2026-07-13T14:00:00Z"));
        assertThat(response.reason()).isEqualTo("Doctor appointment");
        assertThat(response.createdAt()).isNotNull();

        ArgumentCaptor<AvailabilityConstraint> captor = ArgumentCaptor.forClass(AvailabilityConstraint.class);
        verify(availabilityConstraintRepository).save(captor.capture());
        assertThat(captor.getValue().getEmployee()).isSameAs(employee);
    }

    @Test
    void createConstraintAcceptsFullDayRange() {
        User employee = employee();
        CreateAvailabilityConstraintRequest request = new CreateAvailabilityConstraintRequest(
                Instant.parse("2026-07-20T00:00:00Z"),
                Instant.parse("2026-07-21T00:00:00Z"),
                "Full day unavailable"
        );

        when(userRepository.findByUsername("employee1")).thenReturn(Optional.of(employee));
        when(assignmentRepository.findByEmployee_IdAndShift_StartTimeLessThanAndShift_EndTimeGreaterThan(
                2L,
                request.endTime(),
                request.startTime()
        )).thenReturn(List.of());
        when(availabilityConstraintRepository.save(any(AvailabilityConstraint.class))).thenAnswer(invocation -> {
            AvailabilityConstraint constraint = invocation.getArgument(0);
            ReflectionTestUtils.setField(constraint, "id", 41L);
            return constraint;
        });

        AvailabilityConstraintResponse response = availabilityConstraintService.createConstraint("employee1", request);

        assertThat(response.id()).isEqualTo(41L);
        assertThat(response.startTime()).isEqualTo(Instant.parse("2026-07-20T00:00:00Z"));
        assertThat(response.endTime()).isEqualTo(Instant.parse("2026-07-21T00:00:00Z"));
        assertThat(response.reason()).isEqualTo("Full day unavailable");
    }

    @Test
    void createConstraintRejectsInvalidTimeRange() {
        CreateAvailabilityConstraintRequest request = new CreateAvailabilityConstraintRequest(
                Instant.parse("2026-07-13T14:00:00Z"),
                Instant.parse("2026-07-13T06:00:00Z"),
                "Invalid constraint"
        );

        assertThatThrownBy(() -> availabilityConstraintService.createConstraint("employee1", request))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));

        verify(availabilityConstraintRepository, never()).save(any());
    }

    @Test
    void createConstraintRejectsExistingAssignmentOverlap() {
        User employee = employee();
        CreateAvailabilityConstraintRequest request = validRequest();

        when(userRepository.findByUsername("employee1")).thenReturn(Optional.of(employee));
        when(assignmentRepository.findByEmployee_IdAndShift_StartTimeLessThanAndShift_EndTimeGreaterThan(
                2L,
                request.endTime(),
                request.startTime()
        )).thenReturn(List.of(mock(Assignment.class)));

        assertThatThrownBy(() -> availabilityConstraintService.createConstraint("employee1", request))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));

        verify(availabilityConstraintRepository, never()).save(any());
    }

    @Test
    void listMyConstraintsReturnsCurrentUsersConstraints() {
        User employee = employee();
        AvailabilityConstraint constraint = constraint(employee);

        when(userRepository.findByUsername("employee1")).thenReturn(Optional.of(employee));
        when(availabilityConstraintRepository.findByEmployee_IdOrderByStartTime(2L)).thenReturn(List.of(constraint));

        List<AvailabilityConstraintResponse> responses = availabilityConstraintService.listMyConstraints("employee1");

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).id()).isEqualTo(40L);
        assertThat(responses.get(0).employeeId()).isEqualTo(2L);
        assertThat(responses.get(0).reason()).isEqualTo("Doctor appointment");
    }

    @Test
    void deleteMyConstraintDeletesCurrentUsersConstraint() {
        User employee = employee();
        AvailabilityConstraint constraint = constraint(employee);

        when(userRepository.findByUsername("employee1")).thenReturn(Optional.of(employee));
        when(availabilityConstraintRepository.findById(40L)).thenReturn(Optional.of(constraint));

        availabilityConstraintService.deleteMyConstraint("employee1", 40L);

        verify(availabilityConstraintRepository).delete(constraint);
    }

    @Test
    void deleteMyConstraintRejectsMissingConstraint() {
        User employee = employee();

        when(userRepository.findByUsername("employee1")).thenReturn(Optional.of(employee));
        when(availabilityConstraintRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> availabilityConstraintService.deleteMyConstraint("employee1", 99L))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));

        verify(availabilityConstraintRepository, never()).delete(any());
    }

    @Test
    void deleteMyConstraintRejectsAnotherUsersConstraint() {
        User employee = employee();
        AvailabilityConstraint constraint = constraint(otherEmployee());

        when(userRepository.findByUsername("employee1")).thenReturn(Optional.of(employee));
        when(availabilityConstraintRepository.findById(40L)).thenReturn(Optional.of(constraint));

        assertThatThrownBy(() -> availabilityConstraintService.deleteMyConstraint("employee1", 40L))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));

        verify(availabilityConstraintRepository, never()).delete(any());
    }

    private CreateAvailabilityConstraintRequest validRequest() {
        return new CreateAvailabilityConstraintRequest(
                Instant.parse("2026-07-13T06:00:00Z"),
                Instant.parse("2026-07-13T14:00:00Z"),
                "Doctor appointment"
        );
    }

    private AvailabilityConstraint constraint(User employee) {
        AvailabilityConstraint constraint = new AvailabilityConstraint(
                employee,
                Instant.parse("2026-07-13T06:00:00Z"),
                Instant.parse("2026-07-13T14:00:00Z"),
                "Doctor appointment",
                Instant.parse("2026-07-12T08:00:00Z")
        );
        ReflectionTestUtils.setField(constraint, "id", 40L);
        return constraint;
    }

    private User employee() {
        User employee = new User(
                "employee1",
                "password-hash",
                "Demo Employee",
                "employee1@example.com",
                ApplicationRole.EMPLOYEE
        );
        ReflectionTestUtils.setField(employee, "id", 2L);
        return employee;
    }

    private User otherEmployee() {
        User employee = new User(
                "employee2",
                "password-hash",
                "Other Employee",
                "employee2@example.com",
                ApplicationRole.EMPLOYEE
        );
        ReflectionTestUtils.setField(employee, "id", 3L);
        return employee;
    }
}
