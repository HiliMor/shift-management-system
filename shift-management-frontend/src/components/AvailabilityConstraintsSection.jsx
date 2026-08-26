function AvailabilityConstraintsSection({
  availabilityActionError,
  availabilityActionMessage,
  availabilityConstraints,
  availabilityError,
  availabilityForm,
  deletingAvailabilityId,
  formatDateTime,
  isCreatingAvailability,
  isLoadingAvailability,
  onAvailabilityFormChange,
  onCreateAvailabilityConstraint,
  onDeleteAvailabilityConstraint,
  onRefreshAvailabilityConstraints,
}) {
  return (
    <section className="section-block" id="availability">
      <div className="section-heading">
        <h2>My availability</h2>
        <div className="section-actions">
          <span>{availabilityConstraints.length}</span>
          <button
            className="secondary-button compact-button"
            disabled={isLoadingAvailability}
            onClick={onRefreshAvailabilityConstraints}
            type="button"
          >
            Refresh
          </button>
        </div>
      </div>

      <form className="availability-form" onSubmit={onCreateAvailabilityConstraint}>
        <label className="checkbox-field">
          <input
            checked={availabilityForm.allDay}
            name="allDay"
            onChange={onAvailabilityFormChange}
            type="checkbox"
          />
          Full day
        </label>

        {availabilityForm.allDay ? (
          <label>
            Date
            <input
              name="date"
              onChange={onAvailabilityFormChange}
              required
              type="date"
              value={availabilityForm.date}
            />
          </label>
        ) : (
          <>
            <label>
              Start time
              <input
                name="startTime"
                onChange={onAvailabilityFormChange}
                required
                type="datetime-local"
                value={availabilityForm.startTime}
              />
            </label>

            <label>
              End time
              <input
                name="endTime"
                onChange={onAvailabilityFormChange}
                required
                type="datetime-local"
                value={availabilityForm.endTime}
              />
            </label>
          </>
        )}

        <label>
          Reason
          <input
            maxLength="500"
            name="reason"
            onChange={onAvailabilityFormChange}
            placeholder="Optional"
            type="text"
            value={availabilityForm.reason}
          />
        </label>

        <button disabled={isCreatingAvailability} type="submit">
          {isCreatingAvailability ? "Saving..." : "Add constraint"}
        </button>
      </form>

      {availabilityActionError ? <p className="error-message">{availabilityActionError}</p> : null}
      {availabilityActionMessage ? <p className="success-message">{availabilityActionMessage}</p> : null}
      {isLoadingAvailability ? <p className="muted">Loading availability constraints...</p> : null}
      {availabilityError ? <p className="error-message">{availabilityError}</p> : null}

      {!isLoadingAvailability && !availabilityError && availabilityConstraints.length === 0 ? (
        <p className="muted">No availability constraints were submitted yet.</p>
      ) : null}

      {availabilityConstraints.length > 0 ? (
        <div className="availability-list">
          {availabilityConstraints.map((constraint) => (
            <article className="availability-row" key={constraint.id}>
              <div>
                <h3>
                  {formatDateTime(constraint.startTime)} to {formatDateTime(constraint.endTime)}
                </h3>
                <p>{constraint.reason || "No reason provided"}</p>
                <p className="notification-meta">Created {formatDateTime(constraint.createdAt)}</p>
              </div>

              <button
                className="secondary-button compact-button"
                disabled={deletingAvailabilityId !== null}
                onClick={() => onDeleteAvailabilityConstraint(constraint.id)}
                type="button"
              >
                {deletingAvailabilityId === constraint.id ? "Deleting..." : "Delete"}
              </button>
            </article>
          ))}
        </div>
      ) : null}
    </section>
  );
}

export default AvailabilityConstraintsSection;
