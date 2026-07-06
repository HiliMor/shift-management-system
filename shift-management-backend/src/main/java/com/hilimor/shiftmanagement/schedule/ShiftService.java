package com.hilimor.shiftmanagement.schedule;

import java.util.List;
import java.util.Objects;

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

        Schedule schedule = managedSchedule(username, scheduleId);

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

    @Transactional(readOnly = true)
    public List<ShiftResponse> listShifts(String username, Long scheduleId) {
        managedSchedule(username, scheduleId);

        return shiftRepository.findBySchedule_IdOrderByStartTime(scheduleId)
                .stream()
                .map(ShiftResponse::from)
                .toList();
    }

    @Transactional
    public ShiftResponse updateShift(String username, Long scheduleId, Long shiftId, UpdateShiftRequest request) {
        if (!request.endTime().isAfter(request.startTime())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Shift end time must be after start time");
        }

        Schedule schedule = managedSchedule(username, scheduleId);

        if (schedule.getStatus() != ScheduleStatus.DRAFT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Shifts can be updated only in draft schedules");
        }

        Shift shift = shiftRepository.findById(shiftId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Shift not found"));

        if (!Objects.equals(shift.getSchedule().getId(), scheduleId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Shift not found");
        }

        shift.updateDetails(
                request.startTime(),
                request.endTime(),
                request.description(),
                request.requiredWorkers(),
                request.minRestHours()
        );

        return ShiftResponse.from(shift);
    }

    @Transactional
    public void deleteShift(String username, Long scheduleId, Long shiftId) {
        Schedule schedule = managedSchedule(username, scheduleId);

        if (schedule.getStatus() != ScheduleStatus.DRAFT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Shifts can be deleted only in draft schedules");
        }

        Shift shift = shiftRepository.findById(shiftId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Shift not found"));

        if (!Objects.equals(shift.getSchedule().getId(), scheduleId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Shift not found");
        }

        shiftRepository.delete(shift);
    }

    private Schedule managedSchedule(String username, Long scheduleId) {
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Schedule not found"));

        Long teamId = schedule.getTeam().getId();
        if (!teamManagerRepository.existsByManager_UsernameAndTeam_Id(username, teamId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only a team manager can access shifts for this schedule");
        }

        return schedule;
    }
}
