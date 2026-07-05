package com.hilimor.shiftmanagement.schedule;

import com.hilimor.shiftmanagement.team.TeamManagerRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ShiftService {

    private final ScheduleRepository scheduleRepository;
    private final ShiftRepository shiftRepository;
    private final TeamManagerRepository teamManagerRepository;

    public ShiftService(
            ScheduleRepository scheduleRepository,
            ShiftRepository shiftRepository,
            TeamManagerRepository teamManagerRepository
    ) {
        this.scheduleRepository = scheduleRepository;
        this.shiftRepository = shiftRepository;
        this.teamManagerRepository = teamManagerRepository;
    }

    @Transactional
    public ShiftResponse createShift(String username, Long scheduleId, CreateShiftRequest request) {
        if (!request.endTime().isAfter(request.startTime())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Shift end time must be after start time");
        }

        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Schedule not found"));

        Long teamId = schedule.getTeam().getId();
        if (!teamManagerRepository.existsByManager_UsernameAndTeam_Id(username, teamId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only a team manager can create shifts for this schedule");
        }

        if (schedule.getStatus() != ScheduleStatus.DRAFT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Shifts can be created only in draft schedules");
        }

        Shift shift = new Shift(
                schedule,
                request.startTime(),
                request.endTime(),
                request.description(),
                request.requiredWorkers(),
                request.minRestHours()
        );
        Shift savedShift = shiftRepository.save(shift);

        return ShiftResponse.from(savedShift);
    }
}
