function AssignEmployeePanel({
  assignmentForm,
  assignmentShiftMap,
  assignmentShifts,
  assignmentShiftsError,
  draftSchedulesError,
  formatDate,
  formatDateTime,
  isCreatingAssignment,
  isLoadingAssignmentShifts,
  isLoadingDraftSchedules,
  isLoadingScheduleAssignments,
  isLoadingTeamEmployees,
  managedDraftSchedules,
  onAssignmentFormChange,
  onCreateAssignment,
  scheduleAssignments,
  scheduleAssignmentsError,
  selectedAssignmentSchedule,
  teamEmployees,
  teamEmployeesError,
}) {
  return (
    <section className="manager-panel" id="manager-assignments">
      <div className="manager-panel-heading">
        <span>3</span>
        <h3>Manual assignment</h3>
      </div>

      {assignmentShiftsError ? <p className="error-message">{assignmentShiftsError}</p> : null}
      {teamEmployeesError ? <p className="error-message">{teamEmployeesError}</p> : null}
      {scheduleAssignmentsError ? <p className="error-message">{scheduleAssignmentsError}</p> : null}

      {!isLoadingDraftSchedules && !draftSchedulesError && managedDraftSchedules.length === 0 ? (
        <p className="muted">Create a draft schedule before assigning employees.</p>
      ) : null}

      {managedDraftSchedules.length > 0 ? (
        <form className="assignment-form" onSubmit={onCreateAssignment}>
          <label>
            Draft schedule
            <select
              name="scheduleId"
              onChange={onAssignmentFormChange}
              required
              value={assignmentForm.scheduleId}
            >
              {managedDraftSchedules.map((schedule) => (
                <option key={schedule.id} value={schedule.id}>
                  #{schedule.id} - {schedule.teamName}, {formatDate(schedule.startDate)} to{" "}
                  {formatDate(schedule.endDate)}
                </option>
              ))}
            </select>
          </label>

          <label>
            Shift
            <select
              disabled={isLoadingAssignmentShifts || assignmentShifts.length === 0}
              name="shiftId"
              onChange={onAssignmentFormChange}
              required
              value={assignmentForm.shiftId}
            >
              {assignmentShifts.map((shift) => (
                <option key={shift.id} value={shift.id}>
                  #{shift.id} - {shift.description || "Shift"}, {formatDateTime(shift.startTime)}
                </option>
              ))}
            </select>
          </label>

          <label>
            Employee
            <select
              disabled={isLoadingTeamEmployees || teamEmployees.length === 0}
              name="employeeId"
              onChange={onAssignmentFormChange}
              required
              value={assignmentForm.employeeId}
            >
              {teamEmployees.map((employee) => (
                <option key={employee.id} value={employee.id}>
                  {employee.fullName || employee.username}
                </option>
              ))}
            </select>
          </label>

          <button
            disabled={
              isCreatingAssignment ||
              isLoadingAssignmentShifts ||
              isLoadingTeamEmployees ||
              assignmentShifts.length === 0 ||
              teamEmployees.length === 0
            }
            type="submit"
          >
            {isCreatingAssignment ? "Assigning..." : "Assign employee"}
          </button>
        </form>
      ) : null}

      {isLoadingAssignmentShifts ? <p className="muted">Loading shifts...</p> : null}
      {isLoadingTeamEmployees ? <p className="muted">Loading employees...</p> : null}

      {!isLoadingAssignmentShifts &&
      !assignmentShiftsError &&
      managedDraftSchedules.length > 0 &&
      assignmentShifts.length === 0 ? (
        <p className="muted">Add a shift before assigning employees.</p>
      ) : null}

      {!isLoadingTeamEmployees && !teamEmployeesError && selectedAssignmentSchedule && teamEmployees.length === 0 ? (
        <p className="muted">No active employees are available for this team.</p>
      ) : null}

      <div className="assignment-panel-list">
        <h4>Current assignments</h4>
        {isLoadingScheduleAssignments ? <p className="muted">Loading assignments...</p> : null}

        {!isLoadingScheduleAssignments &&
        !scheduleAssignmentsError &&
        assignmentForm.scheduleId &&
        scheduleAssignments.length === 0 ? (
          <p className="muted">No employees assigned in this draft schedule yet.</p>
        ) : null}

        {scheduleAssignments.map((assignment) => {
          const assignedShift = assignmentShiftMap.get(assignment.shiftId);

          return (
            <div className="assignment-row" key={assignment.id}>
              <strong>{assignment.employeeFullName || assignment.employeeUsername}</strong>
              <span>
                {assignedShift
                  ? `#${assignedShift.id} - ${assignedShift.description || "Shift"}, ${formatDateTime(
                      assignedShift.startTime,
                    )}`
                  : `Shift #${assignment.shiftId}`}
              </span>
            </div>
          );
        })}
      </div>
    </section>
  );
}

export default AssignEmployeePanel;
