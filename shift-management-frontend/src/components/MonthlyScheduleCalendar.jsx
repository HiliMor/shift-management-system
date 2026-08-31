import { useMemo, useState } from "react";
import { useLanguage } from "../i18n/LanguageContext.jsx";
import { buildMonthDays, monthDistance, monthStart, parseDateOnly, shiftsByDate } from "../utils/calendarDates.js";
import CalendarShift from "./CalendarShift.jsx";

function MonthlyScheduleCalendar({ schedule, shifts }) {
  const { language, t } = useLanguage();
  const [monthOffset, setMonthOffset] = useState(0);
  const firstMonth = monthStart(parseDateOnly(schedule.startDate));
  const lastOffset = monthDistance(firstMonth, parseDateOnly(schedule.endDate));
  const currentOffset = Math.min(monthOffset, lastOffset);
  const month = monthStart(firstMonth, currentOffset);
  const days = buildMonthDays(month, schedule);
  const grouped = useMemo(() => shiftsByDate(shifts), [shifts]);
  const locale = language === "he" ? "he-IL" : "en-GB";
  const monthLabel = new Intl.DateTimeFormat(locale, { month: "long", year: "numeric" }).format(month);
  const weekdayFormatter = new Intl.DateTimeFormat(locale, { weekday: "short" });
  const dayFormatter = new Intl.DateTimeFormat(locale, { day: "numeric", month: "long", year: "numeric" });
  const timeFormatter = new Intl.DateTimeFormat(locale, { hour: "2-digit", minute: "2-digit" });

  return (
    <div className="calendar-view">
      <div className="calendar-toolbar">
        <h3 aria-live="polite">{monthLabel}</h3>
        <div className="calendar-navigation">
          <button className="secondary-button compact-button" disabled={currentOffset === 0}
            onClick={() => setMonthOffset(currentOffset - 1)} type="button">{t("previousMonth")}</button>
          <button className="secondary-button compact-button" disabled={currentOffset === lastOffset}
            onClick={() => setMonthOffset(currentOffset + 1)} type="button">{t("nextMonth")}</button>
        </div>
      </div>
      <div className="monthly-calendar-scroll" tabIndex={0} role="region" aria-label={`${t("monthlyCalendar")}: ${monthLabel}`}>
        <div className="monthly-calendar">
          {days.slice(0, 7).map((day) => (
            <div className="month-weekday" key={`weekday-${day.key}`}>{weekdayFormatter.format(day.date)}</div>
          ))}
          {days.map((day) => (
            <section className={`calendar-day month-day${day.outsideMonth || day.outsideSchedule ? " calendar-day-outside" : ""}`}
              key={day.key} aria-label={dayFormatter.format(day.date)}>
              <header className="calendar-day-heading">
                <time dateTime={day.key}>{day.date.getDate()}</time>
              </header>
              <div className="calendar-day-content">
                {!day.outsideMonth && !day.outsideSchedule ? (grouped.get(day.key) ?? []).map((shift) => (
                  <CalendarShift compact key={shift.id} shift={shift} timeFormatter={timeFormatter} />
                )) : null}
              </div>
            </section>
          ))}
        </div>
      </div>
    </div>
  );
}

export default MonthlyScheduleCalendar;
