import AssignEmployeePanel from "./manager/AssignEmployeePanel.jsx";
import CreateSchedulePanel from "./manager/CreateSchedulePanel.jsx";
import CreateShiftPanel from "./manager/CreateShiftPanel.jsx";

function ManagerActionsSection({
  assignmentCreationError,
  assignmentForm,
  assignmentShiftMap,
  assignmentShifts,
  assignmentShiftsError,
  createdAssignment,
  createdSchedule,
  createdShift,
  draftSchedulesError,
  formatDate,
  formatDateTime,
  isCreatingAssignment,
  isCreatingSchedule,
  isCreatingShift,
  isLoadingAssignmentShifts,
  isLoadingDraftSchedules,
  isLoadingManagedTeams,
  isLoadingScheduleAssignments,
  isLoadingStaffingRoles,
  isLoadingTeamEmployees,
  managedDraftSchedules,
  managedTeams,
  managedTeamsError,
  onAssignmentFormChange,
  onCreateAssignment,
  onCreateSchedule,
  onCreateShift,
  onScheduleFormChange,
  onShiftFormChange,
  scheduleAssignments,
  scheduleAssignmentsError,
  scheduleCreationError,
  scheduleForm,
  selectedAssignmentSchedule,
  shiftCreationError,
  shiftForm,
  staffingRoles,
  staffingRolesError,
  teamEmployees,
  teamEmployeesError,
}) {
  return (
    <section className="section-block" id="manager">
      <div className="section-heading">
        <h2>Manager actions</h2>
        <span>{managedTeams.length}</span>
      </div>

      {isLoadingManagedTeams ? <p className="muted">Loading managed teams...</p> : null}
      {managedTeamsError ? <p className="error-message">{managedTeamsError}</p> : null}

      {!isLoadingManagedTeams && !managedTeamsError && managedTeams.length === 0 ? (
        <p className="muted">No managed teams are available for this user.</p>
      ) : null}

      {managedTeams.length > 0 ? (
        <div className="manager-stack">
          <CreateSchedulePanel
            isCreatingSchedule={isCreatingSchedule}
            managedTeams={managedTeams}
            onCreateSchedule={onCreateSchedule}
            onScheduleFormChange={onScheduleFormChange}
            scheduleForm={scheduleForm}
          />

          <CreateShiftPanel
            draftSchedulesError={draftSchedulesError}
            formatDate={formatDate}
            isCreatingShift={isCreatingShift}
            isLoadingDraftSchedules={isLoadingDraftSchedules}
            isLoadingStaffingRoles={isLoadingStaffingRoles}
            managedDraftSchedules={managedDraftSchedules}
            onCreateShift={onCreateShift}
            onShiftFormChange={onShiftFormChange}
            shiftForm={shiftForm}
            staffingRoles={staffingRoles}
            staffingRolesError={staffingRolesError}
          />

          <AssignEmployeePanel
            assignmentForm={assignmentForm}
            assignmentShiftMap={assignmentShiftMap}
            assignmentShifts={assignmentShifts}
            assignmentShiftsError={assignmentShiftsError}
            draftSchedulesError={draftSchedulesError}
            formatDate={formatDate}
            formatDateTime={formatDateTime}
            isCreatingAssignment={isCreatingAssignment}
            isLoadingAssignmentShifts={isLoadingAssignmentShifts}
            isLoadingDraftSchedules={isLoadingDraftSchedules}
            isLoadingScheduleAssignments={isLoadingScheduleAssignments}
            isLoadingTeamEmployees={isLoadingTeamEmployees}
            managedDraftSchedules={managedDraftSchedules}
            onAssignmentFormChange={onAssignmentFormChange}
            onCreateAssignment={onCreateAssignment}
            scheduleAssignments={scheduleAssignments}
            scheduleAssignmentsError={scheduleAssignmentsError}
            selectedAssignmentSchedule={selectedAssignmentSchedule}
            teamEmployees={teamEmployees}
            teamEmployeesError={teamEmployeesError}
          />
        </div>
      ) : null}

      {scheduleCreationError ? <p className="error-message">{scheduleCreationError}</p> : null}
      {shiftCreationError ? <p className="error-message">{shiftCreationError}</p> : null}
      {assignmentCreationError ? <p className="error-message">{assignmentCreationError}</p> : null}

      {createdSchedule ? (
        <div className="success-message">
          <strong>Draft schedule #{createdSchedule.id} created</strong>
          <span>
            {createdSchedule.teamName}: {formatDate(createdSchedule.startDate)} to{" "}
            {formatDate(createdSchedule.endDate)}
          </span>
        </div>
      ) : null}

      {createdShift ? (
        <div className="success-message">
          <strong>Shift #{createdShift.id} created</strong>
          <span>
            {formatDateTime(createdShift.startTime)} to {formatDateTime(createdShift.endTime)}
          </span>
        </div>
      ) : null}

      {createdAssignment ? (
        <div className="success-message">
          <strong>Assignment #{createdAssignment.id} created</strong>
          <span>
            {createdAssignment.employeeFullName || createdAssignment.employeeUsername} assigned to shift #
            {createdAssignment.shiftId}
          </span>
        </div>
      ) : null}
    </section>
  );
}

export default ManagerActionsSection;
