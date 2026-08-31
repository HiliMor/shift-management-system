export function parseDateOnly(value) {
  return new Date(`${value}T12:00:00`);
}

export function dateKey(date) {
  return [date.getFullYear(), String(date.getMonth() + 1).padStart(2, "0"), String(date.getDate()).padStart(2, "0")].join("-");
}

export function addDays(date, amount) {
  const result = new Date(date);
  result.setDate(result.getDate() + amount);
  return result;
}

export function startOfWeek(date) {
  return addDays(date, -date.getDay());
}

export function monthStart(date, offset = 0) {
  return new Date(date.getFullYear(), date.getMonth() + offset, 1, 12);
}

export function monthDistance(first, last) {
  return (last.getFullYear() - first.getFullYear()) * 12 + last.getMonth() - first.getMonth();
}

export function buildMonthDays(month, schedule) {
  const firstDay = startOfWeek(monthStart(month));
  // Six complete Sunday-first weeks keep navigation from resizing the grid.
  return Array.from({ length: 42 }, (_, index) => {
    const date = addDays(firstDay, index);
    const key = dateKey(date);
    return {
      date,
      key,
      outsideMonth: date.getMonth() !== month.getMonth(),
      outsideSchedule: key < schedule.startDate || key > schedule.endDate,
    };
  });
}

export function shiftsByDate(shifts) {
  const grouped = new Map();
  [...shifts].sort((a, b) => new Date(a.startTime) - new Date(b.startTime)).forEach((shift) => {
    const key = dateKey(new Date(shift.startTime));
    if (!grouped.has(key)) grouped.set(key, []);
    grouped.get(key).push(shift);
  });
  return grouped;
}

export function personalShifts(shifts, employeeId) {
  if (employeeId == null) return [];
  return shifts.filter((shift) => shift.assignments?.some(
    (assignment) => assignment.employeeId != null && String(assignment.employeeId) === String(employeeId),
  ));
}
