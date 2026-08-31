package com.hilimor.shiftmanagement.schedule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
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
import com.hilimor.shiftmanagement.assignment.CreateAssignmentRequest;
import com.hilimor.shiftmanagement.availability.AvailabilityConstraintService;
import com.hilimor.shiftmanagement.availability.CreateAvailabilityConstraintRequest;
import com.hilimor.shiftmanagement.request.CreateSwapRequest;
import com.hilimor.shiftmanagement.request.CreateTransferRequest;
import com.hilimor.shiftmanagement.request.SwapRequestRepository;
import com.hilimor.shiftmanagement.request.SwapRequestResponse;
import com.hilimor.shiftmanagement.request.SwapRequestService;
import com.hilimor.shiftmanagement.request.SwapRequestStatus;
import com.hilimor.shiftmanagement.shift.CreateShiftRequest;
import com.hilimor.shiftmanagement.shift.Shift;
import com.hilimor.shiftmanagement.shift.ShiftRepository;
import com.hilimor.shiftmanagement.shift.ShiftService;
import com.hilimor.shiftmanagement.shift.UpdateShiftRequest;
import com.hilimor.shiftmanagement.team.SwapApprovalPolicy;
import com.hilimor.shiftmanagement.team.Team;
import com.hilimor.shiftmanagement.team.TeamManager;
import com.hilimor.shiftmanagement.team.TeamManagerRepository;
import com.hilimor.shiftmanagement.team.TeamMember;
import com.hilimor.shiftmanagement.team.TeamMemberRepository;
import com.hilimor.shiftmanagement.team.TeamRepository;
import com.hilimor.shiftmanagement.template.CreateShiftTemplateRequest;
import com.hilimor.shiftmanagement.template.CreateTemplateSlotRequest;
import com.hilimor.shiftmanagement.template.GenerateTemplateShiftsRequest;
import com.hilimor.shiftmanagement.template.ShiftTemplateService;
import com.hilimor.shiftmanagement.user.ApplicationRole;
import com.hilimor.shiftmanagement.user.User;
import com.hilimor.shiftmanagement.user.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.dao.OptimisticLockingFailureException;
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
class ScheduleWorkflowConcurrencyIT {

    private static final Instant START = Instant.parse("2030-01-10T09:00:00Z");
    private static final Instant END = Instant.parse("2030-01-10T17:00:00Z");

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16")
            .withDatabaseName("schedule_workflow_test").withUsername("test").withPassword("test");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired private ScheduleService scheduleService;
    @Autowired private ShiftService shiftService;
    @Autowired private AssignmentService assignmentService;
    @Autowired private AvailabilityConstraintService availabilityService;
    @Autowired private ShiftTemplateService templateService;
    @Autowired private SwapRequestService requestService;
    @Autowired private SwapRequestRepository requestRepository;
    @Autowired private ScheduleRepository scheduleRepository;
    @Autowired private ShiftRepository shiftRepository;
    @Autowired private AssignmentRepository assignmentRepository;
    @Autowired private TeamRepository teamRepository;
    @Autowired private TeamManagerRepository managerRepository;
    @Autowired private TeamMemberRepository memberRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private TransactionTemplate transactions;
    @Autowired private JdbcTemplate jdbc;

    private User manager;
    private User employee;
    private Team team;
    private Schedule schedule;
    private Shift shift;
    private Shift assignedShift;
    private Assignment assignment;
    private Long templateId;

    enum DraftWrite {
        MANUAL_ASSIGNMENT, AUTOMATIC_ASSIGNMENT, CREATE_SHIFT, EDIT_SHIFT,
        DELETE_SHIFT, DELETE_ASSIGNMENT, GENERATE_TEMPLATE, DELETE_SCHEDULE, PUBLISH
    }

    enum RequestMode { TRANSFER, SWAP }

