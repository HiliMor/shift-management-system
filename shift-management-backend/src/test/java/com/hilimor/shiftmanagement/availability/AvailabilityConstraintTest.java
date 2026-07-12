package com.hilimor.shiftmanagement.availability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.hilimor.shiftmanagement.user.ApplicationRole;
import com.hilimor.shiftmanagement.user.User;

class AvailabilityConstraintTest {

    @Test
    void newAvailabilityConstraintStoresEmployeeTimeRangeAndReason() {
        User employee = employee();
        Instant startTime = Instant.parse("2026-07-13T06:00:00Z");
        Instant endTime = Instant.parse("2026-07-13T14:00:00Z");
        Instant createdAt = Instant.parse("2026-07-12T08:00:00Z");

        AvailabilityConstraint constraint = new AvailabilityConstraint(
                employee,
                startTime,
                endTime,
                "Doctor appointment",
                createdAt
        );

        assertThat(constraint.getEmployee()).isSameAs(employee);
        assertThat(constraint.getStartTime()).isEqualTo(startTime);
        assertThat(constraint.getEndTime()).isEqualTo(endTime);
        assertThat(constraint.getReason()).isEqualTo("Doctor appointment");
        assertThat(constraint.getCreatedAt()).isEqualTo(createdAt);
    }

    @Test
    void endTimeMustBeAfterStartTime() {
        Instant startTime = Instant.parse("2026-07-13T06:00:00Z");

        assertThatIllegalArgumentException()
                .isThrownBy(() -> new AvailabilityConstraint(
                        employee(),
                        startTime,
                        startTime,
                        "Invalid constraint",
                        Instant.parse("2026-07-12T08:00:00Z")));
    }

    @Test
    void employeeMustNotBeNull() {
        assertThatNullPointerException()
                .isThrownBy(() -> new AvailabilityConstraint(
                        null,
                        Instant.parse("2026-07-13T06:00:00Z"),
                        Instant.parse("2026-07-13T14:00:00Z"),
                        null,
                        Instant.parse("2026-07-12T08:00:00Z")));
    }

    private User employee() {
        return new User(
                "employee1",
                "password-hash",
                "Demo Employee",
                "employee1@example.com",
                ApplicationRole.EMPLOYEE
        );
    }
}
