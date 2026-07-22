package com.hilimor.shiftmanagement.schedule;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

    List<Schedule> findByTeam_IdOrderByStartDateDesc(Long teamId);

    List<Schedule> findByTeam_IdAndStatusOrderByStartDateDesc(Long teamId, ScheduleStatus status);

    List<Schedule> findByTeam_IdInAndStatusOrderByStartDateDesc(List<Long> teamIds, ScheduleStatus status);
}
