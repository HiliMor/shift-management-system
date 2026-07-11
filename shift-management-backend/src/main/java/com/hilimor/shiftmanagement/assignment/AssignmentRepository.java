package com.hilimor.shiftmanagement.assignment;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {

    boolean existsByShift_IdAndEmployee_Id(Long shiftId, Long employeeId);

    long countByShift_Id(Long shiftId);

    List<Assignment> findByEmployee_IdAndShift_StartTimeLessThanAndShift_EndTimeGreaterThan(
            Long employeeId,
            Instant endTime,
            Instant startTime
    );

    Optional<Assignment> findTopByEmployee_IdAndShift_EndTimeLessThanEqualOrderByShift_EndTimeDesc(
            Long employeeId,
            Instant startTime
    );

    Optional<Assignment> findTopByEmployee_IdAndShift_StartTimeGreaterThanEqualOrderByShift_StartTimeAsc(
            Long employeeId,
            Instant endTime
    );
}
