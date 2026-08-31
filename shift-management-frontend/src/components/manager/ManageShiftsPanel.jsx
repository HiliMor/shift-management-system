import { useLanguage } from "../../i18n/LanguageContext.jsx";
import ShiftFormFields from "./ShiftFormFields.jsx";

export default function ManageShiftsPanel({ scheduleId, shifts, isLoading, loadError,
  staffingRoles, isLoadingStaffingRoles, staffingRolesError, editor, onRefresh, formatDateTime }) {
  const { t } = useLanguage();
  const currentShifts = shifts.filter((shift) => String(shift.scheduleId) === String(scheduleId));
  const editingShift = String(editor.editingShift?.scheduleId) === String(scheduleId) ? editor.editingShift : null;
  return (
    <section className="manager-panel" aria-label={t("manageDraftShifts")}>
      <div className="section-heading">
        <h3>{t("manageDraftShifts")}</h3>
        <button className="secondary-button compact-button" type="button" disabled={isLoading || editor.isBusy}
          onClick={() => { editor.cancel(); onRefresh(); }}>{t("refresh")}</button>
      </div>
      {isLoading ? <p className="muted">{t("loadingShifts")}</p> : null}
      {loadError ? <p className="error-message" role="alert">{loadError}</p> : null}
      {staffingRolesError ? <p className="error-message" role="alert">{staffingRolesError}</p> : null}
      {editor.error ? <p className="error-message" role="alert">{editor.error}</p> : null}
      {editor.message ? <p className="success-message" role="status">{t(editor.message)}</p> : null}
      {!isLoading && !loadError && currentShifts.length === 0 ? <p className="muted">{t("noShiftsForDraft")}</p> : null}

      {editingShift && editor.form ? (
        <form className="shift-edit-form" onSubmit={editor.save}>
          <h4>{t("editShift")} #{editingShift.id}</h4>
          <fieldset className="shift-form" disabled={editor.isBusy}>
            <ShiftFormFields form={editor.form} onChange={editor.change}
              staffingRoles={staffingRoles} isLoadingStaffingRoles={isLoadingStaffingRoles} />
            <div className="section-actions">
              <button type="submit" disabled={isLoading || isLoadingStaffingRoles || !!staffingRolesError}>
                {editor.isBusy ? t("saving") : t("saveShift")}
              </button>
              <button className="secondary-button" type="button" onClick={editor.cancel}>{t("cancel")}</button>
            </div>
          </fieldset>
        </form>
      ) : null}

      {!isLoading && !loadError ? (
        <div className="managed-shift-list">
          {currentShifts.map((shift) => (
            <div className="managed-shift-row" key={shift.id}>
              <div>
                <strong>#{shift.id} · {shift.description || t("shift")}</strong>
                <p>{formatDateTime(shift.startTime)} {t("dateRangeSeparator")} {formatDateTime(shift.endTime)}</p>
                <small>{shift.requiredStaffingRoleName || t("noSpecificRole")} · {shift.requiredWorkers} {t("workers")}</small>
              </div>
              <div className="row-actions">
                <button className="secondary-button compact-button" type="button" disabled={editor.isBusy}
                  onClick={() => editor.edit(shift)} aria-label={`${t("editShift")} #${shift.id}`}>{t("editShift")}</button>
                <button className="danger-button compact-button" type="button" disabled={editor.isBusy}
                  onClick={() => editor.remove(shift)} aria-label={`${t("deleteShift")} #${shift.id}`}>{t("deleteShift")}</button>
              </div>
            </div>
          ))}
        </div>
      ) : null}
    </section>
  );
}
