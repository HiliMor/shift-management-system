package com.hilimor.shiftmanagement.shift;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.hilimor.shiftmanagement.schedule.Schedule;
import com.hilimor.shiftmanagement.schedule.ScheduleRepository;
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
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest(properties = {
        "app.seed.enabled=false",
        "app.messaging.notifications.enabled=false",
        "app.messaging.outbox-dispatch.enabled=false",
        "spring.jms.listener.auto-startup=false"
})
@AutoConfigureMockMvc
@Testcontainers
class ShiftEditingIT {

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16")
            .withDatabaseName("shift_editing_test").withUsername("test").withPassword("test");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;
    @Autowired private ShiftRepository shifts;
    @Autowired private ScheduleRepository schedules;
    @Autowired private TeamRepository teams;
    @Autowired private TeamManagerRepository managers;
    @Autowired private UserRepository users;
    @Autowired private TransactionTemplate transactions;
    @Autowired private JdbcTemplate jdbc;

    private User manager;
    private Schedule schedule;
    private Shift shift;

    enum InvalidVersion { MISSING, NULL, NEGATIVE }

    @BeforeEach
    void createFixture() {
        transactions.executeWithoutResult(status -> {
            String suffix = UUID.randomUUID().toString();
            manager = users.save(new User("manager-" + suffix, "unused-test-hash", "Manager", null, ApplicationRole.MANAGER));
            Team team = teams.save(new Team("Team " + suffix, SwapApprovalPolicy.MANAGER, 0, "UTC"));
            managers.save(new TeamManager(manager, team));
            schedule = schedules.save(new Schedule(team, LocalDate.of(2030, 1, 1), LocalDate.of(2030, 1, 31)));
            shift = shifts.save(new Shift(schedule, Instant.parse("2030-01-07T08:00:00Z"),
                    Instant.parse("2030-01-07T16:00:00Z"), "Original", 1, 0));
        });
    }

    @Test
    void staleDescriptionEditCannotRestoreOldHours() throws Exception {
        Map<String, Object> oldScreen = editBody("08:00", "16:00", "Edited from old screen", shift.getVersion());
        edit(editBody("09:00", "17:00", "New hours", shift.getVersion()))
                .andExpect(status().isOk());
        var saved = snapshot();

        edit(oldScreen)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("STALE_VERSION"))
                .andExpect(jsonPath("$.message").value("This record has changed. Reload it and review your changes before saving again."));

