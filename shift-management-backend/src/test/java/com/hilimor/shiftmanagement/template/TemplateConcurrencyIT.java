package com.hilimor.shiftmanagement.template;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
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

import com.hilimor.shiftmanagement.schedule.Schedule;
import com.hilimor.shiftmanagement.schedule.ScheduleRepository;
import com.hilimor.shiftmanagement.schedule.ScheduleService;
import com.hilimor.shiftmanagement.team.SwapApprovalPolicy;
import com.hilimor.shiftmanagement.team.Team;
import com.hilimor.shiftmanagement.team.TeamManager;
import com.hilimor.shiftmanagement.team.TeamManagerRepository;
import com.hilimor.shiftmanagement.team.TeamRepository;
import com.hilimor.shiftmanagement.user.ApplicationRole;
import com.hilimor.shiftmanagement.user.User;
import com.hilimor.shiftmanagement.user.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
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
        "spring.datasource.hikari.connection-init-sql=SET lock_timeout TO '2s'"
})
@AutoConfigureMockMvc
@Testcontainers
class TemplateConcurrencyIT {

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16")
            .withDatabaseName("template_concurrency_test").withUsername("test").withPassword("test");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired private ShiftTemplateService templateService;
    @Autowired private ShiftTemplateRepository templates;
    @Autowired private ScheduleService scheduleService;
    @Autowired private ScheduleRepository schedules;
    @Autowired private TeamRepository teams;
    @Autowired private TeamManagerRepository managers;
    @Autowired private UserRepository users;
    @Autowired private TransactionTemplate transactions;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private MockMvc mvc;

    private User manager;
    private Team team;
    private Schedule schedule;
    private Long templateId;

    enum TemplateAction { GENERATE, ADD_SLOT, DELETE }

    @BeforeEach
    void createFixture() {
        transactions.executeWithoutResult(status -> {
            String suffix = UUID.randomUUID().toString();
            manager = users.save(new User("manager-" + suffix, "unused-test-hash", "Manager", null, ApplicationRole.MANAGER));
            team = managedTeam();
            schedule = schedules.save(new Schedule(team, LocalDate.of(2030, 1, 7), LocalDate.of(2030, 1, 9)));
        });
        templateId = createTemplate(team.getId(), "Daily").id();
        templateService.createSlot(manager.getUsername(), templateId,
                new CreateTemplateSlotRequest(0, LocalTime.of(8, 0), 480, "Morning", 1, null));
    }

    @Test
    void concurrentSameNameCreationReturnsConflictInsteadOfUniqueConstraintError() throws Exception {
        Object result = runConcurrently(() -> createTemplate(team.getId(), "  New daily  "),
                () -> createTemplate(team.getId(), "New daily"));

        assertHttpError(result, HttpStatus.CONFLICT, "Template name already exists for this team");
        assertThat(jdbc.queryForObject("select count(*) from shift_templates where team_id = ? and name = ?",
                Integer.class, team.getId(), "New daily")).isEqualTo(1);
    }

    @ParameterizedTest
    @EnumSource(TemplateAction.class)
    void deletionCommittedFirstRejectsWaitingTemplateAction(TemplateAction action) throws Exception {
        Object result = runConcurrently(this::deleteTemplate, () -> switch (action) {
            case GENERATE -> generate();
            case ADD_SLOT -> addSlot();
            case DELETE -> deleteTemplate();
        });

        assertHttpError(result, HttpStatus.NOT_FOUND, "Template not found");
        assertThat(templates.existsById(templateId)).isFalse();
        assertThat(slotSnapshot()).isEmpty();
        assertThat(shiftSnapshot()).isEmpty();
    }

