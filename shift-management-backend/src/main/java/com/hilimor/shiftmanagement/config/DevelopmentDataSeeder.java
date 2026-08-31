package com.hilimor.shiftmanagement.config;

import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import com.hilimor.shiftmanagement.assignment.Assignment;
import com.hilimor.shiftmanagement.assignment.AssignmentRepository;
import com.hilimor.shiftmanagement.messaging.EventOutboxRepository;
import com.hilimor.shiftmanagement.notification.SchedulePublishedNotificationService;
import com.hilimor.shiftmanagement.request.SwapRequest;
import com.hilimor.shiftmanagement.request.SwapRequestRepository;
import com.hilimor.shiftmanagement.schedule.Schedule;
import com.hilimor.shiftmanagement.schedule.SchedulePublishedEvent;
import com.hilimor.shiftmanagement.schedule.ScheduleRepository;
import com.hilimor.shiftmanagement.shift.Shift;
import com.hilimor.shiftmanagement.shift.ShiftRepository;
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
import com.hilimor.shiftmanagement.template.ShiftTemplate;
import com.hilimor.shiftmanagement.template.ShiftTemplateRepository;
import com.hilimor.shiftmanagement.template.TemplateSlot;
import com.hilimor.shiftmanagement.template.TemplateSlotRepository;
import com.hilimor.shiftmanagement.user.ApplicationRole;
import com.hilimor.shiftmanagement.user.User;
import com.hilimor.shiftmanagement.user.UserRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration
@ConditionalOnProperty(prefix = "app.seed", name = "enabled", havingValue = "true")
public class DevelopmentDataSeeder {

    private static final Logger log = LoggerFactory.getLogger(DevelopmentDataSeeder.class);
    private static final long INITIALIZATION_LOCK_ID = 739_418_205L;

    @Bean
    CommandLineRunner seedInitialData(
            UserRepository userRepository,
            TeamRepository teamRepository,
            TeamMemberRepository teamMemberRepository,
            TeamManagerRepository teamManagerRepository,
            StaffingRoleRepository staffingRoleRepository,
            TeamMemberStaffingRoleRepository teamMemberStaffingRoleRepository,
            ScheduleRepository scheduleRepository,
            ShiftRepository shiftRepository,
            AssignmentRepository assignmentRepository,
            SwapRequestRepository swapRequestRepository,
            ShiftTemplateRepository shiftTemplateRepository,
            TemplateSlotRepository templateSlotRepository,
            SchedulePublishedNotificationService schedulePublishedNotificationService,
            PasswordEncoder passwordEncoder,
            TransactionTemplate transactionTemplate,
            JdbcTemplate jdbcTemplate,
            EventOutboxRepository eventOutboxRepository
    ) {
        return args -> {
            boolean created = Boolean.TRUE.equals(transactionTemplate.execute(status -> {
                // No team row exists yet to lock; serialize initializers until commit/rollback.
                jdbcTemplate.queryForObject("select pg_advisory_xact_lock(?)", Object.class, INITIALIZATION_LOCK_ID);
                // Other business tables depend on users/teams through foreign keys. Outbox does not.
                if (userRepository.count() > 0 || teamRepository.count() > 0 || eventOutboxRepository.count() > 0) {
                    return false;
                }
                createInitialData(userRepository, teamRepository, teamMemberRepository, teamManagerRepository,
                        staffingRoleRepository, teamMemberStaffingRoleRepository, scheduleRepository, shiftRepository,
                        assignmentRepository, swapRequestRepository, shiftTemplateRepository, templateSlotRepository,
                        schedulePublishedNotificationService, passwordEncoder);
                return true;
            }));
            if (created) {
                log.info("Demo data initialized in an empty database. Use app.seed.enabled=false for normal startup.");
            } else {
                log.info("Demo initialization skipped: the database already contains application data. Nothing was changed.");
            }
        };
    }

