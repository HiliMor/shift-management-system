package com.hilimor.shiftmanagement.shift;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;

import com.hilimor.shiftmanagement.assignment.AssignmentRepository;
import com.hilimor.shiftmanagement.assignment.AssignmentValidator;
import com.hilimor.shiftmanagement.schedule.Schedule;
import com.hilimor.shiftmanagement.schedule.ScheduleRepository;
import com.hilimor.shiftmanagement.schedule.ScheduleStatus;
import com.hilimor.shiftmanagement.staffing.StaffingRole;
import com.hilimor.shiftmanagement.staffing.StaffingRoleRepository;
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
    private final StaffingRoleRepository staffingRoleRepository;
    private final AssignmentRepository assignmentRepository;
    private final AssignmentValidator assignmentValidator;

    public ShiftService(
            ScheduleRepository scheduleRepository,
            ShiftRepository shiftRepository,
            TeamManagerRepository teamManagerRepository,
            StaffingRoleRepository staffingRoleRepository,
            AssignmentRepository assignmentRepository,
            AssignmentValidator assignmentValidator
    ) {
        this.scheduleRepository = scheduleRepository;
        this.shiftRepository = shiftRepository;
        this.teamManagerRepository = teamManagerRepository;
        this.staffingRoleRepository = staffingRoleRepository;
        this.assignmentRepository = assignmentRepository;
        this.assignmentValidator = assignmentValidator;
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
        validateShiftWithinSchedule(schedule, request.startTime(), request.endTime());
        StaffingRole requiredStaffingRole = requiredStaffingRole(schedule, request.requiredStaffingRoleId());

        Shift shift = new Shift(
                schedule,
                request.startTime(),
                request.endTime(),
                request.description(),
                request.requiredWorkers(),
                request.minRestHours(),
                requiredStaffingRole
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
        validateShiftWithinSchedule(schedule, request.startTime(), request.endTime());
        StaffingRole requiredStaffingRole = requiredStaffingRole(schedule, request.requiredStaffingRoleId());

        shift.updateDetails(
                request.startTime(),
                request.endTime(),
                request.description(),
                request.requiredWorkers(),
                request.minRestHours(),
                requiredStaffingRole
        );
        assignmentValidator.validateExistingAssignments(shift, assignmentRepository.findByShift_IdOrderById(shiftId));

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

    private void validateShiftWithinSchedule(Schedule schedule, Instant startTime, Instant endTime) {
        ZoneId zoneId = ZoneId.of(schedule.getTeam().getTimeZone());
        LocalDate shiftStartDate = startTime.atZone(zoneId).toLocalDate();
        LocalDate shiftEndDate = endTime.minusNanos(1).atZone(zoneId).toLocalDate();

        if (shiftStartDate.isBefore(schedule.getStartDate()) || shiftEndDate.isAfter(schedule.getEndDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Shift must be within the schedule date range");
        }
    }

    private StaffingRole requiredStaffingRole(Schedule schedule, Long requiredStaffingRoleId) {
        if (requiredStaffingRoleId == null) {
            return null;
        }

        StaffingRole staffingRole = staffingRoleRepository.findById(requiredStaffingRoleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Required staffing role not found"));

        if (!Objects.equals(staffingRole.getTeam().getId(), schedule.getTeam().getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Required staffing role must belong to the schedule team");
        }

        return staffingRole;
    }
}
