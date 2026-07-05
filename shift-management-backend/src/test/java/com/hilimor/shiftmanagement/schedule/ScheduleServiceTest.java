package com.hilimor.shiftmanagement.schedule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
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
import com.hilimor.shiftmanagement.team.TeamRepository;

@ExtendWith(MockitoExtension.class)
class ScheduleServiceTest {

    @Mock
    private ScheduleRepository scheduleRepository;

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private TeamManagerRepository teamManagerRepository;

    @InjectMocks
    private ScheduleService scheduleService;

    @Test
    void createDraftScheduleSavesScheduleForManagedTeam() {
        Team team = team();
        CreateScheduleRequest request = new CreateScheduleRequest(
                1L,
                LocalDate.of(2026, 7, 5),
                LocalDate.of(2026, 7, 11)
        );

        when(teamRepository.findById(1L)).thenReturn(Optional.of(team));
        when(teamManagerRepository.existsByManager_UsernameAndTeam_Id("manager1", 1L)).thenReturn(true);
        when(scheduleRepository.save(any(Schedule.class))).thenAnswer(invocation -> {
            Schedule schedule = invocation.getArgument(0);
            ReflectionTestUtils.setField(schedule, "id", 10L);
            return schedule;
        });

        ScheduleResponse response = scheduleService.createDraftSchedule("manager1", request);

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.teamId()).isEqualTo(1L);
        assertThat(response.teamName()).isEqualTo("Operations");
        assertThat(response.startDate()).isEqualTo(LocalDate.of(2026, 7, 5));
        assertThat(response.endDate()).isEqualTo(LocalDate.of(2026, 7, 11));
        assertThat(response.status()).isEqualTo(ScheduleStatus.DRAFT);
        assertThat(response.publicationNumber()).isZero();
        assertThat(response.publishedAt()).isNull();

        ArgumentCaptor<Schedule> captor = ArgumentCaptor.forClass(Schedule.class);
        verify(scheduleRepository).save(captor.capture());
        assertThat(captor.getValue().getTeam()).isSameAs(team);
    }

    @Test
    void createDraftScheduleRejectsUnmanagedTeam() {
        Team team = team();
        CreateScheduleRequest request = new CreateScheduleRequest(
                1L,
                LocalDate.of(2026, 7, 5),
                LocalDate.of(2026, 7, 11)
        );

        when(teamRepository.findById(1L)).thenReturn(Optional.of(team));
        when(teamManagerRepository.existsByManager_UsernameAndTeam_Id("manager2", 1L)).thenReturn(false);

        assertThatThrownBy(() -> scheduleService.createDraftSchedule("manager2", request))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));

        verify(scheduleRepository, never()).save(any());
    }

    @Test
    void createDraftScheduleRejectsInvalidDateRange() {
        CreateScheduleRequest request = new CreateScheduleRequest(
                1L,
                LocalDate.of(2026, 7, 11),
                LocalDate.of(2026, 7, 5)
        );

        assertThatThrownBy(() -> scheduleService.createDraftSchedule("manager1", request))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));

        verify(scheduleRepository, never()).save(any());
    }

    private Team team() {
        Team team = new Team("Operations", SwapApprovalPolicy.MANAGER, 8, "Asia/Jerusalem");
        ReflectionTestUtils.setField(team, "id", 1L);
        return team;
    }
}
