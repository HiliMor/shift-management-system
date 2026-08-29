package com.hilimor.shiftmanagement.assignment;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {

    boolean existsByShift_IdAndEmployee_Id(Long shiftId, Long employeeId);

    Optional<Assignment> findByShift_IdAndEmployee_Id(Long shiftId, Long employeeId);

    long countByShift_Id(Long shiftId);

    long deleteByShift_Schedule_Id(Long scheduleId);

    List<Assignment> findByShift_Schedule_IdOrderByShift_StartTimeAscEmployee_FullNameAsc(Long scheduleId);

    List<Assignment> findByEmployee_IdAndShift_StartTimeLessThanAndShift_EndTimeGreaterThan(
            Long employeeId,
            Instant endTime,
            Instant startTime
    );

    List<Assignment> findByEmployee_IdAndIdNotAndShift_StartTimeLessThanAndShift_EndTimeGreaterThan(
            Long employeeId,
            Long ignoredAssignmentId,
            Instant endTime,
            Instant startTime
    );

    Optional<Assignment> findTopByEmployee_IdAndShift_EndTimeLessThanEqualOrderByShift_EndTimeDesc(
            Long employeeId,
            Instant startTime
    );

    Optional<Assignment> findTopByEmployee_IdAndIdNotAndShift_EndTimeLessThanEqualOrderByShift_EndTimeDesc(
            Long employeeId,
            Long ignoredAssignmentId,
            Instant startTime
    );

    Optional<Assignment> findTopByEmployee_IdAndShift_StartTimeGreaterThanEqualOrderByShift_StartTimeAsc(
            Long employeeId,
            Instant endTime
    );

    Optional<Assignment> findTopByEmployee_IdAndIdNotAndShift_StartTimeGreaterThanEqualOrderByShift_StartTimeAsc(
            Long employeeId,
            Long ignoredAssignmentId,
            Instant endTime
    );
}
