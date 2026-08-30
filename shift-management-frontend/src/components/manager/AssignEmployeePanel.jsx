import { useEffect, useMemo, useState } from "react";
import { useLanguage } from "../../i18n/LanguageContext.jsx";
import WeeklyScheduleCalendar from "../WeeklyScheduleCalendar.jsx";

function AssignEmployeePanel({
  assignmentActionError,
  assignmentActionMessage,
  assignmentCreationError,
  assignmentForm,
  assignmentShiftMap,
  assignmentShifts,
  assignmentShiftsError,
  draftSchedulesError,
  formatDateTime,
  isCreatingAssignment,
  deletingAssignmentId,
  isLoadingAssignmentShifts,
  isLoadingDraftSchedules,
  isLoadingScheduleAssignments,
  isLoadingTeamEmployees,
  managedDraftSchedules,
  onAssignmentFormChange,
  onCreateAssignment,
  onDeleteAssignment,
  onSelectAssignmentShift,
  scheduleAssignments,
  scheduleAssignmentsError,
  selectedAssignmentSchedule,
  teamEmployees,
  teamEmployeesError,
}) {
  const { t, translateDomainValue } = useLanguage();
  const [assignmentViewMode, setAssignmentViewMode] = useState("calendar");
  const selectedAssignmentShift = assignmentShifts.find(
    (shift) => shift.id.toString() === assignmentForm.shiftId,
  );
  const requiredRoleId = selectedAssignmentShift?.requiredStaffingRoleId;
  const requiredRoleName = selectedAssignmentShift?.requiredStaffingRoleName;
  const assignableEmployees = requiredRoleId
    ? teamEmployees.filter((employee) => employee.staffingRoleIds?.includes(requiredRoleId))
    : teamEmployees;
  const selectedEmployeeId = assignableEmployees.some(
    (employee) => employee.id.toString() === assignmentForm.employeeId,
  )
    ? assignmentForm.employeeId
    : "";
  const calendarShifts = useMemo(() => {
    const assignmentsByShiftId = new Map();

    scheduleAssignments.forEach((assignment) => {
      const assignments = assignmentsByShiftId.get(assignment.shiftId) ?? [];
      assignments.push(assignment);
      assignmentsByShiftId.set(assignment.shiftId, assignments);
    });

    return assignmentShifts.map((shift) => ({
      ...shift,
      assignments: assignmentsByShiftId.get(shift.id) ?? [],
    }));
  }, [assignmentShifts, scheduleAssignments]);

  useEffect(() => {
    setAssignmentViewMode("calendar");
  }, [assignmentForm.scheduleId]);

  return (
    <section className="manager-panel" id="manager-assignments">
      <div className="manager-panel-heading">
        <span>3</span>
        <h3>{t("manualAssignment")}</h3>
      </div>

      {assignmentShiftsError ? <p className="error-message">{assignmentShiftsError}</p> : null}
      {teamEmployeesError ? <p className="error-message">{teamEmployeesError}</p> : null}
      {scheduleAssignmentsError ? <p className="error-message">{scheduleAssignmentsError}</p> : null}
      {assignmentCreationError ? <p className="error-message">{assignmentCreationError}</p> : null}
      {assignmentActionError ? <p className="error-message">{assignmentActionError}</p> : null}
      {assignmentActionMessage ? <p className="success-message">{t(assignmentActionMessage)}</p> : null}

      {selectedAssignmentShift ? (
        <div className="assignment-guidance">
          <strong>{t("assignmentGuidance")}</strong>
          <span>
            {requiredRoleName
              ? `${t("requiredRoleForShift")}: ${translateDomainValue(requiredRoleName)}. ${t("employeesWithRole")}`
              : t("noSpecificRoleForShift")}
          </span>
        </div>
      ) : null}

      {!isLoadingDraftSchedules && !draftSchedulesError && managedDraftSchedules.length === 0 ? (
        <p className="muted">{t("createDraftBeforeAssignment")}</p>
      ) : null}

      {managedDraftSchedules.length > 0 ? (
        <form className="assignment-form" onSubmit={onCreateAssignment}>
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
                  {shift.requiredStaffingRoleName ? ` - ${shift.requiredStaffingRoleName}` : ""}
                </option>
              ))}
            </select>
          </label>

          <label>
            {t("employee")}
            <select
              disabled={isLoadingTeamEmployees || assignableEmployees.length === 0}
              name="employeeId"
              onChange={onAssignmentFormChange}
              required
              value={selectedEmployeeId}
            >
              <option disabled value="">
                {t("selectEmployee")}
              </option>
              {assignableEmployees.map((employee) => (
                <option key={employee.id} value={employee.id}>
                  {employee.fullName || employee.username}
                  {employee.staffingRoleNames?.length
                    ? ` - ${employee.staffingRoleNames.join(", ")}`
                    : ` - ${t("noStaffingRoles")}`}
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
              assignableEmployees.length === 0 ||
              !selectedEmployeeId
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

      {!isLoadingTeamEmployees &&
      !teamEmployeesError &&
      requiredRoleName &&
      teamEmployees.length > 0 &&
      assignableEmployees.length === 0 ? (
        <p className="muted">{t("noEmployeesWithRequiredRole")}</p>
      ) : null}

      {assignmentShifts.length > 0 && selectedAssignmentSchedule ? (
        <>
          <div className="assignment-guidance">
            <strong>{t("assignmentBoardTitle")}</strong>
            <span>{t("assignmentBoardHint")}</span>
          </div>

          <div className="details-view-toolbar">
            <div>
              <p className="eyebrow">{t("shiftDisplay")}</p>
              <strong>{assignmentViewMode === "calendar" ? t("weeklyCalendar") : t("listView")}</strong>
            </div>
            <div className="view-toggle" role="group" aria-label={t("shiftDisplay")}>
              <button
                aria-pressed={assignmentViewMode === "calendar"}
                className={assignmentViewMode === "calendar" ? "active-view-button" : "secondary-button"}
                onClick={() => setAssignmentViewMode("calendar")}
                type="button"
              >
                {t("calendarView")}
              </button>
              <button
                aria-pressed={assignmentViewMode === "list"}
                className={assignmentViewMode === "list" ? "active-view-button" : "secondary-button"}
                onClick={() => setAssignmentViewMode("list")}
                type="button"
              >
                {t("listView")}
              </button>
            </div>
          </div>

          {assignmentViewMode === "calendar" ? (
            <WeeklyScheduleCalendar
              onSelectShift={onSelectAssignmentShift}
              schedule={selectedAssignmentSchedule}
              selectedShiftId={assignmentForm.shiftId}
              shifts={calendarShifts}
            />
          ) : null}
        </>
      ) : null}

      {assignmentViewMode === "list" || assignmentShifts.length === 0 ? (
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
                <div className="row-actions">
                  <button
                    className="danger-button compact-button"
                    disabled={deletingAssignmentId !== null}
                    onClick={() => {
                      if (window.confirm(t("confirmRemoveAssignment"))) {
                        onDeleteAssignment(assignment.id);
                      }
                    }}
                    type="button"
                  >
                    {deletingAssignmentId === assignment.id
                      ? t("removingAssignment")
                      : t("removeAssignment")}
                  </button>
                </div>
              </div>
            );
          })}
        </div>
      ) : null}
    </section>
  );
}

export default AssignEmployeePanel;