    @BeforeEach
    void createFixture() {
        transactions.executeWithoutResult(status -> {
            String suffix = UUID.randomUUID().toString();
            manager = user("manager-" + suffix, ApplicationRole.MANAGER);
            employee = user("employee-" + suffix, ApplicationRole.EMPLOYEE);
            team = createTeam(suffix);
            schedule = scheduleRepository.save(new Schedule(team, LocalDate.of(2030, 1, 7), LocalDate.of(2030, 1, 13)));
            shift = shiftRepository.save(new Shift(schedule, START, END, "Empty", 1, 0));
            assignedShift = shiftRepository.save(new Shift(schedule,
                    START.plus(Duration.ofDays(2)), END.plus(Duration.ofDays(2)), "Assigned", 1, 0));
            assignment = assignmentRepository.save(new Assignment(assignedShift, employee, Instant.now()));
        });
        templateId = templateService.createTemplate(manager.getUsername(), team.getId(),
                new CreateShiftTemplateRequest("Weekly", null, 7, 0)).id();
        templateService.createSlot(manager.getUsername(), templateId,
                new CreateTemplateSlotRequest(0, LocalTime.of(9, 0), 480, "Generated", 1, null));
    }

    @ParameterizedTest
    @EnumSource(DraftWrite.class)
    void publicationCommittedFirstRejectsDraftWrite(DraftWrite write) throws Exception {
        var shiftsBefore = shiftSnapshot();
        var assignmentsBefore = assignmentSnapshot();

        Object result = runConcurrently(this::publish, () -> write(write));

        assertHttpError(result, HttpStatus.CONFLICT);
        assertPublished();
        assertThat(shiftSnapshot()).isEqualTo(shiftsBefore);
        assertThat(assignmentSnapshot()).isEqualTo(assignmentsBefore);
    }

    @ParameterizedTest
    @EnumSource(DraftWrite.class)
    void draftWriteCommittedFirstIsObservedByPublication(DraftWrite write) throws Exception {
        Object result = runConcurrently(() -> write(write), this::publish);

        if (write == DraftWrite.DELETE_SCHEDULE) {
            assertHttpError(result, HttpStatus.NOT_FOUND);
            assertThat(scheduleRepository.existsById(schedule.getId())).isFalse();
            assertThat(shiftSnapshot()).isEmpty();
            assertThat(assignmentSnapshot()).isEmpty();
            assertThat(publicationEvents()).isZero();
            return;
        }
        if (write == DraftWrite.PUBLISH) {
            assertHttpError(result, HttpStatus.CONFLICT);
        } else {
            assertThat(result).isInstanceOf(ScheduleResponse.class);
        }
        assertPublished();
        switch (write) {
            case MANUAL_ASSIGNMENT, AUTOMATIC_ASSIGNMENT -> assertThat(assignmentSnapshot()).hasSize(2);
            case CREATE_SHIFT, GENERATE_TEMPLATE -> assertThat(shiftSnapshot()).hasSize(3);
            case DELETE_SHIFT -> assertThat(shiftSnapshot()).hasSize(1);
            case DELETE_ASSIGNMENT -> assertThat(assignmentSnapshot()).isEmpty();
            case EDIT_SHIFT -> assertThat(shiftRepository.findById(shift.getId()).orElseThrow().getDescription())
                    .isEqualTo("Edited");
            default -> { }
        }
    }

    @Test
    void assignmentDeletionCommittedFirstBlocksPublicationWithoutConfirmation() throws Exception {
        assignmentService.createAssignment(manager.getUsername(), new CreateAssignmentRequest(shift.getId(), employee.getId()));

        Object result = runConcurrently(() -> write(DraftWrite.DELETE_ASSIGNMENT),
                () -> scheduleService.publishSchedule(manager.getUsername(), schedule.getId(), false));

        assertHttpError(result, HttpStatus.CONFLICT);
        Schedule stored = scheduleRepository.findById(schedule.getId()).orElseThrow();
        assertThat(stored.getStatus()).isEqualTo(ScheduleStatus.DRAFT);
        assertThat(stored.getPublicationNumber()).isZero();
        assertThat(assignmentRepository.existsById(assignment.getId())).isFalse();
        assertThat(publicationEvents()).isZero();
    }

