package com.hilimor.shiftmanagement.shift;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ShiftRepository extends JpaRepository<Shift, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select shift from Shift shift where shift.id = :shiftId")
    Optional<Shift> findByIdForUpdate(@Param("shiftId") Long shiftId);

    List<Shift> findBySchedule_IdOrderByStartTime(Long scheduleId);

    long deleteBySchedule_Id(Long scheduleId);

    List<Shift> findBySchedule_IdAndStartTimeLessThanAndEndTimeGreaterThan(
            Long scheduleId,
            Instant endTime,
            Instant startTime
    );

    boolean existsBySchedule_IdAndTemplateSlot_IdAndStartTime(
            Long scheduleId,
            Long templateSlotId,
            Instant startTime
    );

    boolean existsByTemplateSlot_ShiftTemplate_Id(Long templateId);
}
