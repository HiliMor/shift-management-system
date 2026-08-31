import assert from "node:assert/strict";
import test from "node:test";
import { localDateTime, shiftEditForm, shiftUpdatePayload } from "./utils/shiftEditing.js";

const shift = { id: 5, scheduleId: 2, version: 3,
  startTime: "2026-09-01T06:00:15.123Z", endTime: "2026-09-01T14:00:15.123Z",
  description: null, requiredWorkers: 2, minRestHours: 8, requiredStaffingRoleId: null };

test("editing starts with existing values and retains the viewed version", () => {
  const form = shiftEditForm(shift);
  assert.equal(form.description, "");
  assert.equal(form.requiredWorkers, "2");
  assert.equal(form.requiredStaffingRoleId, "");
  const body = shiftUpdatePayload(shift, { ...form, description: "Updated", requiredWorkers: "3" });
  assert.equal(body.version, 3);
  assert.equal(body.description, "Updated");
  assert.equal(body.requiredWorkers, 3);
  assert.equal(body.requiredStaffingRoleId, null);
  assert.equal(body.startTime, shift.startTime);
  assert.equal(body.endTime, shift.endTime);
  assert.equal(shift.description, null);
});

test("changed local times become instants and role changes use numeric IDs", () => {
  const form = { ...shiftEditForm(shift), startTime: "2026-09-02T10:30:00", requiredStaffingRoleId: "9" };
  const body = shiftUpdatePayload(shift, form);
  assert.equal(body.startTime, new Date(form.startTime).toISOString());
  assert.equal(body.requiredStaffingRoleId, 9);
  assert.equal(body.minRestHours, 8);
  assert.equal(localDateTime(body.startTime), form.startTime);
});

test("untouched ambiguous DST instants retain their original offsets and precision", () => {
  for (const startTime of ["2026-11-01T01:30:00.123-04:00", "2026-11-01T01:30:00.123-05:00"]) {
    const current = { ...shift, startTime, requiredStaffingRoleId: 9 };
    const form = shiftEditForm(current);
    assert.equal(form.requiredStaffingRoleId, "9");
    assert.equal(shiftUpdatePayload(current, form).startTime, startTime);
    assert.equal(shiftUpdatePayload(current, { ...form, requiredStaffingRoleId: "" }).requiredStaffingRoleId, null);
  }
});
