import { useLanguage } from "../../i18n/LanguageContext.jsx";

function CreateSchedulePanel({
  isCreatingSchedule,
  managedTeams,
  onCreateSchedule,
  onScheduleFormChange,
  scheduleForm,
}) {
  const { t } = useLanguage();

  return (
    <section className="manager-panel" id="manager-drafts">
      <div className="manager-panel-heading">
        <span>1</span>
        <h3>{t("draftSchedule")}</h3>
      </div>
      <form className="manager-form" onSubmit={onCreateSchedule}>
        <label>
          {t("team")}
          <select name="teamId" onChange={onScheduleFormChange} required value={scheduleForm.teamId}>
            {managedTeams.map((team) => (
              <option key={team.id} value={team.id}>
                {team.name}
              </option>
            ))}
          </select>
        </label>

        <label>
          {t("startDate")}
          <input
            name="startDate"
            onChange={onScheduleFormChange}
            required
            type="date"
            value={scheduleForm.startDate}
          />
        </label>

        <label>
          {t("endDate")}
          <input
            name="endDate"
            onChange={onScheduleFormChange}
            required
            type="date"
            value={scheduleForm.endDate}
          />
        </label>

        <button disabled={isCreatingSchedule} type="submit">
          {isCreatingSchedule ? t("creating") : t("createDraftSchedule")}
        </button>
      </form>
    </section>
  );
}

export default CreateSchedulePanel;
