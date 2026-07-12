package com.hilimor.shiftmanagement.availability;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AvailabilityConstraintRepository extends JpaRepository<AvailabilityConstraint, Long> {

    List<AvailabilityConstraint> findByEmployee_IdOrderByStartTime(Long employeeId);

    List<AvailabilityConstraint> findByEmployee_IdAndStartTimeLessThanAndEndTimeGreaterThan(
            Long employeeId,
            Instant endTime,
            Instant startTime
    );
}
