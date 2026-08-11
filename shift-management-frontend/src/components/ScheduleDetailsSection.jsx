function ScheduleDetailsSection({
  detailsError,
  formatDate,
  formatDateTime,
  isLoadingDetails,
  selectedScheduleDetails,
  selectedScheduleId,
}) {
  return (
    <section className="section-block" id="schedule-details">
      <div className="section-heading">
        <h2>Schedule details</h2>
        <span>{selectedScheduleDetails?.shifts?.length ?? 0}</span>
      </div>

      {!selectedScheduleId ? <p className="muted">Select a published schedule to view its shifts.</p> : null}
      {isLoadingDetails ? <p className="muted">Loading schedule details...</p> : null}
      {detailsError ? <p className="error-message">{detailsError}</p> : null}

      {selectedScheduleDetails && !isLoadingDetails ? (
        <div className="details-stack">
          <div className="details-summary">
            <div>
              <p className="eyebrow">Team</p>
              <strong>{selectedScheduleDetails.schedule.teamName}</strong>
            </div>
            <div>
              <p className="eyebrow">Dates</p>
              <strong>
                {formatDate(selectedScheduleDetails.schedule.startDate)} to{" "}
                {formatDate(selectedScheduleDetails.schedule.endDate)}
              </strong>
            </div>
            <div>
              <p className="eyebrow">Publication</p>
              <strong>#{selectedScheduleDetails.schedule.publicationNumber}</strong>
            </div>
          </div>

          {selectedScheduleDetails.shifts.length === 0 ? (
            <p className="muted">This published schedule has no shifts.</p>
          ) : null}

          <div className="shift-list">
            {selectedScheduleDetails.shifts.map((shift) => (
              <article className="shift-row" key={shift.id}>
                <div className="shift-main">
                  <div>
                    <h3>{shift.description || "Shift"}</h3>
                    <p>
                      {formatDateTime(shift.startTime)} to {formatDateTime(shift.endTime)}
                    </p>
                  </div>
                  <div className="shift-meta">
                    <span>{shift.requiredWorkers} required</span>
                    {shift.requiredStaffingRoleName ? <span>{shift.requiredStaffingRoleName}</span> : null}
                  </div>
                </div>

                <div className="assignment-list">
                  {shift.assignments.length === 0 ? (
                    <p className="muted">No employees assigned yet.</p>
                  ) : (
                    shift.assignments.map((assignment) => (
                      <div className="assignment-row" key={assignment.id}>
                        <strong>{assignment.employeeFullName || assignment.employeeUsername}</strong>
                        <span>{assignment.employeeUsername}</span>
                      </div>
                    ))
                  )}
                </div>
              </article>
            ))}
          </div>
        </div>
      ) : null}
    </section>
  );
}

export default ScheduleDetailsSection;
