package com.hilimor.shiftmanagement.availability;

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

import com.hilimor.shiftmanagement.assignment.Assignment;
import com.hilimor.shiftmanagement.assignment.AssignmentRepository;
import com.hilimor.shiftmanagement.assignment.AssignmentResponse;
import com.hilimor.shiftmanagement.assignment.AssignmentService;
import com.hilimor.shiftmanagement.assignment.AssignmentValidationException;
import com.hilimor.shiftmanagement.assignment.AutoAssignmentReportResponse;
import com.hilimor.shiftmanagement.assignment.CreateAssignmentRequest;
import com.hilimor.shiftmanagement.request.CreateSwapRequest;
import com.hilimor.shiftmanagement.request.CreateTransferRequest;
import com.hilimor.shiftmanagement.request.SwapRequestRepository;
import com.hilimor.shiftmanagement.request.SwapRequestResponse;
import com.hilimor.shiftmanagement.request.SwapRequestService;
import com.hilimor.shiftmanagement.request.SwapRequestStatus;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
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
class AvailabilityConcurrencyIT {

    private static final Instant START = Instant.parse("2030-01-10T09:00:00Z");
    private static final Instant END = Instant.parse("2030-01-10T17:00:00Z");

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16")
            .withDatabaseName("availability_concurrency_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private AvailabilityConstraintService availabilityService;
    @Autowired
    private AssignmentService assignmentService;
    @Autowired
    private AssignmentRepository assignmentRepository;
    @Autowired
    private SwapRequestService requestService;
    @Autowired
    private SwapRequestRepository requestRepository;
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

    private User manager;
    private User employee;
    private Team team;
    private Schedule draft;
    private Shift shift;

    enum AssignmentMode { MANUAL, AUTOMATIC }
    enum RequestMode { TRANSFER, SWAP }

    @BeforeEach
    void createFixture() {
        transactionTemplate.executeWithoutResult(status -> {
            String suffix = UUID.randomUUID().toString();
            manager = user("manager-" + suffix, ApplicationRole.MANAGER);
            employee = user("employee-" + suffix, ApplicationRole.EMPLOYEE);
            team = teamRepository.save(new Team("Team " + suffix, SwapApprovalPolicy.EMPLOYEE, 0, "UTC"));
            teamManagerRepository.save(new TeamManager(manager, team));
            teamMemberRepository.save(new TeamMember(employee, team, Instant.now(), true));
            draft = scheduleRepository.save(new Schedule(team, LocalDate.of(2030, 1, 1), LocalDate.of(2030, 1, 31)));
            shift = shiftRepository.save(new Shift(draft, START, END, "Availability test", 1, 0));
        });
    }

    @ParameterizedTest
    @EnumSource(AssignmentMode.class)
    void availabilityCommittedFirstPreventsConcurrentAssignment(AssignmentMode mode) throws Exception {
        Object result = runConcurrently(this::createConstraint, () -> assign(mode));

        if (mode == AssignmentMode.MANUAL) {
            assertThat(result).isInstanceOfSatisfying(AssignmentValidationException.class, exception -> {
                assertThat(exception.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                assertThat(exception.getCode()).isEqualTo("AVAILABILITY_CONFLICT");
            });
        } else {
            assertThat(result).isInstanceOfSatisfying(AutoAssignmentReportResponse.class, report -> {
                assertThat(report.assignmentsCreated()).isZero();
                assertThat(report.totalOpenSlotsAfter()).isEqualTo(1);
            });
        }
        assertThat(assignmentRepository.countByShift_Id(shift.getId())).isZero();
        assertThat(availabilityService.listMyConstraints(employee.getUsername())).hasSize(1);
    }

    @ParameterizedTest
    @EnumSource(AssignmentMode.class)
    void assignmentCommittedFirstRejectsConcurrentAvailability(AssignmentMode mode) throws Exception {
        Object result = runConcurrently(() -> assign(mode), this::createConstraint);

        assertAvailabilityConflict(result);
        assertThat(assignmentRepository.countByShift_Id(shift.getId())).isEqualTo(1);
        assertThat(availabilityService.listMyConstraints(employee.getUsername())).isEmpty();
    }

    @ParameterizedTest
    @EnumSource(RequestMode.class)
    void availabilityCommittedFirstInvalidatesConcurrentRequestExecution(RequestMode mode) throws Exception {
        RequestFixture request = createRequest(mode);

        Object result = runConcurrently(this::createConstraint, () -> approve(request));

        assertThat(result).isInstanceOfSatisfying(SwapRequestResponse.class, response ->
                assertThat(response.status()).isEqualTo(SwapRequestStatus.INVALIDATED));
        assertRequestStatus(request.id(), SwapRequestStatus.INVALIDATED);
        assertOwner(request.source(), request.requester());
        if (request.target() != null) {
            assertOwner(request.target(), employee);
        }
        assertThat(availabilityService.listMyConstraints(employee.getUsername())).hasSize(1);
    }

    @ParameterizedTest
    @EnumSource(RequestMode.class)
    void requestExecutionCommittedFirstRejectsConcurrentAvailability(RequestMode mode) throws Exception {
        RequestFixture request = createRequest(mode);

        Object result = runConcurrently(() -> approve(request), this::createConstraint);

        assertAvailabilityConflict(result);
        assertRequestStatus(request.id(), SwapRequestStatus.APPROVED);
        assertOwner(request.source(), employee);
        if (request.target() != null) {
            assertOwner(request.target(), request.requester());
        }
        assertThat(availabilityService.listMyConstraints(employee.getUsername())).isEmpty();
    }

    @Test
    void constraintDeletionCommittedFirstAllowsConcurrentAssignment() throws Exception {
        Long constraintId = createConstraint().id();

        Object result = runConcurrently(() -> deleteConstraint(constraintId), () -> assign(AssignmentMode.MANUAL));

        assertThat(result).isInstanceOfSatisfying(AssignmentResponse.class, response ->
                assertThat(response.employeeId()).isEqualTo(employee.getId()));
        assertThat(assignmentRepository.countByShift_Id(shift.getId())).isEqualTo(1);
        assertThat(availabilityService.listMyConstraints(employee.getUsername())).isEmpty();
    }

    @Test
    void secondConcurrentDeletionSeesMissingConstraint() throws Exception {
        Long constraintId = createConstraint().id();

        Object result = runConcurrently(() -> deleteConstraint(constraintId), () -> deleteConstraint(constraintId));

        assertThat(result).isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
        assertThat(availabilityService.listMyConstraints(employee.getUsername())).isEmpty();
    }

    private AvailabilityConstraintResponse createConstraint() {
        return availabilityService.createConstraint(employee.getUsername(),
                new CreateAvailabilityConstraintRequest(START, END, "Unavailable"));
    }

    private boolean deleteConstraint(Long id) {
        availabilityService.deleteMyConstraint(employee.getUsername(), id);
        return true;
    }

    private Object assign(AssignmentMode mode) {
        if (mode == AssignmentMode.AUTOMATIC) {
            return assignmentService.autoAssignSchedule(manager.getUsername(), draft.getId());
        }
        return assignmentService.createAssignment(manager.getUsername(),
                new CreateAssignmentRequest(shift.getId(), employee.getId()));
    }

    private RequestFixture createRequest(RequestMode mode) {
        RequestFixture fixture = transactionTemplate.execute(status -> {
            User requester = user("requester-" + UUID.randomUUID(), ApplicationRole.EMPLOYEE);
            teamMemberRepository.save(new TeamMember(requester, team, Instant.now(), true));
            Schedule published = scheduleRepository.save(new Schedule(
                    team, LocalDate.of(2030, 1, 1), LocalDate.of(2030, 1, 31)));
            published.publish(Instant.now());
            Shift sourceShift = shiftRepository.save(new Shift(published, START, END, "Source shift", 1, 0));
            Assignment source = assignmentRepository.save(new Assignment(sourceShift, requester, Instant.now()));
            if (mode == RequestMode.TRANSFER) {
                return new RequestFixture(null, requester, source, null);
            }
            Shift targetShift = shiftRepository.save(new Shift(published,
                    START.plus(Duration.ofDays(2)), END.plus(Duration.ofDays(2)), "Target shift", 1, 0));
            Assignment target = assignmentRepository.save(new Assignment(targetShift, employee, Instant.now()));
            return new RequestFixture(null, requester, source, target);
        });
        Long id = mode == RequestMode.TRANSFER
                ? requestService.createTransferRequest(fixture.requester().getUsername(),
                        new CreateTransferRequest(fixture.source().getId(), employee.getId())).id()
                : requestService.createSwapRequest(fixture.requester().getUsername(),
                        new CreateSwapRequest(fixture.source().getId(), fixture.target().getId())).id();
        return new RequestFixture(id, fixture.requester(), fixture.source(), fixture.target());
    }

    private SwapRequestResponse approve(RequestFixture request) {
        return requestService.approveByTargetEmployee(employee.getUsername(), request.id());
    }

    private User user(String username, ApplicationRole role) {
        return userRepository.save(new User(username, "unused-test-hash", username, null, role));
    }

    private void assertOwner(Assignment assignment, User expected) {
        transactionTemplate.executeWithoutResult(status -> assertThat(assignmentRepository
                .findById(assignment.getId()).orElseThrow().getEmployee().getId()).isEqualTo(expected.getId()));
    }

    private void assertRequestStatus(Long id, SwapRequestStatus expected) {
        assertThat(requestRepository.findById(id).orElseThrow().getStatus()).isEqualTo(expected);
    }

    private static void assertAvailabilityConflict(Object result) {
        assertThat(result).isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
            assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(exception.getReason()).isEqualTo("Availability constraint overlaps an existing assignment");
        });
    }

