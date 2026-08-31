import test from "node:test";
import assert from "node:assert/strict";
import { addDays, buildMonthDays, dateKey, monthDistance, monthStart, parseDateOnly, personalShifts, shiftsByDate, startOfWeek } from "./utils/calendarDates.js";

test("month navigation crosses years without overflowing from the last day", () => {
  const december = parseDateOnly("2026-12-31");
  assert.equal(dateKey(monthStart(december, 1)), "2027-01-01");
  assert.equal(dateKey(monthStart(parseDateOnly("2027-03-31"), -1)), "2027-02-01");
  assert.equal(monthDistance(december, parseDateOnly("2027-02-01")), 2);
});

test("monthly grid has six Sunday-first weeks and includes leap day exactly once", () => {
  const days = buildMonthDays(parseDateOnly("2028-02-01"), { startDate: "2028-02-01", endDate: "2028-02-29" });
  assert.equal(days.length, 42);
  assert.equal(days[0].date.getDay(), 0);
  assert.equal(new Set(days.map((day) => day.key)).size, 42);
  assert.equal(days.filter((day) => !day.outsideMonth).length, 29);
  assert.equal(days.filter((day) => !day.outsideSchedule).length, 29);
  assert.equal(days.filter((day) => day.key === "2028-02-29").length, 1);
});

test("partial schedule days are distinguished from padding and unrelated dates", () => {
  const days = buildMonthDays(parseDateOnly("2026-09-01"), { startDate: "2026-09-07", endDate: "2026-09-27" });
  assert.equal(days.filter((day) => !day.outsideSchedule).length, 21);
  assert.equal(days.find((day) => day.key === "2026-09-01").outsideSchedule, true);
  assert.equal(days.find((day) => day.key === "2026-09-27").outsideSchedule, false);
});

test("local calendar dates stay consecutive across daylight saving changes", () => {
  for (const start of ["2026-03-01", "2026-10-01", "2026-11-01"]) {
    const days = buildMonthDays(parseDateOnly(start), { startDate: start, endDate: "2026-12-31" });
    for (let index = 1; index < days.length; index++) {
      assert.equal(days[index].key, dateKey(addDays(days[index - 1].date, 1)));
    }
  }
  assert.equal(dateKey(startOfWeek(parseDateOnly("2026-09-01"))), "2026-08-30");
});

test("grouping sorts shifts by local start date without mutating input", () => {
  const early = { id: 1, startTime: new Date(2026, 8, 1, 8).toISOString() };
  const late = { id: 2, startTime: new Date(2026, 8, 1, 23).toISOString() };
  const next = { id: 3, startTime: new Date(2026, 8, 2, 7).toISOString() };
  const input = [next, late, early];
  const grouped = shiftsByDate(input);
  assert.deepEqual(grouped.get("2026-09-01"), [early, late]);
  assert.deepEqual(grouped.get("2026-09-02"), [next]);
  assert.deepEqual(input, [next, late, early]);
});

test("personal filter uses employee IDs and preserves coworkers and staffing counts", () => {
  const shared = { id: 1, requiredWorkers: 2, assignments: [{ id: 10, employeeId: 7 }, { id: 11, employeeId: 8 }] };
  const other = { id: 2, assignments: [{ id: 12, employeeId: 9 }] };
  assert.deepEqual(personalShifts([shared, other, { id: 3 }], "7"), [shared]);
  assert.equal(shared.assignments.length, 2);
  assert.deepEqual(personalShifts([shared], null), []);
  assert.deepEqual(personalShifts([shared], 999), []);
});
