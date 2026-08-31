import { useEffect, useMemo, useState } from "react";
import { useLanguage } from "../i18n/LanguageContext.jsx";
import CalendarShift from "./CalendarShift.jsx";
import { addDays, dateKey, parseDateOnly, shiftsByDate as groupCalendarShifts, startOfWeek } from "../utils/calendarDates.js";

const DAY_IN_MILLISECONDS = 24 * 60 * 60 * 1000;

function utcDateValue(date) {
  return Date.UTC(date.getFullYear(), date.getMonth(), date.getDate());
}

function WeeklyScheduleCalendar({ onSelectShift, schedule, selectedShiftId, shifts }) {
  const { language, t } = useLanguage();
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
  const shiftsByDate = useMemo(() => groupCalendarShifts(shifts), [shifts]);

  useEffect(() => {
    setWeekStart(firstWeekStart);
  }, [firstWeekStart, schedule.id]);

  const currentWeekNumber = Math.floor((utcDateValue(weekStart) - utcDateValue(firstWeekStart)) / (7 * DAY_IN_MILLISECONDS)) + 1;
  const totalWeeks = Math.floor((utcDateValue(lastWeekStart) - utcDateValue(firstWeekStart)) / (7 * DAY_IN_MILLISECONDS)) + 1;
  const canMoveToPreviousWeek = utcDateValue(weekStart) > utcDateValue(firstWeekStart);
  const canMoveToNextWeek = utcDateValue(weekStart) < utcDateValue(lastWeekStart);

  function renderShift(shift) {
    return (
      <CalendarShift
        key={shift.id}
        onSelectShift={onSelectShift}
        selectedShiftId={selectedShiftId}
        shift={shift}
        timeFormatter={timeFormatter}
      />
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
