package com.hilimor.shiftmanagement.template;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import com.hilimor.shiftmanagement.schedule.Schedule;
import com.hilimor.shiftmanagement.schedule.ScheduleRepository;
import com.hilimor.shiftmanagement.schedule.ScheduleStatus;
import com.hilimor.shiftmanagement.shift.Shift;
import com.hilimor.shiftmanagement.shift.ShiftRepository;
import com.hilimor.shiftmanagement.staffing.StaffingRole;
import com.hilimor.shiftmanagement.staffing.StaffingRoleRepository;
import com.hilimor.shiftmanagement.team.SwapApprovalPolicy;
import com.hilimor.shiftmanagement.team.Team;
import com.hilimor.shiftmanagement.team.TeamManagerRepository;
import com.hilimor.shiftmanagement.team.TeamRepository;

@ExtendWith(MockitoExtension.class)
class ShiftTemplateServiceTest {

    @Mock
    private ShiftTemplateRepository shiftTemplateRepository;

    @Mock
    private TemplateSlotRepository templateSlotRepository;

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private TeamManagerRepository teamManagerRepository;

    @Mock
    private StaffingRoleRepository staffingRoleRepository;

    @Mock
    private ScheduleRepository scheduleRepository;

    @Mock
    private ShiftRepository shiftRepository;

    @InjectMocks
    private ShiftTemplateService shiftTemplateService;

    @Test
    void createTemplateSavesTemplateForManagedTeam() {
        Team team = team(1L, "Operations");
        CreateShiftTemplateRequest request = new CreateShiftTemplateRequest(
                "  Routine Week  ",
                "Three shifts per day",
                7,
                8
        );

        when(teamRepository.findById(1L)).thenReturn(Optional.of(team));
        when(teamManagerRepository.existsByManager_UsernameAndTeam_Id("manager1", 1L)).thenReturn(true);
        when(shiftTemplateRepository.existsByTeam_IdAndName(1L, "Routine Week")).thenReturn(false);
        when(shiftTemplateRepository.save(any(ShiftTemplate.class))).thenAnswer(invocation -> {
            ShiftTemplate template = invocation.getArgument(0);
            ReflectionTestUtils.setField(template, "id", 50L);
            return template;
        });

        ShiftTemplateResponse response = shiftTemplateService.createTemplate("manager1", 1L, request);

        assertThat(response.id()).isEqualTo(50L);
        assertThat(response.teamId()).isEqualTo(1L);
        assertThat(response.teamName()).isEqualTo("Operations");
        assertThat(response.name()).isEqualTo("Routine Week");
        assertThat(response.description()).isEqualTo("Three shifts per day");
        assertThat(response.cycleDays()).isEqualTo(7);
        assertThat(response.defaultMinRestHours()).isEqualTo(8);
        assertThat(response.active()).isFalse();

        ArgumentCaptor<ShiftTemplate> captor = ArgumentCaptor.forClass(ShiftTemplate.class);
        verify(shiftTemplateRepository).save(captor.capture());
        assertThat(captor.getValue().getTeam()).isSameAs(team);
        assertThat(captor.getValue().getName()).isEqualTo("Routine Week");
    }

