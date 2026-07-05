package com.hilimor.shiftmanagement.schedule;

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
}
