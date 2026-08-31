package com.hilimor.shiftmanagement.schedule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hilimor.shiftmanagement.assignment.AssignmentService;
import com.hilimor.shiftmanagement.assignment.CreateAssignmentRequest;
import com.hilimor.shiftmanagement.request.CreateTransferRequest;
import com.hilimor.shiftmanagement.request.SwapRequestService;
import com.hilimor.shiftmanagement.shift.CreateShiftRequest;
import com.hilimor.shiftmanagement.shift.ShiftResponse;
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
import com.hilimor.shiftmanagement.template.ShiftTemplateService;
import com.hilimor.shiftmanagement.user.ApplicationRole;
import com.hilimor.shiftmanagement.user.User;
import com.hilimor.shiftmanagement.user.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest(properties = {
        "app.seed.enabled=false", "app.messaging.notifications.enabled=false",
        "app.messaging.outbox-dispatch.enabled=false", "spring.jms.listener.auto-startup=false"
})
@AutoConfigureMockMvc
@Testcontainers
class DeletionPreconditionIT {
    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16")
            .withDatabaseName("deletion_test").withUsername("test").withPassword("test");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired private ScheduleService scheduleService;
    @Autowired private ShiftTemplateService templateService;
    @Autowired private ShiftService shiftService;
    @Autowired private AssignmentService assignmentService;
    @Autowired private SwapRequestService requestService;
    @Autowired private UserRepository users;
    @Autowired private TeamRepository teams;
    @Autowired private TeamManagerRepository managers;
    @Autowired private TeamMemberRepository members;
    @Autowired private TransactionTemplate transactions;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;

    private User manager;
    private User employee;
    private User otherEmployee;
    private Long scheduleId;
    private Long templateId;
    private ShiftResponse shift;
    private Long assignmentId;
    private static final Instant START = Instant.parse("2030-01-07T08:00:00Z");
    private static final Instant END = Instant.parse("2030-01-07T16:00:00Z");

    enum Resource { SCHEDULE, TEMPLATE }
    enum Change { ADD_SHIFT, EDIT_SHIFT, REMOVE_SHIFT, ADD_ASSIGNMENT, REMOVE_ASSIGNMENT, REPLACE_ASSIGNMENT, PUBLISH_REOPEN }

    @BeforeEach
    void createFixture() {
        Long teamId = transactions.execute(status -> {
            String suffix = UUID.randomUUID().toString();
            manager = users.save(new User("manager-" + suffix, "unused", "Manager", null, ApplicationRole.MANAGER));
            employee = users.save(new User("employee-" + suffix, "unused", "Employee", null, ApplicationRole.EMPLOYEE));
            otherEmployee = users.save(new User("other-" + suffix, "unused", "Other", null, ApplicationRole.EMPLOYEE));
            Team team = teams.save(new Team("Deletion team", SwapApprovalPolicy.MANAGER, 0, "UTC"));
            managers.save(new TeamManager(manager, team));
            members.save(new TeamMember(employee, team, Instant.now(), true));
            members.save(new TeamMember(otherEmployee, team, Instant.now(), true));
            return team.getId();
        });
        scheduleId = scheduleService.createDraftSchedule(manager.getUsername(),
                new CreateScheduleRequest(teamId, LocalDate.of(2030, 1, 7), LocalDate.of(2030, 1, 9))).id();
        shift = shiftService.createShift(manager.getUsername(), scheduleId, new CreateShiftRequest(START, END, "Original", 2, 0));
        assignmentId = assignmentService.createAssignment(manager.getUsername(), new CreateAssignmentRequest(shift.id(), employee.getId())).id();
        templateId = templateService.createTemplate(manager.getUsername(), teamId, new CreateShiftTemplateRequest("Daily", null, 1, 0)).id();
        addSlot();
    }

