export function groupShiftsByDate(shifts, formatDate) {
  const groups = new Map();

  [...shifts]
    .sort((firstShift, secondShift) => new Date(firstShift.startTime) - new Date(secondShift.startTime))
    .forEach((shift) => {
      const dateLabel = formatDate(shift.startTime);

      if (!groups.has(dateLabel)) {
        groups.set(dateLabel, []);
      }

      groups.get(dateLabel).push(shift);
    });

  return [...groups.entries()].map(([dateLabel, groupedShifts]) => ({
    dateLabel,
    shifts: groupedShifts,
  }));
}
