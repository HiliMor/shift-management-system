package com.hilimor.shiftmanagement.schedule;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

    List<Schedule> findByTeam_IdOrderByStartDateDesc(Long teamId);

    Optional<Schedule> findByTeam_IdAndStartDateAndEndDateAndStatus(
            Long teamId,
            LocalDate startDate,
            LocalDate endDate,
            ScheduleStatus status
    );

    List<Schedule> findByTeam_IdAndStatusOrderByStartDateDesc(Long teamId, ScheduleStatus status);

    List<Schedule> findByTeam_IdInAndStatusOrderByStartDateDesc(List<Long> teamIds, ScheduleStatus status);
}
