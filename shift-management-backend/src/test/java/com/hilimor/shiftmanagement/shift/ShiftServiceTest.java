package com.hilimor.shiftmanagement.shift;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.hilimor.shiftmanagement.assignment.AssignmentRepository;
import com.hilimor.shiftmanagement.assignment.AssignmentValidator;
import com.hilimor.shiftmanagement.request.SwapRequestRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import com.hilimor.shiftmanagement.schedule.Schedule;
import com.hilimor.shiftmanagement.schedule.ScheduleRepository;
import com.hilimor.shiftmanagement.schedule.ScheduleStatus;
import com.hilimor.shiftmanagement.schedule.ScheduleWriteLock;
import com.hilimor.shiftmanagement.staffing.StaffingRole;
import com.hilimor.shiftmanagement.staffing.StaffingRoleRepository;
import com.hilimor.shiftmanagement.team.SwapApprovalPolicy;
import com.hilimor.shiftmanagement.team.Team;
import com.hilimor.shiftmanagement.team.TeamManagerRepository;

@ExtendWith(MockitoExtension.class)
class ShiftServiceTest {

    @Mock
    private ScheduleRepository scheduleRepository;

    @Mock
    private ShiftRepository shiftRepository;

    @Mock
    private TeamManagerRepository teamManagerRepository;

    @Mock
    private StaffingRoleRepository staffingRoleRepository;

    @Mock
    private AssignmentRepository assignmentRepository;

    @Mock
    private AssignmentValidator assignmentValidator;

    @Mock
    private ScheduleWriteLock writeLock;

    @Mock
    private SwapRequestRepository requestRepository;

    @InjectMocks
    private ShiftService shiftService;

    @Test
    void createShiftSavesShiftForManagedDraftSchedule() {
        Schedule schedule = schedule(ScheduleStatus.DRAFT);
        CreateShiftRequest request = new CreateShiftRequest(
                Instant.parse("2026-07-06T06:00:00Z"),
                Instant.parse("2026-07-06T14:00:00Z"),
                "Morning shift",
                2,
                8
        );

        when(scheduleRepository.findById(10L)).thenReturn(Optional.of(schedule));
        when(teamManagerRepository.existsByManager_UsernameAndTeam_Id("manager1", 1L)).thenReturn(true);
        when(shiftRepository.save(any(Shift.class))).thenAnswer(invocation -> {
            Shift shift = invocation.getArgument(0);
            ReflectionTestUtils.setField(shift, "id", 20L);
            return shift;
        });

        ShiftResponse response = shiftService.createShift("manager1", 10L, request);

        assertThat(response.id()).isEqualTo(20L);
        assertThat(response.scheduleId()).isEqualTo(10L);
        assertThat(response.startTime()).isEqualTo(Instant.parse("2026-07-06T06:00:00Z"));
        assertThat(response.endTime()).isEqualTo(Instant.parse("2026-07-06T14:00:00Z"));
        assertThat(response.description()).isEqualTo("Morning shift");
        assertThat(response.requiredWorkers()).isEqualTo(2);
        assertThat(response.minRestHours()).isEqualTo(8);
        assertThat(response.requiredStaffingRoleId()).isNull();
        assertThat(response.requiredStaffingRoleName()).isNull();

        ArgumentCaptor<Shift> captor = ArgumentCaptor.forClass(Shift.class);
        verify(shiftRepository).save(captor.capture());
        assertThat(captor.getValue().getSchedule()).isSameAs(schedule);
    }

    @Test
    void createShiftCanRequireStaffingRoleFromScheduleTeam() {
        Schedule schedule = schedule(ScheduleStatus.DRAFT);
        StaffingRole staffingRole = staffingRole(schedule.getTeam(), 30L, "Shift Supervisor");
        CreateShiftRequest request = new CreateShiftRequest(
                Instant.parse("2026-07-06T06:00:00Z"),
                Instant.parse("2026-07-06T14:00:00Z"),
                "Supervisor shift",
                1,
                8,
                30L
        );

        when(scheduleRepository.findById(10L)).thenReturn(Optional.of(schedule));
        when(teamManagerRepository.existsByManager_UsernameAndTeam_Id("manager1", 1L)).thenReturn(true);
        when(staffingRoleRepository.findById(30L)).thenReturn(Optional.of(staffingRole));
        when(shiftRepository.save(any(Shift.class))).thenAnswer(invocation -> {
            Shift shift = invocation.getArgument(0);
            ReflectionTestUtils.setField(shift, "id", 20L);
            return shift;
        });

        ShiftResponse response = shiftService.createShift("manager1", 10L, request);

        assertThat(response.requiredStaffingRoleId()).isEqualTo(30L);
        assertThat(response.requiredStaffingRoleName()).isEqualTo("Shift Supervisor");

        ArgumentCaptor<Shift> captor = ArgumentCaptor.forClass(Shift.class);
        verify(shiftRepository).save(captor.capture());
        assertThat(captor.getValue().getRequiredStaffingRole()).isSameAs(staffingRole);
    }

