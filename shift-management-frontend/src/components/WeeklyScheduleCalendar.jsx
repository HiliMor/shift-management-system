import { useEffect, useMemo, useState } from "react";
import { useLanguage } from "../i18n/LanguageContext.jsx";

const DAY_IN_MILLISECONDS = 24 * 60 * 60 * 1000;

function parseDateOnly(value) {
  return new Date(`${value}T12:00:00`);
}

function dateKey(date) {
  return [date.getFullYear(), String(date.getMonth() + 1).padStart(2, "0"), String(date.getDate()).padStart(2, "0")].join("-");
}

function shiftDateKey(value) {
  const date = new Date(value);

  return dateKey(date);
}

function startOfWeek(date) {
  const result = new Date(date);
  const day = result.getDay();
  const daysFromMonday = day === 0 ? 6 : day - 1;

  result.setDate(result.getDate() - daysFromMonday);
  return result;
}

function addDays(date, amount) {
  const result = new Date(date);
  result.setDate(result.getDate() + amount);
  return result;
}

function utcDateValue(date) {
  return Date.UTC(date.getFullYear(), date.getMonth(), date.getDate());
}

function WeeklyScheduleCalendar({ onSelectShift, schedule, selectedShiftId, shifts }) {
  const { language, t, translateDomainValue } = useLanguage();
  const scheduleStart = useMemo(() => parseDateOnly(schedule.startDate), [schedule.startDate]);
  const scheduleEnd = useMemo(() => parseDateOnly(schedule.endDate), [schedule.endDate]);
  const firstWeekStart = useMemo(() => startOfWeek(scheduleStart), [scheduleStart]);
  const lastWeekStart = useMemo(() => startOfWeek(scheduleEnd), [scheduleEnd]);
  const [weekStart, setWeekStart] = useState(firstWeekStart);

  const timeFormatter = useMemo(
    () => new Intl.DateTimeFormat(language === "he" ? "he-IL" : "en-GB", {
      hour: "2-digit",
      minute: "2-digit",
    }),
    [language],
  );
  const dayFormatter = useMemo(
    () => new Intl.DateTimeFormat(language === "he" ? "he-IL" : "en-GB", {
      day: "numeric",
      month: "short",
      weekday: "short",
    }),
    [language],
  );
  const weekDays = useMemo(
    () => Array.from({ length: 7 }, (_, index) => {
      const date = addDays(weekStart, index);

      return {
        date,
        key: dateKey(date),
        label: dayFormatter.format(date),
        outsideSchedule: date < scheduleStart || date > scheduleEnd,
      };
    }),
    [dayFormatter, scheduleEnd, scheduleStart, weekStart],
  );
  const shiftsByDate = useMemo(() => {
    const groupedShifts = new Map();

    [...shifts]
      .sort((firstShift, secondShift) => new Date(firstShift.startTime) - new Date(secondShift.startTime))
      .forEach((shift) => {
        const key = shiftDateKey(shift.startTime);

        if (!groupedShifts.has(key)) {
          groupedShifts.set(key, []);
        }

        groupedShifts.get(key).push(shift);
      });

    return groupedShifts;
  }, [shifts]);

  useEffect(() => {
    setWeekStart(firstWeekStart);
  }, [firstWeekStart, schedule.id]);

  const currentWeekNumber = Math.floor((utcDateValue(weekStart) - utcDateValue(firstWeekStart)) / (7 * DAY_IN_MILLISECONDS)) + 1;
  const totalWeeks = Math.floor((utcDateValue(lastWeekStart) - utcDateValue(firstWeekStart)) / (7 * DAY_IN_MILLISECONDS)) + 1;
  const canMoveToPreviousWeek = utcDateValue(weekStart) > utcDateValue(firstWeekStart);
  const canMoveToNextWeek = utcDateValue(weekStart) < utcDateValue(lastWeekStart);

  function formatTime(value) {
    return timeFormatter.format(new Date(value));
  }

  function renderShift(shift) {
    const assignments = shift.assignments ?? [];
    const openSlots = Math.max(shift.requiredWorkers - assignments.length, 0);
    const isSelected = selectedShiftId?.toString() === shift.id.toString();
    const shiftClassName = [
      "calendar-shift",
      onSelectShift ? "calendar-shift-selectable" : "",
      isSelected ? "calendar-shift-selected" : "",
    ].filter(Boolean).join(" ");
    const ShiftContainer = onSelectShift ? "button" : "article";

    return (
      <ShiftContainer
        aria-pressed={onSelectShift ? isSelected : undefined}
        className={shiftClassName}
        key={shift.id}
        onClick={onSelectShift ? () => onSelectShift(shift.id) : undefined}
        type={onSelectShift ? "button" : undefined}
      >
        <div className="calendar-shift-heading">
          <strong>{shift.description || t("shift")}</strong>
          <span>{formatTime(shift.startTime)} {t("dateRangeSeparator")} {formatTime(shift.endTime)}</span>
        </div>
        {shift.requiredStaffingRoleName ? (
          <span className="calendar-shift-role">{translateDomainValue(shift.requiredStaffingRoleName)}</span>
        ) : null}
        <div className="calendar-shift-meta">
          <span>{assignments.length}/{shift.requiredWorkers} {t("workers")}</span>
          {openSlots > 0 ? <span className="calendar-open-slots">{openSlots} {t("openSlots")}</span> : null}
        </div>
        {assignments.length > 0 ? (
          <div className="calendar-assignees">
            {assignments.map((assignment) => (
              <span key={assignment.id}>{assignment.employeeFullName || assignment.employeeUsername}</span>
            ))}
          </div>
        ) : (
          <span className="calendar-unassigned">{t("noEmployeesAssigned")}</span>
        )}
      </ShiftContainer>
    );
  }

  return (
    <div className="calendar-view">
      <div className="calendar-toolbar">
        <div>
          <h3>{t("weeklyCalendar")}</h3>
          <p className="muted">{t("week")} {currentWeekNumber} {t("of")} {totalWeeks}</p>
        </div>
        <div className="calendar-navigation">
          <button
            className="secondary-button compact-button"
            disabled={!canMoveToPreviousWeek}
            onClick={() => setWeekStart((current) => addDays(current, -7))}
            type="button"
          >
            {t("previousWeek")}
          </button>
          <span className="calendar-range">
            {dayFormatter.format(weekDays[0].date)} {t("dateRangeSeparator")} {dayFormatter.format(weekDays[6].date)}
          </span>
          <button
            className="secondary-button compact-button"
            disabled={!canMoveToNextWeek}
            onClick={() => setWeekStart((current) => addDays(current, 7))}
            type="button"
          >
            {t("nextWeek")}
          </button>
        </div>
      </div>

      <div className="weekly-calendar" aria-label={t("weeklyCalendar")}>
        {weekDays.map((day) => {
          const dayShifts = shiftsByDate.get(day.key) ?? [];

          return (
            <section className={`calendar-day${day.outsideSchedule ? " calendar-day-outside" : ""}`} key={day.key}>
              <header className="calendar-day-heading">
                <strong>{day.label}</strong>
                <span>{dayShifts.length} {t("shifts")}</span>
              </header>
              <div className="calendar-day-content">
                {dayShifts.length > 0 ? dayShifts.map(renderShift) : <p className="muted">{t("noShiftsOnDay")}</p>}
              </div>
            </section>
          );
        })}
      </div>
    </div>
  );
}

export default WeeklyScheduleCalendar;
