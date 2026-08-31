package com.hilimor.shiftmanagement.schedule;

import java.util.Comparator;
import java.util.List;

import com.hilimor.shiftmanagement.assignment.Assignment;
import com.hilimor.shiftmanagement.shift.Shift;
import com.hilimor.shiftmanagement.team.Team;
import com.hilimor.shiftmanagement.template.ShiftTemplate;
import com.hilimor.shiftmanagement.user.User;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.LockModeType;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Component
@Transactional(propagation = Propagation.MANDATORY)
public class ScheduleWriteLock {

    private final EntityManager entityManager;

    public ScheduleWriteLock(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public void lockTeam(Team team) {
        // Match request execution: team first, then shifts, then employees. Hold until commit.
        refresh(team, LockModeType.PESSIMISTIC_WRITE, "Team not found");
    }

    public void lockTemplate(ShiftTemplate template) {
        lockTeam(template.getTeam());
        refresh(template, LockModeType.NONE, "Template not found");
    }

    public void lockSchedule(Schedule schedule) {
        lockTeam(schedule.getTeam());
        refresh(schedule, LockModeType.NONE, "Schedule not found");
    }

    public void lockShift(Shift shift) {
        lockSchedule(shift.getSchedule());
        refresh(shift, LockModeType.PESSIMISTIC_WRITE, "Shift not found");
    }

    public void lockAssignment(Assignment assignment) {
        lockShift(assignment.getShift());
        refresh(assignment, LockModeType.NONE, "Assignment not found");
    }

    public void lockAssignedEmployees(List<Assignment> assignments) {
        assignments.stream()
                .map(Assignment::getEmployee)
                .distinct()
                .sorted(Comparator.comparing(User::getId))
                .forEach(employee -> entityManager.lock(employee, LockModeType.PESSIMISTIC_WRITE));
    }

    private void refresh(Object entity, LockModeType mode, String missingMessage) {
        try {
            // Replace any state read before waiting, including an old draft status or owner.
            entityManager.refresh(entity, mode);
        } catch (EntityNotFoundException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, missingMessage);
        }
    }
}
