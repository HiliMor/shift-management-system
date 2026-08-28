import { useState } from "react";
import AssignEmployeePanel from "./manager/AssignEmployeePanel.jsx";
import AutomaticAssignmentPanel from "./manager/AutomaticAssignmentPanel.jsx";
import CreateSchedulePanel from "./manager/CreateSchedulePanel.jsx";
import CreateShiftPanel from "./manager/CreateShiftPanel.jsx";
import SchedulePublicationPanel from "./manager/SchedulePublicationPanel.jsx";
import ShiftTemplatePanel from "./manager/ShiftTemplatePanel.jsx";

function ManagerActionsSection({
  automaticAssignmentError,
  automaticAssignmentForm,
  automaticAssignmentMessage,
  automaticAssignmentReport,
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
  generationDraftSchedules,
  isCreatingAssignment,
  isCreatingSchedule,
  isCreatingShift,
  isCreatingTemplate,
  isCreatingTemplateSlot,
  isGeneratingTemplateShifts,
  isLoadingAssignmentShifts,
  isLoadingDraftSchedules,
  isLoadingManagedTeams,
  isLoadingManagedPublishedSchedules,
  isLoadingPublicationReadiness,
  isLoadingScheduleAssignments,
  isLoadingStaffingRoles,
  isLoadingTemplateSlots,
  isLoadingTemplateStaffingRoles,
  isLoadingTemplates,
  isLoadingTeamEmployees,
  isRunningAutomaticAssignment,
  isPublishingSchedule,
  managedDraftSchedules,
  managedPublishedSchedules,
  managedPublishedSchedulesError,
  managedTeams,
  managedTeamsError,
  onAutomaticAssignmentFormChange,
  onAssignmentFormChange,
  onCreateAssignment,
  onCreateSchedule,
  onCreateShift,
  onCreateTemplate,
  onCreateTemplateSlot,
  onGenerateTemplateShifts,
  onPublicationFormChange,
  onPublishSchedule,
  onRefreshPublishedSchedules,
  onRefreshPublicationReadiness,
  onRefreshTemplateSlots,
  onRefreshTemplates,
  onReopenSchedule,
  onRunAutomaticAssignment,
  onScheduleFormChange,
  onShiftFormChange,
  onTemplateFormChange,
  onTemplateGenerationFormChange,
  onTemplateSlotFormChange,
  publicationActionError,
  publicationActionMessage,
  publicationError,
  publicationForm,
  publicationReadiness,
  reopeningScheduleId,
  scheduleAssignments,
  scheduleAssignmentsError,
  scheduleCreationError,
  scheduleForm,
  selectedGenerationTemplate,
  selectedAssignmentSchedule,
  selectedTemplate,
  shiftCreationError,
  shiftForm,
  staffingRoles,
  staffingRolesError,
  templateActionError,
  templateActionMessage,
  templateForm,
  templateGenerationForm,
  templateGenerationReport,
  templateListError,
  templates,
  templateSlotError,
  templateSlotForm,
  templateSlots,
  templateStaffingRoles,
  templateStaffingRolesError,
  teamEmployees,
  teamEmployeesError,
}) {
  const [activeWorkflowStep, setActiveWorkflowStep] = useState("draft");
  const readinessLabel = publicationReadiness
    ? publicationReadiness.readyToPublish
      ? "Ready"
      : `${publicationReadiness.totalOpenSlots} open slots`
    : "Not checked";

  const workflowSteps = [
    {
      id: "draft",
      number: "1",
      label: "Draft",
      summary: `${managedDraftSchedules.length} drafts`,
      panelId: "manager-workflow-draft",
    },
    {
      id: "build",
      number: "2",
      label: "Build shifts",
      summary: `${templates.length} templates`,
      panelId: "manager-workflow-build",
    },
    {
      id: "assign",
      number: "3",
      label: "Assign",
      summary: `${scheduleAssignments.length} visible`,
      panelId: "manager-workflow-assign",
    },
    {
      id: "publish",
      number: "4",
      label: "Publish",
      summary: readinessLabel,
      panelId: "manager-workflow-publish",
    },
  ];

  return (
    <section className="section-block" id="manager">
      <div className="section-heading">
        <h2>Schedule workflow</h2>
        <span>{managedTeams.length} teams</span>
      </div>

      {isLoadingManagedTeams ? <p className="muted">Loading managed teams...</p> : null}
      {managedTeamsError ? <p className="error-message">{managedTeamsError}</p> : null}

      {!isLoadingManagedTeams && !managedTeamsError && managedTeams.length === 0 ? (
        <p className="muted">No managed teams are available for this user.</p>
      ) : null}

      {managedTeams.length > 0 ? (
        <div className="manager-stack">
          <div className="workflow-steps" aria-label="Manager schedule workflow" role="tablist">
            {workflowSteps.map((step) => (
              <button
                aria-controls={step.panelId}
                aria-selected={activeWorkflowStep === step.id}
                className={`workflow-step ${activeWorkflowStep === step.id ? "active-workflow-step" : ""}`}
                id={`${step.id}-workflow-tab`}
                key={step.id}
                onClick={() => setActiveWorkflowStep(step.id)}
                role="tab"
                type="button"
              >
                <span>{step.number}</span>
                <strong>{step.label}</strong>
                <small>{step.summary}</small>
              </button>
            ))}
          </div>

          <div
            aria-labelledby={`${activeWorkflowStep}-workflow-tab`}
            className="workflow-panel-content"
            id={`manager-workflow-${activeWorkflowStep}`}
            role="tabpanel"
          >
            {activeWorkflowStep === "draft" ? (
              <CreateSchedulePanel
                isCreatingSchedule={isCreatingSchedule}
                managedTeams={managedTeams}
                onCreateSchedule={onCreateSchedule}
                onScheduleFormChange={onScheduleFormChange}
                scheduleForm={scheduleForm}
              />
            ) : null}

            {activeWorkflowStep === "build" ? (
              <>
                <ShiftTemplatePanel
                  draftSchedulesError={draftSchedulesError}
                  formatDate={formatDate}
                  formatDateTime={formatDateTime}
                  generationDraftSchedules={generationDraftSchedules}
                  isCreatingTemplate={isCreatingTemplate}
                  isCreatingTemplateSlot={isCreatingTemplateSlot}
                  isGeneratingTemplateShifts={isGeneratingTemplateShifts}
                  isLoadingTemplateSlots={isLoadingTemplateSlots}
                  isLoadingTemplateStaffingRoles={isLoadingTemplateStaffingRoles}
                  isLoadingTemplates={isLoadingTemplates}
                  managedTeams={managedTeams}
                  onCreateTemplate={onCreateTemplate}
                  onCreateTemplateSlot={onCreateTemplateSlot}
                  onGenerateTemplateShifts={onGenerateTemplateShifts}
                  onRefreshTemplateSlots={onRefreshTemplateSlots}
                  onRefreshTemplates={onRefreshTemplates}
                  onTemplateFormChange={onTemplateFormChange}
                  onTemplateGenerationFormChange={onTemplateGenerationFormChange}
                  onTemplateSlotFormChange={onTemplateSlotFormChange}
                  selectedGenerationTemplate={selectedGenerationTemplate}
                  selectedTemplate={selectedTemplate}
                  templateActionError={templateActionError}
                  templateActionMessage={templateActionMessage}
                  templateForm={templateForm}
                  templateGenerationForm={templateGenerationForm}
                  templateGenerationReport={templateGenerationReport}
                  templateListError={templateListError}
                  templates={templates}
                  templateSlotError={templateSlotError}
                  templateSlotForm={templateSlotForm}
                  templateSlots={templateSlots}
                  templateStaffingRoles={templateStaffingRoles}
                  templateStaffingRolesError={templateStaffingRolesError}
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
              </>
            ) : null}

            {activeWorkflowStep === "assign" ? (
              <>
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

                <AutomaticAssignmentPanel
                  automaticAssignmentError={automaticAssignmentError}
                  automaticAssignmentForm={automaticAssignmentForm}
                  automaticAssignmentMessage={automaticAssignmentMessage}
                  automaticAssignmentReport={automaticAssignmentReport}
                  draftSchedulesError={draftSchedulesError}
                  formatDate={formatDate}
                  formatDateTime={formatDateTime}
                  isLoadingDraftSchedules={isLoadingDraftSchedules}
                  isRunningAutomaticAssignment={isRunningAutomaticAssignment}
                  managedDraftSchedules={managedDraftSchedules}
                  onAutomaticAssignmentFormChange={onAutomaticAssignmentFormChange}
                  onRunAutomaticAssignment={onRunAutomaticAssignment}
                />
              </>
            ) : null}

            {activeWorkflowStep === "publish" ? (
              <SchedulePublicationPanel
                draftSchedulesError={draftSchedulesError}
                formatDate={formatDate}
                formatDateTime={formatDateTime}
                isLoadingDraftSchedules={isLoadingDraftSchedules}
                isLoadingManagedPublishedSchedules={isLoadingManagedPublishedSchedules}
                isLoadingPublicationReadiness={isLoadingPublicationReadiness}
                isPublishingSchedule={isPublishingSchedule}
                managedDraftSchedules={managedDraftSchedules}
                managedPublishedSchedules={managedPublishedSchedules}
                managedPublishedSchedulesError={managedPublishedSchedulesError}
                onPublicationFormChange={onPublicationFormChange}
                onPublishSchedule={onPublishSchedule}
                onRefreshPublishedSchedules={onRefreshPublishedSchedules}
                onRefreshPublicationReadiness={onRefreshPublicationReadiness}
                onReopenSchedule={onReopenSchedule}
                publicationError={publicationError}
                publicationForm={publicationForm}
                publicationReadiness={publicationReadiness}
                reopeningScheduleId={reopeningScheduleId}
              />
            ) : null}
          </div>
        </div>
      ) : null}

      {scheduleCreationError ? <p className="error-message">{scheduleCreationError}</p> : null}
      {shiftCreationError ? <p className="error-message">{shiftCreationError}</p> : null}
      {assignmentCreationError ? <p className="error-message">{assignmentCreationError}</p> : null}
      {publicationActionError ? <p className="error-message">{publicationActionError}</p> : null}

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

      {publicationActionMessage ? (
        <div className="success-message">
          <strong>{publicationActionMessage}</strong>
        </div>
      ) : null}
    </section>
  );
}

export default ManagerActionsSection;
