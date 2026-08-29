import { useLanguage } from "../../i18n/LanguageContext.jsx";

function renderAssignedEmployees(assignments, t) {
  if (assignments.length === 0) {
    return <span>{t("noNewAssignments")}</span>;
  }

  return assignments.map((assignment) => (
    <span key={assignment.id}>{assignment.employeeFullName || assignment.employeeUsername}</span>
  ));
}

function automaticAssignmentMessageLabel(message, t) {
  if (!message) {
    return "";
  }

  if (typeof message === "string") {
    return t(message);
  }

  return `${t(message.key)} ${message.count} ${t("assignments")}.`;
}

function AutomaticAssignmentPanel({
  automaticAssignmentError,
  automaticAssignmentForm,
  automaticAssignmentMessage,
  automaticAssignmentReport,
  draftSchedulesError,
  formatDateTime,
  isLoadingDraftSchedules,
  isRunningAutomaticAssignment,
  managedDraftSchedules,
  onAutomaticAssignmentFormChange,
  onRunAutomaticAssignment,
}) {
  const { t } = useLanguage();

  return (
    <section className="manager-panel" id="manager-auto-assign">
      <div className="manager-panel-heading">
        <span>3</span>
        <h3>{t("automaticAssignment")}</h3>
      </div>

      {draftSchedulesError ? <p className="error-message">{draftSchedulesError}</p> : null}
      {automaticAssignmentError ? <p className="error-message">{automaticAssignmentError}</p> : null}

      {!isLoadingDraftSchedules && !draftSchedulesError && managedDraftSchedules.length === 0 ? (
        <p className="muted">{t("createDraftBeforeAutoAssign")}</p>
      ) : null}

      {managedDraftSchedules.length > 0 ? (
        <form className="automatic-assignment-form" onSubmit={onRunAutomaticAssignment}>
          <button
            disabled={
              isRunningAutomaticAssignment || isLoadingDraftSchedules || !automaticAssignmentForm.scheduleId
            }
            type="submit"
          >
            {isRunningAutomaticAssignment ? t("assigning") : t("runAutoAssignment")}
          </button>
        </form>
      ) : null}

      {automaticAssignmentMessage ? (
        <div className="success-message">
          <strong>{automaticAssignmentMessageLabel(automaticAssignmentMessage, t)}</strong>
        </div>
      ) : null}

      {automaticAssignmentReport ? (
        <div className="readiness-panel">
          <div className="readiness-summary">
            <span>{automaticAssignmentReport.totalShifts} {t("shifts")}</span>
            <span>{automaticAssignmentReport.assignmentsCreated} {t("assignmentsCreated")}</span>
            <span>{automaticAssignmentReport.totalOpenSlotsBefore} {t("openBefore")}</span>
            <span
              className={
                automaticAssignmentReport.totalOpenSlotsAfter === 0 && automaticAssignmentReport.totalShifts > 0
                  ? "ready-badge"
                  : "warning-badge"
              }
            >
              {automaticAssignmentReport.totalOpenSlotsAfter} {t("openAfter")}
            </span>
          </div>

          {automaticAssignmentReport.shifts.length > 0 ? (
            <div className="auto-assignment-report-list">
              {automaticAssignmentReport.shifts.map((shift) => (
                <div className="assignment-row auto-assignment-row" key={shift.shiftId}>
                  <div className="auto-assignment-main">
                    <strong>{t("shift")} #{shift.shiftId}</strong>
                    <span>
                      {shift.description || t("shift")}, {formatDateTime(shift.startTime)} {t("dateRangeSeparator")} {" "}
                      {formatDateTime(shift.endTime)}
                    </span>
                    <span>{shift.message}</span>
                  </div>

                  <div className="auto-assignment-created-list">
                    <strong>
                      {shift.assignmentsCreated}/{shift.openSlotsBefore} {t("createdCount")}
                    </strong>
                    {renderAssignedEmployees(shift.createdAssignments, t)}
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <p className="muted">{t("noShiftsInDraft")}</p>
          )}
        </div>
      ) : null}
    </section>
  );
}

export default AutomaticAssignmentPanel;
