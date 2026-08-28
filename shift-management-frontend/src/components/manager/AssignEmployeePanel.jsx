import { useLanguage } from "../../i18n/LanguageContext.jsx";

function renderScheduleOption(schedule, formatDate, t) {
  return `#${schedule.id} - ${schedule.teamName}, ${formatDate(schedule.startDate)} ${t("dateRangeSeparator")} ${formatDate(
    schedule.endDate,
  )}`;
}

function AssignEmployeePanel({
  assignmentForm,
  assignmentShiftMap,
  assignmentShifts,
  assignmentShiftsError,
  draftSchedulesError,
  formatDate,
  formatDateTime,
  isCreatingAssignment,
  isLoadingAssignmentShifts,
  isLoadingDraftSchedules,
  isLoadingScheduleAssignments,
  isLoadingTeamEmployees,
  managedDraftSchedules,
  onAssignmentFormChange,
  onCreateAssignment,
  scheduleAssignments,
  scheduleAssignmentsError,
  selectedAssignmentSchedule,
  teamEmployees,
  teamEmployeesError,
}) {
  const { t } = useLanguage();

  return (
    <section className="manager-panel" id="manager-assignments">
      <div className="manager-panel-heading">
        <span>3</span>
        <h3>{t("manualAssignment")}</h3>
      </div>

      {assignmentShiftsError ? <p className="error-message">{assignmentShiftsError}</p> : null}
      {teamEmployeesError ? <p className="error-message">{teamEmployeesError}</p> : null}
      {scheduleAssignmentsError ? <p className="error-message">{scheduleAssignmentsError}</p> : null}

      {!isLoadingDraftSchedules && !draftSchedulesError && managedDraftSchedules.length === 0 ? (
        <p className="muted">{t("createDraftBeforeAssignment")}</p>
      ) : null}

      {managedDraftSchedules.length > 0 ? (
        <form className="assignment-form" onSubmit={onCreateAssignment}>
          <label>
            {t("draftSchedule")}
            <select
              name="scheduleId"
              onChange={onAssignmentFormChange}
              required
              value={assignmentForm.scheduleId}
            >
              {managedDraftSchedules.map((schedule) => (
                <option key={schedule.id} value={schedule.id}>
                  {renderScheduleOption(schedule, formatDate, t)}
                </option>
              ))}
            </select>
          </label>

          <label>
            {t("shift")}
            <select
              disabled={isLoadingAssignmentShifts || assignmentShifts.length === 0}
              name="shiftId"
              onChange={onAssignmentFormChange}
              required
              value={assignmentForm.shiftId}
            >
              {assignmentShifts.map((shift) => (
                <option key={shift.id} value={shift.id}>
                  #{shift.id} - {shift.description || t("shift")}, {formatDateTime(shift.startTime)}
                </option>
              ))}
            </select>
          </label>

          <label>
            {t("employee")}
            <select
              disabled={isLoadingTeamEmployees || teamEmployees.length === 0}
              name="employeeId"
              onChange={onAssignmentFormChange}
              required
              value={assignmentForm.employeeId}
            >
              {teamEmployees.map((employee) => (
                <option key={employee.id} value={employee.id}>
                  {employee.fullName || employee.username}
                </option>
              ))}
            </select>
          </label>

          <button
            disabled={
              isCreatingAssignment ||
              isLoadingAssignmentShifts ||
              isLoadingTeamEmployees ||
              assignmentShifts.length === 0 ||
              teamEmployees.length === 0
            }
            type="submit"
          >
            {isCreatingAssignment ? t("assigning") : t("assignEmployee")}
          </button>
        </form>
      ) : null}

      {isLoadingAssignmentShifts ? <p className="muted">{t("loadingShifts")}</p> : null}
      {isLoadingTeamEmployees ? <p className="muted">{t("loadingEmployees")}</p> : null}

      {!isLoadingAssignmentShifts &&
      !assignmentShiftsError &&
      managedDraftSchedules.length > 0 &&
      assignmentShifts.length === 0 ? (
        <p className="muted">{t("addShiftBeforeAssignment")}</p>
      ) : null}

      {!isLoadingTeamEmployees && !teamEmployeesError && selectedAssignmentSchedule && teamEmployees.length === 0 ? (
        <p className="muted">{t("noActiveEmployees")}</p>
      ) : null}

      <div className="assignment-panel-list">
        <h4>{t("currentAssignments")}</h4>
        {isLoadingScheduleAssignments ? <p className="muted">{t("loadingAssignments")}</p> : null}

        {!isLoadingScheduleAssignments &&
        !scheduleAssignmentsError &&
        assignmentForm.scheduleId &&
        scheduleAssignments.length === 0 ? (
          <p className="muted">{t("noAssignmentsInDraft")}</p>
        ) : null}

        {scheduleAssignments.map((assignment) => {
          const assignedShift = assignmentShiftMap.get(assignment.shiftId);

          return (
            <div className="assignment-row" key={assignment.id}>
              <strong>{assignment.employeeFullName || assignment.employeeUsername}</strong>
              <span>
                {assignedShift
                  ? `#${assignedShift.id} - ${assignedShift.description || t("shift")}, ${formatDateTime(
                      assignedShift.startTime,
                    )}`
                  : `${t("shift")} #${assignment.shiftId}`}
              </span>
            </div>
          );
        })}
      </div>
    </section>
  );
}

export default AssignEmployeePanel;
