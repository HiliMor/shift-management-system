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
            SchedulePublishedNotificationService schedulePublishedNotificationService,
            PasswordEncoder passwordEncoder
    ) {
        User manager = findOrCreateUser(
                userRepository,
                passwordEncoder,
                "manager1",
                "Demo Manager",
                "manager1@example.com",
                ApplicationRole.MANAGER
        );
        User employeeOne = findOrCreateUser(
                userRepository,
                passwordEncoder,
                "employee1",
                "Demo Employee One",
                "employee1@example.com",
                ApplicationRole.EMPLOYEE
        );
        User employeeTwo = findOrCreateUser(
                userRepository,
                passwordEncoder,
                "employee2",
                "Demo Employee Two",
                "employee2@example.com",
                ApplicationRole.EMPLOYEE
        );

        Team operationsTeam = findOrCreateTeam(teamRepository);
        Instant seededAt = Instant.now();
        TeamMember employeeOneMembership = findOrCreateTeamMember(
                teamMemberRepository,
                employeeOne,
                operationsTeam,
                seededAt
        );
        TeamMember employeeTwoMembership = findOrCreateTeamMember(
                teamMemberRepository,
                employeeTwo,
                operationsTeam,
                seededAt
        );
        ensureTeamManager(teamManagerRepository, manager, operationsTeam);

        StaffingRole cashierRole = findOrCreateStaffingRole(
                staffingRoleRepository,
                operationsTeam,
                "Cashier",
                "Handles customer-facing counter work."
        );
        StaffingRole shiftLeadRole = findOrCreateStaffingRole(
                staffingRoleRepository,
                operationsTeam,
                "Shift Lead",
                "Responsible for opening, closing, and shift coordination."
        );
        ensureTeamMemberRole(teamMemberStaffingRoleRepository, employeeOneMembership, cashierRole, seededAt);
        ensureTeamMemberRole(teamMemberStaffingRoleRepository, employeeTwoMembership, cashierRole, seededAt);
        ensureTeamMemberRole(teamMemberStaffingRoleRepository, employeeTwoMembership, shiftLeadRole, seededAt);

        ZoneId teamZone = ZoneId.of(operationsTeam.getTimeZone());
        LocalDate currentWeekStart = LocalDate.now(teamZone).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate currentWeekEnd = currentWeekStart.plusDays(6);
        LocalDate nextWeekStart = currentWeekStart.plusWeeks(1);
        LocalDate nextWeekEnd = nextWeekStart.plusDays(6);

        Schedule publishedSchedule = findOrCreateSchedule(
                scheduleRepository,
                operationsTeam,
                currentWeekStart,
                currentWeekEnd,
                ScheduleStatus.PUBLISHED,
                seededAt.minusSeconds(86_400)
        );
        Schedule draftSchedule = findOrCreateSchedule(
                scheduleRepository,
                operationsTeam,
                nextWeekStart,
                nextWeekEnd,
                ScheduleStatus.DRAFT,
                seededAt
        );

        Shift publishedMorningShift = findOrCreateShift(
                shiftRepository,
                publishedSchedule,
                at(teamZone, currentWeekStart.plusDays(1), 9),
                at(teamZone, currentWeekStart.plusDays(1), 17),
                "Morning counter",
                1,
                operationsTeam.getDefaultMinRestHours(),
                cashierRole
        );
        Shift publishedEveningShift = findOrCreateShift(
                shiftRepository,
                publishedSchedule,
                at(teamZone, currentWeekStart.plusDays(2), 14),
                at(teamZone, currentWeekStart.plusDays(2), 22),
                "Evening operations",
                1,
                operationsTeam.getDefaultMinRestHours(),
                shiftLeadRole
        );
        Shift transferSourceShift = findOrCreateShift(
                shiftRepository,
                publishedSchedule,
                at(teamZone, currentWeekStart.plusDays(3), 9),
                at(teamZone, currentWeekStart.plusDays(3), 17),
                "Inventory coverage",
                1,
                operationsTeam.getDefaultMinRestHours(),
                null
        );

        findOrCreateAssignment(assignmentRepository, publishedMorningShift, employeeOne, seededAt);
        findOrCreateAssignment(assignmentRepository, publishedEveningShift, employeeTwo, seededAt);
        Assignment transferSourceAssignment = findOrCreateAssignment(
                assignmentRepository,
                transferSourceShift,
                employeeOne,
                seededAt
        );

        findOrCreateShift(
                shiftRepository,
                draftSchedule,
                at(teamZone, nextWeekStart.plusDays(1), 9),
                at(teamZone, nextWeekStart.plusDays(1), 17),
                "Draft morning counter",
                1,
                operationsTeam.getDefaultMinRestHours(),
                cashierRole
        );
        Shift draftEveningShift = findOrCreateShift(
                shiftRepository,
                draftSchedule,
                at(teamZone, nextWeekStart.plusDays(2), 14),
                at(teamZone, nextWeekStart.plusDays(2), 22),
                "Draft evening lead",
                1,
                operationsTeam.getDefaultMinRestHours(),
                shiftLeadRole
        );
        findOrCreateAssignment(assignmentRepository, draftEveningShift, employeeTwo, seededAt);

        ensurePendingTransferRequest(
                swapRequestRepository,
                employeeOne,
                transferSourceAssignment,
                employeeTwo,
                seededAt.minusSeconds(3_600)
        );
        ensureSchedulePublishedNotifications(schedulePublishedNotificationService, publishedSchedule);
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
        return teamRepository.findFirstByNameOrderByIdAsc("Operations")
                .orElseGet(() -> teamRepository.save(new Team(
                        "Operations",
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