        assertThat(snapshot()).isEqualTo(saved);
    }

    @Test
    void createReturnsTheStoredInitialVersion() throws Exception {
        var body = editBody("10:00", "18:00", "Created", 0L);
        body.remove("version");

        JsonNode created = response(mvc.perform(post(path())
                .with(user(manager.getUsername()).roles("MANAGER"))
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.version").value(0)));

        assertThat(shifts.findById(created.path("id").asLong()).orElseThrow().getVersion())
                .isEqualTo(created.path("version").asLong());
    }

    @Test
    void listAndUpdateReturnStoredVersionsThatCanBeUsedForTheNextEdit() throws Exception {
        JsonNode listed = response(mvc.perform(get(path())
                .with(user(manager.getUsername()).roles("MANAGER")))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].version").value(0))).get(0);

        JsonNode first = response(edit(editBody("09:00", "17:00", "New hours", listed.path("version").asLong()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.version").value(1)));
        assertThat(snapshot().get("version")).isEqualTo(first.path("version").asLong());

        var secondBody = editBody("09:00", "17:00", "Reviewed description", first.path("version").asLong());
        JsonNode second = response(edit(secondBody)
                .andExpect(status().isOk()).andExpect(jsonPath("$.version").value(2)));
        assertThat(snapshot().get("version")).isEqualTo(second.path("version").asLong());

        secondBody.put("version", second.path("version").asLong());
        var saved = snapshot();
        edit(secondBody).andExpect(status().isOk()).andExpect(jsonPath("$.version").value(2));
        assertThat(snapshot()).as("an unchanged save does not need to increment the version").isEqualTo(saved);
        mvc.perform(get(path()).with(user(manager.getUsername()).roles("MANAGER")))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].version").value(2));
    }

    @ParameterizedTest
    @EnumSource(InvalidVersion.class)
    void missingNullOrNegativeVersionIsRejectedWithoutWriting(InvalidVersion invalid) throws Exception {
        var body = editBody("09:00", "17:00", "Invalid version", shift.getVersion());
        switch (invalid) {
            case MISSING -> body.remove("version");
            case NULL -> body.put("version", null);
            case NEGATIVE -> body.put("version", -1);
        }
        var before = snapshot();

        edit(body).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        assertThat(snapshot()).isEqualTo(before);
    }

    @Test
    void refreshAndReviewAfterConflictAllowsAnIntentionalEdit() throws Exception {
        edit(editBody("09:00", "17:00", "New hours", shift.getVersion())).andExpect(status().isOk());
        edit(editBody("08:00", "16:00", "Stale description", shift.getVersion())).andExpect(status().isConflict());
        JsonNode current = response(mvc.perform(get(path())
                .with(user(manager.getUsername()).roles("MANAGER"))).andExpect(status().isOk())).get(0);
        var reviewed = new HashMap<String, Object>();
        reviewed.put("startTime", current.path("startTime").asText());
        reviewed.put("endTime", current.path("endTime").asText());
        reviewed.put("requiredWorkers", current.path("requiredWorkers").asInt());
        reviewed.put("minRestHours", current.path("minRestHours").asInt());
        reviewed.put("version", current.path("version").asLong());
        reviewed.put("description", "Reviewed description");

        edit(reviewed).andExpect(status().isOk()).andExpect(jsonPath("$.version").value(2));

        Shift stored = shifts.findById(shift.getId()).orElseThrow();
        assertThat(stored.getStartTime()).isEqualTo(Instant.parse("2030-01-07T09:00:00Z"));
        assertThat(stored.getEndTime()).isEqualTo(Instant.parse("2030-01-07T17:00:00Z"));
        assertThat(stored.getDescription()).isEqualTo("Reviewed description");
    }

    @Test
    void deletedShiftReturnsNotFoundInsteadOfRecreatingIt() throws Exception {
        var oldScreen = editBody("09:00", "17:00", "Old screen", shift.getVersion());
        shifts.deleteById(shift.getId());

        edit(oldScreen).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("NOT_FOUND"));

        assertThat(shifts.existsById(shift.getId())).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"MANAGER", "EMPLOYEE"})
    void versionCannotBypassTeamAuthorization(String role) throws Exception {
        User outsider = users.save(new User("outsider-" + UUID.randomUUID(), "unused-test-hash", "Other user",
                null, ApplicationRole.valueOf(role)));
        var before = snapshot();

        mvc.perform(put(path() + "/" + shift.getId()).with(user(outsider.getUsername()).roles(role))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsBytes(editBody("09:00", "17:00", "Unauthorized", 999L))))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("FORBIDDEN"));

        assertThat(snapshot()).isEqualTo(before);
    }

    @Test
    void unauthenticatedEditIsRejected() throws Exception {
        var before = snapshot();

        mvc.perform(put(path() + "/" + shift.getId()).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsBytes(editBody("09:00", "17:00", "Anonymous", shift.getVersion()))))
                .andExpect(status().isUnauthorized());

        assertThat(snapshot()).isEqualTo(before);
    }

    private Map<String, Object> editBody(String start, String end, String description, Long version) {
        return new HashMap<>(Map.of("startTime", "2030-01-07T" + start + ":00Z",
                "endTime", "2030-01-07T" + end + ":00Z", "description", description,
                "requiredWorkers", 1, "minRestHours", 0, "version", version));
    }

    private ResultActions edit(Map<String, Object> body) throws Exception {
        return mvc.perform(put(path() + "/" + shift.getId())
                .with(user(manager.getUsername()).roles("MANAGER"))
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(body)));
    }

    private String path() {
        return "/api/schedules/" + schedule.getId() + "/shifts";
    }

    private JsonNode response(ResultActions result) throws Exception {
        return json.readTree(result.andReturn().getResponse().getContentAsByteArray());
    }

    private Map<String, Object> snapshot() {
        return jdbc.queryForMap("select * from shifts where id = ?", shift.getId());
    }
}