    @Test
    void reopeningCommittedFirstAllowsWaitingManualAssignment() throws Exception {
        publish();

        Object result = runConcurrently(this::reopen, () -> write(DraftWrite.MANUAL_ASSIGNMENT));

        assertThat(result).isInstanceOf(AssignmentResponse.class);
        assertThat(assignmentSnapshot()).hasSize(2);
        assertThat(scheduleRepository.findById(schedule.getId()).orElseThrow().getStatus()).isEqualTo(ScheduleStatus.DRAFT);
        assertThat(publicationEvents()).isEqualTo(1);
    }

    @Test
    void twoConcurrentEditsWithTheSameVersionCannotBothSucceed() throws Exception {
        var ownersBefore = assignmentSnapshot();
        UpdateShiftRequest secondEdit = new UpdateShiftRequest(
                START, END, "Stale description", 1, 0, null, shift.getVersion());

        Object result = runConcurrently(() -> write(DraftWrite.EDIT_SHIFT),
                () -> shiftService.updateShift(manager.getUsername(), schedule.getId(), shift.getId(), secondEdit));

        assertThat(result).isInstanceOf(OptimisticLockingFailureException.class);
        Shift stored = shiftRepository.findById(shift.getId()).orElseThrow();
        assertThat(stored.getDescription()).isEqualTo("Edited");
        assertThat(stored.getRequiredWorkers()).isEqualTo(2);
        assertThat(stored.getVersion()).isEqualTo(shift.getVersion() + 1);
        assertThat(assignmentSnapshot()).isEqualTo(ownersBefore);
    }

    @ParameterizedTest
    @EnumSource(RequestMode.class)
    void reopeningCommittedFirstInvalidatesRequestWithoutChangingOwners(RequestMode mode) throws Exception {
        RequestFixture request = request(mode);
        var ownersBefore = assignmentSnapshot();

        Object result = runConcurrently(this::reopen, () -> approve(request.id()));

        assertThat(result).isInstanceOfSatisfying(SwapRequestResponse.class, response ->
                assertThat(response.status()).isEqualTo(SwapRequestStatus.INVALIDATED));
        assertThat(requestRepository.findById(request.id()).orElseThrow().getStatus()).isEqualTo(SwapRequestStatus.INVALIDATED);
        assertThat(assignmentSnapshot()).isEqualTo(ownersBefore);
        assertThat(scheduleRepository.findById(schedule.getId()).orElseThrow().getStatus()).isEqualTo(ScheduleStatus.DRAFT);
        assertThat(publicationEvents()).isEqualTo(1);
    }

