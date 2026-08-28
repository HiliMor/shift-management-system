import { useLanguage } from "../i18n/LanguageContext.jsx";

function ScheduleDetailsSection({
  detailsError,
  formatDate,
  formatDateTime,
  isLoadingDetails,
  selectedScheduleDetails,
  selectedScheduleId,
}) {
  const { t } = useLanguage();

  return (
    <section className="section-block" id="schedule-details">
      <div className="section-heading">
        <h2>{t("scheduleDetails")}</h2>
        <span>{selectedScheduleDetails?.shifts?.length ?? 0}</span>
      </div>

      {!selectedScheduleId ? <p className="muted">{t("selectPublishedSchedule")}</p> : null}
      {isLoadingDetails ? <p className="muted">{t("loadingScheduleDetails")}</p> : null}
      {detailsError ? <p className="error-message">{detailsError}</p> : null}

      {selectedScheduleDetails && !isLoadingDetails ? (
        <div className="details-stack">
          <div className="details-summary">
            <div>
              <p className="eyebrow">{t("team")}</p>
              <strong>{selectedScheduleDetails.schedule.teamName}</strong>
            </div>
            <div>
              <p className="eyebrow">{t("dates")}</p>
              <strong>
                {formatDate(selectedScheduleDetails.schedule.startDate)} {t("dateRangeSeparator")} {" "}
                {formatDate(selectedScheduleDetails.schedule.endDate)}
              </strong>
            </div>
            <div>
              <p className="eyebrow">{t("publication")}</p>
              <strong>#{selectedScheduleDetails.schedule.publicationNumber}</strong>
            </div>
          </div>

          {selectedScheduleDetails.shifts.length === 0 ? (
            <p className="muted">{t("noShifts")}</p>
          ) : null}

          <div className="shift-list">
            {selectedScheduleDetails.shifts.map((shift) => (
              <article className="shift-row" key={shift.id}>
                <div className="shift-main">
                  <div>
                    <h3>{shift.description || t("shift")}</h3>
                    <p>
                      {formatDateTime(shift.startTime)} {t("dateRangeSeparator")} {formatDateTime(shift.endTime)}
                    </p>
                  </div>
                  <div className="shift-meta">
                    <span>
                      {shift.requiredWorkers} {t("required")}
                    </span>
                    {shift.requiredStaffingRoleName ? <span>{shift.requiredStaffingRoleName}</span> : null}
                  </div>
                </div>

                <div className="assignment-list">
                  {shift.assignments.length === 0 ? (
                    <p className="muted">{t("noEmployeesAssigned")}</p>
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
