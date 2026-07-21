package com.hilimor.shiftmanagement.schedule;

import java.time.Instant;

import com.hilimor.shiftmanagement.team.Team;
import com.hilimor.shiftmanagement.team.TeamManagerRepository;
import com.hilimor.shiftmanagement.team.TeamRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final TeamRepository teamRepository;
    private final TeamManagerRepository teamManagerRepository;

    public ScheduleService(
            ScheduleRepository scheduleRepository,
            TeamRepository teamRepository,
            TeamManagerRepository teamManagerRepository
    ) {
        this.scheduleRepository = scheduleRepository;
        this.teamRepository = teamRepository;
        this.teamManagerRepository = teamManagerRepository;
    }

    @Transactional
    public ScheduleResponse createDraftSchedule(String username, CreateScheduleRequest request) {
        if (request.endDate().isBefore(request.startDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Schedule end date must not be before start date");
        }

        Team team = teamRepository.findById(request.teamId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Team not found"));

        if (!teamManagerRepository.existsByManager_UsernameAndTeam_Id(username, request.teamId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only a team manager can create schedules for this team");
        }

        Schedule schedule = new Schedule(team, request.startDate(), request.endDate());
        Schedule savedSchedule = scheduleRepository.save(schedule);

        return ScheduleResponse.from(savedSchedule);
    }

    @Transactional
    public ScheduleResponse publishSchedule(String username, Long scheduleId) {
        Schedule schedule = managedSchedule(
                username,
                scheduleId,
                "Only a team manager can publish this schedule"
        );

        try {
            schedule.publish(Instant.now());
        } catch (IllegalStateException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, exception.getMessage());
        }

        return ScheduleResponse.from(schedule);
    }

    @Transactional
    public ScheduleResponse reopenSchedule(String username, Long scheduleId) {
        Schedule schedule = managedSchedule(
                username,
                scheduleId,
                "Only a team manager can reopen this schedule"
        );

        try {
            schedule.reopen();
        } catch (IllegalStateException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, exception.getMessage());
        }

        return ScheduleResponse.from(schedule);
    }

    private Schedule managedSchedule(String username, Long scheduleId, String errorMessage) {
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Schedule not found"));

        Long teamId = schedule.getTeam().getId();
        if (!teamManagerRepository.existsByManager_UsernameAndTeam_Id(username, teamId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, errorMessage);
        }

        return schedule;
    }
}
