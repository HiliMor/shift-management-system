import { useLanguage } from "../../i18n/LanguageContext.jsx";

function renderScheduleOption(schedule, formatDate, t) {
  return `#${schedule.id} - ${schedule.teamName}, ${formatDate(schedule.startDate)} ${t("dateRangeSeparator")} ${formatDate(
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
  const { t } = useLanguage();

  return (
    <section className="manager-panel" id="manager-publication">
      <div className="manager-panel-heading">
        <span>4</span>
        <h3>{t("publishOrReopen")}</h3>
      </div>

      {draftSchedulesError ? <p className="error-message">{draftSchedulesError}</p> : null}
      {publicationError ? <p className="error-message">{publicationError}</p> : null}

      {!isLoadingDraftSchedules && !draftSchedulesError && managedDraftSchedules.length === 0 ? (
        <p className="muted">{t("noDraftSchedulesForPublication")}</p>
      ) : null}

      {managedDraftSchedules.length > 0 ? (
        <form className="publication-form" onSubmit={onPublishSchedule}>
          <label className="checkbox-field">
            <input
              checked={publicationForm.confirmUnfilled}
              name="confirmUnfilled"
              onChange={onPublicationFormChange}
              type="checkbox"
            />
            {t("publishWithUnfilled")}
          </label>

          <button
            className="secondary-button"
            disabled={isLoadingPublicationReadiness}
            onClick={onRefreshPublicationReadiness}
            type="button"
          >
            {isLoadingPublicationReadiness ? t("checking") : t("checkReadiness")}
          </button>

          <button disabled={isPublishingSchedule} type="submit">
            {isPublishingSchedule ? t("publishing") : t("publishSchedule")}
          </button>
        </form>
      ) : null}

      {isLoadingPublicationReadiness ? <p className="muted" aria-live="polite">{t("checkingReadinessDetails")}</p> : null}

      {publicationReadiness ? (
        <div className="readiness-panel">
          <div className="readiness-summary">
            <span className={publicationReadiness.readyToPublish ? "ready-badge" : "warning-badge"}>
              {publicationReadiness.readyToPublish ? t("ready") : t("needsConfirmation")}
            </span>
            <span>{publicationReadiness.totalShifts} {t("shifts")}</span>
            <span>
              {publicationReadiness.totalAssignedWorkers}/{publicationReadiness.totalRequiredWorkers} {t("workers")}
            </span>
            <span>{publicationReadiness.totalOpenSlots} {t("openSlots")}</span>
          </div>

          {publicationReadiness.unfilledShifts.length > 0 ? (
            <div className="unfilled-shift-list">
              <h4>{t("unfilledShifts")}</h4>
              {publicationReadiness.unfilledShifts.map((shift) => (
                <div className="assignment-row" key={shift.shiftId}>
                  <strong>{t("shift")} #{shift.shiftId}</strong>
                  <span>
                    {shift.description || t("shift")}, {formatDateTime(shift.startTime)} {t("dateRangeSeparator")} {" "}
                    {formatDateTime(shift.endTime)} - {shift.openSlots} {t("openSlots")}
                  </span>
                </div>
              ))}
            </div>
          ) : null}
        </div>
      ) : null}

      <div className="assignment-panel-list">
        <div className="section-heading compact-heading">
          <h4>{t("publishedSchedules")}</h4>
          <button
            className="secondary-button compact-button"
            disabled={isLoadingManagedPublishedSchedules}
            onClick={onRefreshPublishedSchedules}
            type="button"
          >
            {t("refresh")}
          </button>
        </div>

        {isLoadingManagedPublishedSchedules ? <p className="muted">{t("loadingPublishedSchedules")}</p> : null}
        {managedPublishedSchedulesError ? <p className="error-message">{managedPublishedSchedulesError}</p> : null}

        {!isLoadingManagedPublishedSchedules &&
        !managedPublishedSchedulesError &&
        managedPublishedSchedules.length === 0 ? (
          <p className="muted">{t("noPublishedSchedulesToReopen")}</p>
        ) : null}

        {managedPublishedSchedules.map((schedule) => (
          <div className="assignment-row" key={schedule.id}>
            <strong>{renderScheduleOption(schedule, formatDate, t)}</strong>
            <span>
              {t("published")} #{schedule.publicationNumber}
              {schedule.publishedAt ? ` ${t("onDate")} ${formatDateTime(schedule.publishedAt)}` : ""}
            </span>
            <button
              className="secondary-button compact-button"
              disabled={reopeningScheduleId !== null}
              onClick={() => onReopenSchedule(schedule.id)}
              type="button"
            >
              {reopeningScheduleId === schedule.id ? t("reopening") : t("reopen")}
            </button>
          </div>
        ))}
      </div>
    </section>
  );
}

export default SchedulePublicationPanel;
