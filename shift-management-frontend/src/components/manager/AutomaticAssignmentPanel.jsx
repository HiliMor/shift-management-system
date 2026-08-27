function renderScheduleOption(schedule, formatDate) {
  return `#${schedule.id} - ${schedule.teamName}, ${formatDate(schedule.startDate)} to ${formatDate(
    schedule.endDate,
  )}`;
}

function renderAssignedEmployees(assignments) {
  if (assignments.length === 0) {
    return <span>No new assignments</span>;
  }

  return assignments.map((assignment) => (
    <span key={assignment.id}>{assignment.employeeFullName || assignment.employeeUsername}</span>
  ));
}

function AutomaticAssignmentPanel({
  automaticAssignmentError,
  automaticAssignmentForm,
  automaticAssignmentMessage,
  automaticAssignmentReport,
  draftSchedulesError,
  formatDate,
  formatDateTime,
  isLoadingDraftSchedules,
  isRunningAutomaticAssignment,
  managedDraftSchedules,
  onAutomaticAssignmentFormChange,
  onRunAutomaticAssignment,
}) {
  return (
    <section className="manager-panel">
      <h3>Automatic assignment</h3>

      {draftSchedulesError ? <p className="error-message">{draftSchedulesError}</p> : null}
      {automaticAssignmentError ? <p className="error-message">{automaticAssignmentError}</p> : null}

      {!isLoadingDraftSchedules && !draftSchedulesError && managedDraftSchedules.length === 0 ? (
        <p className="muted">Create a draft schedule before running automatic assignment.</p>
      ) : null}

      {managedDraftSchedules.length > 0 ? (
        <form className="automatic-assignment-form" onSubmit={onRunAutomaticAssignment}>
          <label>
            Draft schedule
            <select
              name="scheduleId"
              onChange={onAutomaticAssignmentFormChange}
              required
              value={automaticAssignmentForm.scheduleId}
            >
              {managedDraftSchedules.map((schedule) => (
                <option key={schedule.id} value={schedule.id}>
                  {renderScheduleOption(schedule, formatDate)}
                </option>
              ))}
            </select>
          </label>

          <button
            disabled={
              isRunningAutomaticAssignment || isLoadingDraftSchedules || !automaticAssignmentForm.scheduleId
            }
            type="submit"
          >
            {isRunningAutomaticAssignment ? "Assigning..." : "Run auto assignment"}
          </button>
        </form>
      ) : null}

      {automaticAssignmentMessage ? (
        <div className="success-message">
          <strong>{automaticAssignmentMessage}</strong>
        </div>
      ) : null}

      {automaticAssignmentReport ? (
        <div className="readiness-panel">
          <div className="readiness-summary">
            <span>{automaticAssignmentReport.totalShifts} shifts</span>
            <span>{automaticAssignmentReport.assignmentsCreated} assignments created</span>
            <span>{automaticAssignmentReport.totalOpenSlotsBefore} open before</span>
            <span
              className={
                automaticAssignmentReport.totalOpenSlotsAfter === 0 && automaticAssignmentReport.totalShifts > 0
                  ? "ready-badge"
                  : "warning-badge"
              }
            >
              {automaticAssignmentReport.totalOpenSlotsAfter} open after
            </span>
          </div>

          {automaticAssignmentReport.shifts.length > 0 ? (
            <div className="auto-assignment-report-list">
              {automaticAssignmentReport.shifts.map((shift) => (
                <div className="assignment-row auto-assignment-row" key={shift.shiftId}>
                  <div className="auto-assignment-main">
                    <strong>Shift #{shift.shiftId}</strong>
                    <span>
                      {shift.description || "Shift"}, {formatDateTime(shift.startTime)} to{" "}
                      {formatDateTime(shift.endTime)}
                    </span>
                    <span>{shift.message}</span>
                  </div>

                  <div className="auto-assignment-created-list">
                    <strong>
                      {shift.assignmentsCreated}/{shift.openSlotsBefore} created
                    </strong>
                    {renderAssignedEmployees(shift.createdAssignments)}
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <p className="muted">No shifts are available in this draft schedule.</p>
          )}
        </div>
      ) : null}
    </section>
  );
}

export default AutomaticAssignmentPanel;