    @ParameterizedTest
    @EnumSource(Change.class)
    void changedScheduleRejectsOldRevisionAndPreservesCurrentData(Change change) throws Exception {
        String revision = preview(Resource.SCHEDULE);
        switch (change) {
            case ADD_SHIFT -> shiftService.createShift(manager.getUsername(), scheduleId,
                    new CreateShiftRequest(START.plusSeconds(86400), END.plusSeconds(86400), "New", 1, 0));
            case EDIT_SHIFT -> shiftService.updateShift(manager.getUsername(), scheduleId, shift.id(),
                    new UpdateShiftRequest(START, END, "Changed description", 2, 0, null, shift.version()));
            case REMOVE_SHIFT -> shiftService.deleteShift(manager.getUsername(), scheduleId, shift.id());
            case ADD_ASSIGNMENT -> assignmentService.createAssignment(manager.getUsername(), new CreateAssignmentRequest(shift.id(), otherEmployee.getId()));
            case REMOVE_ASSIGNMENT -> assignmentService.deleteAssignment(manager.getUsername(), assignmentId);
            case REPLACE_ASSIGNMENT -> {
                assignmentService.deleteAssignment(manager.getUsername(), assignmentId);
                assignmentService.createAssignment(manager.getUsername(), new CreateAssignmentRequest(shift.id(), otherEmployee.getId()));
            }
            case PUBLISH_REOPEN -> {
                scheduleService.publishSchedule(manager.getUsername(), scheduleId, true);
                scheduleService.reopenSchedule(manager.getUsername(), scheduleId);
            }
        }
        var before = snapshot();
        mvc.perform(delete(path(Resource.SCHEDULE)).param("revision", revision).with(user(manager.getUsername()).roles("MANAGER")))
                .andExpect(status().isConflict());
        assertThat(snapshot()).isEqualTo(before);
        assertThat(preview(Resource.SCHEDULE)).isNotEqualTo(revision);
    }

    @Test
    void addedTemplateSlotRejectsOldRevisionEvenWhenParentVersionDidNotChange() throws Exception {
        String revision = preview(Resource.TEMPLATE);
        Long parentVersion = jdbc.queryForObject("select version from shift_templates where id = ?", Long.class, templateId);
        addSlot();
        var before = snapshot();
        mvc.perform(delete(path(Resource.TEMPLATE)).param("revision", revision).with(user(manager.getUsername()).roles("MANAGER")))
                .andExpect(status().isConflict());
        assertThat(jdbc.queryForObject("select version from shift_templates where id = ?", Long.class, templateId)).isEqualTo(parentVersion);
        assertThat(snapshot()).isEqualTo(before);
        assertThat(preview(Resource.TEMPLATE)).isNotEqualTo(revision);
    }

