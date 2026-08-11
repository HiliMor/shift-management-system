function PublishedSchedulesSection({
  formatDate,
  isLoadingSchedules,
  onSelectSchedule,
  publishedSchedules,
  scheduleError,
  selectedScheduleId,
}) {
  return (
    <section className="section-block" id="schedules">
      <div className="section-heading">
        <h2>Published schedules</h2>
        <span>{publishedSchedules.length}</span>
      </div>

      {isLoadingSchedules ? <p className="muted">Loading schedules...</p> : null}
      {scheduleError ? <p className="error-message">{scheduleError}</p> : null}

      {!isLoadingSchedules && !scheduleError && publishedSchedules.length === 0 ? (
        <p className="muted">No published schedules are available for this user.</p>
      ) : null}

      <div className="schedule-list">
        {publishedSchedules.map((schedule) => (
          <article
            className={schedule.id === selectedScheduleId ? "schedule-row selected-row" : "schedule-row"}
            key={schedule.id}
          >
            <div>
              <h3>{schedule.teamName}</h3>
              <p>
                {formatDate(schedule.startDate)} to {formatDate(schedule.endDate)}
              </p>
            </div>
            <span>{schedule.status}</span>
            <button
              className="secondary-button compact-button"
              onClick={() => onSelectSchedule(schedule.id)}
              type="button"
            >
              View details
            </button>
          </article>
        ))}
      </div>
    </section>
  );
}

export default PublishedSchedulesSection;
