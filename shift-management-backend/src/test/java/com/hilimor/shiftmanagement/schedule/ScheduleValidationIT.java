package com.hilimor.shiftmanagement.schedule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.hilimor.shiftmanagement.assignment.Assignment;
import com.hilimor.shiftmanagement.assignment.AssignmentRepository;
import com.hilimor.shiftmanagement.assignment.AssignmentValidationException;
import com.hilimor.shiftmanagement.availability.AvailabilityConstraint;
import com.hilimor.shiftmanagement.availability.AvailabilityConstraintRepository;
import com.hilimor.shiftmanagement.messaging.EventOutboxRepository;
import com.hilimor.shiftmanagement.shift.Shift;
import com.hilimor.shiftmanagement.shift.ShiftRepository;
import com.hilimor.shiftmanagement.shift.ShiftResponse;
import com.hilimor.shiftmanagement.shift.ShiftService;
import com.hilimor.shiftmanagement.shift.UpdateShiftRequest;
import com.hilimor.shiftmanagement.staffing.StaffingRole;
import com.hilimor.shiftmanagement.staffing.StaffingRoleRepository;
import com.hilimor.shiftmanagement.staffing.TeamMemberStaffingRole;
import com.hilimor.shiftmanagement.staffing.TeamMemberStaffingRoleRepository;
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
        "spring.jms.listener.auto-startup=false"
})
@Testcontainers
class ScheduleValidationIT {

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16")
            .withDatabaseName("schedule_validation_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired private ShiftService shiftService;
    @Autowired private ScheduleService scheduleService;
    @Autowired private AssignmentRepository assignmentRepository;
    @Autowired private ScheduleRepository scheduleRepository;
    @Autowired private ShiftRepository shiftRepository;
    @Autowired private TeamRepository teamRepository;
    @Autowired private TeamManagerRepository teamManagerRepository;
    @Autowired private TeamMemberRepository teamMemberRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private StaffingRoleRepository staffingRoleRepository;
    @Autowired private TeamMemberStaffingRoleRepository memberRoleRepository;
    @Autowired private AvailabilityConstraintRepository availabilityRepository;
    @Autowired private EventOutboxRepository outboxRepository;
    @Autowired private TransactionTemplate transactionTemplate;
    @Autowired private JdbcTemplate jdbc;

    private User manager;
    private User employee;
    private Team team;
    private TeamMember member;
    private Schedule schedule;
    private Shift shift;
    private Long assignmentId;

    @BeforeEach
    void createFixture() {
        transactionTemplate.executeWithoutResult(status -> {
            String suffix = UUID.randomUUID().toString();
            manager = user("manager-" + suffix, ApplicationRole.MANAGER);
            employee = user("employee-" + suffix, ApplicationRole.EMPLOYEE);
            team = teamRepository.save(new Team("Team " + suffix, SwapApprovalPolicy.MANAGER, 8, "UTC"));
            teamManagerRepository.save(new TeamManager(manager, team));
            member = teamMemberRepository.save(new TeamMember(employee, team, Instant.now(), true));
            schedule = scheduleRepository.save(new Schedule(
                    team, LocalDate.of(2030, 1, 1), LocalDate.of(2030, 1, 31)));
            shift = shiftRepository.save(new Shift(schedule, at("07T09:00"), at("07T17:00"), "Original", 1, 8));
            assignmentId = assignmentRepository.save(new Assignment(shift, employee, Instant.now())).getId();
        });
    }

    @ParameterizedTest
    @EnumSource(Conflict.class)
    void invalidEditRollsBackAllShiftFieldsAndKeepsAssignments(Conflict conflict) {
        UpdateShiftRequest request = invalidUpdate(conflict);
        ShiftResponse before = persistedShift();
        List<Long> ownersBefore = ownerIds();

        assertConflict(() -> update(request), conflict.code);

        assertThat(persistedShift()).isEqualTo(before);
        assertThat(ownerIds()).isEqualTo(ownersBefore);
    }

    @ParameterizedTest
    @EnumSource(Conflict.class)
    void readinessAndPublicationRejectLegacyInvalidAssignmentsEvenWithConfirmation(Conflict conflict) {
        UpdateShiftRequest request = invalidUpdate(conflict);
        // Seed an edit accepted by the old implementation, bypassing the service under test.
        transactionTemplate.executeWithoutResult(status -> {
            Shift existing = shiftRepository.findById(shift.getId()).orElseThrow();
            StaffingRole role = request.requiredStaffingRoleId() == null ? null
                    : staffingRoleRepository.findById(request.requiredStaffingRoleId()).orElseThrow();
            existing.updateDetails(request.startTime(), request.endTime(), request.description(),
                    request.requiredWorkers(), request.minRestHours(), role);
        });

        assertPublicationBlocked(conflict.code);
    }

    @Test
    void inactiveMemberBlocksEditReadinessAndPublication() {
        jdbc.update("update team_members set active = false where id = ?", member.getId());
        ShiftResponse before = persistedShift();

        assertConflict(() -> update(request(at("07T09:00"), at("07T17:00"), 1, 8, null)), "TEAM_MEMBERSHIP");

        assertThat(persistedShift()).isEqualTo(before);
        assertPublicationBlocked("TEAM_MEMBERSHIP");
    }

    @Test
    void validEditPreservesFullAssignmentAtExactRestAndAvailabilityBoundaries() {
        StaffingRole role = staffingRole();
        memberRoleRepository.save(new TeamMemberStaffingRole(member, role, Instant.now()));
        otherAssignment(at("06T17:00"), at("07T01:00"));
        otherAssignment(at("08T03:00"), at("08T11:00"));
        unavailable(at("07T18:00"), at("07T19:00"));

        ShiftResponse response = update(request(at("07T10:00"), at("07T18:00"), 1, 9, role.getId()));

        assertThat(response.startTime()).isEqualTo(at("07T10:00"));
        assertThat(response.endTime()).isEqualTo(at("07T18:00"));
        assertThat(response.minRestHours()).isEqualTo(9);
        assertThat(response.requiredStaffingRoleId()).isEqualTo(role.getId());
        assertThat(persistedShift()).isEqualTo(response);
        assertThat(ownerIds()).containsExactly(employee.getId());
        assertThat(readiness().readyToPublish()).isTrue();
        assertPublished(false);
    }

    @Test
    void unfilledButValidScheduleStillRequiresAndAcceptsExplicitConfirmation() {
        update(request(at("07T09:00"), at("07T17:00"), 2, 8, null));

        assertThat(readiness().readyToPublish()).isFalse();
        assertThat(readiness().totalOpenSlots()).isEqualTo(1);
        assertThatThrownBy(() -> publish(false)).isInstanceOfSatisfying(ResponseStatusException.class,
                exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
        assertThat(scheduleRepository.findById(schedule.getId()).orElseThrow().getStatus()).isEqualTo(ScheduleStatus.DRAFT);
        assertPublished(true);
    }

    @Test
    void unassignedShiftCanBeEditedWithoutEmployeeEligibilityChecks() {
        assignmentRepository.deleteById(assignmentId);
        StaffingRole role = staffingRole();

        ShiftResponse response = update(request(at("08T10:00"), at("08T18:00"), 2, 10, role.getId()));

        assertThat(persistedShift()).isEqualTo(response);
        assertThat(ownerIds()).isEmpty();
        assertThat(readiness().totalOpenSlots()).isEqualTo(2);
        assertPublished(true);
    }

    private UpdateShiftRequest invalidUpdate(Conflict conflict) {
        return switch (conflict) {
            case AVAILABILITY -> {
                unavailable(at("07T18:00"), at("07T22:00"));
                yield request(at("07T09:00"), at("07T19:00"), 1, 8, null);
            }
            case OVERLAP -> {
                otherAssignment(at("08T01:00"), at("08T09:00"));
                yield request(at("07T09:00"), at("08T02:00"), 1, 8, null);
            }
            case REST_BEFORE -> {
                otherAssignment(at("06T17:00"), at("07T01:00"));
                yield request(at("07T08:00"), at("07T17:00"), 1, 8, null);
            }
            case REST_AFTER -> {
                otherAssignment(at("08T01:00"), at("08T09:00"));
                yield request(at("07T09:00"), at("07T18:00"), 1, 8, null);
            }
            case INCREASED_REST -> {
                otherAssignment(at("06T17:00"), at("07T01:00"));
                yield request(at("07T09:00"), at("07T17:00"), 1, 9, null);
            }
            case ROLE -> request(at("07T09:00"), at("07T17:00"), 1, 8, staffingRole().getId());
            case CAPACITY -> {
                transactionTemplate.executeWithoutResult(status -> {
                    Shift existing = shiftRepository.findById(shift.getId()).orElseThrow();
                    existing.updateDetails(existing.getStartTime(), existing.getEndTime(), "Two workers", 2, 8);
                    User second = user("second-" + UUID.randomUUID(), ApplicationRole.EMPLOYEE);
                    teamMemberRepository.save(new TeamMember(second, team, Instant.now(), true));
                    assignmentRepository.save(new Assignment(existing, second, Instant.now()));
                });
                yield request(at("07T09:00"), at("07T17:00"), 1, 8, null);
            }
        };
    }

    private void assertPublicationBlocked(String code) {
        long eventsBefore = outboxRepository.count();
        List<Long> ownersBefore = ownerIds();
        assertConflict(this::readiness, code);
        assertConflict(() -> publish(false), code);
        assertConflict(() -> publish(true), code);

        Schedule stored = scheduleRepository.findById(schedule.getId()).orElseThrow();
        assertThat(stored.getStatus()).isEqualTo(ScheduleStatus.DRAFT);
        assertThat(stored.getPublicationNumber()).isZero();
        assertThat(stored.getPublishedAt()).isNull();
        assertThat(outboxRepository.count()).isEqualTo(eventsBefore);
        assertThat(ownerIds()).isEqualTo(ownersBefore);
    }

    private void assertPublished(boolean confirmUnfilled) {
        long eventsBefore = outboxRepository.count();
        assertThat(publish(confirmUnfilled).status()).isEqualTo(ScheduleStatus.PUBLISHED);
        Schedule stored = scheduleRepository.findById(schedule.getId()).orElseThrow();
        assertThat(stored.getStatus()).isEqualTo(ScheduleStatus.PUBLISHED);
        assertThat(stored.getPublicationNumber()).isEqualTo(1);
        assertThat(stored.getPublishedAt()).isNotNull();
        assertThat(outboxRepository.count()).isEqualTo(eventsBefore + 1);
        assertThat(outboxRepository.findAll()).anySatisfy(event -> {
            assertThat(event.getEventType()).isEqualTo("schedule.published");
            assertThat(event.getPayload().path("scheduleId").asLong()).isEqualTo(schedule.getId());
        });
    }

    private ShiftResponse persistedShift() {
        return transactionTemplate.execute(status -> ShiftResponse.from(shiftRepository.findById(shift.getId()).orElseThrow()));
    }

    private List<Long> ownerIds() {
        return transactionTemplate.execute(status -> assignmentRepository
                .findByShift_Schedule_IdOrderByShift_StartTimeAscEmployee_FullNameAsc(schedule.getId()).stream()
                .map(assignment -> assignment.getEmployee().getId()).toList());
    }

    private ShiftResponse update(UpdateShiftRequest request) {
        return shiftService.updateShift(manager.getUsername(), schedule.getId(), shift.getId(), request);
    }

    private SchedulePublicationReadinessResponse readiness() {
        return scheduleService.getPublicationReadiness(manager.getUsername(), schedule.getId());
    }

    private ScheduleResponse publish(boolean confirmUnfilled) {
        return scheduleService.publishSchedule(manager.getUsername(), schedule.getId(), confirmUnfilled);
    }

    private UpdateShiftRequest request(Instant start, Instant end, int workers, int rest, Long roleId) {
        return new UpdateShiftRequest(start, end, "Updated", workers, rest, roleId, persistedShift().version());
    }

    private User user(String username, ApplicationRole role) {
        return userRepository.save(new User(username, "not-used-for-login", username, null, role));
    }

    private StaffingRole staffingRole() {
        return staffingRoleRepository.save(new StaffingRole(team, "Required role", null));
    }

    private void otherAssignment(Instant start, Instant end) {
        transactionTemplate.executeWithoutResult(status -> {
            Schedule other = scheduleRepository.save(new Schedule(team, schedule.getStartDate(), schedule.getEndDate()));
            Shift otherShift = shiftRepository.save(new Shift(other, start, end, "Other schedule", 1, 8));
            assignmentRepository.save(new Assignment(otherShift, employee, Instant.now()));
        });
    }

    private void unavailable(Instant start, Instant end) {
        availabilityRepository.save(new AvailabilityConstraint(employee, start, end, "Unavailable", Instant.now()));
    }

    private static Instant at(String dayAndTime) {
        return Instant.parse("2030-01-" + dayAndTime + ":00Z");
    }

    private static void assertConflict(Runnable action, String code) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(AssignmentValidationException.class, exception -> {
            assertThat(exception.getStatus()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(exception.getCode()).isEqualTo(code);
        });
    }

    enum Conflict {
        AVAILABILITY("AVAILABILITY_CONFLICT"), OVERLAP("SHIFT_OVERLAP"),
        REST_BEFORE("MINIMUM_REST"), REST_AFTER("MINIMUM_REST"), INCREASED_REST("MINIMUM_REST"),
        ROLE("STAFFING_ROLE_REQUIRED"), CAPACITY("SHIFT_CAPACITY");

        private final String code;

        Conflict(String code) {
            this.code = code;
        }
    }
}
