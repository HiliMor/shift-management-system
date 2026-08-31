package com.hilimor.shiftmanagement.request;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import com.hilimor.shiftmanagement.assignment.Assignment;
import com.hilimor.shiftmanagement.shift.Shift;
import com.hilimor.shiftmanagement.user.User;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional(propagation = Propagation.MANDATORY)
public class SwapRequestLock {

    private final EntityManager entityManager;

    public SwapRequestLock(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public void lockSource(Assignment sourceAssignment) {
        // One team lock covers requests referencing assignments as either source or target.
        entityManager.refresh(sourceAssignment.getShift().getSchedule().getTeam(), LockModeType.PESSIMISTIC_WRITE);
        entityManager.refresh(sourceAssignment);
        entityManager.refresh(sourceAssignment.getShift().getSchedule());
    }

    public void lockRequest(SwapRequest request) {
        lockSource(request.getSourceAssignment());
        // Entities loaded before waiting must not retain an earlier status or owner.
        entityManager.refresh(request);
        if (request.getTargetAssignment() != null) {
            entityManager.refresh(request.getTargetAssignment());
        }
    }

    public void lockExecution(SwapRequest request) {
        entityManager.lock(request.getSourceAssignment().getShift().getSchedule().getTeam(),
                LockModeType.PESSIMISTIC_WRITE);

        List<Assignment> assignments = Stream.of(request.getSourceAssignment(), request.getTargetAssignment())
                .filter(Objects::nonNull)
                .toList();

        // Match manual/automatic assignment ordering: all shifts, then all employees, by ID.
        assignments.stream()
                .map(Assignment::getShift)
                .distinct()
                .sorted(Comparator.comparing(Shift::getId))
                .forEach(shift -> entityManager.refresh(shift, LockModeType.PESSIMISTIC_WRITE));
        Stream.of(request.getRequester(), request.getTargetEmployee())
                .distinct()
                .sorted(Comparator.comparing(User::getId))
                .forEach(user -> entityManager.lock(user, LockModeType.PESSIMISTIC_WRITE));

        assignments.forEach(entityManager::refresh);
        assignments.stream()
                .map(assignment -> assignment.getShift().getSchedule())
                .distinct()
                .forEach(entityManager::refresh);
    }
}
