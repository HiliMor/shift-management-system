package com.hilimor.shiftmanagement.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import jakarta.persistence.EntityManager;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.hilimor.shiftmanagement.assignment.AssignmentService;
import com.hilimor.shiftmanagement.messaging.EventOutbox;
import com.hilimor.shiftmanagement.messaging.EventOutboxRepository;
import com.hilimor.shiftmanagement.notification.NotificationService;
import com.hilimor.shiftmanagement.notification.SchedulePublishedNotificationService;
import com.hilimor.shiftmanagement.request.SwapRequestService;
import com.hilimor.shiftmanagement.schedule.CreateScheduleRequest;
import com.hilimor.shiftmanagement.schedule.ScheduleService;
import com.hilimor.shiftmanagement.shift.CreateShiftRequest;
import com.hilimor.shiftmanagement.shift.ShiftService;
import com.hilimor.shiftmanagement.team.SwapApprovalPolicy;
import com.hilimor.shiftmanagement.team.Team;
import com.hilimor.shiftmanagement.team.TeamRepository;
import com.hilimor.shiftmanagement.template.ShiftTemplateRepository;
import com.hilimor.shiftmanagement.template.ShiftTemplateService;
import com.hilimor.shiftmanagement.user.ApplicationRole;
import com.hilimor.shiftmanagement.user.User;
import com.hilimor.shiftmanagement.user.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest(properties = {
        "app.seed.enabled=true", "app.messaging.notifications.enabled=false",
        "app.messaging.outbox-dispatch.enabled=false", "spring.jms.listener.auto-startup=false",
        "spring.datasource.hikari.connection-init-sql=SET statement_timeout TO '15s'"
})
@Testcontainers
class DevelopmentDataSeederIT {
    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16")
            .withDatabaseName("seed_test").withUsername("test").withPassword("test");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired @Qualifier("seedInitialData") private CommandLineRunner initializer;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private TransactionTemplate transactions;
    @Autowired private EntityManager entityManager;
    @Autowired private UserRepository users;
    @Autowired private TeamRepository teams;
    @Autowired private ShiftTemplateRepository templates;
    @Autowired private EventOutboxRepository outbox;
    @Autowired private PasswordEncoder passwords;
    @Autowired private ScheduleService schedules;
    @Autowired private ShiftService shifts;
    @Autowired private AssignmentService assignments;
    @Autowired private ShiftTemplateService templateService;
    @Autowired private SwapRequestService requests;
    @Autowired private NotificationService notifications;
    @SpyBean private SchedulePublishedNotificationService publicationNotifications;

    private static final List<String> TABLES = List.of("users", "teams", "team_members", "team_managers",
            "schedules", "shifts", "assignments", "availability_constraints", "staffing_roles",
            "team_member_staffing_roles", "shift_templates", "template_slots", "notifications",
            "swap_requests", "event_outbox");

    enum ExistingData { USER_ONLY, TEAM_ONLY, OUTBOX_ONLY }

    @BeforeEach
    void emptyDisposableDatabase() {
        // This test container is isolated from the developer's database.
        jdbc.execute("truncate users, teams, event_outbox restart identity cascade");
    }

    @Test
    void freshInitializationCreatesValidScenarioAndSecondRunChangesNothing() throws Exception {
        initializer.run();
        assertThat(TABLES.stream().map(this::count).toList())
                .containsExactly(9, 1, 8, 1, 3, 3, 3, 0, 3, 8, 1, 3, 8, 1, 0);
        assertThat(users.findAll()).allSatisfy(user ->
                assertThat(passwords.matches("password", user.getPasswordHash())).isTrue());
        assertThat(schedules.getPublicationReadiness("manager1", publishedId()).readyToPublish()).isTrue();
        assertThat(jdbc.queryForList("select end_date - start_date + 1 from schedules where status = 'DRAFT' order by start_date", Integer.class))
                .containsExactly(7, 21);
        assertThat(jdbc.queryForObject("select count(*) from shifts sh join schedules s on s.id = sh.schedule_id where s.status = 'DRAFT'", Integer.class))
                .isZero();
        assertThat(jdbc.queryForList("select duration_minutes from template_slots order by id", Integer.class))
                .containsExactly(480, 480, 480);
        assertUnchangedOnRestart();
    }