    @Test
    void createTemplateRejectsUnmanagedTeam() {
        CreateShiftTemplateRequest request = new CreateShiftTemplateRequest("Routine Week", null, 7, 8);

        when(teamRepository.findById(1L)).thenReturn(Optional.of(team(1L, "Operations")));
        when(teamManagerRepository.existsByManager_UsernameAndTeam_Id("employee1", 1L)).thenReturn(false);

        assertThatThrownBy(() -> shiftTemplateService.createTemplate("employee1", 1L, request))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));

        verify(shiftTemplateRepository, never()).save(any());
    }

    @Test
    void createTemplateRejectsMissingTeam() {
        CreateShiftTemplateRequest request = new CreateShiftTemplateRequest("Routine Week", null, 7, 8);

        when(teamRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> shiftTemplateService.createTemplate("manager1", 99L, request))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));

        verify(shiftTemplateRepository, never()).save(any());
    }

    @Test
    void createTemplateRejectsDuplicateNameInSameTeam() {
        CreateShiftTemplateRequest request = new CreateShiftTemplateRequest("Routine Week", null, 7, 8);

        when(teamRepository.findById(1L)).thenReturn(Optional.of(team(1L, "Operations")));
        when(teamManagerRepository.existsByManager_UsernameAndTeam_Id("manager1", 1L)).thenReturn(true);
        when(shiftTemplateRepository.existsByTeam_IdAndName(1L, "Routine Week")).thenReturn(true);

        assertThatThrownBy(() -> shiftTemplateService.createTemplate("manager1", 1L, request))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));

        verify(shiftTemplateRepository, never()).save(any());
    }

    @Test
    void listTeamTemplatesReturnsTemplatesForManagedTeam() {
        Team team = team(1L, "Operations");
        ShiftTemplate emergency = shiftTemplate(team, 51L, "Emergency Week");
        ShiftTemplate routine = shiftTemplate(team, 50L, "Routine Week");

        when(teamRepository.findById(1L)).thenReturn(Optional.of(team));
        when(teamManagerRepository.existsByManager_UsernameAndTeam_Id("manager1", 1L)).thenReturn(true);
        when(shiftTemplateRepository.findByTeam_IdOrderByName(1L)).thenReturn(List.of(emergency, routine));

        List<ShiftTemplateResponse> responses = shiftTemplateService.listTeamTemplates("manager1", 1L);

        assertThat(responses).extracting(ShiftTemplateResponse::id).containsExactly(51L, 50L);
        assertThat(responses).extracting(ShiftTemplateResponse::name).containsExactly("Emergency Week", "Routine Week");
    }

    @Test
    void listTeamTemplatesRejectsUnmanagedTeam() {
        when(teamRepository.findById(1L)).thenReturn(Optional.of(team(1L, "Operations")));
        when(teamManagerRepository.existsByManager_UsernameAndTeam_Id("employee1", 1L)).thenReturn(false);

        assertThatThrownBy(() -> shiftTemplateService.listTeamTemplates("employee1", 1L))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));

        verify(shiftTemplateRepository, never()).findByTeam_IdOrderByName(1L);
    }

    @Test
    void deleteTemplateDeletesUnusedTemplateForTeamManager() {
        ShiftTemplate template = shiftTemplate(team(1L, "Operations"), 50L, "Routine Week");

        when(shiftTemplateRepository.findById(50L)).thenReturn(Optional.of(template));
        when(teamManagerRepository.existsByManager_UsernameAndTeam_Id("manager1", 1L)).thenReturn(true);
        when(shiftRepository.existsByTemplateSlot_ShiftTemplate_Id(50L)).thenReturn(false);

        shiftTemplateService.deleteTemplate("manager1", 50L);

        verify(shiftTemplateRepository).delete(template);
    }

    @Test
    void deleteTemplateRejectsTemplateUsedByExistingShift() {
        ShiftTemplate template = shiftTemplate(team(1L, "Operations"), 50L, "Routine Week");

        when(shiftTemplateRepository.findById(50L)).thenReturn(Optional.of(template));
        when(teamManagerRepository.existsByManager_UsernameAndTeam_Id("manager1", 1L)).thenReturn(true);
        when(shiftRepository.existsByTemplateSlot_ShiftTemplate_Id(50L)).thenReturn(true);

        assertThatThrownBy(() -> shiftTemplateService.deleteTemplate("manager1", 50L))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));

        verify(shiftTemplateRepository, never()).delete(any(ShiftTemplate.class));
    }

    @Test
    void deleteTemplateRejectsUnmanagedTemplate() {
        ShiftTemplate template = shiftTemplate(team(1L, "Operations"), 50L, "Routine Week");

        when(shiftTemplateRepository.findById(50L)).thenReturn(Optional.of(template));
        when(teamManagerRepository.existsByManager_UsernameAndTeam_Id("employee1", 1L)).thenReturn(false);

        assertThatThrownBy(() -> shiftTemplateService.deleteTemplate("employee1", 50L))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));

        verify(shiftRepository, never()).existsByTemplateSlot_ShiftTemplate_Id(any());
        verify(shiftTemplateRepository, never()).delete(any(ShiftTemplate.class));
    }

    @Test
    void createSlotSavesSlotForManagedTemplate() {
        Team team = team(1L, "Operations");
        ShiftTemplate template = shiftTemplate(team, 50L, "Routine Week");
        StaffingRole role = staffingRole(team, 20L, "Shift Supervisor");
        CreateTemplateSlotRequest request = new CreateTemplateSlotRequest(
                2,
                LocalTime.of(8, 0),
                480,
                "Morning supervisor shift",
                1,
                20L
        );

        when(shiftTemplateRepository.findById(50L)).thenReturn(Optional.of(template));
        when(teamManagerRepository.existsByManager_UsernameAndTeam_Id("manager1", 1L)).thenReturn(true);
        when(staffingRoleRepository.findById(20L)).thenReturn(Optional.of(role));
        when(templateSlotRepository.save(any(TemplateSlot.class))).thenAnswer(invocation -> {
            TemplateSlot slot = invocation.getArgument(0);
            ReflectionTestUtils.setField(slot, "id", 70L);
            return slot;
        });

        TemplateSlotResponse response = shiftTemplateService.createSlot("manager1", 50L, request);

        assertThat(response.id()).isEqualTo(70L);
        assertThat(response.shiftTemplateId()).isEqualTo(50L);
        assertThat(response.dayOffset()).isEqualTo(2);
        assertThat(response.startTime()).isEqualTo(LocalTime.of(8, 0));
        assertThat(response.durationMinutes()).isEqualTo(480);
        assertThat(response.description()).isEqualTo("Morning supervisor shift");
        assertThat(response.requiredWorkers()).isEqualTo(1);
        assertThat(response.requiredStaffingRoleId()).isEqualTo(20L);
        assertThat(response.requiredStaffingRoleName()).isEqualTo("Shift Supervisor");

        ArgumentCaptor<TemplateSlot> captor = ArgumentCaptor.forClass(TemplateSlot.class);
        verify(templateSlotRepository).save(captor.capture());
        assertThat(captor.getValue().getShiftTemplate()).isSameAs(template);
        assertThat(captor.getValue().getRequiredStaffingRole()).isSameAs(role);
    }

    @Test
    void createSlotRejectsUnmanagedTemplate() {
        ShiftTemplate template = shiftTemplate(team(1L, "Operations"), 50L, "Routine Week");
        CreateTemplateSlotRequest request = validSlotRequest();

        when(shiftTemplateRepository.findById(50L)).thenReturn(Optional.of(template));
        when(teamManagerRepository.existsByManager_UsernameAndTeam_Id("employee1", 1L)).thenReturn(false);

        assertThatThrownBy(() -> shiftTemplateService.createSlot("employee1", 50L, request))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));

        verify(templateSlotRepository, never()).save(any());
    }

    @Test
    void createSlotRejectsMissingTemplate() {
        when(shiftTemplateRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> shiftTemplateService.createSlot("manager1", 99L, validSlotRequest()))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));

        verify(templateSlotRepository, never()).save(any());
    }

    @Test
    void createSlotRejectsDayOffsetOutsideTemplateCycle() {
        ShiftTemplate template = shiftTemplate(team(1L, "Operations"), 50L, "Routine Week");
        CreateTemplateSlotRequest request = new CreateTemplateSlotRequest(
                7,
                LocalTime.of(8, 0),
                480,
                "Morning shift",
                2,
                null
        );

        when(shiftTemplateRepository.findById(50L)).thenReturn(Optional.of(template));
        when(teamManagerRepository.existsByManager_UsernameAndTeam_Id("manager1", 1L)).thenReturn(true);

        assertThatThrownBy(() -> shiftTemplateService.createSlot("manager1", 50L, request))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));

        verify(templateSlotRepository, never()).save(any());
    }

    @Test
    void createSlotRejectsRequiredStaffingRoleFromAnotherTeam() {
        Team team = team(1L, "Operations");
        Team otherTeam = team(2L, "Support");
        ShiftTemplate template = shiftTemplate(team, 50L, "Routine Week");
        StaffingRole role = staffingRole(otherTeam, 20L, "Shift Supervisor");
        CreateTemplateSlotRequest request = new CreateTemplateSlotRequest(
                0,
                LocalTime.of(8, 0),
                480,
                "Supervisor shift",
                1,
                20L
        );

        when(shiftTemplateRepository.findById(50L)).thenReturn(Optional.of(template));
        when(teamManagerRepository.existsByManager_UsernameAndTeam_Id("manager1", 1L)).thenReturn(true);
        when(staffingRoleRepository.findById(20L)).thenReturn(Optional.of(role));

        assertThatThrownBy(() -> shiftTemplateService.createSlot("manager1", 50L, request))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));

        verify(templateSlotRepository, never()).save(any());
    }

    @Test
    void createSlotRejectsMissingRequiredStaffingRole() {
        ShiftTemplate template = shiftTemplate(team(1L, "Operations"), 50L, "Routine Week");
        CreateTemplateSlotRequest request = new CreateTemplateSlotRequest(
                0,
                LocalTime.of(8, 0),
                480,
                "Supervisor shift",
                1,
                99L
        );

        when(shiftTemplateRepository.findById(50L)).thenReturn(Optional.of(template));
        when(teamManagerRepository.existsByManager_UsernameAndTeam_Id("manager1", 1L)).thenReturn(true);
        when(staffingRoleRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> shiftTemplateService.createSlot("manager1", 50L, request))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));

        verify(templateSlotRepository, never()).save(any());
    }

    @Test
    void listTemplateSlotsReturnsSlotsForManagedTemplate() {
        ShiftTemplate template = shiftTemplate(team(1L, "Operations"), 50L, "Routine Week");
        TemplateSlot morning = templateSlot(template, 70L, 0, LocalTime.of(8, 0), "Morning shift");
        TemplateSlot evening = templateSlot(template, 71L, 0, LocalTime.of(16, 0), "Evening shift");

        when(shiftTemplateRepository.findById(50L)).thenReturn(Optional.of(template));
        when(teamManagerRepository.existsByManager_UsernameAndTeam_Id("manager1", 1L)).thenReturn(true);
        when(templateSlotRepository.findByShiftTemplate_IdOrderByDayOffsetAscStartTimeAsc(50L))
                .thenReturn(List.of(morning, evening));

        List<TemplateSlotResponse> responses = shiftTemplateService.listTemplateSlots("manager1", 50L);

        assertThat(responses).extracting(TemplateSlotResponse::id).containsExactly(70L, 71L);
        assertThat(responses).extracting(TemplateSlotResponse::description)
                .containsExactly("Morning shift", "Evening shift");
    }

    @Test
    void listTemplateSlotsRejectsUnmanagedTemplate() {
        ShiftTemplate template = shiftTemplate(team(1L, "Operations"), 50L, "Routine Week");

        when(shiftTemplateRepository.findById(50L)).thenReturn(Optional.of(template));
        when(teamManagerRepository.existsByManager_UsernameAndTeam_Id("employee1", 1L)).thenReturn(false);

        assertThatThrownBy(() -> shiftTemplateService.listTemplateSlots("employee1", 50L))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));

        verify(templateSlotRepository, never()).findByShiftTemplate_IdOrderByDayOffsetAscStartTimeAsc(50L);
    }

    @Test
    void generateShiftsCreatesShiftsFromTemplateSlotsAcrossScheduleDates() {
        Team team = team(1L, "Operations");
        Schedule schedule = schedule(
                team,
                10L,
                LocalDate.of(2026, 7, 6),
                LocalDate.of(2026, 7, 8),
                ScheduleStatus.DRAFT
        );
        ShiftTemplate template = shiftTemplate(team, 50L, "Two Day Cycle", 2);
        StaffingRole role = staffingRole(team, 20L, "Shift Supervisor");
        TemplateSlot dayZeroSlot = templateSlot(
                template,
                70L,
                0,
                LocalTime.of(8, 0),
                480,
                "Morning shift",
                2,
                role
        );
        TemplateSlot dayOneSlot = templateSlot(
                template,
                71L,
                1,
                LocalTime.of(16, 0),
                360,
                "Evening shift",
                1,
                null
        );
        AtomicLong nextShiftId = new AtomicLong(100L);

        when(shiftTemplateRepository.findById(50L)).thenReturn(Optional.of(template));
        when(teamManagerRepository.existsByManager_UsernameAndTeam_Id("manager1", 1L)).thenReturn(true);
        when(scheduleRepository.findById(10L)).thenReturn(Optional.of(schedule));
        when(templateSlotRepository.findByShiftTemplate_IdOrderByDayOffsetAscStartTimeAsc(50L))
                .thenReturn(List.of(dayZeroSlot, dayOneSlot));
        when(shiftRepository.existsBySchedule_IdAndTemplateSlot_IdAndStartTime(any(), any(), any()))
                .thenReturn(false);
        when(shiftRepository.save(any(Shift.class))).thenAnswer(invocation -> {
            Shift shift = invocation.getArgument(0);
            ReflectionTestUtils.setField(shift, "id", nextShiftId.getAndIncrement());
            return shift;
        });

        GenerateTemplateShiftsResponse response = shiftTemplateService.generateShifts(
                "manager1",
                50L,
                new GenerateTemplateShiftsRequest(10L)
        );

        assertThat(response.templateId()).isEqualTo(50L);
        assertThat(response.scheduleId()).isEqualTo(10L);
        assertThat(response.shiftsCreated()).isEqualTo(3);
        assertThat(response.skippedExistingShifts()).isZero();
        assertThat(response.skippedOutsideSchedule()).isZero();
        assertThat(response.shifts()).extracting(shift -> shift.templateSlotId())
                .containsExactly(70L, 71L, 70L);

        ArgumentCaptor<Shift> captor = ArgumentCaptor.forClass(Shift.class);
        verify(shiftRepository, times(3)).save(captor.capture());
        List<Shift> savedShifts = captor.getAllValues();

        assertThat(savedShifts).extracting(Shift::getStartTime)
                .containsExactly(
                        Instant.parse("2026-07-06T05:00:00Z"),
                        Instant.parse("2026-07-07T13:00:00Z"),
                        Instant.parse("2026-07-08T05:00:00Z")
                );
        assertThat(savedShifts).extracting(Shift::getEndTime)
                .containsExactly(
                        Instant.parse("2026-07-06T13:00:00Z"),
                        Instant.parse("2026-07-07T19:00:00Z"),
                        Instant.parse("2026-07-08T13:00:00Z")
                );
        assertThat(savedShifts).extracting(Shift::getTemplateSlot)
                .containsExactly(dayZeroSlot, dayOneSlot, dayZeroSlot);
        assertThat(savedShifts).extracting(Shift::getMinRestHours)
                .containsExactly(8, 8, 8);
        assertThat(savedShifts.get(0).getRequiredStaffingRole()).isSameAs(role);
    }

    @Test
    void generateShiftsSkipsExistingGeneratedShift() {
        Team team = team(1L, "Operations");
        Schedule schedule = schedule(
                team,
                10L,
                LocalDate.of(2026, 7, 6),
                LocalDate.of(2026, 7, 6),
                ScheduleStatus.DRAFT
        );
        ShiftTemplate template = shiftTemplate(team, 50L, "Routine Week");
        TemplateSlot slot = templateSlot(template, 70L, 0, LocalTime.of(8, 0), "Morning shift");

        when(shiftTemplateRepository.findById(50L)).thenReturn(Optional.of(template));
        when(teamManagerRepository.existsByManager_UsernameAndTeam_Id("manager1", 1L)).thenReturn(true);
        when(scheduleRepository.findById(10L)).thenReturn(Optional.of(schedule));
        when(templateSlotRepository.findByShiftTemplate_IdOrderByDayOffsetAscStartTimeAsc(50L))
                .thenReturn(List.of(slot));
        when(shiftRepository.existsBySchedule_IdAndTemplateSlot_IdAndStartTime(
                10L,
                70L,
                Instant.parse("2026-07-06T05:00:00Z")
        )).thenReturn(true);

        GenerateTemplateShiftsResponse response = shiftTemplateService.generateShifts(
                "manager1",
                50L,
                new GenerateTemplateShiftsRequest(10L)
        );

        assertThat(response.shiftsCreated()).isZero();
        assertThat(response.skippedExistingShifts()).isEqualTo(1);
        assertThat(response.skippedOutsideSchedule()).isZero();
        assertThat(response.shifts()).isEmpty();

        verify(shiftRepository, never()).save(any());
    }

    @Test
    void generateShiftsSkipsSlotThatEndsOutsideScheduleRange() {
        Team team = team(1L, "Operations");
        Schedule schedule = schedule(
                team,
                10L,
                LocalDate.of(2026, 7, 6),
                LocalDate.of(2026, 7, 6),
                ScheduleStatus.DRAFT
        );
        ShiftTemplate template = shiftTemplate(team, 50L, "Routine Week");
        TemplateSlot slot = templateSlot(
                template,
                70L,
                0,
                LocalTime.of(22, 0),
                480,
                "Night shift",
                1,
                null
        );

        when(shiftTemplateRepository.findById(50L)).thenReturn(Optional.of(template));
        when(teamManagerRepository.existsByManager_UsernameAndTeam_Id("manager1", 1L)).thenReturn(true);
        when(scheduleRepository.findById(10L)).thenReturn(Optional.of(schedule));
        when(templateSlotRepository.findByShiftTemplate_IdOrderByDayOffsetAscStartTimeAsc(50L))
                .thenReturn(List.of(slot));

        GenerateTemplateShiftsResponse response = shiftTemplateService.generateShifts(
                "manager1",
                50L,
                new GenerateTemplateShiftsRequest(10L)
        );

        assertThat(response.shiftsCreated()).isZero();
        assertThat(response.skippedExistingShifts()).isZero();
        assertThat(response.skippedOutsideSchedule()).isEqualTo(1);
        assertThat(response.shifts()).isEmpty();

        verify(shiftRepository, never()).save(any());
    }

    @Test
    void generateShiftsRejectsPublishedSchedule() {
        Team team = team(1L, "Operations");
        ShiftTemplate template = shiftTemplate(team, 50L, "Routine Week");
        Schedule schedule = schedule(
                team,
                10L,
                LocalDate.of(2026, 7, 6),
                LocalDate.of(2026, 7, 12),
                ScheduleStatus.PUBLISHED
        );

        when(shiftTemplateRepository.findById(50L)).thenReturn(Optional.of(template));
        when(teamManagerRepository.existsByManager_UsernameAndTeam_Id("manager1", 1L)).thenReturn(true);
        when(scheduleRepository.findById(10L)).thenReturn(Optional.of(schedule));

        assertThatThrownBy(() -> shiftTemplateService.generateShifts(
                "manager1",
                50L,
                new GenerateTemplateShiftsRequest(10L)
        )).isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));

        verify(templateSlotRepository, never()).findByShiftTemplate_IdOrderByDayOffsetAscStartTimeAsc(50L);
        verify(shiftRepository, never()).save(any());
    }

    @Test
    void generateShiftsRejectsScheduleFromAnotherTeam() {
        Team templateTeam = team(1L, "Operations");
        Team scheduleTeam = team(2L, "Support");
        ShiftTemplate template = shiftTemplate(templateTeam, 50L, "Routine Week");
        Schedule schedule = schedule(
                scheduleTeam,
                10L,
                LocalDate.of(2026, 7, 6),
                LocalDate.of(2026, 7, 12),
                ScheduleStatus.DRAFT
        );

        when(shiftTemplateRepository.findById(50L)).thenReturn(Optional.of(template));
        when(teamManagerRepository.existsByManager_UsernameAndTeam_Id("manager1", 1L)).thenReturn(true);
        when(scheduleRepository.findById(10L)).thenReturn(Optional.of(schedule));

        assertThatThrownBy(() -> shiftTemplateService.generateShifts(
                "manager1",
                50L,
                new GenerateTemplateShiftsRequest(10L)
        )).isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));

        verify(templateSlotRepository, never()).findByShiftTemplate_IdOrderByDayOffsetAscStartTimeAsc(50L);
        verify(shiftRepository, never()).save(any());
    }

    @Test
    void generateShiftsRejectsTemplateWithoutSlots() {
        Team team = team(1L, "Operations");
        ShiftTemplate template = shiftTemplate(team, 50L, "Routine Week");
        Schedule schedule = schedule(
                team,
                10L,
                LocalDate.of(2026, 7, 6),
                LocalDate.of(2026, 7, 12),
                ScheduleStatus.DRAFT
        );

        when(shiftTemplateRepository.findById(50L)).thenReturn(Optional.of(template));
        when(teamManagerRepository.existsByManager_UsernameAndTeam_Id("manager1", 1L)).thenReturn(true);
        when(scheduleRepository.findById(10L)).thenReturn(Optional.of(schedule));
        when(templateSlotRepository.findByShiftTemplate_IdOrderByDayOffsetAscStartTimeAsc(50L))
                .thenReturn(List.of());

        assertThatThrownBy(() -> shiftTemplateService.generateShifts(
                "manager1",
                50L,
                new GenerateTemplateShiftsRequest(10L)
        )).isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));

        verify(shiftRepository, never()).save(any());
    }

    private CreateTemplateSlotRequest validSlotRequest() {
        return new CreateTemplateSlotRequest(0, LocalTime.of(8, 0), 480, "Morning shift", 2, null);
    }

    private ShiftTemplate shiftTemplate(Team team, Long id, String name) {
        return shiftTemplate(team, id, name, 7);
    }

    private ShiftTemplate shiftTemplate(Team team, Long id, String name, int cycleDays) {
        ShiftTemplate shiftTemplate = new ShiftTemplate(team, name, null, cycleDays, 8);
        ReflectionTestUtils.setField(shiftTemplate, "id", id);
        return shiftTemplate;
    }

    private TemplateSlot templateSlot(
            ShiftTemplate shiftTemplate,
            Long id,
            int dayOffset,
            LocalTime startTime,
            String description
    ) {
        return templateSlot(shiftTemplate, id, dayOffset, startTime, 480, description, 2, null);
    }

    private TemplateSlot templateSlot(
            ShiftTemplate shiftTemplate,
            Long id,
            int dayOffset,
            LocalTime startTime,
            int durationMinutes,
            String description,
            int requiredWorkers,
            StaffingRole requiredStaffingRole
    ) {
        TemplateSlot templateSlot = new TemplateSlot(
                shiftTemplate,
                dayOffset,
                startTime,
                durationMinutes,
                description,
                requiredWorkers,
                requiredStaffingRole
        );
        ReflectionTestUtils.setField(templateSlot, "id", id);
        return templateSlot;
    }

    private StaffingRole staffingRole(Team team, Long id, String name) {
        StaffingRole staffingRole = new StaffingRole(team, name, null);
        ReflectionTestUtils.setField(staffingRole, "id", id);
        return staffingRole;
    }

    private Schedule schedule(
            Team team,
            Long id,
            LocalDate startDate,
            LocalDate endDate,
            ScheduleStatus status
    ) {
        Schedule schedule = new Schedule(team, startDate, endDate);
        ReflectionTestUtils.setField(schedule, "id", id);

        if (status == ScheduleStatus.PUBLISHED) {
            schedule.publish(Instant.parse("2026-07-01T09:00:00Z"));
        }

        return schedule;
    }

    private Team team(Long id, String name) {
        Team team = new Team(name, SwapApprovalPolicy.MANAGER, 8, "Asia/Jerusalem");
        ReflectionTestUtils.setField(team, "id", id);
        return team;
    }
}
