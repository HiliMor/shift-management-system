function ManagerActionsSection({
  assignmentCreationError,
  assignmentForm,
  assignmentShiftMap,
  assignmentShifts,
  assignmentShiftsError,
  createdAssignment,
  createdSchedule,
  createdShift,
  draftSchedulesError,
  formatDate,
  formatDateTime,
  isCreatingAssignment,
  isCreatingSchedule,
  isCreatingShift,
  isLoadingAssignmentShifts,
  isLoadingDraftSchedules,
  isLoadingManagedTeams,
  isLoadingScheduleAssignments,
  isLoadingStaffingRoles,
  isLoadingTeamEmployees,
  managedDraftSchedules,
  managedTeams,
  managedTeamsError,
  onAssignmentFormChange,
  onCreateAssignment,
  onCreateSchedule,
  onCreateShift,
  onScheduleFormChange,
  onShiftFormChange,
  scheduleAssignments,
  scheduleAssignmentsError,
  scheduleCreationError,
  scheduleForm,
  selectedAssignmentSchedule,
  shiftCreationError,
  shiftForm,
  staffingRoles,
  staffingRolesError,
  teamEmployees,
  teamEmployeesError,
}) {
  return (
    <section className="section-block" id="manager">
      <div className="section-heading">
        <h2>Manager actions</h2>
        <span>{managedTeams.length}</span>
      </div>

      {isLoadingManagedTeams ? <p className="muted">Loading managed teams...</p> : null}
      {managedTeamsError ? <p className="error-message">{managedTeamsError}</p> : null}

      {!isLoadingManagedTeams && !managedTeamsError && managedTeams.length === 0 ? (
        <p className="muted">No managed teams are available for this user.</p>
      ) : null}

      {managedTeams.length > 0 ? (
        <div className="manager-stack">
          <section className="manager-panel">
            <h3>Create draft schedule</h3>
            <form className="manager-form" onSubmit={onCreateSchedule}>
              <label>
                Team
                <select name="teamId" onChange={onScheduleFormChange} required value={scheduleForm.teamId}>
                  {managedTeams.map((team) => (
                    <option key={team.id} value={team.id}>
                      {team.name}
                    </option>
                  ))}
                </select>
              </label>

              <label>
                Start date
                <input
                  name="startDate"
                  onChange={onScheduleFormChange}
                  required
                  type="date"
                  value={scheduleForm.startDate}
                />
              </label>

              <label>
                End date
                <input
                  name="endDate"
                  onChange={onScheduleFormChange}
                  required
                  type="date"
                  value={scheduleForm.endDate}
                />
              </label>

              <button disabled={isCreatingSchedule} type="submit">
                {isCreatingSchedule ? "Creating..." : "Create draft schedule"}
              </button>
            </form>
          </section>

          <section className="manager-panel">
            <h3>Create shift</h3>

            {isLoadingDraftSchedules ? <p className="muted">Loading draft schedules...</p> : null}
            {draftSchedulesError ? <p className="error-message">{draftSchedulesError}</p> : null}
            {staffingRolesError ? <p className="error-message">{staffingRolesError}</p> : null}

            {!isLoadingDraftSchedules && !draftSchedulesError && managedDraftSchedules.length === 0 ? (
              <p className="muted">Create a draft schedule before adding shifts.</p>
            ) : null}

            {managedDraftSchedules.length > 0 ? (
              <form className="shift-form" onSubmit={onCreateShift}>
                <label>
                  Draft schedule
                  <select name="scheduleId" onChange={onShiftFormChange} required value={shiftForm.scheduleId}>
                    {managedDraftSchedules.map((schedule) => (
                      <option key={schedule.id} value={schedule.id}>
                        #{schedule.id} - {schedule.teamName}, {formatDate(schedule.startDate)} to{" "}
                        {formatDate(schedule.endDate)}
                      </option>
                    ))}
                  </select>
                </label>

                <label>
                  Start time
                  <input
                    name="startTime"
                    onChange={onShiftFormChange}
                    required
                    type="datetime-local"
                    value={shiftForm.startTime}
                  />
                </label>

                <label>
                  End time
                  <input
                    name="endTime"
                    onChange={onShiftFormChange}
                    required
                    type="datetime-local"
                    value={shiftForm.endTime}
                  />
                </label>

                <label>
                  Description
                  <input
                    maxLength="500"
                    name="description"
                    onChange={onShiftFormChange}
                    type="text"
                    value={shiftForm.description}
                  />
                </label>

                <label>
                  Required workers
                  <input
                    min="1"
                    name="requiredWorkers"
                    onChange={onShiftFormChange}
                    required
                    type="number"
                    value={shiftForm.requiredWorkers}
                  />
                </label>

                <label>
                  Minimum rest hours
                  <input
                    min="0"
                    name="minRestHours"
                    onChange={onShiftFormChange}
                    required
                    type="number"
                    value={shiftForm.minRestHours}
                  />
                </label>

                <label>
                  Required role
                  <select
                    disabled={isLoadingStaffingRoles}
                    name="requiredStaffingRoleId"
                    onChange={onShiftFormChange}
                    value={shiftForm.requiredStaffingRoleId}
                  >
                    <option value="">No specific role</option>
                    {staffingRoles.map((role) => (
                      <option key={role.id} value={role.id}>
                        {role.name}
                      </option>
                    ))}
                  </select>
                </label>

                <button disabled={isCreatingShift} type="submit">
                  {isCreatingShift ? "Creating..." : "Create shift"}
                </button>
              </form>
            ) : null}
          </section>

          <section className="manager-panel">
            <h3>Assign employee</h3>

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
        </div>
      ) : null}

      {scheduleCreationError ? <p className="error-message">{scheduleCreationError}</p> : null}
      {shiftCreationError ? <p className="error-message">{shiftCreationError}</p> : null}
      {assignmentCreationError ? <p className="error-message">{assignmentCreationError}</p> : null}

      {createdSchedule ? (
        <div className="success-message">
          <strong>Draft schedule #{createdSchedule.id} created</strong>
          <span>
            {createdSchedule.teamName}: {formatDate(createdSchedule.startDate)} to{" "}
            {formatDate(createdSchedule.endDate)}
          </span>
        </div>
      ) : null}

      {createdShift ? (
        <div className="success-message">
          <strong>Shift #{createdShift.id} created</strong>
          <span>
            {formatDateTime(createdShift.startTime)} to {formatDateTime(createdShift.endTime)}
          </span>
        </div>
      ) : null}

      {createdAssignment ? (
        <div className="success-message">
          <strong>Assignment #{createdAssignment.id} created</strong>
          <span>
            {createdAssignment.employeeFullName || createdAssignment.employeeUsername} assigned to shift #
            {createdAssignment.shiftId}
          </span>
        </div>
      ) : null}
    </section>
  );
}

export default ManagerActionsSection;