    @Test
    void completedTransferAndReadNotificationAreNotRestored() throws Exception {
        initializer.run();
        Long requestId = id("select id from swap_requests");
        Long sourceId = id("select source_assignment_id from swap_requests");
        requests.approveByTargetEmployee("employee2", requestId);
        requests.approveByManager("manager1", requestId);
        assertThat(jdbc.queryForObject("select u.username from assignments a join users u on u.id = a.employee_id where a.id = ?",
                String.class, sourceId)).isEqualTo("employee2");
        assertThat(count("assignments")).isEqualTo(3);
        notifications.markMyNotificationRead("employee1", notifications.listMyNotifications("employee1").get(0).id());
        assertUnchangedOnRestart();
    }

    @Test
    void deletedDraftTemplateShiftAndAssignmentStayDeleted() throws Exception {
        initializer.run();
        Long scheduleId = publishedId();
        Long assignmentId = id("select min(id) from assignments");
        Long shiftId = id("select shift_id from assignments where id = ?", assignmentId);
        Long templateId = id("select id from shift_templates");
        Long emptyDraftId = id("select min(id) from schedules where status = 'DRAFT'");
        schedules.reopenSchedule("manager1", scheduleId);
        assignments.deleteAssignment("manager1", assignmentId,
                assignments.previewAssignmentDeletion("manager1", assignmentId).revision());
        shifts.deleteShift("manager1", scheduleId, shiftId,
                shifts.previewShiftDeletion("manager1", scheduleId, shiftId).revision());
        templateService.deleteTemplate("manager1", templateId,
                templateService.previewTemplateDeletion("manager1", templateId).revision());
        schedules.deleteDraftSchedule("manager1", emptyDraftId,
                schedules.previewDraftDeletion("manager1", emptyDraftId).revision());
        assertThat(count("schedules")).isEqualTo(2);
        assertThat(count("shifts")).isEqualTo(2);
        assertThat(count("assignments")).isEqualTo(2);
        assertThat(count("template_slots")).isZero();
        assertThat(count("shift_templates")).isZero();
        assertUnchangedOnRestart();
    }

    @Test
    void manuallyCreatedScheduleWithSameDatesIsNotAdopted() throws Exception {
        initializer.run();
        LocalDate start = jdbc.queryForObject("select start_date from schedules where id = ?", LocalDate.class, publishedId());
        var manual = schedules.createDraftSchedule("manager1", new CreateScheduleRequest(id("select id from teams"), start, start.plusDays(6)));
        Instant shiftStart = start.atTime(10, 0).atZone(ZoneId.of("Asia/Jerusalem")).toInstant();
        shifts.createShift("manager1", manual.id(), new CreateShiftRequest(shiftStart, shiftStart.plusSeconds(3600), "Manual shift", 1, 8));
        schedules.publishSchedule("manager1", manual.id(), true);
        assertUnchangedOnRestart();
        assertThat(jdbc.queryForObject("select count(*) from shifts where schedule_id = ?", Integer.class, manual.id())).isEqualTo(1);
    }

    @Test
    void editedTemplateMembershipAndProfileAreNotReset() throws Exception {
        initializer.run();
        transactions.executeWithoutResult(status -> {
            var template = templates.findAll().get(0);
            template.updateDetails("Custom template", "Edited by manager", 7, 12);
            template.deactivate();
            jdbc.update("delete from team_member_staffing_roles where team_member_id = (select min(id) from team_members)");
            jdbc.update("update team_members set active = false where id = (select min(id) from team_members)");
            jdbc.update("update users set full_name = 'Changed name', password_hash = 'Changed password' where username = 'manager1'");
        });
        assertUnchangedOnRestart();
    }