    @ParameterizedTest
    @EnumSource(Resource.class)
    void freshConfirmationDeletesChildrenAndRepeatedDeleteIsNotFound(Resource resource) throws Exception {
        String revision = preview(resource);
        assertThat(preview(resource)).isEqualTo(revision);
        mvc.perform(delete(path(resource)).param("revision", revision).with(user(manager.getUsername()).roles("MANAGER")))
                .andExpect(status().isNoContent());
        if (resource == Resource.SCHEDULE) {
            assertThat(jdbc.queryForObject("select count(*) from schedules where id = ?", Integer.class, scheduleId)).isZero();
            assertThat(jdbc.queryForList("select * from shifts where schedule_id = ?", scheduleId)).isEmpty();
            assertThat(jdbc.queryForList("select * from assignments where id = ?", assignmentId)).isEmpty();
        } else {
            assertThat(jdbc.queryForList("select * from shift_templates where id = ?", templateId)).isEmpty();
            assertThat(jdbc.queryForList("select * from template_slots where shift_template_id = ?", templateId)).isEmpty();
        }
        mvc.perform(delete(path(resource)).param("revision", revision).with(user(manager.getUsername()).roles("MANAGER")))
                .andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @EnumSource(Resource.class)
    void missingMalformedAndOtherResourceRevisionCannotDelete(Resource resource) throws Exception {
        var before = snapshot();
        mvc.perform(delete(path(resource)).with(user(manager.getUsername()).roles("MANAGER"))).andExpect(status().isBadRequest());
        mvc.perform(delete(path(resource)).param("revision", "garbage").with(user(manager.getUsername()).roles("MANAGER")))
                .andExpect(status().isBadRequest());
        String wrong = preview(resource == Resource.SCHEDULE ? Resource.TEMPLATE : Resource.SCHEDULE);
        mvc.perform(delete(path(resource)).param("revision", wrong).with(user(manager.getUsername()).roles("MANAGER")))
                .andExpect(status().isConflict());
        assertThat(snapshot()).isEqualTo(before);
    }

    @ParameterizedTest
    @EnumSource(Resource.class)
    void previewAndDeletionRequireAuthenticationAndTeamOwnership(Resource resource) throws Exception {
        String revision = preview(resource);
        var before = snapshot();
        mvc.perform(get(path(resource) + "/deletion-preview")).andExpect(status().isUnauthorized());
        mvc.perform(delete(path(resource)).param("revision", revision)).andExpect(status().isUnauthorized());
        for (String role : List.of("MANAGER", "EMPLOYEE")) {
            mvc.perform(get(path(resource) + "/deletion-preview").with(user("outsider").roles(role)))
                    .andExpect(status().isForbidden());
            mvc.perform(delete(path(resource)).param("revision", revision).with(user("outsider").roles(role)))
                    .andExpect(status().isForbidden());
        }
        assertThat(snapshot()).isEqualTo(before);
    }

    @Test
    void reopenedScheduleWithRequestHistoryReturnsConflictInsteadOfForeignKeyFailure() throws Exception {
        String revision = preview(Resource.SCHEDULE);
        scheduleService.publishSchedule(manager.getUsername(), scheduleId, true);
        requestService.createTransferRequest(employee.getUsername(), new CreateTransferRequest(assignmentId, otherEmployee.getId()));
        scheduleService.reopenSchedule(manager.getUsername(), scheduleId);
        var before = snapshot();
        mvc.perform(get(path(Resource.SCHEDULE) + "/deletion-preview").with(user(manager.getUsername()).roles("MANAGER")))
                .andExpect(status().isConflict());
        mvc.perform(delete(path(Resource.SCHEDULE)).param("revision", revision).with(user(manager.getUsername()).roles("MANAGER")))
                .andExpect(status().isConflict());
        assertThat(snapshot()).isEqualTo(before);
        assertThat(jdbc.queryForObject("select count(*) from swap_requests where source_assignment_id = ?", Integer.class, assignmentId)).isEqualTo(1);
    }

    private String path(Resource resource) {
        return resource == Resource.SCHEDULE ? "/api/schedules/" + scheduleId : "/api/templates/" + templateId;
    }

    private String preview(Resource resource) throws Exception {
        var result = mvc.perform(get(path(resource) + "/deletion-preview").with(user(manager.getUsername()).roles("MANAGER")))
                .andExpect(status().isOk()).andReturn();
        var body = json.readTree(result.getResponse().getContentAsString());
        if (resource == Resource.SCHEDULE) {
            assertThat(body.path("schedule").path("id").asLong()).isEqualTo(scheduleId);
            assertThat(body.path("shiftCount").asInt()).isEqualTo(jdbc.queryForObject("select count(*) from shifts where schedule_id = ?", Integer.class, scheduleId));
            assertThat(body.path("assignmentCount").asInt()).isEqualTo(jdbc.queryForObject("select count(*) from assignments a join shifts s on s.id = a.shift_id where s.schedule_id = ?", Integer.class, scheduleId));
        } else {
            assertThat(body.path("template").path("id").asLong()).isEqualTo(templateId);
            assertThat(body.path("slotCount").asInt()).isEqualTo(jdbc.queryForObject("select count(*) from template_slots where shift_template_id = ?", Integer.class, templateId));
        }
        return body.path("revision").asText();
    }

    private void addSlot() {
        templateService.createSlot(manager.getUsername(), templateId, new CreateTemplateSlotRequest(0, LocalTime.of(8, 0), 480, "Slot", 1, null));
    }

    private List<?> snapshot() {
        return List.of(jdbc.queryForList("select * from schedules where id = ?", scheduleId),
                jdbc.queryForList("select * from shifts where schedule_id = ? order by id", scheduleId),
                jdbc.queryForList("select a.* from assignments a join shifts s on s.id = a.shift_id where s.schedule_id = ? order by a.id", scheduleId),
                jdbc.queryForList("select * from shift_templates where id = ?", templateId),
                jdbc.queryForList("select * from template_slots where shift_template_id = ? order by id", templateId));
    }
}
