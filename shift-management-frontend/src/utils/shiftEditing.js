export function localDateTime(value) {
  const date = new Date(value);
  const pad = (number) => String(number).padStart(2, "0");
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`;
}

export function shiftEditForm(shift) {
  return {
    startTime: localDateTime(shift.startTime),
    endTime: localDateTime(shift.endTime),
    description: shift.description ?? "",
    requiredWorkers: String(shift.requiredWorkers),
    minRestHours: String(shift.minRestHours),
    requiredStaffingRoleId: shift.requiredStaffingRoleId == null ? "" : String(shift.requiredStaffingRoleId),
  };
}

export function shiftUpdatePayload(shift, form) {
  return {
    // Preserve untouched instants, including subsecond precision and DST ambiguity.
    startTime: form.startTime === localDateTime(shift.startTime) ? shift.startTime : new Date(form.startTime).toISOString(),
    endTime: form.endTime === localDateTime(shift.endTime) ? shift.endTime : new Date(form.endTime).toISOString(),
    description: form.description || null,
    requiredWorkers: Number(form.requiredWorkers),
    minRestHours: Number(form.minRestHours),
    requiredStaffingRoleId: form.requiredStaffingRoleId ? Number(form.requiredStaffingRoleId) : null,
    version: shift.version,
  };
}
