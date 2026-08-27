package com.hilimor.shiftmanagement.template;

import java.util.List;
import java.util.Objects;

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

    public ShiftTemplateService(
            ShiftTemplateRepository shiftTemplateRepository,
            TemplateSlotRepository templateSlotRepository,
            TeamRepository teamRepository,
            TeamManagerRepository teamManagerRepository,
            StaffingRoleRepository staffingRoleRepository
    ) {
        this.shiftTemplateRepository = shiftTemplateRepository;
        this.templateSlotRepository = templateSlotRepository;
        this.teamRepository = teamRepository;
        this.teamManagerRepository = teamManagerRepository;
        this.staffingRoleRepository = staffingRoleRepository;
    }

    @Transactional
    public ShiftTemplateResponse createTemplate(
            String username,
            Long teamId,
            CreateShiftTemplateRequest request
    ) {
        Team team = managedTeam(username, teamId);
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
    public TemplateSlotResponse createSlot(
            String username,
            Long templateId,
            CreateTemplateSlotRequest request
    ) {
        ShiftTemplate shiftTemplate = managedTemplate(username, templateId);
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
}