    @ParameterizedTest
    @EnumSource(RequestMode.class)
    void requestExecutionCommittedFirstIsPreservedWhenReopening(RequestMode mode) throws Exception {
        RequestFixture request = request(mode);

        Object result = runConcurrently(() -> approve(request.id()), this::reopen);

        assertThat(result).isInstanceOfSatisfying(ScheduleResponse.class, response ->
                assertThat(response.status()).isEqualTo(ScheduleStatus.DRAFT));
        assertThat(requestRepository.findById(request.id()).orElseThrow().getStatus()).isEqualTo(SwapRequestStatus.APPROVED);
        assertOwner(request.source(), employee);
        if (mode == RequestMode.SWAP) {
            assertOwner(assignment.getId(), request.requester());
        }
        assertThat(publicationEvents()).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void assignedShiftEditAndAvailabilityCannotCommitConflictingTimes(boolean editFirst) throws Exception {
        Supplier<?> edit = () -> editAssignedShift();
        Supplier<?> constraint = () -> availabilityService.createConstraint(employee.getUsername(),
                new CreateAvailabilityConstraintRequest(START, END, "Unavailable"));

        Object result = runConcurrently(editFirst ? edit : constraint, editFirst ? constraint : edit);

        assertHttpError(result, HttpStatus.CONFLICT);
        if (!editFirst) {
            assertValidationCode(result, "AVAILABILITY_CONFLICT");
        }
        assertThat(shiftRepository.findById(assignedShift.getId()).orElseThrow().getStartTime())
                .isEqualTo(editFirst ? START : START.plus(Duration.ofDays(2)));
        assertThat(availabilityService.listMyConstraints(employee.getUsername())).hasSize(editFirst ? 0 : 1);
        assertOwner(assignment.getId(), employee);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void assignedShiftEditAndOtherTeamAssignmentCannotOverlap(boolean editFirst) throws Exception {
        Long otherShiftId = transactions.execute(status -> {
            Team otherTeam = createTeam(UUID.randomUUID().toString());
            Schedule otherSchedule = scheduleRepository.save(new Schedule(
                    otherTeam, schedule.getStartDate(), schedule.getEndDate()));
            return shiftRepository.save(new Shift(otherSchedule, START, END, "Other team", 1, 0)).getId();
        });
        Supplier<?> edit = () -> editAssignedShift();
        Supplier<?> assign = () -> assignmentService.createAssignment(manager.getUsername(),
                new CreateAssignmentRequest(otherShiftId, employee.getId()));

        Object result = runConcurrently(editFirst ? edit : assign, editFirst ? assign : edit);

        assertValidationCode(result, "SHIFT_OVERLAP");
        assertThat(shiftRepository.findById(assignedShift.getId()).orElseThrow().getStartTime())
                .isEqualTo(editFirst ? START : START.plus(Duration.ofDays(2)));
        assertThat(assignmentRepository.countByShift_Id(otherShiftId)).isEqualTo(editFirst ? 0 : 1);
    }

    private Object write(DraftWrite write) {
        switch (write) {
            case MANUAL_ASSIGNMENT:
                return assignmentService.createAssignment(manager.getUsername(), new CreateAssignmentRequest(shift.getId(), employee.getId()));
            case AUTOMATIC_ASSIGNMENT:
                return assignmentService.autoAssignSchedule(manager.getUsername(), schedule.getId());
            case CREATE_SHIFT:
                return shiftService.createShift(manager.getUsername(), schedule.getId(), new CreateShiftRequest(
                        START.plus(Duration.ofDays(1)), END.plus(Duration.ofDays(1)), "Created", 1, 0));
            case EDIT_SHIFT:
                return shiftService.updateShift(manager.getUsername(), schedule.getId(), shift.getId(),
                        new UpdateShiftRequest(START, END, "Edited", 2, 0, null, shift.getVersion()));
            case DELETE_SHIFT:
                shiftService.deleteShift(manager.getUsername(), schedule.getId(), shift.getId());
                break;
            case DELETE_ASSIGNMENT:
                assignmentService.deleteAssignment(manager.getUsername(), assignment.getId());
                break;
            case GENERATE_TEMPLATE:
                return templateService.generateShifts(manager.getUsername(), templateId, new GenerateTemplateShiftsRequest(schedule.getId()));
            case DELETE_SCHEDULE:
                scheduleService.deleteDraftSchedule(manager.getUsername(), schedule.getId());
                break;
            case PUBLISH:
                return publish();
        }
        return true;
    }

    private Object editAssignedShift() {
        return shiftService.updateShift(manager.getUsername(), schedule.getId(), assignedShift.getId(),
                new UpdateShiftRequest(START, END, "Moved", 1, 0, null, assignedShift.getVersion()));
    }

    private ScheduleResponse publish() {
        return scheduleService.publishSchedule(manager.getUsername(), schedule.getId(), true);
    }

    private ScheduleResponse reopen() {
        return scheduleService.reopenSchedule(manager.getUsername(), schedule.getId());
    }

    private RequestFixture request(RequestMode mode) {
        RequestFixture fixture = transactions.execute(status -> {
            User requester = user("requester-" + UUID.randomUUID(), ApplicationRole.EMPLOYEE);
            memberRepository.save(new TeamMember(requester, team, Instant.now(), true));
            Assignment source = assignmentRepository.save(new Assignment(shift, requester, Instant.now()));
            return new RequestFixture(null, requester, source.getId());
        });
        publish();
        Long id = mode == RequestMode.TRANSFER
                ? requestService.createTransferRequest(fixture.requester().getUsername(),
                        new CreateTransferRequest(fixture.source(), employee.getId())).id()
                : requestService.createSwapRequest(fixture.requester().getUsername(),
                        new CreateSwapRequest(fixture.source(), assignment.getId())).id();
        return new RequestFixture(id, fixture.requester(), fixture.source());
    }

    private SwapRequestResponse approve(Long id) {
        return requestService.approveByTargetEmployee(employee.getUsername(), id);
    }

    private User user(String username, ApplicationRole role) {
        return userRepository.save(new User(username, "unused-test-hash", username, null, role));
    }

    private Team createTeam(String suffix) {
        Team created = teamRepository.save(new Team("Team " + suffix, SwapApprovalPolicy.EMPLOYEE, 0, "UTC"));
        managerRepository.save(new TeamManager(manager, created));
        memberRepository.save(new TeamMember(employee, created, Instant.now(), true));
        return created;
    }

    private List<Map<String, Object>> shiftSnapshot() {
        return jdbc.queryForList("select * from shifts where schedule_id = ? order by id", schedule.getId());
    }

    private List<Map<String, Object>> assignmentSnapshot() {
        return jdbc.queryForList("select a.* from assignments a join shifts s on s.id = a.shift_id where s.schedule_id = ? order by a.id",
                schedule.getId());
    }

    private int publicationEvents() {
        return jdbc.queryForObject("select count(*) from event_outbox where event_type = 'schedule.published' and payload::jsonb ->> 'scheduleId' = ?",
                Integer.class, schedule.getId().toString());
    }

    private void assertPublished() {
        Schedule stored = scheduleRepository.findById(schedule.getId()).orElseThrow();
        assertThat(stored.getStatus()).isEqualTo(ScheduleStatus.PUBLISHED);
        assertThat(stored.getPublicationNumber()).isEqualTo(1);
        assertThat(publicationEvents()).isEqualTo(1);
    }

    private void assertOwner(Long id, User expected) {
        transactions.executeWithoutResult(status -> assertThat(assignmentRepository.findById(id).orElseThrow()
                .getEmployee().getId()).isEqualTo(expected.getId()));
    }

    private static void assertValidationCode(Object result, String code) {
        assertThat(result).isInstanceOfSatisfying(AssignmentValidationException.class, exception -> {
            assertThat(exception.getStatus()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(exception.getCode()).isEqualTo(code);
        });
    }

    private static void assertHttpError(Object result, HttpStatus expected) {
        if (result instanceof AssignmentValidationException exception) {
            assertThat(exception.getStatus()).isEqualTo(expected);
        } else {
            assertThat(result).isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                    assertThat(exception.getStatusCode()).isEqualTo(expected));
        }
    }

    // Observe a real database lock wait while the first service write is still uncommitted.
    private Object runConcurrently(Supplier<?> firstAction, Supplier<?> secondAction) throws Exception {
        CountDownLatch firstFinished = new CountDownLatch(1);
        CountDownLatch allowCommit = new CountDownLatch(1);
        CompletableFuture<Integer> secondConnection = new CompletableFuture<>();
        try (var pool = Executors.newFixedThreadPool(2)) {
            var first = pool.submit(() -> transactions.execute(status -> {
                Object result = firstAction.get();
                firstFinished.countDown();
                awaitCommitPermission(allowCommit);
                return result;
            }));
            try {
                assertThat(firstFinished.await(10, TimeUnit.SECONDS)).as("first operation finished before commit").isTrue();
                var second = pool.submit(() -> {
                    try {
                        return transactions.execute(status -> {
                            secondConnection.complete(jdbc.queryForObject("select pg_backend_pid()", Integer.class));
                            return secondAction.get();
                        });
                    } catch (ResponseStatusException | AssignmentValidationException | OptimisticLockingFailureException exception) {
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
                throw new IllegalStateException("Timed out waiting to commit the first schedule operation");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while coordinating schedule operations", exception);
        }
    }

    private record RequestFixture(Long id, User requester, Long source) { }
}
