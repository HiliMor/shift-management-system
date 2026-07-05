package com.hilimor.shiftmanagement.schedule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

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

        ArgumentCaptor<Shift> captor = ArgumentCaptor.forClass(Shift.class);
        verify(shiftRepository).save(captor.capture());
        assertThat(captor.getValue().getSchedule()).isSameAs(schedule);
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

    private CreateShiftRequest validRequest() {
        return new CreateShiftRequest(
                Instant.parse("2026-07-06T06:00:00Z"),
                Instant.parse("2026-07-06T14:00:00Z"),
                "Morning shift",
                2,
                8
        );
    }

    private Schedule schedule(ScheduleStatus status) {
        Team team = new Team("Operations", SwapApprovalPolicy.MANAGER, 8, "Asia/Jerusalem");
        ReflectionTestUtils.setField(team, "id", 1L);

        Schedule schedule = new Schedule(team, LocalDate.of(2026, 7, 6), LocalDate.of(2026, 7, 12));
        ReflectionTestUtils.setField(schedule, "id", 10L);
        ReflectionTestUtils.setField(schedule, "status", status);
        return schedule;
    }
}
