import { useLanguage } from "../i18n/LanguageContext.jsx";

function CalendarShift({ compact = false, onSelectShift, selectedShiftId, shift, timeFormatter }) {
  const { t, translateDomainValue } = useLanguage();
  const assignments = shift.assignments ?? [];
  const openSlots = Math.max(shift.requiredWorkers - assignments.length, 0);
  const isSelected = String(selectedShiftId) === String(shift.id);
  const className = ["calendar-shift", onSelectShift ? "calendar-shift-selectable" : "",
    isSelected ? "calendar-shift-selected" : "", compact ? "calendar-shift-compact" : ""].filter(Boolean).join(" ");
  const Container = onSelectShift ? "button" : "article";

  return (
    <Container
      aria-pressed={onSelectShift ? isSelected : undefined}
      className={className}
      onClick={onSelectShift ? () => onSelectShift(shift.id) : undefined}
      type={onSelectShift ? "button" : undefined}
    >
      <div className="calendar-shift-heading">
        <strong>{shift.description || t("shift")}</strong>
        <span>{timeFormatter.format(new Date(shift.startTime))} {t("dateRangeSeparator")} {timeFormatter.format(new Date(shift.endTime))}</span>
      </div>
      {!compact && shift.requiredStaffingRoleName ? (
        <span className="calendar-shift-role">{translateDomainValue(shift.requiredStaffingRoleName)}</span>
      ) : null}
      {!compact ? (
        <div className="calendar-shift-meta">
          <span>{assignments.length}/{shift.requiredWorkers} {t("workers")}</span>
          {openSlots > 0 ? <span className="calendar-open-slots">{openSlots} {t("openSlots")}</span> : null}
        </div>
      ) : null}
      {assignments.length > 0 ? (
        <div className="calendar-assignees">
          {assignments.map((assignment) => (
            <span key={assignment.id}>{assignment.employeeFullName || assignment.employeeUsername}</span>
          ))}
        </div>
      ) : <span className="calendar-unassigned">{t("noEmployeesAssigned")}</span>}
    </Container>
  );
}

export default CalendarShift;
