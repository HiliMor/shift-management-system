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

import com.hilimor.shiftmanagement.assignment.Assignment;
import com.hilimor.shiftmanagement.assignment.AssignmentRepository;
import com.hilimor.shiftmanagement.notification.SchedulePublishedNotificationService;
import com.hilimor.shiftmanagement.request.SwapRequest;
import com.hilimor.shiftmanagement.request.SwapRequestRepository;
import com.hilimor.shiftmanagement.request.SwapRequestStatus;
import com.hilimor.shiftmanagement.schedule.Schedule;
import com.hilimor.shiftmanagement.schedule.SchedulePublishedEvent;
import com.hilimor.shiftmanagement.schedule.ScheduleRepository;
import com.hilimor.shiftmanagement.schedule.ScheduleStatus;
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
import com.hilimor.shiftmanagement.user.ApplicationRole;
import com.hilimor.shiftmanagement.user.User;
import com.hilimor.shiftmanagement.user.UserRepository;
import com.hilimor.shiftmanagement.template.ShiftTemplate;
import com.hilimor.shiftmanagement.template.ShiftTemplateRepository;
import com.hilimor.shiftmanagement.template.TemplateSlot;
import com.hilimor.shiftmanagement.template.TemplateSlotRepository;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration
@ConditionalOnProperty(prefix = "app.seed", name = "enabled", havingValue = "true")
public class DevelopmentDataSeeder {

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
            TransactionTemplate transactionTemplate
    ) {
        return args -> transactionTemplate.executeWithoutResult(status -> seedInitialData(
                userRepository,
                teamRepository,
                teamMemberRepository,
                teamManagerRepository,
                staffingRoleRepository,
                teamMemberStaffingRoleRepository,
                scheduleRepository,
                shiftRepository,
                assignmentRepository,
                swapRequestRepository,
                shiftTemplateRepository,
                templateSlotRepository,
                schedulePublishedNotificationService,
                passwordEncoder
        ));
    }

    private void seedInitialData(
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
            PasswordEncoder passwordEncoder
    ) {
        User manager = findOrCreateUser(
                userRepository,
                passwordEncoder,
                "manager1",
                "מנהל הדמו",
                "manager1@example.com",
                ApplicationRole.MANAGER
        );
        List<User> employees = List.of(
                findOrCreateUser(userRepository, passwordEncoder, "employee1", "אלון כהן", "employee1@example.com", ApplicationRole.EMPLOYEE),
                findOrCreateUser(userRepository, passwordEncoder, "employee2", "נועה לוי", "employee2@example.com", ApplicationRole.EMPLOYEE),
                findOrCreateUser(userRepository, passwordEncoder, "employee3", "יובל מזרחי", "employee3@example.com", ApplicationRole.EMPLOYEE),
                findOrCreateUser(userRepository, passwordEncoder, "employee4", "מאיה אברהם", "employee4@example.com", ApplicationRole.EMPLOYEE),
                findOrCreateUser(userRepository, passwordEncoder, "employee5", "דניאל פרץ", "employee5@example.com", ApplicationRole.EMPLOYEE),
                findOrCreateUser(userRepository, passwordEncoder, "employee6", "שירה בן דוד", "employee6@example.com", ApplicationRole.EMPLOYEE),
                findOrCreateUser(userRepository, passwordEncoder, "employee7", "עומר ישראלי", "employee7@example.com", ApplicationRole.EMPLOYEE),
                findOrCreateUser(userRepository, passwordEncoder, "employee8", "תמר רון", "employee8@example.com", ApplicationRole.EMPLOYEE)
        );

        Team developmentTeam = findOrCreateTeam(teamRepository);
        Instant seededAt = Instant.now();
        List<TeamMember> employeeMemberships = employees.stream()
                .map(employee -> findOrCreateTeamMember(teamMemberRepository, employee, developmentTeam, seededAt))
                .toList();
        ensureTeamManager(teamManagerRepository, manager, developmentTeam);

        StaffingRole backendDeveloperRole = findOrCreateStaffingRole(
                staffingRoleRepository,
                developmentTeam,
                "Backend Developer",
                "פיתוח ותחזוקה של שירותי Backend."
        );
        StaffingRole frontendDeveloperRole = findOrCreateStaffingRole(
                staffingRoleRepository,
                developmentTeam,
                "Frontend Developer",
                "פיתוח ותחזוקה של יכולות Frontend."
        );
        StaffingRole qaEngineerRole = findOrCreateStaffingRole(
                staffingRoleRepository,
                developmentTeam,
                "QA Engineer",
                "בדיקת גרסאות ואימות איכות המוצר."
        );
        employeeMemberships.subList(0, 3).forEach(
                membership -> ensureTeamMemberRole(teamMemberStaffingRoleRepository, membership, backendDeveloperRole, seededAt)
        );
        employeeMemberships.subList(3, 6).forEach(
                membership -> ensureTeamMemberRole(teamMemberStaffingRoleRepository, membership, frontendDeveloperRole, seededAt)
        );
        employeeMemberships.subList(6, 8).forEach(
                membership -> ensureTeamMemberRole(teamMemberStaffingRoleRepository, membership, qaEngineerRole, seededAt)
        );

        ZoneId teamZone = ZoneId.of(developmentTeam.getTimeZone());
        LocalDate currentWeekStart = LocalDate.now(teamZone).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate currentWeekEnd = currentWeekStart.plusDays(6);
        LocalDate nextWeekStart = currentWeekStart.plusWeeks(1);
        LocalDate manualAssignmentDraftEnd = nextWeekStart.plusDays(6);
        LocalDate automaticAssignmentDraftStart = nextWeekStart.plusWeeks(1);
        LocalDate automaticAssignmentDraftEnd = automaticAssignmentDraftStart.plusDays(20);

        Schedule publishedSchedule = findOrCreateSchedule(
                scheduleRepository,
                developmentTeam,
                currentWeekStart,
                currentWeekEnd,
                ScheduleStatus.PUBLISHED,
                seededAt.minusSeconds(86_400)
        );
        findOrCreateSchedule(
                scheduleRepository,
                developmentTeam,
                nextWeekStart,
                manualAssignmentDraftEnd,
                ScheduleStatus.DRAFT,
                seededAt
        );
        findOrCreateSchedule(
                scheduleRepository,
                developmentTeam,
                automaticAssignmentDraftStart,
                automaticAssignmentDraftEnd,
                ScheduleStatus.DRAFT,
                seededAt
        );

        ShiftTemplate dailyCoverageTemplate = findOrCreateTemplate(
                shiftTemplateRepository,
                developmentTeam,
                "כיסוי פיתוח יומי בחירום",
                "שלוש משמרות טכניות קבועות של שמונה שעות החוזרות בכל יום.",
                1,
                developmentTeam.getDefaultMinRestHours()
        );
        dailyCoverageTemplate.activate();
        shiftTemplateRepository.save(dailyCoverageTemplate);
        ensureTemplateSlot(
                templateSlotRepository,
                dailyCoverageTemplate,
                LocalTime.MIDNIGHT,
                480,
                "תמיכת Backend בלילה",
                1,
                backendDeveloperRole
        );
        ensureTemplateSlot(
                templateSlotRepository,
                dailyCoverageTemplate,
                LocalTime.of(8, 0),
                480,
                "פיתוח Frontend ביום",
                1,
                frontendDeveloperRole
        );
        ensureTemplateSlot(
                templateSlotRepository,
                dailyCoverageTemplate,
                LocalTime.of(16, 0),
                480,
                "תמיכת QA ושחרור גרסאות בערב",
                1,
                qaEngineerRole
        );

        Shift publishedMorningShift = findOrCreateShift(
                shiftRepository,
                publishedSchedule,
                at(teamZone, currentWeekStart.plusDays(1), 9),
                at(teamZone, currentWeekStart.plusDays(1), 17),
                "תמיכת שירותי Backend",
                1,
                developmentTeam.getDefaultMinRestHours(),
                backendDeveloperRole
        );
        Shift publishedEveningShift = findOrCreateShift(
                shiftRepository,
                publishedSchedule,
                at(teamZone, currentWeekStart.plusDays(2), 14),
                at(teamZone, currentWeekStart.plusDays(2), 22),
                "תמיכה בשחרור גרסת Frontend",
                1,
                developmentTeam.getDefaultMinRestHours(),
                frontendDeveloperRole
        );
        Shift transferSourceShift = findOrCreateShift(
                shiftRepository,
                publishedSchedule,
                at(teamZone, currentWeekStart.plusDays(3), 9),
                at(teamZone, currentWeekStart.plusDays(3), 17),
                "חקירת תקלה",
                1,
                developmentTeam.getDefaultMinRestHours(),
                null
        );

        User employeeOne = employees.get(0);
        User employeeTwo = employees.get(1);
        findOrCreateAssignment(assignmentRepository, publishedMorningShift, employeeOne, seededAt);
        findOrCreateAssignment(assignmentRepository, publishedEveningShift, employeeTwo, seededAt);
        Assignment transferSourceAssignment = findOrCreateAssignment(
                assignmentRepository,
                transferSourceShift,
                employeeOne,
                seededAt
        );

        ensurePendingTransferRequest(
                swapRequestRepository,
                employeeOne,
                transferSourceAssignment,
                employeeTwo,
                seededAt.minusSeconds(3_600)
        );
        ensureSchedulePublishedNotifications(schedulePublishedNotificationService, publishedSchedule);
    }

    private ShiftTemplate findOrCreateTemplate(
            ShiftTemplateRepository shiftTemplateRepository,
            Team team,
            String name,
            String description,
            int cycleDays,
            int defaultMinRestHours
    ) {
        return shiftTemplateRepository.findByTeam_IdAndName(team.getId(), name)
                .map(existingTemplate -> {
                    existingTemplate.updateDetails(name, description, cycleDays, defaultMinRestHours);
                    return existingTemplate;
                })
                .orElseGet(() -> shiftTemplateRepository.save(new ShiftTemplate(
                        team,
                        name,
                        description,
                        cycleDays,
                        defaultMinRestHours
                )));
    }

    private void ensureTemplateSlot(
            TemplateSlotRepository templateSlotRepository,
            ShiftTemplate shiftTemplate,
            LocalTime startTime,
            int durationMinutes,
            String description,
            int requiredWorkers,
            StaffingRole requiredStaffingRole
    ) {
        boolean slotExists = templateSlotRepository
                .findByShiftTemplate_IdOrderByDayOffsetAscStartTimeAsc(shiftTemplate.getId())
                .stream()
                .anyMatch(slot -> slot.getDayOffset() == 0 && slot.getStartTime().equals(startTime));

        if (!slotExists) {
            templateSlotRepository.save(new TemplateSlot(
                    shiftTemplate,
                    0,
                    startTime,
                    durationMinutes,
                    description,
                    requiredWorkers,
                    requiredStaffingRole
            ));
        }
    }

    private User findOrCreateUser(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            String username,
            String fullName,
            String email,
            ApplicationRole role
    ) {
        return userRepository.findByUsername(username)
                .orElseGet(() -> userRepository.save(new User(
                        username,
                        passwordEncoder.encode("password"),
                        fullName,
                        email,
                        role
                )));
    }

    private Team findOrCreateTeam(TeamRepository teamRepository) {
        return teamRepository.findFirstByNameOrderByIdAsc("צוות פיתוח")
                .orElseGet(() -> teamRepository.save(new Team(
                        "צוות פיתוח",
                        SwapApprovalPolicy.MANAGER,
                        8,
                        "Asia/Jerusalem"
                )));
    }

    private TeamMember findOrCreateTeamMember(
            TeamMemberRepository teamMemberRepository,
            User user,
            Team team,
            Instant joinedAt
    ) {
        return teamMemberRepository.findByUser_IdAndTeam_Id(user.getId(), team.getId())
                .orElseGet(() -> teamMemberRepository.save(new TeamMember(user, team, joinedAt, true)));
    }

    private void ensureTeamManager(TeamManagerRepository teamManagerRepository, User manager, Team team) {
        if (!teamManagerRepository.existsByManager_IdAndTeam_Id(manager.getId(), team.getId())) {
            teamManagerRepository.save(new TeamManager(manager, team));
        }
    }

    private StaffingRole findOrCreateStaffingRole(
            StaffingRoleRepository staffingRoleRepository,
            Team team,
            String name,
            String description
    ) {
        return staffingRoleRepository.findByTeam_IdAndName(team.getId(), name)
                .orElseGet(() -> staffingRoleRepository.save(new StaffingRole(team, name, description)));
    }

    private void ensureTeamMemberRole(
            TeamMemberStaffingRoleRepository teamMemberStaffingRoleRepository,
            TeamMember teamMember,
            StaffingRole staffingRole,
            Instant assignedAt
    ) {
        if (!teamMemberStaffingRoleRepository.existsByTeamMember_IdAndStaffingRole_Id(
                teamMember.getId(),
                staffingRole.getId()
        )) {
            teamMemberStaffingRoleRepository.save(new TeamMemberStaffingRole(teamMember, staffingRole, assignedAt));
        }
    }

    private Schedule findOrCreateSchedule(
            ScheduleRepository scheduleRepository,
            Team team,
            LocalDate startDate,
            LocalDate endDate,
            ScheduleStatus status,
            Instant publishedAt
    ) {
        return scheduleRepository.findByTeam_IdAndStartDateAndEndDateAndStatus(
                        team.getId(),
                        startDate,
                        endDate,
                        status
                )
                .orElseGet(() -> {
                    Schedule schedule = scheduleRepository.save(new Schedule(team, startDate, endDate));

                    if (status == ScheduleStatus.PUBLISHED) {
                        schedule.publish(publishedAt);
                    }

                    return schedule;
                });
    }

    private Shift findOrCreateShift(
            ShiftRepository shiftRepository,
            Schedule schedule,
            Instant startTime,
            Instant endTime,
            String description,
            int requiredWorkers,
            int minRestHours,
            StaffingRole requiredStaffingRole
    ) {
        return shiftRepository.findBySchedule_IdOrderByStartTime(schedule.getId())
                .stream()
                .filter(shift -> shift.getStartTime().equals(startTime) && shift.getEndTime().equals(endTime))
                .findFirst()
                .orElseGet(() -> shiftRepository.save(new Shift(
                        schedule,
                        startTime,
                        endTime,
                        description,
                        requiredWorkers,
                        minRestHours,
                        requiredStaffingRole
                )));
    }

    private Assignment findOrCreateAssignment(
            AssignmentRepository assignmentRepository,
            Shift shift,
            User employee,
            Instant assignedAt
    ) {
        return assignmentRepository.findByShift_IdAndEmployee_Id(shift.getId(), employee.getId())
                .orElseGet(() -> assignmentRepository.save(new Assignment(shift, employee, assignedAt)));
    }

    private void ensurePendingTransferRequest(
            SwapRequestRepository swapRequestRepository,
            User requester,
            Assignment sourceAssignment,
            User targetEmployee,
            Instant createdAt
    ) {
        if (!swapRequestRepository.existsBySourceAssignment_IdAndStatusIn(
                sourceAssignment.getId(),
                List.of(SwapRequestStatus.PENDING_EMPLOYEE, SwapRequestStatus.PENDING_MANAGER)
        )) {
            swapRequestRepository.save(SwapRequest.createTransfer(
                    requester,
                    sourceAssignment,
                    targetEmployee,
                    createdAt
            ));
        }
    }

    private void ensureSchedulePublishedNotifications(
            SchedulePublishedNotificationService schedulePublishedNotificationService,
            Schedule schedule
    ) {
        UUID eventId = UUID.nameUUIDFromBytes(
                ("demo.schedule.published." + schedule.getId()).getBytes(StandardCharsets.UTF_8)
        );

        schedulePublishedNotificationService.createNotifications(eventId, new SchedulePublishedEvent(
                schedule.getId(),
                schedule.getTeam().getId(),
                schedule.getTeam().getName(),
                schedule.getStartDate(),
                schedule.getEndDate(),
                schedule.getPublicationNumber(),
                schedule.getPublishedAt()
        ));
    }

    private Instant at(ZoneId zoneId, LocalDate date, int hour) {
        return date.atTime(LocalTime.of(hour, 0)).atZone(zoneId).toInstant();
    }
}
