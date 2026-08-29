import { useLanguage } from "../../i18n/LanguageContext.jsx";

function CreateShiftPanel({
  draftSchedulesError,
  isCreatingShift,
  isLoadingDraftSchedules,
  isLoadingStaffingRoles,
  managedDraftSchedules,
  onCreateShift,
  onShiftFormChange,
  shiftForm,
  staffingRoles,
  staffingRolesError,
}) {
  const { t } = useLanguage();

  return (
    <section className="manager-panel" id="manager-shifts">
      <div className="manager-panel-heading">
        <span>2</span>
        <h3>{t("manualShifts")}</h3>
      </div>

      {isLoadingDraftSchedules ? <p className="muted">{t("loadingDraftSchedules")}</p> : null}
      {draftSchedulesError ? <p className="error-message">{draftSchedulesError}</p> : null}
      {staffingRolesError ? <p className="error-message">{staffingRolesError}</p> : null}

      {!isLoadingDraftSchedules && !draftSchedulesError && managedDraftSchedules.length === 0 ? (
        <p className="muted">{t("createDraftBeforeShifts")}</p>
      ) : null}

      {managedDraftSchedules.length > 0 ? (
        <form className="shift-form" onSubmit={onCreateShift}>
          <label>
            {t("startTime")}
            <input
              name="startTime"
              onChange={onShiftFormChange}
              required
              type="datetime-local"
              value={shiftForm.startTime}
            />
          </label>

          <label>
            {t("endTime")}
            <input
              name="endTime"
              onChange={onShiftFormChange}
              required
              type="datetime-local"
              value={shiftForm.endTime}
            />
          </label>

          <label>
            {t("description")}
            <input
              maxLength="500"
              name="description"
              onChange={onShiftFormChange}
              type="text"
              value={shiftForm.description}
            />
          </label>

          <label>
            {t("requiredWorkers")}
            <input
              min="1"
              name="requiredWorkers"
              onChange={onShiftFormChange}
              required
              type="number"
              value={shiftForm.requiredWorkers}
            />
          </label>

          <label>
            {t("minimumRestHours")}
            <input
              min="0"
              name="minRestHours"
              onChange={onShiftFormChange}
              required
              type="number"
              value={shiftForm.minRestHours}
            />
          </label>

          <label>
            {t("requiredRole")}
            <select
              disabled={isLoadingStaffingRoles}
              name="requiredStaffingRoleId"
              onChange={onShiftFormChange}
              value={shiftForm.requiredStaffingRoleId}
            >
              <option value="">{t("noSpecificRole")}</option>
              {staffingRoles.map((role) => (
                <option key={role.id} value={role.id}>
                  {role.name}
                </option>
              ))}
            </select>
          </label>

          <button disabled={isCreatingShift} type="submit">
            {isCreatingShift ? t("creating") : t("createShift")}
          </button>
        </form>
      ) : null}
    </section>
  );
}

export default CreateShiftPanel;
