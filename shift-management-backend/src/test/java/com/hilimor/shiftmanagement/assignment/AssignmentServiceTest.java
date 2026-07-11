package com.hilimor.shiftmanagement.assignment;

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

import com.hilimor.shiftmanagement.schedule.Schedule;
import com.hilimor.shiftmanagement.schedule.ScheduleRepository;
import com.hilimor.shiftmanagement.schedule.ScheduleStatus;
import com.hilimor.shiftmanagement.shift.Shift;
import com.hilimor.shiftmanagement.shift.ShiftRepository;
import com.hilimor.shiftmanagement.team.SwapApprovalPolicy;
import com.hilimor.shiftmanagement.team.Team;
import com.hilimor.shiftmanagement.team.TeamManagerRepository;
import com.hilimor.shiftmanagement.team.TeamMemberRepository;
import com.hilimor.shiftmanagement.user.ApplicationRole;
import com.hilimor.shiftmanagement.user.User;
import com.hilimor.shiftmanagement.user.UserRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class AssignmentServiceTest {

    @Mock
    private AssignmentRepository assignmentRepository;

    @Mock
    private ScheduleRepository scheduleRepository;

    @Mock
    private ShiftRepository shiftRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TeamMemberRepository teamMemberRepository;

    @Mock
    private TeamManagerRepository teamManagerRepository;

    @InjectMocks
    private AssignmentService assignmentService;

    @Test
    void createAssignmentSavesValidAssignment() {
        Shift shift = shift(schedule(ScheduleStatus.DRAFT), 20L);
        User employee = employee();

        when(shiftRepository.findById(20L)).thenReturn(Optional.of(shift));
        when(teamManagerRepository.existsByManager_UsernameAndTeam_Id("manager1", 1L)).thenReturn(true);
        when(userRepository.findById(2L)).thenReturn(Optional.of(employee));
        when(teamMemberRepository.existsByUser_IdAndTeam_IdAndActiveTrue(2L, 1L)).thenReturn(true);
        when(assignmentRepository.existsByShift_IdAndEmployee_Id(20L, 2L)).thenReturn(false);
        when(assignmentRepository.countByShift_Id(20L)).thenReturn(0L);
        when(assignmentRepository.findByEmployee_IdAndShift_StartTimeLessThanAndShift_EndTimeGreaterThan(
                2L,
                shift.getEndTime(),
                shift.getStartTime()
        )).thenReturn(List.of());
        when(assignmentRepository.findTopByEmployee_IdAndShift_EndTimeLessThanEqualOrderByShift_EndTimeDesc(
                2L,
                shift.getStartTime()
        )).thenReturn(Optional.empty());
        when(assignmentRepository.findTopByEmployee_IdAndShift_StartTimeGreaterThanEqualOrderByShift_StartTimeAsc(
                2L,
                shift.getEndTime()
        )).thenReturn(Optional.empty());
        when(assignmentRepository.save(any(Assignment.class))).thenAnswer(invocation -> {
            Assignment assignment = invocation.getArgument(0);
            ReflectionTestUtils.setField(assignment, "id", 30L);
            return assignment;
        });

        AssignmentResponse response = assignmentService.createAssignment("manager1", validRequest());

        assertThat(response.id()).isEqualTo(30L);
        assertThat(response.shiftId()).isEqualTo(20L);
        assertThat(response.employeeId()).isEqualTo(2L);
        assertThat(response.employeeUsername()).isEqualTo("employee1");
        assertThat(response.employeeFullName()).isEqualTo("Demo Employee");
        assertThat(response.assignedAt()).isNotNull();

        ArgumentCaptor<Assignment> captor = ArgumentCaptor.forClass(Assignment.class);
        verify(assignmentRepository).save(captor.capture());
        assertThat(captor.getValue().getShift()).isSameAs(shift);
        assertThat(captor.getValue().getEmployee()).isSameAs(employee);
    }

    @Test
    void createAssignmentRejectsUnmanagedShiftTeam() {
        Shift shift = shift(schedule(ScheduleStatus.DRAFT), 20L);

        when(shiftRepository.findById(20L)).thenReturn(Optional.of(shift));
        when(teamManagerRepository.existsByManager_UsernameAndTeam_Id("manager2", 1L)).thenReturn(false);

        assertThatThrownBy(() -> assignmentService.createAssignment("manager2", validRequest()))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));

        verify(assignmentRepository, never()).save(any());
    }

    @Test
    void createAssignmentRejectsPublishedSchedule() {
        Shift shift = shift(schedule(ScheduleStatus.PUBLISHED), 20L);

        when(shiftRepository.findById(20L)).thenReturn(Optional.of(shift));
        when(teamManagerRepository.existsByManager_UsernameAndTeam_Id("manager1", 1L)).thenReturn(true);

        assertThatThrownBy(() -> assignmentService.createAssignment("manager1", validRequest()))
                .isInstanceOfSatisfying(AssignmentValidationException.class, exception -> {
                    assertThat(exception.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(exception.getCode()).isEqualTo("SCHEDULE_NOT_DRAFT");
                });

        verify(assignmentRepository, never()).save(any());
    }

    @Test
    void createAssignmentRejectsEmployeeOutsideTeam() {
        Shift shift = shift(schedule(ScheduleStatus.DRAFT), 20L);
        User employee = employee();

        when(shiftRepository.findById(20L)).thenReturn(Optional.of(shift));
        when(teamManagerRepository.existsByManager_UsernameAndTeam_Id("manager1", 1L)).thenReturn(true);
        when(userRepository.findById(2L)).thenReturn(Optional.of(employee));
        when(teamMemberRepository.existsByUser_IdAndTeam_IdAndActiveTrue(2L, 1L)).thenReturn(false);

        assertAssignmentConflict("TEAM_MEMBERSHIP");

        verify(assignmentRepository, never()).save(any());
    }

    @Test
    void createAssignmentRejectsDuplicateAssignment() {
        Shift shift = shift(schedule(ScheduleStatus.DRAFT), 20L);
        User employee = employee();

        when(shiftRepository.findById(20L)).thenReturn(Optional.of(shift));
        when(teamManagerRepository.existsByManager_UsernameAndTeam_Id("manager1", 1L)).thenReturn(true);
        when(userRepository.findById(2L)).thenReturn(Optional.of(employee));
        when(teamMemberRepository.existsByUser_IdAndTeam_IdAndActiveTrue(2L, 1L)).thenReturn(true);
        when(assignmentRepository.existsByShift_IdAndEmployee_Id(20L, 2L)).thenReturn(true);

        assertAssignmentConflict("DUPLICATE_ASSIGNMENT");

        verify(assignmentRepository, never()).save(any());
    }

    @Test
    void createAssignmentRejectsFullShift() {
        Shift shift = shift(schedule(ScheduleStatus.DRAFT), 20L);
        User employee = employee();

        when(shiftRepository.findById(20L)).thenReturn(Optional.of(shift));
        when(teamManagerRepository.existsByManager_UsernameAndTeam_Id("manager1", 1L)).thenReturn(true);
        when(userRepository.findById(2L)).thenReturn(Optional.of(employee));
        when(teamMemberRepository.existsByUser_IdAndTeam_IdAndActiveTrue(2L, 1L)).thenReturn(true);
        when(assignmentRepository.existsByShift_IdAndEmployee_Id(20L, 2L)).thenReturn(false);
        when(assignmentRepository.countByShift_Id(20L)).thenReturn(2L);

        assertAssignmentConflict("SHIFT_CAPACITY");

        verify(assignmentRepository, never()).save(any());
    }

    @Test
    void createAssignmentRejectsOverlappingAssignment() {
        Shift shift = shift(schedule(ScheduleStatus.DRAFT), 20L);
        User employee = employee();
        Shift overlappingShift = shift(schedule(ScheduleStatus.PUBLISHED), 21L);

        when(shiftRepository.findById(20L)).thenReturn(Optional.of(shift));
        when(teamManagerRepository.existsByManager_UsernameAndTeam_Id("manager1", 1L)).thenReturn(true);
        when(userRepository.findById(2L)).thenReturn(Optional.of(employee));
        when(teamMemberRepository.existsByUser_IdAndTeam_IdAndActiveTrue(2L, 1L)).thenReturn(true);
        when(assignmentRepository.existsByShift_IdAndEmployee_Id(20L, 2L)).thenReturn(false);
        when(assignmentRepository.countByShift_Id(20L)).thenReturn(0L);
        when(assignmentRepository.findByEmployee_IdAndShift_StartTimeLessThanAndShift_EndTimeGreaterThan(
                2L,
                shift.getEndTime(),
                shift.getStartTime()
        )).thenReturn(List.of(assignment(overlappingShift, employee)));

        assertAssignmentConflict("SHIFT_OVERLAP");

        verify(assignmentRepository, never()).save(any());
    }

    @Test
    void createAssignmentRejectsInsufficientRestBeforeShift() {
        Shift shift = shift(schedule(ScheduleStatus.DRAFT), 20L);
        User employee = employee();
        Shift previousShift = new Shift(
                schedule(ScheduleStatus.PUBLISHED),
                Instant.parse("2026-07-05T14:00:00Z"),
                Instant.parse("2026-07-05T23:00:00Z"),
                "Previous shift",
                1,
                8
        );
        ReflectionTestUtils.setField(previousShift, "id", 21L);

        when(shiftRepository.findById(20L)).thenReturn(Optional.of(shift));
        when(teamManagerRepository.existsByManager_UsernameAndTeam_Id("manager1", 1L)).thenReturn(true);
        when(userRepository.findById(2L)).thenReturn(Optional.of(employee));
        when(teamMemberRepository.existsByUser_IdAndTeam_IdAndActiveTrue(2L, 1L)).thenReturn(true);
        when(assignmentRepository.existsByShift_IdAndEmployee_Id(20L, 2L)).thenReturn(false);
        when(assignmentRepository.countByShift_Id(20L)).thenReturn(0L);
        when(assignmentRepository.findByEmployee_IdAndShift_StartTimeLessThanAndShift_EndTimeGreaterThan(
                2L,
                shift.getEndTime(),
                shift.getStartTime()
        )).thenReturn(List.of());
        when(assignmentRepository.findTopByEmployee_IdAndShift_EndTimeLessThanEqualOrderByShift_EndTimeDesc(
                2L,
                shift.getStartTime()
        )).thenReturn(Optional.of(assignment(previousShift, employee)));

        assertAssignmentConflict("MINIMUM_REST");

        verify(assignmentRepository, never()).save(any());
    }

    @Test
    void createAssignmentRejectsInsufficientRestAfterShift() {
        Shift shift = shift(schedule(ScheduleStatus.DRAFT), 20L);
        User employee = employee();
        Shift nextShift = new Shift(
                schedule(ScheduleStatus.PUBLISHED),
                Instant.parse("2026-07-06T20:00:00Z"),
                Instant.parse("2026-07-07T04:00:00Z"),
                "Next shift",
                1,
                8
        );
        ReflectionTestUtils.setField(nextShift, "id", 22L);

        when(shiftRepository.findById(20L)).thenReturn(Optional.of(shift));
        when(teamManagerRepository.existsByManager_UsernameAndTeam_Id("manager1", 1L)).thenReturn(true);
        when(userRepository.findById(2L)).thenReturn(Optional.of(employee));
        when(teamMemberRepository.existsByUser_IdAndTeam_IdAndActiveTrue(2L, 1L)).thenReturn(true);
        when(assignmentRepository.existsByShift_IdAndEmployee_Id(20L, 2L)).thenReturn(false);
        when(assignmentRepository.countByShift_Id(20L)).thenReturn(0L);
        when(assignmentRepository.findByEmployee_IdAndShift_StartTimeLessThanAndShift_EndTimeGreaterThan(
                2L,
                shift.getEndTime(),
                shift.getStartTime()
        )).thenReturn(List.of());
        when(assignmentRepository.findTopByEmployee_IdAndShift_EndTimeLessThanEqualOrderByShift_EndTimeDesc(
                2L,
                shift.getStartTime()
        )).thenReturn(Optional.empty());
        when(assignmentRepository.findTopByEmployee_IdAndShift_StartTimeGreaterThanEqualOrderByShift_StartTimeAsc(
                2L,
                shift.getEndTime()
        )).thenReturn(Optional.of(assignment(nextShift, employee)));

        assertAssignmentConflict("MINIMUM_REST");

        verify(assignmentRepository, never()).save(any());
    }

    @Test
    void listScheduleAssignmentsReturnsAssignmentsForManagedSchedule() {
        Schedule schedule = schedule(ScheduleStatus.DRAFT);
        Shift shift = shift(schedule, 20L);
        User employee = employee();
        Assignment assignment = assignment(shift, employee);

        when(scheduleRepository.findById(10L)).thenReturn(Optional.of(schedule));
        when(teamManagerRepository.existsByManager_UsernameAndTeam_Id("manager1", 1L)).thenReturn(true);
        when(assignmentRepository.findByShift_Schedule_IdOrderByShift_StartTimeAscEmployee_FullNameAsc(10L))
                .thenReturn(List.of(assignment));

        List<AssignmentResponse> responses = assignmentService.listScheduleAssignments("manager1", 10L);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).id()).isEqualTo(30L);
        assertThat(responses.get(0).shiftId()).isEqualTo(20L);
        assertThat(responses.get(0).employeeId()).isEqualTo(2L);
    }

    @Test
    void listScheduleAssignmentsRejectsUnmanagedSchedule() {
        Schedule schedule = schedule(ScheduleStatus.DRAFT);

        when(scheduleRepository.findById(10L)).thenReturn(Optional.of(schedule));
        when(teamManagerRepository.existsByManager_UsernameAndTeam_Id("manager2", 1L)).thenReturn(false);

        assertThatThrownBy(() -> assignmentService.listScheduleAssignments("manager2", 10L))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void deleteAssignmentRemovesAssignmentFromManagedDraftSchedule() {
        Shift shift = shift(schedule(ScheduleStatus.DRAFT), 20L);
        Assignment assignment = assignment(shift, employee());

        when(assignmentRepository.findById(30L)).thenReturn(Optional.of(assignment));
        when(teamManagerRepository.existsByManager_UsernameAndTeam_Id("manager1", 1L)).thenReturn(true);

        assignmentService.deleteAssignment("manager1", 30L);

        verify(assignmentRepository).delete(assignment);
    }

    @Test
    void deleteAssignmentRejectsUnmanagedSchedule() {
        Shift shift = shift(schedule(ScheduleStatus.DRAFT), 20L);
        Assignment assignment = assignment(shift, employee());

        when(assignmentRepository.findById(30L)).thenReturn(Optional.of(assignment));
        when(teamManagerRepository.existsByManager_UsernameAndTeam_Id("manager2", 1L)).thenReturn(false);

        assertThatThrownBy(() -> assignmentService.deleteAssignment("manager2", 30L))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));

        verify(assignmentRepository, never()).delete(any());
    }

    @Test
    void deleteAssignmentRejectsPublishedSchedule() {
        Shift shift = shift(schedule(ScheduleStatus.PUBLISHED), 20L);
        Assignment assignment = assignment(shift, employee());

        when(assignmentRepository.findById(30L)).thenReturn(Optional.of(assignment));
        when(teamManagerRepository.existsByManager_UsernameAndTeam_Id("manager1", 1L)).thenReturn(true);

        assertThatThrownBy(() -> assignmentService.deleteAssignment("manager1", 30L))
                .isInstanceOfSatisfying(AssignmentValidationException.class, exception -> {
                    assertThat(exception.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(exception.getCode()).isEqualTo("SCHEDULE_NOT_DRAFT");
                });

        verify(assignmentRepository, never()).delete(any());
    }

    private void assertAssignmentConflict(String expectedCode) {
        assertThatThrownBy(() -> assignmentService.createAssignment("manager1", validRequest()))
                .isInstanceOfSatisfying(AssignmentValidationException.class, exception -> {
                    assertThat(exception.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(exception.getCode()).isEqualTo(expectedCode);
                });
    }

    private CreateAssignmentRequest validRequest() {
        return new CreateAssignmentRequest(20L, 2L);
    }

    private Assignment assignment(Shift shift, User employee) {
        Assignment assignment = new Assignment(shift, employee, Instant.parse("2026-07-05T10:00:00Z"));
        ReflectionTestUtils.setField(assignment, "id", 30L);
        return assignment;
    }

    private User employee() {
        User employee = new User(
                "employee1",
                "password-hash",
                "Demo Employee",
                "employee1@example.com",
                ApplicationRole.EMPLOYEE
        );
        ReflectionTestUtils.setField(employee, "id", 2L);
        return employee;
    }

    private Shift shift(Schedule schedule, Long id) {
        Shift shift = new Shift(
                schedule,
                Instant.parse("2026-07-06T06:00:00Z"),
                Instant.parse("2026-07-06T14:00:00Z"),
                "Morning shift",
                2,
                8
        );
        ReflectionTestUtils.setField(shift, "id", id);
        return shift;
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
