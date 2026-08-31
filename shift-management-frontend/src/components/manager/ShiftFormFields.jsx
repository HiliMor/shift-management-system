import { useLanguage } from "../../i18n/LanguageContext.jsx";

export default function ShiftFormFields({ form, onChange, staffingRoles, isLoadingStaffingRoles }) {
  const { t } = useLanguage();
  return (
    <>
      <label>
        {t("startTime")}
        <input name="startTime" onChange={onChange} required step="1" type="datetime-local" value={form.startTime} />
      </label>
      <label>
        {t("endTime")}
        <input name="endTime" onChange={onChange} required step="1" type="datetime-local" value={form.endTime} />
      </label>
      <label>
        {t("description")}
        <input maxLength="500" name="description" onChange={onChange} type="text" value={form.description} />
      </label>
      <label>
        {t("requiredWorkers")}
        <input min="1" name="requiredWorkers" onChange={onChange} required type="number" value={form.requiredWorkers} />
      </label>
      <label>
        {t("minimumRestHours")}
        <input min="0" name="minRestHours" onChange={onChange} required type="number" value={form.minRestHours} />
      </label>
      <label>
        {t("requiredRole")}
        <select disabled={isLoadingStaffingRoles} name="requiredStaffingRoleId" onChange={onChange} value={form.requiredStaffingRoleId}>
          <option value="">{t("noSpecificRole")}</option>
          {staffingRoles.map((role) => <option key={role.id} value={role.id}>{role.name}</option>)}
        </select>
      </label>
    </>
  );
}
