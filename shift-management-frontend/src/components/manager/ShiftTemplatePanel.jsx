function renderScheduleOption(schedule, formatDate) {
  return `#${schedule.id} - ${schedule.teamName}, ${formatDate(schedule.startDate)} to ${formatDate(
    schedule.endDate,
  )}`;
}

function renderTemplateOption(template) {
  return `#${template.id} - ${template.name}`;
}

function ShiftTemplatePanel({
  draftSchedulesError,
  formatDate,
  formatDateTime,
  generationDraftSchedules,
  isCreatingTemplate,
  isCreatingTemplateSlot,
  isGeneratingTemplateShifts,
  isLoadingTemplateSlots,
  isLoadingTemplateStaffingRoles,
  isLoadingTemplates,
  managedTeams,
  onCreateTemplate,
  onCreateTemplateSlot,
  onGenerateTemplateShifts,
  onRefreshTemplateSlots,
  onRefreshTemplates,
  onTemplateFormChange,
  onTemplateGenerationFormChange,
  onTemplateSlotFormChange,
  selectedGenerationTemplate,
  selectedTemplate,
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
}) {
  const selectedTemplateMaxDayOffset = selectedTemplate ? selectedTemplate.cycleDays - 1 : 0;
  const canCreateSlot = templates.length > 0 && templateSlotForm.templateId;
  const canGenerate = selectedGenerationTemplate && templateGenerationForm.scheduleId;

  return (
    <section className="manager-panel">
      <div className="section-heading compact-heading">
        <h3>Shift templates</h3>
        <button className="secondary-button compact-button" disabled={isLoadingTemplates} onClick={onRefreshTemplates} type="button">
          Refresh
        </button>
      </div>

      {templateActionError ? <p className="error-message">{templateActionError}</p> : null}
      {templateListError ? <p className="error-message">{templateListError}</p> : null}
      {templateSlotError ? <p className="error-message">{templateSlotError}</p> : null}
      {templateStaffingRolesError ? <p className="error-message">{templateStaffingRolesError}</p> : null}

      <form className="template-form" onSubmit={onCreateTemplate}>
        <label>
          Team
          <select name="teamId" onChange={onTemplateFormChange} required value={templateForm.teamId}>
            {managedTeams.map((team) => (
              <option key={team.id} value={team.id}>
                {team.name}
              </option>
            ))}
          </select>
        </label>

        <label>
          Name
          <input maxLength="150" name="name" onChange={onTemplateFormChange} required type="text" value={templateForm.name} />
        </label>

        <label>
          Cycle days
          <input min="1" name="cycleDays" onChange={onTemplateFormChange} required type="number" value={templateForm.cycleDays} />
        </label>

        <label>
          Rest hours
          <input
            min="0"
            name="defaultMinRestHours"
            onChange={onTemplateFormChange}
            required
            type="number"
            value={templateForm.defaultMinRestHours}
          />
        </label>

        <label>
          Description
          <input
            maxLength="500"
            name="description"
            onChange={onTemplateFormChange}
            type="text"
            value={templateForm.description}
          />
        </label>

        <button disabled={isCreatingTemplate} type="submit">
          {isCreatingTemplate ? "Creating..." : "Create template"}
        </button>
      </form>

      {templateActionMessage ? (
        <div className="success-message">
          <strong>{templateActionMessage}</strong>
        </div>
      ) : null}

      <div className="template-grid">
        <div className="template-column">
          <div className="section-heading compact-heading">
            <h4>Templates</h4>
            <span>{templates.length}</span>
          </div>

          {isLoadingTemplates ? <p className="muted">Loading templates...</p> : null}
          {!isLoadingTemplates && !templateListError && templates.length === 0 ? (
            <p className="muted">No templates for this team.</p>
          ) : null}

          {templates.length > 0 ? (
            <div className="template-list">
              {templates.map((template) => (
                <div className="template-row" key={template.id}>
                  <strong>{template.name}</strong>
                  <span>{template.cycleDays} days</span>
                  <span>{template.defaultMinRestHours} rest hours</span>
                </div>
              ))}
            </div>
          ) : null}
        </div>

        <div className="template-column">
          <div className="section-heading compact-heading">
            <h4>Template slots</h4>
            <button
              className="secondary-button compact-button"
              disabled={isLoadingTemplateSlots || !templateSlotForm.templateId}
              onClick={onRefreshTemplateSlots}
              type="button"
            >
              Refresh
            </button>
          </div>

          {templates.length === 0 ? <p className="muted">Create a template before adding slots.</p> : null}

          {canCreateSlot ? (
            <form className="template-slot-form" onSubmit={onCreateTemplateSlot}>
              <label>
                Template
                <select name="templateId" onChange={onTemplateSlotFormChange} required value={templateSlotForm.templateId}>
                  {templates.map((template) => (
                    <option key={template.id} value={template.id}>
                      {renderTemplateOption(template)}
                    </option>
                  ))}
                </select>
              </label>

              <label>
                Day offset
                <input
                  max={selectedTemplateMaxDayOffset}
                  min="0"
                  name="dayOffset"
                  onChange={onTemplateSlotFormChange}
                  required
                  type="number"
                  value={templateSlotForm.dayOffset}
                />
              </label>

              <label>
                Start time
                <input name="startTime" onChange={onTemplateSlotFormChange} required type="time" value={templateSlotForm.startTime} />
              </label>

              <label>
                Duration minutes
                <input
                  min="1"
                  name="durationMinutes"
                  onChange={onTemplateSlotFormChange}
                  required
                  type="number"
                  value={templateSlotForm.durationMinutes}
                />
              </label>

              <label>
                Required workers
                <input
                  min="1"
                  name="requiredWorkers"
                  onChange={onTemplateSlotFormChange}
                  required
                  type="number"
                  value={templateSlotForm.requiredWorkers}
                />
              </label>

              <label>
                Required role
                <select
                  disabled={isLoadingTemplateStaffingRoles}
                  name="requiredStaffingRoleId"
                  onChange={onTemplateSlotFormChange}
                  value={templateSlotForm.requiredStaffingRoleId}
                >
                  <option value="">No specific role</option>
                  {templateStaffingRoles.map((role) => (
                    <option key={role.id} value={role.id}>
                      {role.name}
                    </option>
                  ))}
                </select>
              </label>

              <label>
                Description
                <input
                  maxLength="500"
                  name="description"
                  onChange={onTemplateSlotFormChange}
                  type="text"
                  value={templateSlotForm.description}
                />
              </label>

              <button disabled={isCreatingTemplateSlot} type="submit">
                {isCreatingTemplateSlot ? "Adding..." : "Add slot"}
              </button>
            </form>
          ) : null}

          {isLoadingTemplateSlots ? <p className="muted">Loading template slots...</p> : null}

          {templateSlots.length > 0 ? (
            <div className="template-list">
              {templateSlots.map((slot) => (
                <div className="template-row" key={slot.id}>
                  <strong>
                    Day {slot.dayOffset}, {slot.startTime}
                  </strong>
                  <span>{slot.durationMinutes} minutes</span>
                  <span>{slot.requiredWorkers} workers</span>
                  <span>{slot.requiredStaffingRoleName || "No role"}</span>
                  <p>{slot.description || "No description"}</p>
                </div>
              ))}
            </div>
          ) : null}
        </div>
      </div>

      {templates.length > 0 ? (
        <form className="template-generation-form" onSubmit={onGenerateTemplateShifts}>
          <label>
            Template
            <select
              name="templateId"
              onChange={onTemplateGenerationFormChange}
              required
              value={templateGenerationForm.templateId}
            >
              {templates.map((template) => (
                <option key={template.id} value={template.id}>
                  {renderTemplateOption(template)}
                </option>
              ))}
            </select>
          </label>

          <label>
            Draft schedule
            <select
              name="scheduleId"
              onChange={onTemplateGenerationFormChange}
              required
              value={templateGenerationForm.scheduleId}
            >
              {generationDraftSchedules.map((schedule) => (
                <option key={schedule.id} value={schedule.id}>
                  {renderScheduleOption(schedule, formatDate)}
                </option>
              ))}
            </select>
          </label>

          <button disabled={isGeneratingTemplateShifts || !canGenerate || Boolean(draftSchedulesError)} type="submit">
            {isGeneratingTemplateShifts ? "Generating..." : "Generate shifts"}
          </button>
        </form>
      ) : null}

      {draftSchedulesError ? <p className="error-message">{draftSchedulesError}</p> : null}
      {selectedGenerationTemplate && generationDraftSchedules.length === 0 ? (
        <p className="muted">No draft schedule for the selected template team.</p>
      ) : null}

      {templateGenerationReport ? (
        <div className="readiness-panel">
          <div className="readiness-summary">
            <span>{templateGenerationReport.shiftsCreated} created</span>
            <span>{templateGenerationReport.skippedExistingShifts} existing skipped</span>
            <span>{templateGenerationReport.skippedOutsideSchedule} outside skipped</span>
          </div>

          {templateGenerationReport.shifts.length > 0 ? (
            <div className="auto-assignment-report-list">
              {templateGenerationReport.shifts.map((shift) => (
                <div className="assignment-row auto-assignment-row" key={shift.id}>
                  <div className="auto-assignment-main">
                    <strong>Shift #{shift.id}</strong>
                    <span>
                      {shift.description || "Shift"}, {formatDateTime(shift.startTime)} to{" "}
                      {formatDateTime(shift.endTime)}
                    </span>
                    <span>
                      {shift.requiredWorkers} workers, template slot #{shift.templateSlotId}
                    </span>
                  </div>
                </div>
              ))}
            </div>
          ) : null}
        </div>
      ) : null}
    </section>
  );
}

export default ShiftTemplatePanel;
