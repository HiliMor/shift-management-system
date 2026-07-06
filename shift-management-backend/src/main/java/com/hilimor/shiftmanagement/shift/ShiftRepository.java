package com.hilimor.shiftmanagement.shift;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ShiftRepository extends JpaRepository<Shift, Long> {

    List<Shift> findBySchedule_IdOrderByStartTime(Long scheduleId);

    List<Shift> findBySchedule_IdAndStartTimeLessThanAndEndTimeGreaterThan(
            Long scheduleId,
            Instant endTime,
            Instant startTime
    );
}
