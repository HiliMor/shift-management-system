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

import com.hilimor.shiftmanagement.assignment.Assignment;
import com.hilimor.shiftmanagement.assignment.AssignmentRepository;
import com.hilimor.shiftmanagement.shift.Shift;
import com.hilimor.shiftmanagement.shift.ShiftRepository;
import com.hilimor.shiftmanagement.team.SwapApprovalPolicy;
import com.hilimor.shiftmanagement.team.Team;
import com.hilimor.shiftmanagement.team.TeamManagerRepository;
import com.hilimor.shiftmanagement.team.TeamMember;
import com.hilimor.shiftmanagement.team.TeamMemberRepository;
import com.hilimor.shiftmanagement.team.TeamRepository;
import com.hilimor.shiftmanagement.user.ApplicationRole;
import com.hilimor.shiftmanagement.user.User;
import com.hilimor.shiftmanagement.user.UserRepository;

@ExtendWith(MockitoExtension.class)
class ScheduleServiceTest {

    @Mock
    private ScheduleRepository scheduleRepository;

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private TeamManagerRepository teamManagerRepository;

    @Mock
    private TeamMemberRepository teamMemberRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ShiftRepository shiftRepository;

    @Mock
    private AssignmentRepository assignmentRepository;

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

    @Test
    void publishSchedulePublishesManagedDraftSchedule() {
        Schedule schedule = schedule(ScheduleStatus.DRAFT);

        when(scheduleRepository.findById(10L)).thenReturn(Optional.of(schedule));
        when(teamManagerRepository.existsByManager_UsernameAndTeam_Id("manager1", 1L)).thenReturn(true);

        ScheduleResponse response = scheduleService.publishSchedule("manager1", 10L);

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.teamId()).isEqualTo(1L);
        assertThat(response.status()).isEqualTo(ScheduleStatus.PUBLISHED);
        assertThat(response.publicationNumber()).isEqualTo(1);
        assertThat(response.publishedAt()).isNotNull();
        assertThat(schedule.getStatus()).isEqualTo(ScheduleStatus.PUBLISHED);
        assertThat(schedule.getPublicationNumber()).isEqualTo(1);
        assertThat(schedule.getPublishedAt()).isNotNull();
    }

    @Test
    void publishScheduleRejectsMissingSchedule() {
        when(scheduleRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> scheduleService.publishSchedule("manager1", 10L))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void publishScheduleRejectsUnmanagedSchedule() {
        Schedule schedule = schedule(ScheduleStatus.DRAFT);

        when(scheduleRepository.findById(10L)).thenReturn(Optional.of(schedule));
        when(teamManagerRepository.existsByManager_UsernameAndTeam_Id("manager2", 1L)).thenReturn(false);

        assertThatThrownBy(() -> scheduleService.publishSchedule("manager2", 10L))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));

        assertThat(schedule.getStatus()).isEqualTo(ScheduleStatus.DRAFT);
    }

    @Test
    void publishScheduleRejectsAlreadyPublishedSchedule() {
        Schedule schedule = schedule(ScheduleStatus.PUBLISHED);

        when(scheduleRepository.findById(10L)).thenReturn(Optional.of(schedule));
        when(teamManagerRepository.existsByManager_UsernameAndTeam_Id("manager1", 1L)).thenReturn(true);

        assertThatThrownBy(() -> scheduleService.publishSchedule("manager1", 10L))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void reopenScheduleReopensManagedPublishedSchedule() {
        Schedule schedule = schedule(ScheduleStatus.PUBLISHED);
        Instant previousPublishedAt = schedule.getPublishedAt();

        when(scheduleRepository.findById(10L)).thenReturn(Optional.of(schedule));
        when(teamManagerRepository.existsByManager_UsernameAndTeam_Id("manager1", 1L)).thenReturn(true);

        ScheduleResponse response = scheduleService.reopenSchedule("manager1", 10L);

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.teamId()).isEqualTo(1L);
        assertThat(response.status()).isEqualTo(ScheduleStatus.DRAFT);
        assertThat(response.publicationNumber()).isEqualTo(1);
        assertThat(response.publishedAt()).isEqualTo(previousPublishedAt);
        assertThat(schedule.getStatus()).isEqualTo(ScheduleStatus.DRAFT);
        assertThat(schedule.getPublicationNumber()).isEqualTo(1);
        assertThat(schedule.getPublishedAt()).isEqualTo(previousPublishedAt);
    }

    @Test
    void reopenScheduleRejectsMissingSchedule() {
        when(scheduleRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> scheduleService.reopenSchedule("manager1", 10L))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void reopenScheduleRejectsUnmanagedSchedule() {
        Schedule schedule = schedule(ScheduleStatus.PUBLISHED);

        when(scheduleRepository.findById(10L)).thenReturn(Optional.of(schedule));
        when(teamManagerRepository.existsByManager_UsernameAndTeam_Id("manager2", 1L)).thenReturn(false);

        assertThatThrownBy(() -> scheduleService.reopenSchedule("manager2", 10L))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));

        assertThat(schedule.getStatus()).isEqualTo(ScheduleStatus.PUBLISHED);
    }

    @Test
    void reopenScheduleRejectsDraftSchedule() {
        Schedule schedule = schedule(ScheduleStatus.DRAFT);

        when(scheduleRepository.findById(10L)).thenReturn(Optional.of(schedule));
        when(teamManagerRepository.existsByManager_UsernameAndTeam_Id("manager1", 1L)).thenReturn(true);

        assertThatThrownBy(() -> scheduleService.reopenSchedule("manager1", 10L))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void listPublishedSchedulesForUserReturnsPublishedSchedulesForActiveTeamMemberships() {
        User employee = employee();
        Team team = team();
        TeamMember teamMember = teamMember(employee, team);
        Schedule publishedSchedule = schedule(team, ScheduleStatus.PUBLISHED, 11L, LocalDate.of(2026, 7, 12));

        when(userRepository.findByUsername("employee1")).thenReturn(Optional.of(employee));
        when(teamMemberRepository.findByUser_IdAndActiveTrue(2L)).thenReturn(List.of(teamMember));
        when(scheduleRepository.findByTeam_IdInAndStatusOrderByStartDateDesc(
                List.of(1L),
                ScheduleStatus.PUBLISHED
        )).thenReturn(List.of(publishedSchedule));

        List<ScheduleResponse> responses = scheduleService.listPublishedSchedulesForUser("employee1");

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).id()).isEqualTo(11L);
        assertThat(responses.get(0).teamId()).isEqualTo(1L);
        assertThat(responses.get(0).teamName()).isEqualTo("Operations");
        assertThat(responses.get(0).status()).isEqualTo(ScheduleStatus.PUBLISHED);
    }

    @Test
    void listPublishedSchedulesForUserReturnsEmptyListWhenUserHasNoActiveTeams() {
        User employee = employee();

        when(userRepository.findByUsername("employee1")).thenReturn(Optional.of(employee));
        when(teamMemberRepository.findByUser_IdAndActiveTrue(2L)).thenReturn(List.of());

        List<ScheduleResponse> responses = scheduleService.listPublishedSchedulesForUser("employee1");

        assertThat(responses).isEmpty();
        verify(scheduleRepository, never()).findByTeam_IdInAndStatusOrderByStartDateDesc(any(), any());
    }

    @Test
    void listPublishedSchedulesForUserRejectsMissingUser() {
        when(userRepository.findByUsername("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> scheduleService.listPublishedSchedulesForUser("missing"))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void getPublishedScheduleDetailsForUserReturnsShiftsAndAssignmentsForActiveTeamMember() {
        User employee = employee();
        Team team = team();
        TeamMember teamMember = teamMember(employee, team);
        Schedule schedule = schedule(team, ScheduleStatus.PUBLISHED, 12L, LocalDate.of(2026, 7, 12));
        Shift morningShift = shift(
                schedule,
                101L,
                Instant.parse("2026-07-12T06:00:00Z"),
                Instant.parse("2026-07-12T14:00:00Z"),
                "Morning shift"
        );
        Shift eveningShift = shift(
                schedule,
                102L,
                Instant.parse("2026-07-12T14:00:00Z"),
                Instant.parse("2026-07-12T22:00:00Z"),
                "Evening shift"
        );
        Assignment assignment = assignment(morningShift, employee, 201L);

        when(userRepository.findByUsername("employee1")).thenReturn(Optional.of(employee));
        when(scheduleRepository.findById(12L)).thenReturn(Optional.of(schedule));
        when(teamMemberRepository.findByUser_IdAndTeam_IdAndActiveTrue(2L, 1L)).thenReturn(Optional.of(teamMember));
        when(assignmentRepository.findByShift_Schedule_IdOrderByShift_StartTimeAscEmployee_FullNameAsc(12L))
                .thenReturn(List.of(assignment));
        when(shiftRepository.findBySchedule_IdOrderByStartTime(12L)).thenReturn(List.of(morningShift, eveningShift));

        PublishedScheduleDetailsResponse response = scheduleService.getPublishedScheduleDetailsForUser(
                "employee1",
                12L
        );

        assertThat(response.schedule().id()).isEqualTo(12L);
        assertThat(response.schedule().status()).isEqualTo(ScheduleStatus.PUBLISHED);
        assertThat(response.shifts()).hasSize(2);
        assertThat(response.shifts().get(0).id()).isEqualTo(101L);
        assertThat(response.shifts().get(0).assignments()).hasSize(1);
        assertThat(response.shifts().get(0).assignments().get(0).employeeFullName()).isEqualTo("Demo Employee");
        assertThat(response.shifts().get(1).id()).isEqualTo(102L);
        assertThat(response.shifts().get(1).assignments()).isEmpty();
    }

    @Test
    void getPublishedScheduleDetailsForUserRejectsDraftSchedule() {
        User employee = employee();
        Schedule schedule = schedule(ScheduleStatus.DRAFT);

        when(userRepository.findByUsername("employee1")).thenReturn(Optional.of(employee));
        when(scheduleRepository.findById(10L)).thenReturn(Optional.of(schedule));

        assertThatThrownBy(() -> scheduleService.getPublishedScheduleDetailsForUser("employee1", 10L))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));

        verify(teamMemberRepository, never()).findByUser_IdAndTeam_IdAndActiveTrue(any(), any());
        verify(shiftRepository, never()).findBySchedule_IdOrderByStartTime(any());
        verify(assignmentRepository, never()).findByShift_Schedule_IdOrderByShift_StartTimeAscEmployee_FullNameAsc(any());
    }

    @Test
    void getPublishedScheduleDetailsForUserRejectsScheduleOutsideActiveMembership() {
        User employee = employee();
        Schedule schedule = schedule(ScheduleStatus.PUBLISHED);

        when(userRepository.findByUsername("employee1")).thenReturn(Optional.of(employee));
        when(scheduleRepository.findById(10L)).thenReturn(Optional.of(schedule));
        when(teamMemberRepository.findByUser_IdAndTeam_IdAndActiveTrue(2L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> scheduleService.getPublishedScheduleDetailsForUser("employee1", 10L))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));

        verify(shiftRepository, never()).findBySchedule_IdOrderByStartTime(any());
        verify(assignmentRepository, never()).findByShift_Schedule_IdOrderByShift_StartTimeAscEmployee_FullNameAsc(any());
    }

    @Test
    void getPublicationReadinessReportsUnfilledShiftsForManagedSchedule() {
        Schedule schedule = schedule(ScheduleStatus.DRAFT);
        Shift morningShift = shift(
                schedule,
                101L,
                Instant.parse("2026-07-05T06:00:00Z"),
                Instant.parse("2026-07-05T14:00:00Z"),
                "Morning shift"
        );
        Shift eveningShift = shift(
                schedule,
                102L,
                Instant.parse("2026-07-05T14:00:00Z"),
                Instant.parse("2026-07-05T22:00:00Z"),
                "Evening shift"
        );
        User employee = employee();
        User secondEmployee = employee2();

        when(scheduleRepository.findById(10L)).thenReturn(Optional.of(schedule));
        when(teamManagerRepository.existsByManager_UsernameAndTeam_Id("manager1", 1L)).thenReturn(true);
        when(assignmentRepository.findByShift_Schedule_IdOrderByShift_StartTimeAscEmployee_FullNameAsc(10L))
                .thenReturn(List.of(
                        assignment(morningShift, employee, 201L),
                        assignment(eveningShift, employee, 202L),
                        assignment(eveningShift, secondEmployee, 203L)
                ));
        when(shiftRepository.findBySchedule_IdOrderByStartTime(10L)).thenReturn(List.of(morningShift, eveningShift));

        SchedulePublicationReadinessResponse response = scheduleService.getPublicationReadiness("manager1", 10L);

        assertThat(response.schedule().id()).isEqualTo(10L);
        assertThat(response.readyToPublish()).isFalse();
        assertThat(response.totalShifts()).isEqualTo(2);
        assertThat(response.totalRequiredWorkers()).isEqualTo(4);
        assertThat(response.totalAssignedWorkers()).isEqualTo(3);
        assertThat(response.totalOpenSlots()).isEqualTo(1);
        assertThat(response.unfilledShifts()).hasSize(1);
        assertThat(response.unfilledShifts().get(0).shiftId()).isEqualTo(101L);
        assertThat(response.unfilledShifts().get(0).assignedWorkers()).isEqualTo(1);
        assertThat(response.unfilledShifts().get(0).openSlots()).isEqualTo(1);
        assertThat(response.unfilledShifts().get(0).filled()).isFalse();
    }

    @Test
    void getPublicationReadinessReportsReadyScheduleWhenAllShiftsAreFilled() {
        Schedule schedule = schedule(ScheduleStatus.DRAFT);
        Shift shift = shift(
                schedule,
                101L,
                Instant.parse("2026-07-05T06:00:00Z"),
                Instant.parse("2026-07-05T14:00:00Z"),
                "Morning shift"
        );
        User employee = employee();
        User secondEmployee = employee2();

        when(scheduleRepository.findById(10L)).thenReturn(Optional.of(schedule));
        when(teamManagerRepository.existsByManager_UsernameAndTeam_Id("manager1", 1L)).thenReturn(true);
        when(assignmentRepository.findByShift_Schedule_IdOrderByShift_StartTimeAscEmployee_FullNameAsc(10L))
                .thenReturn(List.of(
                        assignment(shift, employee, 201L),
                        assignment(shift, secondEmployee, 202L)
                ));
        when(shiftRepository.findBySchedule_IdOrderByStartTime(10L)).thenReturn(List.of(shift));

        SchedulePublicationReadinessResponse response = scheduleService.getPublicationReadiness("manager1", 10L);

        assertThat(response.readyToPublish()).isTrue();
        assertThat(response.totalShifts()).isEqualTo(1);
        assertThat(response.totalRequiredWorkers()).isEqualTo(2);
        assertThat(response.totalAssignedWorkers()).isEqualTo(2);
        assertThat(response.totalOpenSlots()).isZero();
        assertThat(response.unfilledShifts()).isEmpty();
    }

    @Test
    void getPublicationReadinessRejectsUnmanagedSchedule() {
        Schedule schedule = schedule(ScheduleStatus.DRAFT);

        when(scheduleRepository.findById(10L)).thenReturn(Optional.of(schedule));
        when(teamManagerRepository.existsByManager_UsernameAndTeam_Id("manager2", 1L)).thenReturn(false);

        assertThatThrownBy(() -> scheduleService.getPublicationReadiness("manager2", 10L))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));

        verify(shiftRepository, never()).findBySchedule_IdOrderByStartTime(any());
        verify(assignmentRepository, never()).findByShift_Schedule_IdOrderByShift_StartTimeAscEmployee_FullNameAsc(any());
    }

    private Team team() {
        Team team = new Team("Operations", SwapApprovalPolicy.MANAGER, 8, "Asia/Jerusalem");
        ReflectionTestUtils.setField(team, "id", 1L);
        return team;
    }

    private User employee() {
        return employee("employee1", 2L, "Demo Employee");
    }

    private User employee2() {
        return employee("employee2", 3L, "Second Employee");
    }

    private User employee(String username, Long id, String fullName) {
        User user = new User(
                username,
                "password-hash",
                fullName,
                username + "@example.com",
                ApplicationRole.EMPLOYEE
        );
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private TeamMember teamMember(User user, Team team) {
        TeamMember teamMember = new TeamMember(
                user,
                team,
                Instant.parse("2026-07-01T08:00:00Z"),
                true
        );
        ReflectionTestUtils.setField(teamMember, "id", 20L);
        return teamMember;
    }

    private Schedule schedule(ScheduleStatus status) {
        return schedule(team(), status, 10L, LocalDate.of(2026, 7, 5));
    }

    private Schedule schedule(Team team, ScheduleStatus status, Long id, LocalDate startDate) {
        Schedule schedule = new Schedule(
                team,
                startDate,
                startDate.plusDays(6)
        );
        ReflectionTestUtils.setField(schedule, "id", id);
        if (status == ScheduleStatus.PUBLISHED) {
            schedule.publish(Instant.parse("2026-07-20T18:00:00Z"));
        }
        return schedule;
    }

    private Shift shift(Schedule schedule, Long id, Instant startTime, Instant endTime, String description) {
        Shift shift = new Shift(
                schedule,
                startTime,
                endTime,
                description,
                2,
                8
        );
        ReflectionTestUtils.setField(shift, "id", id);
        return shift;
    }

    private Assignment assignment(Shift shift, User employee, Long id) {
        Assignment assignment = new Assignment(
                shift,
                employee,
                Instant.parse("2026-07-11T08:00:00Z")
        );
        ReflectionTestUtils.setField(assignment, "id", id);
        return assignment;
    }
}