    @Test
    void generationCommittedFirstPreventsDeletingTheUsedTemplate() throws Exception {
        var slotsBefore = slotSnapshot();

        Object result = runConcurrently(this::generate, this::deleteTemplate);

        assertHttpError(result, HttpStatus.CONFLICT, "Template cannot be deleted because it is used by existing shifts");
        assertThat(templates.existsById(templateId)).isTrue();
        assertThat(slotSnapshot()).isEqualTo(slotsBefore);
        assertThat(shiftSnapshot()).hasSize(3);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void slotCreationAndGenerationUseAConsistentCommittedTemplate(boolean slotFirst) throws Exception {
        Object result = runConcurrently(slotFirst ? this::addSlot : this::generate,
                slotFirst ? this::generate : this::addSlot);

        if (slotFirst) {
            assertThat(result).isInstanceOfSatisfying(GenerateTemplateShiftsResponse.class,
                    response -> assertThat(response.shiftsCreated()).isEqualTo(6));
        } else {
            assertThat(result).isInstanceOf(TemplateSlotResponse.class);
        }
        assertThat(slotSnapshot()).hasSize(2);
        assertThat(shiftSnapshot()).hasSize(slotFirst ? 6 : 3);

        GenerateTemplateShiftsResponse next = generate();
        assertThat(next.shiftsCreated()).isEqualTo(slotFirst ? 0 : 3);
        assertThat(next.skippedExistingShifts()).isEqualTo(slotFirst ? 6 : 3);
        assertThat(shiftSnapshot()).hasSize(6);
    }

    @Test
    void repeatedConcurrentGenerationSkipsAlreadyCreatedOccurrences() throws Exception {
        Object result = runConcurrently(this::generate, this::generate);

        assertThat(result).isInstanceOfSatisfying(GenerateTemplateShiftsResponse.class, response -> {
            assertThat(response.shiftsCreated()).isZero();
            assertThat(response.skippedExistingShifts()).isEqualTo(3);
        });
        assertThat(shiftSnapshot()).hasSize(3);
    }

    @Test
    void deletingTheLastUsingDraftAllowsWaitingTemplateDeletion() throws Exception {
        generate();

        Object result = runConcurrently(() -> {
            scheduleService.deleteDraftSchedule(manager.getUsername(), schedule.getId());
            return true;
        }, this::deleteTemplate);

        assertThat(result).isEqualTo(true);
        assertThat(templates.existsById(templateId)).isFalse();
        assertThat(schedules.existsById(schedule.getId())).isFalse();
        assertThat(slotSnapshot()).isEmpty();
        assertThat(shiftSnapshot()).isEmpty();
    }

    @Test
    void anotherTeamCanCreateTheSameNameBeforeTheFirstTeamCommits() throws Exception {
        Team other = transactions.execute(status -> managedTeam());
        Object result = withFirstUncommitted(() -> createTemplate(team.getId(), "Another"),
                () -> createTemplate(other.getId(), "Another"), false);

        assertThat(result).isInstanceOfSatisfying(ShiftTemplateResponse.class,
                response -> assertThat(response.teamId()).isEqualTo(other.getId()));
        assertThat(templates.findByTeam_IdAndName(team.getId(), "Another")).isPresent();
        assertThat(templates.findByTeam_IdAndName(other.getId(), "Another")).isPresent();
    }

    @Test
    void databaseLockTimeoutReturnsConflictAndDoesNotDeleteData() throws Exception {
        CountDownLatch generationFinished = new CountDownLatch(1);
        CountDownLatch allowCommit = new CountDownLatch(1);
        try (var pool = Executors.newSingleThreadExecutor()) {
            var first = pool.submit(() -> transactions.execute(status -> {
                GenerateTemplateShiftsResponse response = generate();
                generationFinished.countDown();
                awaitCommit(allowCommit);
                return response;
            }));
            try {
                assertThat(generationFinished.await(10, TimeUnit.SECONDS)).isTrue();
                // Keep the first transaction open until PostgreSQL times out the HTTP request.
                mvc.perform(delete("/api/templates/" + templateId)
                        .with(user(manager.getUsername()).roles("MANAGER")))
                        .andExpect(status().isConflict())
                        .andExpect(jsonPath("$.code").value("CONCURRENT_MODIFICATION"))
                        .andExpect(jsonPath("$.message").value("Another operation is using this data. Reload it and try again."));
            } finally {
                allowCommit.countDown();
            }
            assertThat(first.get(10, TimeUnit.SECONDS).shiftsCreated()).isEqualTo(3);
        }
        assertThat(templates.existsById(templateId)).isTrue();
        assertThat(slotSnapshot()).hasSize(1);
        assertThat(shiftSnapshot()).hasSize(3);
    }

    private Team managedTeam() {
        Team created = teams.save(new Team("Team " + UUID.randomUUID(), SwapApprovalPolicy.MANAGER, 0, "UTC"));
        managers.save(new TeamManager(manager, created));
        return created;
    }

    private ShiftTemplateResponse createTemplate(Long teamId, String name) {
        return templateService.createTemplate(manager.getUsername(), teamId,
                new CreateShiftTemplateRequest(name, null, 1, 0));
    }

    private GenerateTemplateShiftsResponse generate() {
        return templateService.generateShifts(manager.getUsername(), templateId,
                new GenerateTemplateShiftsRequest(schedule.getId()));
    }

    private TemplateSlotResponse addSlot() {
        return templateService.createSlot(manager.getUsername(), templateId,
                new CreateTemplateSlotRequest(0, LocalTime.of(16, 0), 480, "Evening", 1, null));
    }

    private boolean deleteTemplate() {
        templateService.deleteTemplate(manager.getUsername(), templateId);
        return true;
    }

    private List<Map<String, Object>> slotSnapshot() {
        return jdbc.queryForList("select * from template_slots where shift_template_id = ? order by id", templateId);
    }

    private List<Map<String, Object>> shiftSnapshot() {
        return jdbc.queryForList("select * from shifts where schedule_id = ? order by id", schedule.getId());
    }

    private static void assertHttpError(Object result, HttpStatus expected, String message) {
        assertThat(result).isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
            assertThat(exception.getStatusCode()).isEqualTo(expected);
            assertThat(exception.getReason()).isEqualTo(message);
        });
    }

    private Object runConcurrently(Supplier<?> firstAction, Supplier<?> secondAction) throws Exception {
        return withFirstUncommitted(firstAction, secondAction, true);
    }

    private Object withFirstUncommitted(Supplier<?> firstAction, Supplier<?> secondAction, boolean waitsForLock) throws Exception {
        CountDownLatch firstFinished = new CountDownLatch(1);
        CountDownLatch allowCommit = new CountDownLatch(1);
        CompletableFuture<Integer> secondConnection = new CompletableFuture<>();
        try (var pool = Executors.newFixedThreadPool(2)) {
            var first = pool.submit(() -> transactions.execute(status -> {
                Object result = firstAction.get();
                templates.flush();
                firstFinished.countDown();
                awaitCommit(allowCommit);
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
                    } catch (RuntimeException exception) {
                        return exception;
                    }
                });
                if (waitsForLock) {
                    int pid = secondConnection.get(10, TimeUnit.SECONDS);
                    await().atMost(Duration.ofSeconds(5)).until(() -> Boolean.TRUE.equals(jdbc.queryForObject(
                            "select wait_event_type = 'Lock' from pg_stat_activity where pid = ?", Boolean.class, pid)));
                    allowCommit.countDown();
                }
                Object result = second.get(10, TimeUnit.SECONDS);
                allowCommit.countDown();
                assertThat(first.get(10, TimeUnit.SECONDS)).isNotNull();
                return result;
            } finally {
                allowCommit.countDown();
            }
        }
    }

    private static void awaitCommit(CountDownLatch latch) {
        try {
            if (!latch.await(15, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timed out waiting to commit template operation");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while coordinating template operations", exception);
        }
    }
}
