import { useLanguage } from "../../i18n/LanguageContext.jsx";

function renderScheduleOption(schedule, formatDate, t) {
  return `#${schedule.id} - ${schedule.teamName}, ${formatDate(schedule.startDate)} ${t("dateRangeSeparator")} ${formatDate(
    schedule.endDate,
  )}`;
}

function renderTemplateOption(template) {
  return `#${template.id} - ${template.name}`;
}

function templateActionMessageLabel(message, t) {
  if (!message) {
    return "";
  }

  if (typeof message === "string") {
    return t(message);
  }

  if (message.key === "templateCreated") {
    return `${t(message.key)} "${message.templateName}".`;
  }

  if (message.key === "templateSlotCreated") {
    return `${t(message.key)} #${message.slotId}.`;
  }

  if (message.key === "templateShiftsGenerated") {
    return `${t(message.key)}: ${message.shiftsCreated}.`;
  }

  return t(message.key);
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
  selectedDraftSchedule,
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
  const { t } = useLanguage();
  const selectedTemplateMaxDayOffset = selectedTemplate ? selectedTemplate.cycleDays - 1 : 0;
  const canCreateSlot = templates.length > 0 && templateSlotForm.templateId;
  const canGenerate = selectedGenerationTemplate && templateGenerationForm.scheduleId;

  return (
    <section className="manager-panel" id="manager-templates">
      <div className="section-heading compact-heading">
        <div className="manager-panel-heading">
          <span>2</span>
          <h3>{t("templates")}</h3>
        </div>
        <button
          className="secondary-button compact-button"
          disabled={isLoadingTemplates}
          onClick={onRefreshTemplates}
          type="button"
        >
          {t("refresh")}
        </button>
      </div>

      {templateActionError ? <p className="error-message">{templateActionError}</p> : null}
      {templateListError ? <p className="error-message">{templateListError}</p> : null}
      {templateSlotError ? <p className="error-message">{templateSlotError}</p> : null}
      {templateStaffingRolesError ? <p className="error-message">{templateStaffingRolesError}</p> : null}

      <form className="template-form" onSubmit={onCreateTemplate}>
        <label>
          {t("team")}
          <select name="teamId" onChange={onTemplateFormChange} required value={templateForm.teamId}>
            {managedTeams.map((team) => (
              <option key={team.id} value={team.id}>
                {team.name}
              </option>
            ))}
          </select>
        </label>

        <label>
          {t("name")}
          <input maxLength="150" name="name" onChange={onTemplateFormChange} required type="text" value={templateForm.name} />
        </label>

        <label>
          {t("cycleDays")}
          <input min="1" name="cycleDays" onChange={onTemplateFormChange} required type="number" value={templateForm.cycleDays} />
        </label>

        <label>
          {t("restHours")}
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
          {t("description")}
          <input
            maxLength="500"
            name="description"
            onChange={onTemplateFormChange}
            type="text"
            value={templateForm.description}
          />
        </label>

        <button disabled={isCreatingTemplate} type="submit">
          {isCreatingTemplate ? t("creating") : t("createTemplate")}
        </button>
      </form>

      {templateActionMessage ? (
        <div className="success-message">
          <strong>{templateActionMessageLabel(templateActionMessage, t)}</strong>
        </div>
      ) : null}

      <div className="template-grid">
        <div className="template-column">
          <div className="section-heading compact-heading">
            <h4>{t("templates")}</h4>
            <span>{templates.length}</span>
          </div>

          {isLoadingTemplates ? <p className="muted">{t("loadingTemplates")}</p> : null}
          {!isLoadingTemplates && !templateListError && templates.length === 0 ? (
            <p className="muted">{t("noTemplatesForTeam")}</p>
          ) : null}

          {templates.length > 0 ? (
            <div className="template-list">
              {templates.map((template) => (
                <div className="template-row" key={template.id}>
                  <strong>{template.name}</strong>
                  <span>{template.cycleDays} {t("days")}</span>
                  <span>{template.defaultMinRestHours} {t("restHours")}</span>
                </div>
              ))}
            </div>
          ) : null}
        </div>

        <div className="template-column">
          <div className="section-heading compact-heading">
            <h4>{t("templateSlots")}</h4>
            <button
              className="secondary-button compact-button"
              disabled={isLoadingTemplateSlots || !templateSlotForm.templateId}
              onClick={onRefreshTemplateSlots}
              type="button"
            >
              {t("refresh")}
            </button>
          </div>

          {templates.length === 0 ? <p className="muted">{t("createTemplateBeforeSlots")}</p> : null}

          {canCreateSlot ? (
            <form className="template-slot-form" onSubmit={onCreateTemplateSlot}>
              <label>
                {t("template")}
                <select name="templateId" onChange={onTemplateSlotFormChange} required value={templateSlotForm.templateId}>
                  {templates.map((template) => (
                    <option key={template.id} value={template.id}>
                      {renderTemplateOption(template)}
                    </option>
                  ))}
                </select>
              </label>

              <label>
                {t("dayOffset")}
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
                {t("startTime")}
                <input
                  name="startTime"
                  onChange={onTemplateSlotFormChange}
                  required
                  type="time"
                  value={templateSlotForm.startTime}
                />
              </label>

              <label>
                {t("durationMinutes")}
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
                {t("requiredWorkers")}
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
                {t("requiredRole")}
                <select
                  disabled={isLoadingTemplateStaffingRoles}
                  name="requiredStaffingRoleId"
                  onChange={onTemplateSlotFormChange}
                  value={templateSlotForm.requiredStaffingRoleId}
                >
                  <option value="">{t("noSpecificRole")}</option>
                  {templateStaffingRoles.map((role) => (
                    <option key={role.id} value={role.id}>
                      {role.name}
                    </option>
                  ))}
                </select>
              </label>

              <label>
                {t("description")}
                <input
                  maxLength="500"
                  name="description"
                  onChange={onTemplateSlotFormChange}
                  type="text"
                  value={templateSlotForm.description}
                />
              </label>

              <button disabled={isCreatingTemplateSlot} type="submit">
                {isCreatingTemplateSlot ? t("adding") : t("addSlot")}
              </button>
            </form>
          ) : null}

          {isLoadingTemplateSlots ? <p className="muted">{t("loadingTemplateSlots")}</p> : null}

          {templateSlots.length > 0 ? (
            <div className="template-list">
              {templateSlots.map((slot) => (
                <div className="template-row" key={slot.id}>
                  <strong>
                    {t("dayOffset")} {slot.dayOffset}, {slot.startTime}
                  </strong>
                  <span>{slot.durationMinutes} {t("minutes")}</span>
                  <span>{slot.requiredWorkers} {t("workers")}</span>
                  <span>{slot.requiredStaffingRoleName || t("noRole")}</span>
                  <p>{slot.description || t("noDescription")}</p>
                </div>
              ))}
            </div>
          ) : null}
        </div>
      </div>

      {templates.length > 0 ? (
        <form className="template-generation-form" onSubmit={onGenerateTemplateShifts}>
          <label>
            {t("template")}
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

          <div className="template-generation-context">
            <span className="eyebrow">{t("generateIntoDraft")}</span>
            <strong>
              {selectedDraftSchedule
                ? renderScheduleOption(selectedDraftSchedule, formatDate, t)
                : t("noDraftSelected")}
            </strong>
            {selectedGenerationTemplate &&
            generationDraftSchedules.length > 0 &&
            !templateGenerationForm.scheduleId ? (
              <small>{t("selectDraftForTemplateTeam")}</small>
            ) : null}
          </div>

          <button disabled={isGeneratingTemplateShifts || !canGenerate || Boolean(draftSchedulesError)} type="submit">
            {isGeneratingTemplateShifts ? t("generating") : t("generateShifts")}
          </button>
        </form>
      ) : null}

      {draftSchedulesError ? <p className="error-message">{draftSchedulesError}</p> : null}
      {selectedGenerationTemplate && generationDraftSchedules.length === 0 ? (
        <p className="muted">{t("noDraftScheduleForTemplate")}</p>
      ) : null}

      {templateGenerationReport ? (
        <div className="readiness-panel">
          <div className="readiness-summary">
            <span>{templateGenerationReport.shiftsCreated} {t("created")}</span>
            <span>{templateGenerationReport.skippedExistingShifts} {t("existingSkipped")}</span>
            <span>{templateGenerationReport.skippedOutsideSchedule} {t("outsideSkipped")}</span>
          </div>

          {templateGenerationReport.shifts.length > 0 ? (
            <div className="auto-assignment-report-list">
              {templateGenerationReport.shifts.map((shift) => (
                <div className="assignment-row auto-assignment-row" key={shift.id}>
                  <div className="auto-assignment-main">
                    <strong>{t("shift")} #{shift.id}</strong>
                    <span>
                      {shift.description || t("shift")}, {formatDateTime(shift.startTime)} {t("dateRangeSeparator")} {" "}
                      {formatDateTime(shift.endTime)}
                    </span>
                    <span>
                      {shift.requiredWorkers} {t("workers")}, {t("templateSlot")} #{shift.templateSlotId}
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
