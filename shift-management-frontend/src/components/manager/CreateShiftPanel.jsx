import { useLanguage } from "../../i18n/LanguageContext.jsx";
import ShiftFormFields from "./ShiftFormFields.jsx";

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
      <div className="panel-title-group">
        <h3>{t("manualShifts")}</h3>
        <p className="panel-description">{t("singleShiftDescription")}</p>
      </div>

      {isLoadingDraftSchedules ? <p className="muted">{t("loadingDraftSchedules")}</p> : null}
      {draftSchedulesError ? <p className="error-message">{draftSchedulesError}</p> : null}
      {staffingRolesError ? <p className="error-message">{staffingRolesError}</p> : null}

      {!isLoadingDraftSchedules && !draftSchedulesError && managedDraftSchedules.length === 0 ? (
        <p className="muted">{t("createDraftBeforeShifts")}</p>
      ) : null}

      {managedDraftSchedules.length > 0 ? (
        <form className="shift-form" onSubmit={onCreateShift}>
          <ShiftFormFields form={shiftForm} onChange={onShiftFormChange}
            staffingRoles={staffingRoles} isLoadingStaffingRoles={isLoadingStaffingRoles} />

          <button disabled={isCreatingShift} type="submit">
            {isCreatingShift ? t("creating") : t("createShift")}
          </button>
        </form>
      ) : null}
    </section>
  );
}

export default CreateShiftPanel;
