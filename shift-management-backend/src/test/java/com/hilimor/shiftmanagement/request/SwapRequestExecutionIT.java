package com.hilimor.shiftmanagement.request;

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
import com.hilimor.shiftmanagement.assignment.AssignmentService;
import com.hilimor.shiftmanagement.assignment.CreateAssignmentRequest;
import com.hilimor.shiftmanagement.availability.AvailabilityConstraintService;
import com.hilimor.shiftmanagement.availability.CreateAvailabilityConstraintRequest;
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
class SwapRequestExecutionIT {

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16")
            .withDatabaseName("request_execution_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private SwapRequestService requestService;
    @Autowired
    private SwapRequestRepository requestRepository;
    @Autowired
    private AvailabilityConstraintService availabilityService;
    @Autowired
    private AssignmentRepository assignmentRepository;
    @Autowired
    private AssignmentService assignmentService;
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
    private User alice;
    private User bob;
    private User charlie;
    private Assignment aliceAssignment;
    private Assignment bobAssignment;
    private Assignment charlieAssignment;

    @BeforeEach
    void createFixture() {
        createFixture(SwapApprovalPolicy.MANAGER);
    }

    private void createFixture(SwapApprovalPolicy policy) {
        transactionTemplate.executeWithoutResult(status -> {
            String suffix = UUID.randomUUID().toString();
            manager = user("manager-" + suffix, ApplicationRole.MANAGER);
            alice = user("alice-" + suffix, ApplicationRole.EMPLOYEE);
            bob = user("bob-" + suffix, ApplicationRole.EMPLOYEE);
            charlie = user("charlie-" + suffix, ApplicationRole.EMPLOYEE);
            Team team = teamRepository.save(new Team("Team " + suffix, policy, 0, "UTC"));
            teamManagerRepository.save(new TeamManager(manager, team));
            for (User employee : new User[]{alice, bob, charlie}) {
                teamMemberRepository.save(new TeamMember(employee, team, Instant.now(), true));
            }
            Schedule schedule = scheduleRepository.save(new Schedule(
                    team, LocalDate.of(2030, 1, 1), LocalDate.of(2030, 1, 31)
            ));
            schedule.publish(Instant.now());
            aliceAssignment = assignment(schedule, alice, "2030-01-07T09:00:00Z", "2030-01-07T17:00:00Z");
            bobAssignment = assignment(schedule, bob, "2030-01-09T09:00:00Z", "2030-01-09T17:00:00Z");
            charlieAssignment = assignment(schedule, charlie, "2030-01-07T10:00:00Z", "2030-01-07T18:00:00Z");
        });
    }

    @Test
    void invalidTransferPersistsInvalidationWithoutChangingOwnership() {
        Long requestId = pendingTransfer(alice, aliceAssignment, bob);
        unavailable(bob, "2030-01-07T09:00:00Z", "2030-01-07T17:00:00Z");

        SwapRequestResponse result = approve(requestId);

        assertThat(result.status()).isEqualTo(SwapRequestStatus.INVALIDATED);
        assertStatus(requestId, SwapRequestStatus.INVALIDATED);
        assertOwner(aliceAssignment, alice);
    }

    @Test
    void invalidSecondSwapLegPersistsInvalidationWithoutChangingEitherOwner() {
        Long requestId = pendingSwap();
        unavailable(alice, "2030-01-09T09:00:00Z", "2030-01-09T17:00:00Z");

        SwapRequestResponse result = approve(requestId);

        assertThat(result.status()).isEqualTo(SwapRequestStatus.INVALIDATED);
        assertStatus(requestId, SwapRequestStatus.INVALIDATED);
        assertOwner(aliceAssignment, alice);
        assertOwner(bobAssignment, bob);
    }

    @Test
    void validSwapCommitsBothOwnersAndApprovalTogether() {
        Long requestId = pendingSwap();

        assertThat(approve(requestId).status()).isEqualTo(SwapRequestStatus.APPROVED);

        assertStatus(requestId, SwapRequestStatus.APPROVED);
        assertOwner(aliceAssignment, bob);
        assertOwner(bobAssignment, alice);
    }

    @Test
    void employeeOnlyApprovalExecutesTransferWithoutManagerApproval() {
        createFixture(SwapApprovalPolicy.EMPLOYEE);
        Long requestId = requestService.createTransferRequest(alice.getUsername(),
                new CreateTransferRequest(aliceAssignment.getId(), bob.getId())).id();

        SwapRequestResponse result = requestService.approveByTargetEmployee(bob.getUsername(), requestId);

        assertThat(result.status()).isEqualTo(SwapRequestStatus.APPROVED);
        assertStatus(requestId, SwapRequestStatus.APPROVED);
        assertOwner(aliceAssignment, bob);
    }

    @Test
    void employeeOnlyApprovalPersistsInvalidationWhenValidationFails() {
        createFixture(SwapApprovalPolicy.EMPLOYEE);
        Long requestId = requestService.createTransferRequest(alice.getUsername(),
                new CreateTransferRequest(aliceAssignment.getId(), bob.getId())).id();
        unavailable(bob, "2030-01-07T09:00:00Z", "2030-01-07T17:00:00Z");

        SwapRequestResponse result = requestService.approveByTargetEmployee(bob.getUsername(), requestId);

        assertThat(result.status()).isEqualTo(SwapRequestStatus.INVALIDATED);
        assertStatus(requestId, SwapRequestStatus.INVALIDATED);
        assertOwner(aliceAssignment, alice);
    }

    @Test
    void transferWaitsForConcurrentManualAssignmentInAnotherTeam() throws Exception {
        Long requestId = pendingTransfer(alice, aliceAssignment, bob);
        Long otherShiftId = transactionTemplate.execute(status -> {
            Team otherTeam = teamRepository.save(new Team("Other team", SwapApprovalPolicy.MANAGER, 0, "UTC"));
            teamManagerRepository.save(new TeamManager(manager, otherTeam));
            teamMemberRepository.save(new TeamMember(bob, otherTeam, Instant.now(), true));
            Schedule draft = scheduleRepository.save(new Schedule(
                    otherTeam, LocalDate.of(2030, 1, 1), LocalDate.of(2030, 1, 31)
            ));
            return shiftRepository.save(new Shift(draft, Instant.parse("2030-01-07T10:00:00Z"),
                    Instant.parse("2030-01-07T18:00:00Z"), "Concurrent manual assignment", 1, 0, null)).getId();
        });

        Object secondResult = runConcurrently(
                () -> assignmentService.createAssignment(manager.getUsername(),
                        new CreateAssignmentRequest(otherShiftId, bob.getId())),
                () -> approve(requestId));

        assertThat(secondResult).isInstanceOfSatisfying(SwapRequestResponse.class, response ->
                assertThat(response.status()).isEqualTo(SwapRequestStatus.INVALIDATED));
        assertStatus(requestId, SwapRequestStatus.INVALIDATED);
        assertOwner(aliceAssignment, alice);
        assertThat(assignmentRepository.existsByShift_IdAndEmployee_Id(otherShiftId, bob.getId())).isTrue();
    }

    @Test
    void duplicateManagerApprovalsReturnConflictInsteadOfExecutingTwice() throws Exception {
        Long requestId = pendingTransfer(alice, aliceAssignment, bob);

        Object secondResult = runConcurrently(() -> approve(requestId), () -> approve(requestId));

        assertHttpError(secondResult, HttpStatus.CONFLICT);
        assertStatus(requestId, SwapRequestStatus.APPROVED);
        assertOwner(aliceAssignment, bob);
    }

    @Test
    void concurrentTransfersToTheSameEmployeeCannotCreateOverlap() throws Exception {
        Long firstId = pendingTransfer(alice, aliceAssignment, bob);
        Long secondId = pendingTransfer(charlie, charlieAssignment, bob);

        Object secondResult = runConcurrently(() -> approve(firstId), () -> approve(secondId));

        assertThat(secondResult).isInstanceOfSatisfying(SwapRequestResponse.class, response ->
                assertThat(response.status()).isEqualTo(SwapRequestStatus.INVALIDATED));
        assertStatus(firstId, SwapRequestStatus.APPROVED);
        assertStatus(secondId, SwapRequestStatus.INVALIDATED);
        assertOwner(aliceAssignment, bob);
        assertOwner(charlieAssignment, charlie);
    }

    @Test
    void concurrentCreationCannotUseAnAssignmentAsBothActiveSourceAndTarget() throws Exception {
        Object secondResult = runConcurrently(
                () -> requestService.createTransferRequest(alice.getUsername(),
                        new CreateTransferRequest(aliceAssignment.getId(), bob.getId())),
                () -> requestService.createSwapRequest(charlie.getUsername(),
                        new CreateSwapRequest(charlieAssignment.getId(), aliceAssignment.getId())));

        assertHttpError(secondResult, HttpStatus.CONFLICT);
        assertThat(requestService.listMyOutgoingRequests(charlie.getUsername())).isEmpty();
    }

    @Test
    void successfulSwapInvalidatesLegacyCompetingRequestBeforeItsApproval() throws Exception {
        Long requestId = pendingSwap();
        // Reproduce a cross-column conflict that the old concurrent creation path allowed.
        Long competingId = transactionTemplate.execute(status -> requestRepository.save(SwapRequest.createTransfer(
                bob, bobAssignment, charlie, Instant.now()
        )).getId());

        Object secondResult = runConcurrently(() -> approve(requestId),
                () -> requestService.approveByTargetEmployee(charlie.getUsername(), competingId));

        assertHttpError(secondResult, HttpStatus.CONFLICT);
        assertStatus(requestId, SwapRequestStatus.APPROVED);
        assertStatus(competingId, SwapRequestStatus.INVALIDATED);
        assertOwner(aliceAssignment, bob);
        assertOwner(bobAssignment, alice);
    }

    @Test
    void concurrentCancellationPreventsLaterManagerApproval() throws Exception {
        Long requestId = pendingTransfer(alice, aliceAssignment, bob);

        Object secondResult = runConcurrently(
                () -> requestService.cancelByRequester(alice.getUsername(), requestId),
                () -> approve(requestId));

        assertHttpError(secondResult, HttpStatus.CONFLICT);
        assertStatus(requestId, SwapRequestStatus.CANCELLED);
        assertOwner(aliceAssignment, alice);
    }

    @Test
    void requestCreationRechecksOwnershipAfterConcurrentTransfer() throws Exception {
        Long requestId = pendingTransfer(alice, aliceAssignment, bob);

        Object secondResult = runConcurrently(() -> approve(requestId),
                () -> requestService.createTransferRequest(alice.getUsername(),
                        new CreateTransferRequest(aliceAssignment.getId(), charlie.getId())));

        assertHttpError(secondResult, HttpStatus.NOT_FOUND);
        assertOwner(aliceAssignment, bob);
    }

    private User user(String username, ApplicationRole role) {
        return userRepository.save(new User(username, "not-used-for-login", username, null, role));
    }

    private Assignment assignment(Schedule schedule, User employee, String start, String end) {
        Shift shift = shiftRepository.save(new Shift(
                schedule, Instant.parse(start), Instant.parse(end), "Request test", 1, 0, null
        ));
        return assignmentRepository.save(new Assignment(shift, employee, Instant.now()));
    }

    private Long pendingTransfer(User requester, Assignment source, User target) {
        Long id = requestService.createTransferRequest(requester.getUsername(),
                new CreateTransferRequest(source.getId(), target.getId())).id();
        requestService.approveByTargetEmployee(target.getUsername(), id);
        return id;
    }

    private Long pendingSwap() {
        Long id = requestService.createSwapRequest(alice.getUsername(),
                new CreateSwapRequest(aliceAssignment.getId(), bobAssignment.getId())).id();
        requestService.approveByTargetEmployee(bob.getUsername(), id);
        return id;
    }

    private SwapRequestResponse approve(Long requestId) {
        return requestService.approveByManager(manager.getUsername(), requestId);
    }

    private void unavailable(User employee, String start, String end) {
        availabilityService.createConstraint(employee.getUsername(), new CreateAvailabilityConstraintRequest(
                Instant.parse(start), Instant.parse(end), "Unavailable"
        ));
    }

    private void assertStatus(Long requestId, SwapRequestStatus expected) {
        assertThat(requestRepository.findById(requestId).orElseThrow().getStatus()).isEqualTo(expected);
    }

    private void assertOwner(Assignment assignment, User expected) {
        Long ownerId = transactionTemplate.execute(status -> assignmentRepository.findById(assignment.getId())
                .orElseThrow().getEmployee().getId());
        assertThat(ownerId).isEqualTo(expected.getId());
    }

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
                    } catch (ResponseStatusException exception) {
                        return exception;
                    }
                });
                int pid = secondConnection.get(10, TimeUnit.SECONDS);
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
                throw new IllegalStateException("Timed out waiting to commit the first request operation");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while coordinating request operations", exception);
        }
    }

    private static void assertHttpError(Object result, HttpStatus expected) {
        assertThat(result).isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                assertThat(exception.getStatusCode()).isEqualTo(expected));
    }
}
