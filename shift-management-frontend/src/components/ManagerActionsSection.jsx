import { useState } from "react";
import { useLanguage } from "../i18n/LanguageContext.jsx";
import AssignEmployeePanel from "./manager/AssignEmployeePanel.jsx";
import AutomaticAssignmentPanel from "./manager/AutomaticAssignmentPanel.jsx";
import CreateSchedulePanel from "./manager/CreateSchedulePanel.jsx";
import CreateShiftPanel from "./manager/CreateShiftPanel.jsx";
import SchedulePublicationPanel from "./manager/SchedulePublicationPanel.jsx";
import ShiftTemplatePanel from "./manager/ShiftTemplatePanel.jsx";

function managerActionMessageLabel(message, t) {
  if (!message) {
    return "";
  }

  if (typeof message === "string") {
    return t(message);
  }

  return `${t(message.key)} #${message.id}.`;
}

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
  isDeletingTemplate,
  isGeneratingTemplateShifts,
  isDeletingSchedule,
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
  onSelectAssignmentShift,
  onCreateSchedule,
  onCreateShift,
  onDeleteDraftSchedule,
  onCreateTemplate,
  onCreateTemplateSlot,
  onDeleteTemplate,
  onGenerateTemplateShifts,
  onPublicationFormChange,
  onPublishSchedule,
  onRefreshPublishedSchedules,
  onRefreshPublicationReadiness,
  onSelectManagedPublishedSchedule,
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
  scheduleActionError,
  scheduleActionMessage,
  scheduleCreationError,
  scheduleForm,
  selectedDraftSchedule,
  selectedDraftScheduleId,
  selectedManagedPublishedScheduleId,
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
  onSelectDraftSchedule,
}) {
  const { t } = useLanguage();
  const [activeWorkflowStep, setActiveWorkflowStep] = useState("draft");
  const readinessLabel = publicationReadiness
    ? publicationReadiness.readyToPublish
      ? t("ready")
      : `${publicationReadiness.totalOpenSlots} ${t("openSlots")}`
    : t("notChecked");
  const shiftsStepSummary = isLoadingAssignmentShifts
    ? t("loadingShifts")
    : `${assignmentShifts.length} ${t("createdShifts")} · ${templates.length} ${t("templates")}`;

  const workflowSteps = [
    {
      id: "draft",
      number: "1",
      label: t("draft"),
      summary: `${managedDraftSchedules.length} ${t("drafts")}`,
      panelId: "manager-workflow-draft",
    },
    {
      id: "build",
      number: "2",
      label: t("shiftsStep"),
      summary: shiftsStepSummary,
      panelId: "manager-workflow-build",
    },
    {
      id: "assign",
      number: "3",
      label: t("assign"),
      summary: `${scheduleAssignments.length} ${t("visible")}`,
      panelId: "manager-workflow-assign",
    },
    {
      id: "publish",
      number: "4",
      label: t("publish"),
      summary: readinessLabel,
      panelId: "manager-workflow-publish",
    },
  ];

  return (
    <section className="section-block" id="manager">
      <div className="section-heading">
        <h2>{t("scheduleWorkflow")}</h2>
        <span>{managedTeams.length} {t("teams")}</span>
      </div>

      {isLoadingManagedTeams ? <p className="muted">{t("loadingManagedTeams")}</p> : null}
      {managedTeamsError ? <p className="error-message">{managedTeamsError}</p> : null}

      {!isLoadingManagedTeams && !managedTeamsError && managedTeams.length === 0 ? (
        <p className="muted">{t("noManagedTeams")}</p>
      ) : null}

      {managedDraftSchedules.length > 0 ? (
        <div className="draft-context-bar">
          <div className="draft-context-copy">
            <span className="eyebrow">{t("currentDraft")}</span>
            {selectedDraftSchedule ? (
              <>
                <strong>
                  #{selectedDraftSchedule.id} - {selectedDraftSchedule.teamName}
                </strong>
                <span>
                  {formatDate(selectedDraftSchedule.startDate)} {t("dateRangeSeparator")} {" "}
                  {formatDate(selectedDraftSchedule.endDate)} · {t("draft")}
                </span>
              </>
            ) : (
              <strong>{t("noDraftSelected")}</strong>
            )}
            <small>{t("draftContextDescription")}</small>
          </div>
          <div className="draft-context-actions">
            <label className="draft-context-select">
              <span>{t("changeDraft")}</span>
              <select value={selectedDraftScheduleId} onChange={(event) => onSelectDraftSchedule(event.target.value)}>
                {managedDraftSchedules.map((schedule) => (
                  <option key={schedule.id} value={schedule.id}>
                    #{schedule.id} - {schedule.teamName}, {formatDate(schedule.startDate)} {t("dateRangeSeparator")} {" "}
                    {formatDate(schedule.endDate)}
                  </option>
                ))}
              </select>
            </label>
            <button
              className="danger-button compact-button"
              disabled={isDeletingSchedule || isLoadingDraftSchedules || !selectedDraftSchedule}
              onClick={() => {
                if (window.confirm(t("confirmDeleteDraftSchedule"))) {
                  onDeleteDraftSchedule();
                }
              }}
              type="button"
            >
              {isDeletingSchedule ? t("deletingDraftSchedule") : t("deleteDraftSchedule")}
            </button>
          </div>
        </div>
      ) : null}

      {!isLoadingManagedTeams && managedTeams.length > 0 && managedDraftSchedules.length === 0 ? (
        <p className="muted">{t("selectDraftToBegin")}</p>
      ) : null}

      {managedTeams.length > 0 ? (
        <div className="manager-stack">
          <div className="workflow-steps" aria-label={t("managerScheduleWorkflow")} role="tablist">
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
                  assignmentShifts={assignmentShifts}
                  isLoadingAssignmentShifts={isLoadingAssignmentShifts}
                  isLoadingScheduleAssignments={isLoadingScheduleAssignments}
                  scheduleAssignments={scheduleAssignments}
                  formatDate={formatDate}
                  formatDateTime={formatDateTime}
                  generationDraftSchedules={generationDraftSchedules}
                  isCreatingTemplate={isCreatingTemplate}
                  isCreatingTemplateSlot={isCreatingTemplateSlot}
                  isDeletingTemplate={isDeletingTemplate}
                  isGeneratingTemplateShifts={isGeneratingTemplateShifts}
                  isLoadingTemplateSlots={isLoadingTemplateSlots}
                  isLoadingTemplateStaffingRoles={isLoadingTemplateStaffingRoles}
                  isLoadingTemplates={isLoadingTemplates}
                  managedTeams={managedTeams}
                  onCreateTemplate={onCreateTemplate}
                  onCreateTemplateSlot={onCreateTemplateSlot}
                  onDeleteTemplate={onDeleteTemplate}
                  onGenerateTemplateShifts={onGenerateTemplateShifts}
                  onRefreshTemplateSlots={onRefreshTemplateSlots}
                  onRefreshTemplates={onRefreshTemplates}
                  onTemplateFormChange={onTemplateFormChange}
                  onTemplateGenerationFormChange={onTemplateGenerationFormChange}
                  onTemplateSlotFormChange={onTemplateSlotFormChange}
                  selectedGenerationTemplate={selectedGenerationTemplate}
                  selectedDraftSchedule={selectedDraftSchedule}
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
                  onSelectAssignmentShift={onSelectAssignmentShift}
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
                onSelectPublishedSchedule={onSelectManagedPublishedSchedule}
                publicationError={publicationError}
                publicationForm={publicationForm}
                publicationReadiness={publicationReadiness}
                reopeningScheduleId={reopeningScheduleId}
                selectedPublishedScheduleId={selectedManagedPublishedScheduleId}
              />
            ) : null}
          </div>
        </div>
      ) : null}

      {scheduleCreationError ? <p className="error-message">{scheduleCreationError}</p> : null}
      {scheduleActionError ? <p className="error-message">{scheduleActionError}</p> : null}
      {shiftCreationError ? <p className="error-message">{shiftCreationError}</p> : null}
      {assignmentCreationError ? <p className="error-message">{assignmentCreationError}</p> : null}
      {publicationActionError ? <p className="error-message">{publicationActionError}</p> : null}

      {createdSchedule ? (
        <div className="success-message">
          <strong>{t("draftScheduleCreated")} #{createdSchedule.id}</strong>
          <span>
            {createdSchedule.teamName}: {formatDate(createdSchedule.startDate)} {t("dateRangeSeparator")} {" "}
            {formatDate(createdSchedule.endDate)}
          </span>
        </div>
      ) : null}

      {scheduleActionMessage ? (
        <div className="success-message">
          <strong>{t(scheduleActionMessage)}</strong>
        </div>
      ) : null}

      {createdShift ? (
        <div className="success-message">
          <strong>{t("shiftCreated")} #{createdShift.id}</strong>
          <span>
            {formatDateTime(createdShift.startTime)} {t("dateRangeSeparator")} {formatDateTime(createdShift.endTime)}
          </span>
        </div>
      ) : null}

      {createdAssignment ? (
        <div className="success-message">
          <strong>{t("assignmentCreated")} #{createdAssignment.id}</strong>
          <span>
            {createdAssignment.employeeFullName || createdAssignment.employeeUsername} {t("assignedToShift")} #
            {createdAssignment.shiftId}
          </span>
        </div>
      ) : null}

      {publicationActionMessage ? (
        <div className="success-message">
          <strong>{managerActionMessageLabel(publicationActionMessage, t)}</strong>
        </div>
      ) : null}
    </section>
  );
}

export default ManagerActionsSection;
