package com.hilimor.shiftmanagement.assignment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import com.hilimor.shiftmanagement.schedule.Schedule;
import com.hilimor.shiftmanagement.schedule.ScheduleRepository;
import com.hilimor.shiftmanagement.shift.Shift;
import com.hilimor.shiftmanagement.shift.ShiftRepository;
import com.hilimor.shiftmanagement.team.SwapApprovalPolicy;
import com.hilimor.shiftmanagement.team.Team;
import com.hilimor.shiftmanagement.team.TeamManager;
import com.hilimor.shiftmanagement.team.TeamManagerRepository;
import com.hilimor.shiftmanagement.team.TeamMember;
import com.hilimor.shiftmanagement.team.TeamMemberRepository;
import com.hilimor.shiftmanagement.team.TeamRepository;
import com.hilimor.shiftmanagement.user.ApplicationRole;
import com.hilimor.shiftmanagement.user.User;
import com.hilimor.shiftmanagement.user.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest(properties = {
        "app.seed.enabled=false",
        "app.messaging.notifications.enabled=false",
        "app.messaging.outbox-dispatch.enabled=false",
        "spring.jms.listener.auto-startup=false",
        "spring.datasource.hikari.connection-init-sql=SET statement_timeout TO '15s'"
})
@Testcontainers
class AssignmentConcurrencyIT {

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16")
            .withDatabaseName("assignment_concurrency_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private AssignmentService assignmentService;
    @Autowired
    private AssignmentRepository assignmentRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TeamRepository teamRepository;
    @Autowired
    private TeamMemberRepository teamMemberRepository;
    @Autowired
    private TeamManagerRepository teamManagerRepository;
    @Autowired
    private ScheduleRepository scheduleRepository;
    @Autowired
    private ShiftRepository shiftRepository;
    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private JdbcTemplate jdbc;

    private String managerUsername;
    private Long employeeId;
    private Schedule firstSchedule;
    private Schedule secondSchedule;

    @BeforeEach
    void createFixture() {
        transactionTemplate.executeWithoutResult(status -> {
            String suffix = UUID.randomUUID().toString();
            managerUsername = "manager-" + suffix;
            User manager = userRepository.save(new User(
                    managerUsername, "not-used-for-login", "Manager", null, ApplicationRole.MANAGER
            ));
            User employee = userRepository.save(new User(
                    "employee-" + suffix, "not-used-for-login", "Employee", null, ApplicationRole.EMPLOYEE
            ));
            employeeId = employee.getId();
            Team team = teamRepository.save(new Team("Team " + suffix, SwapApprovalPolicy.MANAGER, 0, "UTC"));
            teamManagerRepository.save(new TeamManager(manager, team));
            teamMemberRepository.save(new TeamMember(employee, team, Instant.now(), true));
            firstSchedule = scheduleRepository.save(new Schedule(
                    team, LocalDate.of(2030, 1, 1), LocalDate.of(2030, 1, 31)
            ));
            secondSchedule = scheduleRepository.save(new Schedule(
                    team, LocalDate.of(2030, 1, 1), LocalDate.of(2030, 1, 31)
            ));
        });
    }

    @Test
    void concurrentManualAssignmentsCannotOverlapAcrossSchedules() throws Exception {
        Long firstShift = createShift(firstSchedule, "2030-01-07T09:00:00Z", "2030-01-07T17:00:00Z", 0);
        Long secondShift = createShift(secondSchedule, "2030-01-07T10:00:00Z", "2030-01-07T18:00:00Z", 0);

        Object result = runWhileFirstAssignmentIsUncommitted(firstShift, () -> assign(secondShift, employeeId));

        assertConflict(result, "SHIFT_OVERLAP");
        assertThat(assignmentRepository.countByShift_Id(firstShift)).isEqualTo(1);
        assertThat(assignmentRepository.countByShift_Id(secondShift)).isZero();
    }

    @Test
    void concurrentManualAssignmentsMustRespectMinimumRest() throws Exception {
        Long firstShift = createShift(firstSchedule, "2030-01-07T09:00:00Z", "2030-01-07T17:00:00Z", 8);
        Long secondShift = createShift(secondSchedule, "2030-01-07T18:00:00Z", "2030-01-08T02:00:00Z", 8);

        Object result = runWhileFirstAssignmentIsUncommitted(firstShift, () -> assign(secondShift, employeeId));

        assertConflict(result, "MINIMUM_REST");
        assertThat(assignmentRepository.countByShift_Id(secondShift)).isZero();
    }

    @Test
    void automaticAssignmentSeesConcurrentManualAssignmentInAnotherSchedule() throws Exception {
        Long firstShift = createShift(firstSchedule, "2030-01-07T09:00:00Z", "2030-01-07T17:00:00Z", 0);
        Long secondShift = createShift(secondSchedule, "2030-01-07T10:00:00Z", "2030-01-07T18:00:00Z", 0);

        Object result = runWhileFirstAssignmentIsUncommitted(firstShift,
                () -> assignmentService.autoAssignSchedule(managerUsername, secondSchedule.getId()));

        assertThat(result).isInstanceOfSatisfying(AutoAssignmentReportResponse.class, report -> {
            assertThat(report.assignmentsCreated()).isZero();
            assertThat(report.totalOpenSlotsAfter()).isEqualTo(1);
        });
        assertThat(assignmentRepository.countByShift_Id(secondShift)).isZero();
    }

    @Test
    void concurrentAutomaticAssignmentsCannotOverlapAcrossSchedules() throws Exception {
        Long firstShift = createShift(firstSchedule, "2030-01-07T09:00:00Z", "2030-01-07T17:00:00Z", 0);
        Long secondShift = createShift(secondSchedule, "2030-01-07T10:00:00Z", "2030-01-07T18:00:00Z", 0);

        Object result = runWhileFirstOperationIsUncommitted(
                () -> assignmentService.autoAssignSchedule(managerUsername, firstSchedule.getId()),
                () -> assignmentService.autoAssignSchedule(managerUsername, secondSchedule.getId()));

        assertThat(result).isInstanceOfSatisfying(AutoAssignmentReportResponse.class, report -> {
            assertThat(report.assignmentsCreated()).isZero();
            assertThat(report.totalOpenSlotsAfter()).isEqualTo(1);
        });
        assertThat(assignmentRepository.countByShift_Id(firstShift)).isEqualTo(1);
        assertThat(assignmentRepository.countByShift_Id(secondShift)).isZero();
    }

    @Test
    void concurrentAssignmentsCannotOverfillTheSameShift() throws Exception {
        Long firstShift = createShift(firstSchedule, "2030-01-07T09:00:00Z", "2030-01-07T17:00:00Z", 0);
        Long otherEmployeeId = transactionTemplate.execute(status -> {
            User employee = userRepository.save(new User(
                    "other-" + UUID.randomUUID(), "not-used-for-login", "Other employee", null, ApplicationRole.EMPLOYEE
            ));
            teamMemberRepository.save(new TeamMember(employee, firstSchedule.getTeam(), Instant.now(), true));
            return employee.getId();
        });

        Object result = runWhileFirstAssignmentIsUncommitted(firstShift, () -> assign(firstShift, otherEmployeeId));

        assertConflict(result, "SHIFT_CAPACITY");
        assertThat(assignmentRepository.countByShift_Id(firstShift)).isEqualTo(1);
    }

    @Test
    void nonConflictingAssignmentsCanBothCommit() throws Exception {
        Long firstShift = createShift(firstSchedule, "2030-01-07T09:00:00Z", "2030-01-07T17:00:00Z", 8);
        Long secondShift = createShift(secondSchedule, "2030-01-08T09:00:00Z", "2030-01-08T17:00:00Z", 8);

        Object result = runWhileFirstAssignmentIsUncommitted(firstShift, () -> assign(secondShift, employeeId));

        assertThat(result).isInstanceOf(AssignmentResponse.class);
        assertThat(assignmentRepository.countByShift_Id(firstShift)).isEqualTo(1);
        assertThat(assignmentRepository.countByShift_Id(secondShift)).isEqualTo(1);
    }

    private AssignmentResponse assign(Long shiftId, Long targetEmployeeId) {
        return assignmentService.createAssignment(managerUsername, new CreateAssignmentRequest(shiftId, targetEmployeeId));
    }

    private Long createShift(Schedule schedule, String start, String end, int restHours) {
        return transactionTemplate.execute(status -> shiftRepository.save(new Shift(
                schedule, Instant.parse(start), Instant.parse(end), "Concurrency test", 1, restHours, null
        )).getId());
    }

    private Object runWhileFirstAssignmentIsUncommitted(Long firstShiftId, Supplier<?> competingAction) throws Exception {
        return runWhileFirstOperationIsUncommitted(() -> assign(firstShiftId, employeeId), competingAction);
    }

    private Object runWhileFirstOperationIsUncommitted(Supplier<?> firstAction, Supplier<?> competingAction) throws Exception {
        CountDownLatch firstSaved = new CountDownLatch(1);
        CountDownLatch allowCommit = new CountDownLatch(1);
        CompletableFuture<Integer> secondConnection = new CompletableFuture<>();

        try (var pool = Executors.newFixedThreadPool(2)) {
            var first = pool.submit(() -> transactionTemplate.execute(status -> {
                Object response = firstAction.get();
                firstSaved.countDown();
                awaitCommitPermission(allowCommit);
                return response;
            }));
            try {
                assertThat(firstSaved.await(10, TimeUnit.SECONDS)).as("first assignment saved before commit").isTrue();
                var second = pool.submit(() -> {
                    try {
                        return transactionTemplate.execute(status -> {
                            secondConnection.complete(jdbc.queryForObject("select pg_backend_pid()", Integer.class));
                            return competingAction.get();
                        });
                    } catch (AssignmentValidationException exception) {
                        return exception;
                    }
                });

                int pid = secondConnection.get(10, TimeUnit.SECONDS);
                // Observe an actual PostgreSQL lock wait instead of relying on thread timing.
                await().atMost(Duration.ofSeconds(5)).until(() -> Boolean.TRUE.equals(jdbc.queryForObject(
                        "select wait_event_type = 'Lock' from pg_stat_activity where pid = ?", Boolean.class, pid
                )));
                allowCommit.countDown();
                assertThat(first.get(10, TimeUnit.SECONDS)).isNotNull();
                return second.get(10, TimeUnit.SECONDS);
            } finally {
                allowCommit.countDown();
            }
        }
    }

    private static void awaitCommitPermission(CountDownLatch latch) {
        try {
            if (!latch.await(15, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timed out waiting to commit the first assignment");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while coordinating assignments", exception);
        }
    }

    private static void assertConflict(Object result, String code) {
        assertThat(result).isInstanceOfSatisfying(AssignmentValidationException.class, exception -> {
            assertThat(exception.getStatus()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(exception.getCode()).isEqualTo(code);
        });
    }
}