    private void createInitialData(
            UserRepository userRepository,
            TeamRepository teamRepository,
            TeamMemberRepository teamMemberRepository,
            TeamManagerRepository teamManagerRepository,
            StaffingRoleRepository staffingRoleRepository,
            TeamMemberStaffingRoleRepository teamMemberStaffingRoleRepository,
            ScheduleRepository scheduleRepository,
            ShiftRepository shiftRepository,
            AssignmentRepository assignmentRepository,
            SwapRequestRepository swapRequestRepository,
            ShiftTemplateRepository shiftTemplateRepository,
            TemplateSlotRepository templateSlotRepository,
            SchedulePublishedNotificationService notificationService,
            PasswordEncoder passwordEncoder
    ) {
        Instant seededAt = Instant.now();
        User manager = createUser(userRepository, passwordEncoder, "manager1", "מנהל הדמו", ApplicationRole.MANAGER);
        List<String> employeeNames = List.of("אלון כהן", "נועה לוי", "יובל מזרחי", "מאיה אברהם",
                "דניאל פרץ", "שירה בן דוד", "עומר ישראלי", "תמר רון");
        List<User> employees = IntStream.range(0, employeeNames.size())
                .mapToObj(index -> createUser(userRepository, passwordEncoder,
                        "employee" + (index + 1), employeeNames.get(index), ApplicationRole.EMPLOYEE))
                .toList();

        Team team = teamRepository.save(new Team("צוות פיתוח", SwapApprovalPolicy.MANAGER, 8, "Asia/Jerusalem"));
        teamManagerRepository.save(new TeamManager(manager, team));
        List<TeamMember> memberships = employees.stream()
                .map(employee -> teamMemberRepository.save(new TeamMember(employee, team, seededAt, true)))
                .toList();

        StaffingRole backend = staffingRoleRepository.save(new StaffingRole(team, "Backend Developer", "פיתוח ותחזוקה של שירותי Backend."));
        StaffingRole frontend = staffingRoleRepository.save(new StaffingRole(team, "Frontend Developer", "פיתוח ותחזוקה של יכולות Frontend."));
        StaffingRole qa = staffingRoleRepository.save(new StaffingRole(team, "QA Engineer", "בדיקת גרסאות ואימות איכות המוצר."));
        for (int index = 0; index < memberships.size(); index++) {
            StaffingRole role = index < 3 ? backend : index < 6 ? frontend : qa;
            teamMemberStaffingRoleRepository.save(new TeamMemberStaffingRole(memberships.get(index), role, seededAt));
        }

        ZoneId zone = ZoneId.of(team.getTimeZone());
        LocalDate weekStart = LocalDate.now(zone).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        Schedule publishedSchedule = scheduleRepository.save(new Schedule(team, weekStart, weekStart.plusDays(6)));
        scheduleRepository.save(new Schedule(team, weekStart.plusWeeks(1), weekStart.plusWeeks(1).plusDays(6)));
        scheduleRepository.save(new Schedule(team, weekStart.plusWeeks(2), weekStart.plusWeeks(2).plusDays(20)));

        ShiftTemplate template = new ShiftTemplate(team, "כיסוי פיתוח יומי בחירום",
                "שלוש משמרות טכניות קבועות של שמונה שעות החוזרות בכל יום.", 1, team.getDefaultMinRestHours());
        template.activate();
        shiftTemplateRepository.save(template);
        createSlot(templateSlotRepository, template, LocalTime.MIDNIGHT, "תמיכת Backend בלילה", backend);
        createSlot(templateSlotRepository, template, LocalTime.of(8, 0), "פיתוח Frontend ביום", frontend);
        createSlot(templateSlotRepository, template, LocalTime.of(16, 0), "תמיכת QA ושחרור גרסאות בערב", qa);

        Shift morning = shiftRepository.save(new Shift(publishedSchedule,
                at(zone, weekStart.plusDays(1), 9), at(zone, weekStart.plusDays(1), 17),
                "תמיכת שירותי Backend", 1, team.getDefaultMinRestHours(), backend));
        Shift evening = shiftRepository.save(new Shift(publishedSchedule,
                at(zone, weekStart.plusDays(2), 14), at(zone, weekStart.plusDays(2), 22),
                "תמיכה בשחרור גרסת Frontend", 1, team.getDefaultMinRestHours(), frontend));
        Shift transferSource = shiftRepository.save(new Shift(publishedSchedule,
                at(zone, weekStart.plusDays(3), 9), at(zone, weekStart.plusDays(3), 17),
                "חקירת תקלה", 1, team.getDefaultMinRestHours(), null));

        User employeeOne = employees.get(0);
        User employeeTwo = employees.get(1);
        assignmentRepository.save(new Assignment(morning, employeeOne, seededAt));
        assignmentRepository.save(new Assignment(evening, employees.get(3), seededAt));
        Assignment sourceAssignment = assignmentRepository.save(new Assignment(transferSource, employeeOne, seededAt));
        publishedSchedule.publish(seededAt);
        swapRequestRepository.save(SwapRequest.createTransfer(employeeOne, sourceAssignment, employeeTwo, seededAt));

        // Preloaded demo notifications are not evidence of JMS delivery; real actions use the outbox.
        UUID eventId = UUID.nameUUIDFromBytes(("demo.schedule.published." + publishedSchedule.getId()).getBytes(StandardCharsets.UTF_8));
        notificationService.createNotifications(eventId, new SchedulePublishedEvent(
                publishedSchedule.getId(), team.getId(), team.getName(),
                publishedSchedule.getStartDate(), publishedSchedule.getEndDate(),
                publishedSchedule.getPublicationNumber(), publishedSchedule.getPublishedAt()));
    }

    private User createUser(UserRepository repository, PasswordEncoder encoder,
            String username, String fullName, ApplicationRole role) {
        return repository.save(new User(username, encoder.encode("password"), fullName, username + "@example.com", role));
    }

    private void createSlot(TemplateSlotRepository repository, ShiftTemplate template,
            LocalTime startTime, String description, StaffingRole role) {
        repository.save(new TemplateSlot(template, 0, startTime, 480, description, 1, role));
    }

    private Instant at(ZoneId zone, LocalDate date, int hour) {
        return date.atTime(LocalTime.of(hour, 0)).atZone(zone).toInstant();
    }
}
