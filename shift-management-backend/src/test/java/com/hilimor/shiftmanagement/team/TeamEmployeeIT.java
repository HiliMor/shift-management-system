package com.hilimor.shiftmanagement.team;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hilimor.shiftmanagement.staffing.StaffingRole;
import com.hilimor.shiftmanagement.staffing.StaffingRoleRepository;
import com.hilimor.shiftmanagement.staffing.TeamMemberStaffingRoleRepository;
import com.hilimor.shiftmanagement.user.ApplicationRole;
import com.hilimor.shiftmanagement.user.User;
import com.hilimor.shiftmanagement.user.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest(properties = { "app.seed.enabled=false", "app.messaging.notifications.enabled=false",
        "app.messaging.outbox-dispatch.enabled=false", "spring.jms.listener.auto-startup=false" })
@AutoConfigureMockMvc
@Testcontainers
class TeamEmployeeIT {
    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16")
            .withDatabaseName("employee_creation_test").withUsername("test").withPassword("test");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UserRepository users;
    @Autowired TeamRepository teams;
    @Autowired TeamManagerRepository managers;
    @Autowired TeamMemberRepository members;
    @Autowired StaffingRoleRepository roles;
    @Autowired TeamMemberStaffingRoleRepository memberRoles;
    @Autowired PasswordEncoder passwords;
    Team team;
    Team otherTeam;
    User manager;
    StaffingRole role;

    @BeforeEach
    void fixture() {
        manager = users.save(new User("manager-" + UUID.randomUUID(), "unused", "Manager", null, ApplicationRole.MANAGER));
        team = teams.save(new Team("Team " + UUID.randomUUID(), SwapApprovalPolicy.MANAGER, 8, "UTC"));
        otherTeam = teams.save(new Team("Other " + UUID.randomUUID(), SwapApprovalPolicy.MANAGER, 8, "UTC"));
        managers.save(new TeamManager(manager, team));
        role = roles.save(new StaffingRole(team, "Developer", null));
    }

    Map<String, Object> body() {
        return new HashMap<>(Map.of("username", "employee-" + UUID.randomUUID(), "password", "Test-only-123",
                "fullName", "New Employee", "email", "new@example.test", "staffingRoleIds", List.of(role.getId())));
    }

    ResultActions create(Long teamId, Map<String, Object> body) throws Exception {
        return mvc.perform(post("/api/teams/" + teamId + "/employees")
                .with(user(manager.getUsername()).roles("MANAGER"))
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(body)));
    }

    @Test
    void createsEmployeeMembershipAndRolesAndAllowsRealLoginWithoutExposingPassword() throws Exception {
        var body = body();
        body.put("applicationRole", "MANAGER");
        body.put("staffingRoleIds", List.of(role.getId(), role.getId()));
        String content = create(team.getId(), body).andExpect(status().isCreated())
                .andExpect(jsonPath("$.staffingRoleIds.length()").value(1))
                .andExpect(jsonPath("$.password").doesNotExist()).andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        User employee = users.findByUsername((String) body.get("username")).orElseThrow();
        assertThat(employee.getApplicationRole()).isEqualTo(ApplicationRole.EMPLOYEE);
        assertThat(employee.getPasswordHash()).isNotEqualTo(body.get("password"));
        assertThat(passwords.matches((String) body.get("password"), employee.getPasswordHash())).isTrue();
        assertThat(content).doesNotContain((String) body.get("password"), employee.getPasswordHash());
        assertThat(members.existsByUser_IdAndTeam_IdAndActiveTrue(employee.getId(), team.getId())).isTrue();
        assertThat(memberRoles.findByTeamMember_User_Id(employee.getId())).hasSize(1);
        String login = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsBytes(Map.of("username", body.get("username"), "password", body.get("password")))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.user.applicationRole").value("EMPLOYEE"))
                .andReturn().getResponse().getContentAsString();
        String token = json.readTree(login).path("accessToken").asText();
        mvc.perform(get("/api/teams/me/memberships").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].teamId").value(team.getId()));
    }

    @Test
    void supportsNoRolesAndTrimsProfileFields() throws Exception {
        var body = body();
        body.put("username", "  " + body.get("username") + "  ");
        body.put("fullName", "  New Employee  ");
        body.put("email", "  ");
        body.remove("staffingRoleIds");
        create(team.getId(), body).andExpect(status().isCreated())
                .andExpect(jsonPath("$.fullName").value("New Employee"))
                .andExpect(jsonPath("$.staffingRoleIds").isEmpty());
        User employee = users.findByUsername(((String) body.get("username")).trim()).orElseThrow();
        assertThat(employee.getEmail()).isNull();
    }

    @Test
    void invalidInputDoesNotCreatePartialRecords() throws Exception {
        for (var invalid : List.of(Map.entry("password", "short"), Map.entry("fullName", "   "),
                Map.entry("email", "not-an-email"), Map.entry("username", "a b"),
                Map.entry("password", "\u05d0".repeat(40)))) {
            long before = users.count();
            var body = body();
            body.put(invalid.getKey(), invalid.getValue());
            create(team.getId(), body).andExpect(status().isBadRequest());
            assertThat(users.count()).isEqualTo(before);
        }
    }

    @Test
    void rejectsAnonymousEmployeeAndManagerOfAnotherTeam() throws Exception {
        var body = body();
        long before = users.count();
        mvc.perform(post("/api/teams/" + team.getId() + "/employees").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsBytes(body))).andExpect(status().isUnauthorized());
        create(otherTeam.getId(), body).andExpect(status().isForbidden());
        User employee = users.save(new User("outsider-" + UUID.randomUUID(), "unused", "Employee", null, ApplicationRole.EMPLOYEE));
        mvc.perform(post("/api/teams/" + team.getId() + "/employees").with(user(employee.getUsername()).roles("EMPLOYEE"))
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(body))).andExpect(status().isForbidden());
        assertThat(users.count()).isEqualTo(before + 1);
    }

    @Test
    void rejectsMissingAndForeignRolesWithoutCreatingUserOrMembership() throws Exception {
        StaffingRole foreign = roles.save(new StaffingRole(otherTeam, "Foreign", null));
        for (Long roleId : List.of(foreign.getId(), Long.MAX_VALUE)) {
            var body = body();
            body.put("staffingRoleIds", List.of(roleId));
            long before = members.count();
            create(team.getId(), body).andExpect(status().isBadRequest());
            assertThat(users.existsByUsername((String) body.get("username"))).isFalse();
            assertThat(members.count()).isEqualTo(before);
        }
    }

    @Test
    void concurrentDuplicateUsernamesCreateExactlyOneEmployee() throws Exception {
        managers.save(new TeamManager(manager, otherTeam));
        var body = body();
        body.put("staffingRoleIds", List.of());
        long usersBefore = users.count();
        long membersBefore = members.count();
        var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> { start.await(); return create(team.getId(), body).andReturn().getResponse().getStatus(); });
            var second = executor.submit(() -> { start.await(); return create(otherTeam.getId(), body).andReturn().getResponse().getStatus(); });
            start.countDown();
            assertThat(List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(201, 409);
        }
        assertThat(users.count()).isEqualTo(usersBefore + 1);
        assertThat(members.count()).isEqualTo(membersBefore + 1);
        create(team.getId(), body).andExpect(status().isConflict());
    }
}