    @ParameterizedTest
    @EnumSource(ExistingData.class)
    void anyExistingApplicationDataPreventsPartialTopUp(ExistingData existingData) throws Exception {
        switch (existingData) {
            case USER_ONLY -> users.save(new User("manager1", "existing", "Existing manager", null, ApplicationRole.MANAGER));
            case TEAM_ONLY -> teams.save(new Team("Existing team", SwapApprovalPolicy.MANAGER, 8, "Asia/Jerusalem"));
            case OUTBOX_ONLY -> outbox.save(new EventOutbox(UUID.randomUUID(), "existing.event",
                    JsonNodeFactory.instance.objectNode(), Instant.now()));
        }
        assertUnchangedOnRestart();
    }

    @Test
    void lateFailureRollsBackEverythingAndAllowsRetry() throws Exception {
        doThrow(new IllegalStateException("Injected seed failure")).when(publicationNotifications).createNotifications(any(), any());
        assertThatThrownBy(() -> initializer.run()).hasMessage("Injected seed failure");
        assertThat(TABLES.stream().map(this::count).toList()).containsOnly(0);
        reset(publicationNotifications);
        initializer.run();
        assertThat(count("users")).isEqualTo(9);
        assertThat(count("notifications")).isEqualTo(8);
        assertThat(schedules.getPublicationReadiness("manager1", publishedId()).readyToPublish()).isTrue();
    }

    @Test
    void concurrentInitializersWaitForCommitAndCreateOnlyOneScenario() throws Exception {
        CountDownLatch allowCommit = new CountDownLatch(1);
        CompletableFuture<Map<String, List<Map<String, Object>>>> firstReady = new CompletableFuture<>();
        CompletableFuture<Integer> secondPid = new CompletableFuture<>();
        var workers = Executors.newFixedThreadPool(2);
        try {
            var first = workers.submit(() -> transactions.executeWithoutResult(status -> {
                try {
                    initialize();
                    entityManager.flush();
                    firstReady.complete(snapshot());
                    assertThat(allowCommit.await(10, TimeUnit.SECONDS)).isTrue();
                } catch (Exception | AssertionError failure) {
                    firstReady.completeExceptionally(failure);
                    throw new IllegalStateException(failure);
                }
            }));
            var created = firstReady.get(15, TimeUnit.SECONDS);
            var second = workers.submit(() -> transactions.executeWithoutResult(status -> {
                secondPid.complete(jdbc.queryForObject("select pg_backend_pid()", Integer.class));
                initialize();
            }));
            int pid = secondPid.get(5, TimeUnit.SECONDS);
            await().atMost(Duration.ofSeconds(5)).until(() -> Boolean.TRUE.equals(jdbc.queryForObject(
                    "select wait_event_type = 'Lock' and wait_event = 'advisory' from pg_stat_activity where pid = ?", Boolean.class, pid)));
            allowCommit.countDown();
            first.get(15, TimeUnit.SECONDS);
            second.get(15, TimeUnit.SECONDS);
            assertThat(snapshot()).isEqualTo(created);
            assertThat(count("users")).isEqualTo(9);
        } finally {
            allowCommit.countDown();
            workers.shutdown();
            assertThat(workers.awaitTermination(20, TimeUnit.SECONDS)).isTrue();
        }
    }

    private void initialize() {
        try {
            initializer.run();
        } catch (Exception failure) {
            throw new IllegalStateException(failure);
        }
    }

    private void assertUnchangedOnRestart() throws Exception {
        var before = snapshot();
        initializer.run();
        initializer.run();
        assertThat(snapshot()).isEqualTo(before);
    }

    private Map<String, List<Map<String, Object>>> snapshot() {
        Map<String, List<Map<String, Object>>> rows = new LinkedHashMap<>();
        TABLES.forEach(table -> rows.put(table, jdbc.queryForList("select * from " + table + " order by 1")));
        return rows;
    }

    private int count(String table) {
        return jdbc.queryForObject("select count(*) from " + table, Integer.class);
    }

    private Long id(String sql, Object... args) {
        return jdbc.queryForObject(sql, Long.class, args);
    }

    private Long publishedId() {
        return id("select id from schedules where status = 'PUBLISHED'");
    }
}