    @Test
    void createShiftRejectsUnmanagedSchedule() {
        Schedule schedule = schedule(ScheduleStatus.DRAFT);

        when(scheduleRepository.findById(10L)).thenReturn(Optional.of(schedule));
        when(teamManagerRepository.existsByManager_UsernameAndTeam_Id("manager2", 1L)).thenReturn(false);

        assertThatThrownBy(() -> shiftService.createShift("manager2", 10L, validRequest()))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));

        verify(shiftRepository, never()).save(any());
    }

    @Test
    void createShiftRejectsPublishedSchedule() {
        Schedule schedule = schedule(ScheduleStatus.PUBLISHED);

        when(scheduleRepository.findById(10L)).thenReturn(Optional.of(schedule));
        when(teamManagerRepository.existsByManager_UsernameAndTeam_Id("manager1", 1L)).thenReturn(true);

        assertThatThrownBy(() -> shiftService.createShift("manager1", 10L, validRequest()))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));

        verify(shiftRepository, never()).save(any());
    }

    @Test
    void createShiftRejectsInvalidTimeRange() {
        CreateShiftRequest request = new CreateShiftRequest(
                Instant.parse("2026-07-06T14:00:00Z"),
                Instant.parse("2026-07-06T06:00:00Z"),
                "Invalid shift",
                2,
                8
        );

        assertThatThrownBy(() -> shiftService.createShift("manager1", 10L, request))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));

        verify(shiftRepository, never()).save(any());
    }

    @Test
    void createShiftRejectsShiftBeforeScheduleDateRange() {
        Schedule schedule = schedule(ScheduleStatus.DRAFT);
        CreateShiftRequest request = new CreateShiftRequest(
                Instant.parse("2026-07-05T18:00:00Z"),
                Instant.parse("2026-07-05T20:30:00Z"),
                "Before schedule",
                2,
                8
        );

        when(scheduleRepository.findById(10L)).thenReturn(Optional.of(schedule));
        when(teamManagerRepository.existsByManager_UsernameAndTeam_Id("manager1", 1L)).thenReturn(true);

        assertThatThrownBy(() -> shiftService.createShift("manager1", 10L, request))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));

        verify(shiftRepository, never()).save(any());
    }

    @Test
    void createShiftRejectsMissingRequiredStaffingRole() {
        Schedule schedule = schedule(ScheduleStatus.DRAFT);
        CreateShiftRequest request = new CreateShiftRequest(
                Instant.parse("2026-07-06T06:00:00Z"),
                Instant.parse("2026-07-06T14:00:00Z"),
                "Supervisor shift",
                1,
                8,
                99L
        );

        when(scheduleRepository.findById(10L)).thenReturn(Optional.of(schedule));
        when(teamManagerRepository.existsByManager_UsernameAndTeam_Id("manager1", 1L)).thenReturn(true);
        when(staffingRoleRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> shiftService.createShift("manager1", 10L, request))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));

        verify(shiftRepository, never()).save(any());
    }

    @Test
    void createShiftRejectsRequiredStaffingRoleFromAnotherTeam() {
        Schedule schedule = schedule(ScheduleStatus.DRAFT);
        Team otherTeam = new Team("Support", SwapApprovalPolicy.MANAGER, 8, "Asia/Jerusalem");
        ReflectionTestUtils.setField(otherTeam, "id", 2L);
        StaffingRole staffingRole = staffingRole(otherTeam, 30L, "Shift Supervisor");
        CreateShiftRequest request = new CreateShiftRequest(
                Instant.parse("2026-07-06T06:00:00Z"),
                Instant.parse("2026-07-06T14:00:00Z"),
                "Supervisor shift",
                1,
                8,
                30L
        );

        when(scheduleRepository.findById(10L)).thenReturn(Optional.of(schedule));
        when(teamManagerRepository.existsByManager_UsernameAndTeam_Id("manager1", 1L)).thenReturn(true);
        when(staffingRoleRepository.findById(30L)).thenReturn(Optional.of(staffingRole));

        assertThatThrownBy(() -> shiftService.createShift("manager1", 10L, request))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));

        verify(shiftRepository, never()).save(any());
    }

    @Test
    void createShiftAllowsShiftEndingAtMidnightAfterScheduleEndDate() {
        Schedule schedule = schedule(ScheduleStatus.DRAFT);
        CreateShiftRequest request = new CreateShiftRequest(
                Instant.parse("2026-07-12T12:00:00Z"),
                Instant.parse("2026-07-12T21:00:00Z"),
                "Last day shift",
                2,
                8
        );

        when(scheduleRepository.findById(10L)).thenReturn(Optional.of(schedule));
        when(teamManagerRepository.existsByManager_UsernameAndTeam_Id("manager1", 1L)).thenReturn(true);
        when(shiftRepository.save(any(Shift.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ShiftResponse response = shiftService.createShift("manager1", 10L, request);

        assertThat(response.description()).isEqualTo("Last day shift");
    }

    @Test
    void listShiftsReturnsShiftsForManagedSchedule() {
        Schedule schedule = schedule(ScheduleStatus.DRAFT);
        Shift shift = new Shift(
                schedule,
                Instant.parse("2026-07-06T06:00:00Z"),
                Instant.parse("2026-07-06T14:00:00Z"),
                "Morning shift",
                2,
                8
        );
        ReflectionTestUtils.setField(shift, "id", 20L);

        when(scheduleRepository.findById(10L)).thenReturn(Optional.of(schedule));
        when(teamManagerRepository.existsByManager_UsernameAndTeam_Id("manager1", 1L)).thenReturn(true);
        when(shiftRepository.findBySchedule_IdOrderByStartTime(10L)).thenReturn(List.of(shift));

        List<ShiftResponse> responses = shiftService.listShifts("manager1", 10L);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).id()).isEqualTo(20L);
        assertThat(responses.get(0).scheduleId()).isEqualTo(10L);
        assertThat(responses.get(0).description()).isEqualTo("Morning shift");
    }

    @Test
    void listShiftsRejectsUnmanagedSchedule() {
        Schedule schedule = schedule(ScheduleStatus.DRAFT);

        when(scheduleRepository.findById(10L)).thenReturn(Optional.of(schedule));
        when(teamManagerRepository.existsByManager_UsernameAndTeam_Id("manager2", 1L)).thenReturn(false);

        assertThatThrownBy(() -> shiftService.listShifts("manager2", 10L))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void updateShiftChangesShiftForManagedDraftSchedule() {
        Schedule schedule = schedule(ScheduleStatus.DRAFT);
        Shift shift = shift(schedule);
        UpdateShiftRequest request = new UpdateShiftRequest(
                Instant.parse("2026-07-06T14:00:00Z"),
                Instant.parse("2026-07-06T22:00:00Z"),
                "Evening shift",
                3,
                10,
                null,
                0L
        );

        when(scheduleRepository.findById(10L)).thenReturn(Optional.of(schedule));
        when(teamManagerRepository.existsByManager_UsernameAndTeam_Id("manager1", 1L)).thenReturn(true);
        when(shiftRepository.findById(20L)).thenReturn(Optional.of(shift));
        doAnswer(invocation -> {
            ReflectionTestUtils.setField(shift, "version", 1L);
            return null;
        }).when(shiftRepository).flush();

        ShiftResponse response = shiftService.updateShift("manager1", 10L, 20L, request);

        var order = inOrder(writeLock, assignmentValidator, shiftRepository);
        order.verify(writeLock).lockShift(shift);
        order.verify(assignmentValidator).validateExistingAssignments(shift, List.of());
        order.verify(shiftRepository).flush();
        assertThat(response.version()).isEqualTo(1L);
        assertThat(response.id()).isEqualTo(20L);
        assertThat(response.scheduleId()).isEqualTo(10L);
        assertThat(response.startTime()).isEqualTo(Instant.parse("2026-07-06T14:00:00Z"));
        assertThat(response.endTime()).isEqualTo(Instant.parse("2026-07-06T22:00:00Z"));
        assertThat(response.description()).isEqualTo("Evening shift");
        assertThat(response.requiredWorkers()).isEqualTo(3);
        assertThat(response.minRestHours()).isEqualTo(10);
        assertThat(response.requiredStaffingRoleId()).isNull();
        assertThat(response.requiredStaffingRoleName()).isNull();
    }

    @Test
    void updateShiftCanSetRequiredStaffingRole() {
        Schedule schedule = schedule(ScheduleStatus.DRAFT);
        Shift shift = shift(schedule);
        StaffingRole staffingRole = staffingRole(schedule.getTeam(), 30L, "Shift Supervisor");
        UpdateShiftRequest request = new UpdateShiftRequest(
                Instant.parse("2026-07-06T14:00:00Z"),
                Instant.parse("2026-07-06T22:00:00Z"),
                "Evening supervisor shift",
                1,
                10,
                30L,
                0L
        );

        when(scheduleRepository.findById(10L)).thenReturn(Optional.of(schedule));
        when(teamManagerRepository.existsByManager_UsernameAndTeam_Id("manager1", 1L)).thenReturn(true);
        when(shiftRepository.findById(20L)).thenReturn(Optional.of(shift));
        when(staffingRoleRepository.findById(30L)).thenReturn(Optional.of(staffingRole));

        ShiftResponse response = shiftService.updateShift("manager1", 10L, 20L, request);

        assertThat(response.requiredStaffingRoleId()).isEqualTo(30L);
        assertThat(response.requiredStaffingRoleName()).isEqualTo("Shift Supervisor");
        assertThat(shift.getRequiredStaffingRole()).isSameAs(staffingRole);
    }

    @Test
    void updateShiftRejectsPublishedSchedule() {
        Schedule schedule = schedule(ScheduleStatus.PUBLISHED);

        when(scheduleRepository.findById(10L)).thenReturn(Optional.of(schedule));
        when(teamManagerRepository.existsByManager_UsernameAndTeam_Id("manager1", 1L)).thenReturn(true);

        assertThatThrownBy(() -> shiftService.updateShift("manager1", 10L, 20L, validUpdateRequest()))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void updateShiftChecksVersionAfterRefreshingTheLockedShift() {
        Schedule schedule = schedule(ScheduleStatus.DRAFT);
        Shift shift = shift(schedule);
        when(scheduleRepository.findById(10L)).thenReturn(Optional.of(schedule));
        when(teamManagerRepository.existsByManager_UsernameAndTeam_Id("manager1", 1L)).thenReturn(true);
        when(shiftRepository.findById(20L)).thenReturn(Optional.of(shift));
        doAnswer(invocation -> {
            ReflectionTestUtils.setField(shift, "version", 1L);
            return null;
        }).when(writeLock).lockShift(shift);

        assertThatThrownBy(() -> shiftService.updateShift("manager1", 10L, 20L, validUpdateRequest()))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);

        assertThat(shift.getDescription()).isEqualTo("Morning shift");
        verifyNoInteractions(assignmentRepository, assignmentValidator, staffingRoleRepository);
        verify(shiftRepository, never()).flush();
    }

    @Test
    void updateShiftRejectsShiftFromAnotherSchedule() {
        Schedule schedule = schedule(ScheduleStatus.DRAFT);
        Schedule otherSchedule = schedule(ScheduleStatus.DRAFT);
        ReflectionTestUtils.setField(otherSchedule, "id", 99L);
        Shift shift = shift(otherSchedule);

        when(scheduleRepository.findById(10L)).thenReturn(Optional.of(schedule));
        when(teamManagerRepository.existsByManager_UsernameAndTeam_Id("manager1", 1L)).thenReturn(true);
        when(shiftRepository.findById(20L)).thenReturn(Optional.of(shift));

        assertThatThrownBy(() -> shiftService.updateShift("manager1", 10L, 20L, validUpdateRequest()))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void updateShiftRejectsInvalidTimeRange() {
        UpdateShiftRequest request = new UpdateShiftRequest(
                Instant.parse("2026-07-06T22:00:00Z"),
                Instant.parse("2026-07-06T14:00:00Z"),
                "Invalid shift",
                3,
                10,
                null,
                0L
        );

        assertThatThrownBy(() -> shiftService.updateShift("manager1", 10L, 20L, request))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void updateShiftRejectsShiftAfterScheduleDateRange() {
        Schedule schedule = schedule(ScheduleStatus.DRAFT);
        Shift shift = shift(schedule);
        UpdateShiftRequest request = new UpdateShiftRequest(
                Instant.parse("2026-07-12T21:00:00Z"),
                Instant.parse("2026-07-13T05:00:00Z"),
                "After schedule",
                3,
                10,
                null,
                0L
        );

        when(scheduleRepository.findById(10L)).thenReturn(Optional.of(schedule));
        when(teamManagerRepository.existsByManager_UsernameAndTeam_Id("manager1", 1L)).thenReturn(true);
        when(shiftRepository.findById(20L)).thenReturn(Optional.of(shift));

        assertThatThrownBy(() -> shiftService.updateShift("manager1", 10L, 20L, request))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void deleteShiftRemovesShiftForManagedDraftSchedule() {
        Schedule schedule = schedule(ScheduleStatus.DRAFT);
        Shift shift = shift(schedule);

        when(scheduleRepository.findById(10L)).thenReturn(Optional.of(schedule));
        when(teamManagerRepository.existsByManager_UsernameAndTeam_Id("manager1", 1L)).thenReturn(true);
        when(shiftRepository.findById(20L)).thenReturn(Optional.of(shift));

        String revision = shiftService.previewShiftDeletion("manager1", 10L, 20L).revision();
        shiftService.deleteShift("manager1", 10L, 20L, revision);

        verify(shiftRepository).delete(shift);
    }

    @Test
    void deleteShiftRejectsUnmanagedSchedule() {
        Schedule schedule = schedule(ScheduleStatus.DRAFT);

        when(scheduleRepository.findById(10L)).thenReturn(Optional.of(schedule));
        when(teamManagerRepository.existsByManager_UsernameAndTeam_Id("manager2", 1L)).thenReturn(false);

        assertThatThrownBy(() -> shiftService.deleteShift("manager2", 10L, 20L, null))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));

        verify(shiftRepository, never()).delete(any());
    }

    @Test
    void deleteShiftRejectsPublishedSchedule() {
        Schedule schedule = schedule(ScheduleStatus.PUBLISHED);

        when(scheduleRepository.findById(10L)).thenReturn(Optional.of(schedule));
        when(teamManagerRepository.existsByManager_UsernameAndTeam_Id("manager1", 1L)).thenReturn(true);

        assertThatThrownBy(() -> shiftService.deleteShift("manager1", 10L, 20L, null))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));

        verify(shiftRepository, never()).delete(any());
    }

    @Test
    void deleteShiftRejectsShiftFromAnotherSchedule() {
        Schedule schedule = schedule(ScheduleStatus.DRAFT);
        Schedule otherSchedule = schedule(ScheduleStatus.DRAFT);
        ReflectionTestUtils.setField(otherSchedule, "id", 99L);
        Shift shift = shift(otherSchedule);

        when(scheduleRepository.findById(10L)).thenReturn(Optional.of(schedule));
        when(teamManagerRepository.existsByManager_UsernameAndTeam_Id("manager1", 1L)).thenReturn(true);
        when(shiftRepository.findById(20L)).thenReturn(Optional.of(shift));

        assertThatThrownBy(() -> shiftService.deleteShift("manager1", 10L, 20L, null))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));

        verify(shiftRepository, never()).delete(any());
    }

    private CreateShiftRequest validRequest() {
        return new CreateShiftRequest(
                Instant.parse("2026-07-06T06:00:00Z"),
                Instant.parse("2026-07-06T14:00:00Z"),
                "Morning shift",
                2,
                8
        );
    }

    private UpdateShiftRequest validUpdateRequest() {
        return new UpdateShiftRequest(
                Instant.parse("2026-07-06T14:00:00Z"),
                Instant.parse("2026-07-06T22:00:00Z"),
                "Evening shift",
                3,
                10,
                null,
                0L
        );
    }

    private Shift shift(Schedule schedule) {
        Shift shift = new Shift(
                schedule,
                Instant.parse("2026-07-06T06:00:00Z"),
                Instant.parse("2026-07-06T14:00:00Z"),
                "Morning shift",
                2,
                8
        );
        ReflectionTestUtils.setField(shift, "id", 20L);
        ReflectionTestUtils.setField(shift, "version", 0L);
        return shift;
    }

    private StaffingRole staffingRole(Team team, Long id, String name) {
        StaffingRole staffingRole = new StaffingRole(team, name, null);
        ReflectionTestUtils.setField(staffingRole, "id", id);
        return staffingRole;
    }

    private Schedule schedule(ScheduleStatus status) {
        Team team = new Team("Operations", SwapApprovalPolicy.MANAGER, 8, "Asia/Jerusalem");
        ReflectionTestUtils.setField(team, "id", 1L);

        Schedule schedule = new Schedule(team, LocalDate.of(2026, 7, 6), LocalDate.of(2026, 7, 12));
        ReflectionTestUtils.setField(schedule, "id", 10L);
        ReflectionTestUtils.setField(schedule, "version", 0L);
        ReflectionTestUtils.setField(schedule, "status", status);
        return schedule;
    }
}
