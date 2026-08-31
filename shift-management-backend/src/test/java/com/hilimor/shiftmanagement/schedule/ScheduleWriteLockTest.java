package com.hilimor.shiftmanagement.schedule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import com.hilimor.shiftmanagement.assignment.Assignment;
import com.hilimor.shiftmanagement.shift.Shift;
import com.hilimor.shiftmanagement.team.Team;
import com.hilimor.shiftmanagement.user.User;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.LockModeType;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class ScheduleWriteLockTest {

    @Mock private EntityManager entityManager;
    @Mock private Schedule schedule;
    @Mock private Team team;
    @InjectMocks private ScheduleWriteLock writeLock;

    @Test
    void locksTeamBeforeReloadingScheduleStatus() {
        when(schedule.getTeam()).thenReturn(team);

        writeLock.lockSchedule(schedule);

        var order = inOrder(entityManager);
        order.verify(entityManager).refresh(team, LockModeType.PESSIMISTIC_WRITE);
        order.verify(entityManager).refresh(schedule, LockModeType.NONE);
    }

    @Test
    void locksTeamThenShiftBeforeReloadingAssignmentOwner() {
        Shift shift = mock(Shift.class);
        Assignment assignment = mock(Assignment.class);
        when(assignment.getShift()).thenReturn(shift);
        when(shift.getSchedule()).thenReturn(schedule);
        when(schedule.getTeam()).thenReturn(team);

        writeLock.lockAssignment(assignment);

        var order = inOrder(entityManager);
        order.verify(entityManager).refresh(team, LockModeType.PESSIMISTIC_WRITE);
        order.verify(entityManager).refresh(schedule, LockModeType.NONE);
        order.verify(entityManager).refresh(shift, LockModeType.PESSIMISTIC_WRITE);
        order.verify(entityManager).refresh(assignment, LockModeType.NONE);
    }

    @Test
    void locksEachAssignedEmployeeOnceInIdOrder() {
        User first = mock(User.class);
        User second = mock(User.class);
        Assignment firstAssignment = mock(Assignment.class);
        Assignment secondAssignment = mock(Assignment.class);
        when(first.getId()).thenReturn(1L);
        when(second.getId()).thenReturn(2L);
        when(firstAssignment.getEmployee()).thenReturn(first);
        when(secondAssignment.getEmployee()).thenReturn(second);

        writeLock.lockAssignedEmployees(List.of(secondAssignment, firstAssignment, secondAssignment));

        var order = inOrder(entityManager);
        order.verify(entityManager).lock(first, LockModeType.PESSIMISTIC_WRITE);
        order.verify(entityManager).lock(second, LockModeType.PESSIMISTIC_WRITE);
        verifyNoMoreInteractions(entityManager);
    }

    @Test
    void reportsScheduleDeletedWhileWaitingAsNotFound() {
        when(schedule.getTeam()).thenReturn(team);
        doNothing().when(entityManager).refresh(team, LockModeType.PESSIMISTIC_WRITE);
        doThrow(new EntityNotFoundException()).when(entityManager).refresh(schedule, LockModeType.NONE);

        assertThatThrownBy(() -> writeLock.lockSchedule(schedule))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
                    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(exception.getReason()).isEqualTo("Schedule not found");
                });
    }
}
