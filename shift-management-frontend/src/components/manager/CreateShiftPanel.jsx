function CreateShiftPanel({
  draftSchedulesError,
  formatDate,
  isCreatingShift,
  isLoadingDraftSchedules,
  isLoadingStaffingRoles,
  managedDraftSchedules,
  onCreateShift,
  onShiftFormChange,
  shiftForm,
  staffingRoles,
  staffingRolesError,
}) {
  return (
    <section className="manager-panel" id="manager-shifts">
      <div className="manager-panel-heading">
        <span>2</span>
        <h3>Manual shifts</h3>
      </div>

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
  );
}

export default CreateShiftPanel;