    // Keep the first write uncommitted until PostgreSQL reports the second transaction waiting on a lock.
    private Object runConcurrently(Supplier<?> firstAction, Supplier<?> secondAction) throws Exception {
        CountDownLatch firstFinished = new CountDownLatch(1);
        CountDownLatch allowCommit = new CountDownLatch(1);
        CompletableFuture<Integer> secondConnection = new CompletableFuture<>();

        try (var pool = Executors.newFixedThreadPool(2)) {
            var first = pool.submit(() -> transactionTemplate.execute(status -> {
                Object result = firstAction.get();
                firstFinished.countDown();
                awaitCommitPermission(allowCommit);
                return result;
            }));
            try {
                assertThat(firstFinished.await(10, TimeUnit.SECONDS)).as("first operation finished before commit").isTrue();
                var second = pool.submit(() -> {
                    try {
                        return transactionTemplate.execute(status -> {
                            secondConnection.complete(jdbc.queryForObject("select pg_backend_pid()", Integer.class));
                            return secondAction.get();
                        });
                    } catch (ResponseStatusException | AssignmentValidationException exception) {
                        return exception;
                    }
                });
                int pid = secondConnection.get(10, TimeUnit.SECONDS);
                await().atMost(Duration.ofSeconds(5)).until(() -> Boolean.TRUE.equals(jdbc.queryForObject(
                        "select wait_event_type = 'Lock' from pg_stat_activity where pid = ?", Boolean.class, pid)));
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
                throw new IllegalStateException("Timed out waiting to commit the first availability operation");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while coordinating availability operations", exception);
        }
    }

    private record RequestFixture(Long id, User requester, Assignment source, Assignment target) { }
}
