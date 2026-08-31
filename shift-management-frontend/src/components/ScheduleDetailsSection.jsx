import { useEffect, useState } from "react";
import { useLanguage } from "../i18n/LanguageContext.jsx";
import WeeklyScheduleCalendar from "./WeeklyScheduleCalendar.jsx";
import MonthlyScheduleCalendar from "./MonthlyScheduleCalendar.jsx";
import { groupShiftsByDate } from "../utils/shiftGroups.js";
import { personalShifts } from "../utils/calendarDates.js";

function ScheduleDetailsSection({
  currentUserId,
  detailsError,
  formatDate,
  formatDateTime,
  isLoadingDetails,
  selectedScheduleDetails,
  selectedScheduleId,
}) {
  const { t } = useLanguage();
  const [viewMode, setViewMode] = useState("calendar");
  const [onlyMine, setOnlyMine] = useState(false);
  const hasCurrentDetails = selectedScheduleDetails && String(selectedScheduleDetails.schedule.id) === String(selectedScheduleId);
  const allShifts = hasCurrentDetails ? selectedScheduleDetails.shifts : [];
  const visibleShifts = onlyMine && currentUserId != null ? personalShifts(allShifts, currentUserId) : allShifts;
  const shiftGroups = groupShiftsByDate(visibleShifts, formatDate);
  const viewLabels = { calendar: "weeklyCalendar", month: "monthlyCalendar", list: "listView" };

  useEffect(() => {
    setViewMode("calendar");
    setOnlyMine(false);
  }, [selectedScheduleId, currentUserId]);

  return (
    <section className="section-block" id="schedule-details">
      <div className="section-heading">
        <h2>{t("scheduleDetails")}</h2>
        <span>{visibleShifts.length}</span>
      </div>

      {!selectedScheduleId ? <p className="muted">{t("selectPublishedSchedule")}</p> : null}
      {isLoadingDetails ? <p className="muted">{t("loadingScheduleDetails")}</p> : null}
      {detailsError ? <p className="error-message">{detailsError}</p> : null}

      {hasCurrentDetails && !isLoadingDetails ? (
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

          {visibleShifts.length === 0 ? (
            <p className="muted" role="status">{t(onlyMine ? "noPersonalShifts" : "noShifts")}</p>
          ) : null}

          <div className="details-view-toolbar">
            <div>
              <p className="eyebrow">{t("shiftDisplay")}</p>
              <strong>{t(viewLabels[viewMode])}</strong>
            </div>
            {currentUserId != null ? (
              <label className="personal-shifts-filter">
                <input type="checkbox" checked={onlyMine} onChange={(event) => setOnlyMine(event.target.checked)} />
                {t("onlyMyShifts")}
              </label>
            ) : null}
            <div className="view-toggle published-view-toggle" role="group" aria-label={t("shiftDisplay")}>
              {Object.entries(viewLabels).map(([mode, label]) => (
                <button key={mode} aria-pressed={viewMode === mode}
                  className={viewMode === mode ? "active-view-button" : "secondary-button"}
                  onClick={() => setViewMode(mode)} type="button">{t(label)}</button>
              ))}
            </div>
          </div>

          {viewMode === "calendar" ? (
            <WeeklyScheduleCalendar schedule={selectedScheduleDetails.schedule} shifts={visibleShifts} />
          ) : viewMode === "month" ? (
            <MonthlyScheduleCalendar key={`${selectedScheduleId}-${selectedScheduleDetails.schedule.startDate}-${selectedScheduleDetails.schedule.endDate}`}
              schedule={selectedScheduleDetails.schedule} shifts={visibleShifts} />
          ) : (
            <div className="shift-day-list">
              {shiftGroups.map((group) => (
                <section className="shift-day-group" key={group.dateLabel}>
                  <div className="shift-day-heading">
                    <h3>{group.dateLabel}</h3>
                    <span>{group.shifts.length} {t("shifts")}</span>
                  </div>
                  <div className="shift-list">
                    {group.shifts.map((shift) => (
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
                </section>
              ))}
            </div>
          )}
        </div>
      ) : null}
    </section>
  );
}

export default ScheduleDetailsSection;
