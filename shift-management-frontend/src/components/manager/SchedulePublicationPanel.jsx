function renderScheduleOption(schedule, formatDate) {
  return `#${schedule.id} - ${schedule.teamName}, ${formatDate(schedule.startDate)} to ${formatDate(
    schedule.endDate,
  )}`;
}

function SchedulePublicationPanel({
  draftSchedulesError,
  formatDate,
  formatDateTime,
  isLoadingDraftSchedules,
  isLoadingManagedPublishedSchedules,
  isLoadingPublicationReadiness,
  isPublishingSchedule,
  managedDraftSchedules,
  managedPublishedSchedules,
  managedPublishedSchedulesError,
  onPublicationFormChange,
  onPublishSchedule,
  onRefreshPublishedSchedules,
  onRefreshPublicationReadiness,
  onReopenSchedule,
  publicationForm,
  publicationReadiness,
  publicationError,
  reopeningScheduleId,
}) {
  return (
    <section className="manager-panel">
      <h3>Publish schedules</h3>

      {draftSchedulesError ? <p className="error-message">{draftSchedulesError}</p> : null}
      {publicationError ? <p className="error-message">{publicationError}</p> : null}

      {!isLoadingDraftSchedules && !draftSchedulesError && managedDraftSchedules.length === 0 ? (
        <p className="muted">No draft schedules are available for publication.</p>
      ) : null}

      {managedDraftSchedules.length > 0 ? (
        <form className="publication-form" onSubmit={onPublishSchedule}>
          <label>
            Draft schedule
            <select
              name="scheduleId"
              onChange={onPublicationFormChange}
              required
              value={publicationForm.scheduleId}
            >
              {managedDraftSchedules.map((schedule) => (
                <option key={schedule.id} value={schedule.id}>
                  {renderScheduleOption(schedule, formatDate)}
                </option>
              ))}
            </select>
          </label>

          <label className="checkbox-field">
            <input
              checked={publicationForm.confirmUnfilled}
              name="confirmUnfilled"
              onChange={onPublicationFormChange}
              type="checkbox"
            />
            Publish with unfilled shifts
          </label>

          <button
            className="secondary-button"
            disabled={isLoadingPublicationReadiness}
            onClick={onRefreshPublicationReadiness}
            type="button"
          >
            {isLoadingPublicationReadiness ? "Checking..." : "Check readiness"}
          </button>

          <button disabled={isPublishingSchedule} type="submit">
            {isPublishingSchedule ? "Publishing..." : "Publish schedule"}
          </button>
        </form>
      ) : null}

      {publicationReadiness ? (
        <div className="readiness-panel">
          <div className="readiness-summary">
            <span className={publicationReadiness.readyToPublish ? "ready-badge" : "warning-badge"}>
              {publicationReadiness.readyToPublish ? "Ready" : "Needs confirmation"}
            </span>
            <span>{publicationReadiness.totalShifts} shifts</span>
            <span>
              {publicationReadiness.totalAssignedWorkers}/{publicationReadiness.totalRequiredWorkers} workers
            </span>
            <span>{publicationReadiness.totalOpenSlots} open slots</span>
          </div>

          {publicationReadiness.unfilledShifts.length > 0 ? (
            <div className="unfilled-shift-list">
              <h4>Unfilled shifts</h4>
              {publicationReadiness.unfilledShifts.map((shift) => (
                <div className="assignment-row" key={shift.shiftId}>
                  <strong>Shift #{shift.shiftId}</strong>
                  <span>
                    {shift.description || "Shift"}, {formatDateTime(shift.startTime)} to{" "}
                    {formatDateTime(shift.endTime)} - {shift.openSlots} open
                  </span>
                </div>
              ))}
            </div>
          ) : null}
        </div>
      ) : null}

      <div className="assignment-panel-list">
        <div className="section-heading compact-heading">
          <h4>Published schedules</h4>
          <button
            className="secondary-button compact-button"
            disabled={isLoadingManagedPublishedSchedules}
            onClick={onRefreshPublishedSchedules}
            type="button"
          >
            Refresh
          </button>
        </div>

        {isLoadingManagedPublishedSchedules ? <p className="muted">Loading published schedules...</p> : null}
        {managedPublishedSchedulesError ? <p className="error-message">{managedPublishedSchedulesError}</p> : null}

        {!isLoadingManagedPublishedSchedules &&
        !managedPublishedSchedulesError &&
        managedPublishedSchedules.length === 0 ? (
          <p className="muted">No published schedules are available to reopen.</p>
        ) : null}

        {managedPublishedSchedules.map((schedule) => (
          <div className="assignment-row" key={schedule.id}>
            <strong>{renderScheduleOption(schedule, formatDate)}</strong>
            <span>
              Published #{schedule.publicationNumber}
              {schedule.publishedAt ? ` on ${formatDateTime(schedule.publishedAt)}` : ""}
            </span>
            <button
              className="secondary-button compact-button"
              disabled={reopeningScheduleId !== null}
              onClick={() => onReopenSchedule(schedule.id)}
              type="button"
            >
              {reopeningScheduleId === schedule.id ? "Reopening..." : "Reopen"}
            </button>
          </div>
        ))}
      </div>
    </section>
  );
}

export default SchedulePublicationPanel;
