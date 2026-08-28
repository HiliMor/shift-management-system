import { useLanguage } from "../i18n/LanguageContext.jsx";

const statusTranslationKeys = {
  PUBLISHED: "statusPublished",
};

function PublishedSchedulesSection({
  formatDate,
  isLoadingSchedules,
  onSelectSchedule,
  publishedSchedules,
  scheduleError,
  selectedScheduleId,
}) {
  const { t } = useLanguage();

  function statusLabel(status) {
    return t(statusTranslationKeys[status] ?? status);
  }

  return (
    <section className="section-block" id="schedules">
      <div className="section-heading">
        <h2>{t("publishedSchedules")}</h2>
        <span>{publishedSchedules.length}</span>
      </div>

      {isLoadingSchedules ? <p className="muted">{t("loadingSchedules")}</p> : null}
      {scheduleError ? <p className="error-message">{scheduleError}</p> : null}

      {!isLoadingSchedules && !scheduleError && publishedSchedules.length === 0 ? (
        <p className="muted">{t("noPublishedSchedules")}</p>
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
            <span>{statusLabel(schedule.status)}</span>
            <button
              className="secondary-button compact-button"
              onClick={() => onSelectSchedule(schedule.id)}
              type="button"
            >
              {t("viewDetails")}
            </button>
          </article>
        ))}
      </div>
    </section>
  );
}

export default PublishedSchedulesSection;
