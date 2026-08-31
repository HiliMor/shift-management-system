package com.hilimor.shiftmanagement.template;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.hilimor.shiftmanagement.schedule.Schedule;
import com.hilimor.shiftmanagement.schedule.ScheduleRepository;
import com.hilimor.shiftmanagement.schedule.ScheduleStatus;
import com.hilimor.shiftmanagement.schedule.ScheduleWriteLock;
import com.hilimor.shiftmanagement.shift.Shift;
import com.hilimor.shiftmanagement.shift.ShiftRepository;
import com.hilimor.shiftmanagement.shift.ShiftResponse;
import com.hilimor.shiftmanagement.staffing.StaffingRole;
import com.hilimor.shiftmanagement.staffing.StaffingRoleRepository;
import com.hilimor.shiftmanagement.team.Team;
import com.hilimor.shiftmanagement.team.TeamManagerRepository;
import com.hilimor.shiftmanagement.team.TeamRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ShiftTemplateService {

    private static final Logger log = LoggerFactory.getLogger(ShiftTemplateService.class);

    private final ShiftTemplateRepository shiftTemplateRepository;
    private final TemplateSlotRepository templateSlotRepository;
    private final TeamRepository teamRepository;
    private final TeamManagerRepository teamManagerRepository;
    private final StaffingRoleRepository staffingRoleRepository;
    private final ScheduleRepository scheduleRepository;
    private final ShiftRepository shiftRepository;
    private final ScheduleWriteLock writeLock;

    public ShiftTemplateService(
            ShiftTemplateRepository shiftTemplateRepository,
            TemplateSlotRepository templateSlotRepository,
            TeamRepository teamRepository,
            TeamManagerRepository teamManagerRepository,
            StaffingRoleRepository staffingRoleRepository,
            ScheduleRepository scheduleRepository,
            ShiftRepository shiftRepository,
            ScheduleWriteLock writeLock
    ) {
        this.shiftTemplateRepository = shiftTemplateRepository;
        this.templateSlotRepository = templateSlotRepository;
        this.teamRepository = teamRepository;
        this.teamManagerRepository = teamManagerRepository;
        this.staffingRoleRepository = staffingRoleRepository;
        this.scheduleRepository = scheduleRepository;
        this.shiftRepository = shiftRepository;
        this.writeLock = writeLock;
    }

    @Transactional
    public ShiftTemplateResponse createTemplate(
            String username,
            Long teamId,
            CreateShiftTemplateRequest request
    ) {
        Team team = managedTeam(username, teamId);
        writeLock.lockTeam(team);
        ShiftTemplate shiftTemplate = new ShiftTemplate(
                team,
                request.name(),
                request.description(),
                request.cycleDays(),
                request.defaultMinRestHours()
        );

        if (shiftTemplateRepository.existsByTeam_IdAndName(teamId, shiftTemplate.getName())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Template name already exists for this team");
        }

        ShiftTemplate savedTemplate = shiftTemplateRepository.save(shiftTemplate);
        log.info("Shift template {} created for team {} by manager {}", savedTemplate.getId(), teamId, username);

        return ShiftTemplateResponse.from(savedTemplate);
    }

    @Transactional(readOnly = true)
    public List<ShiftTemplateResponse> listTeamTemplates(String username, Long teamId) {
        managedTeam(username, teamId);

        return shiftTemplateRepository.findByTeam_IdOrderByName(teamId)
                .stream()
                .map(ShiftTemplateResponse::from)
                .toList();
    }

    @Transactional
    public void deleteTemplate(String username, Long templateId) {
        ShiftTemplate shiftTemplate = managedTemplate(username, templateId);
        writeLock.lockTemplate(shiftTemplate);

        if (shiftRepository.existsByTemplateSlot_ShiftTemplate_Id(templateId)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Template cannot be deleted because it is used by existing shifts"
            );
        }

        shiftTemplateRepository.delete(shiftTemplate);
        log.info("Shift template {} deleted by manager {}", templateId, username);
    }

    @Transactional
    public TemplateSlotResponse createSlot(
            String username,
            Long templateId,
            CreateTemplateSlotRequest request
    ) {
        ShiftTemplate shiftTemplate = managedTemplate(username, templateId);
        writeLock.lockTemplate(shiftTemplate);
        validateDayOffsetInsideTemplateCycle(shiftTemplate, request.dayOffset());
        StaffingRole requiredStaffingRole = requiredStaffingRole(shiftTemplate, request.requiredStaffingRoleId());

        TemplateSlot templateSlot = new TemplateSlot(
                shiftTemplate,
                request.dayOffset(),
                request.startTime(),
                request.durationMinutes(),
                request.description(),
                request.requiredWorkers(),
                requiredStaffingRole
        );
        TemplateSlot savedSlot = templateSlotRepository.save(templateSlot);
        log.info("Template slot {} created for template {} by manager {}", savedSlot.getId(), templateId, username);

        return TemplateSlotResponse.from(savedSlot);
    }

    @Transactional(readOnly = true)
    public List<TemplateSlotResponse> listTemplateSlots(String username, Long templateId) {
        managedTemplate(username, templateId);

        return templateSlotRepository.findByShiftTemplate_IdOrderByDayOffsetAscStartTimeAsc(templateId)
                .stream()
                .map(TemplateSlotResponse::from)
                .toList();
    }

    @Transactional
    public GenerateTemplateShiftsResponse generateShifts(
            String username,
            Long templateId,
            GenerateTemplateShiftsRequest request
    ) {
        ShiftTemplate shiftTemplate = managedTemplate(username, templateId);
        Schedule schedule = scheduleRepository.findById(request.scheduleId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Schedule not found"));

        if (!Objects.equals(schedule.getTeam().getId(), shiftTemplate.getTeam().getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Template must belong to the schedule team");
        }
        writeLock.lockTemplate(shiftTemplate);
        writeLock.lockSchedule(schedule);
        if (schedule.getStatus() != ScheduleStatus.DRAFT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Template shifts can be generated only for draft schedules");
        }

        List<TemplateSlot> templateSlots = templateSlotRepository
                .findByShiftTemplate_IdOrderByDayOffsetAscStartTimeAsc(templateId);
        if (templateSlots.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Template has no slots to generate");
        }

        ZoneId zoneId = ZoneId.of(schedule.getTeam().getTimeZone());
        List<ShiftResponse> createdShifts = new ArrayList<>();
        int skippedExistingShifts = 0;
        int skippedOutsideSchedule = 0;

        for (LocalDate date = schedule.getStartDate(); !date.isAfter(schedule.getEndDate()); date = date.plusDays(1)) {
            int cycleDay = (int) (ChronoUnit.DAYS.between(schedule.getStartDate(), date) % shiftTemplate.getCycleDays());

            for (TemplateSlot templateSlot : templateSlots) {
                if (templateSlot.getDayOffset() != cycleDay) {
                    continue;
                }

                LocalDateTime startDateTime = LocalDateTime.of(date, templateSlot.getStartTime());
                LocalDateTime endDateTime = startDateTime.plusMinutes(templateSlot.getDurationMinutes());
                Instant startTime = startDateTime.atZone(zoneId).toInstant();
                Instant endTime = endDateTime.atZone(zoneId).toInstant();

                if (endsAfterSchedule(schedule, endTime, zoneId)) {
                    skippedOutsideSchedule++;
                    continue;
                }

                if (shiftRepository.existsBySchedule_IdAndTemplateSlot_IdAndStartTime(
                        schedule.getId(),
                        templateSlot.getId(),
                        startTime
                )) {
                    skippedExistingShifts++;
                    continue;
                }

                Shift shift = new Shift(
                        schedule,
                        startTime,
                        endTime,
                        templateSlot.getDescription(),
                        templateSlot.getRequiredWorkers(),
                        shiftTemplate.getDefaultMinRestHours(),
                        templateSlot.getRequiredStaffingRole(),
                        templateSlot
                );
                Shift savedShift = shiftRepository.save(shift);
                createdShifts.add(ShiftResponse.from(savedShift));
            }
        }

        log.info(
                "Generated {} shifts for schedule {} from template {} by manager {}; skippedExisting={}, skippedOutside={}",
                createdShifts.size(),
                schedule.getId(),
                templateId,
                username,
                skippedExistingShifts,
                skippedOutsideSchedule
        );

        return new GenerateTemplateShiftsResponse(
                templateId,
                schedule.getId(),
                createdShifts.size(),
                skippedExistingShifts,
                skippedOutsideSchedule,
                createdShifts
        );
    }

    private Team managedTeam(String username, Long teamId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Team not found"));

        if (!teamManagerRepository.existsByManager_UsernameAndTeam_Id(username, teamId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only a team manager can manage templates for this team");
        }

        return team;
    }

    private ShiftTemplate managedTemplate(String username, Long templateId) {
        ShiftTemplate shiftTemplate = shiftTemplateRepository.findById(templateId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Template not found"));

        Long teamId = shiftTemplate.getTeam().getId();
        if (!teamManagerRepository.existsByManager_UsernameAndTeam_Id(username, teamId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only a team manager can manage this template");
        }

        return shiftTemplate;
    }

    private void validateDayOffsetInsideTemplateCycle(ShiftTemplate shiftTemplate, int dayOffset) {
        if (dayOffset >= shiftTemplate.getCycleDays()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Template slot day offset must be inside the template cycle");
        }
    }

    private StaffingRole requiredStaffingRole(ShiftTemplate shiftTemplate, Long requiredStaffingRoleId) {
        if (requiredStaffingRoleId == null) {
            return null;
        }

        StaffingRole staffingRole = staffingRoleRepository.findById(requiredStaffingRoleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Required staffing role not found"));

        if (!Objects.equals(staffingRole.getTeam().getId(), shiftTemplate.getTeam().getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Required staffing role must belong to the template team");
        }

        return staffingRole;
    }

    private boolean endsAfterSchedule(Schedule schedule, Instant endTime, ZoneId zoneId) {
        LocalDate shiftEndDate = endTime.minusNanos(1).atZone(zoneId).toLocalDate();

        return shiftEndDate.isAfter(schedule.getEndDate());
    }
}
