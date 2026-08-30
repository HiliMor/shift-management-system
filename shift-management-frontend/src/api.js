const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080";

export class ApiError extends Error {
  constructor(message, status, body) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.body = body;
  }
}

async function request(path, options = {}) {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers: {
      "Content-Type": "application/json",
      ...(options.token ? { Authorization: `Bearer ${options.token}` } : {}),
      ...options.headers,
    },
  });

  if (!response.ok) {
    let errorBody = {};

    try {
      errorBody = await response.json();
    } catch {
      errorBody = {};
    }

    const message = errorBody.message || errorBody.error || `Request failed with status ${response.status}`;
    throw new ApiError(message, response.status, errorBody);
  }

  if (response.status === 204) {
    return null;
  }

  return response.json();
}

export function login(username, password) {
  return request("/api/auth/login", {
    method: "POST",
    body: JSON.stringify({ username, password }),
  });
}

export function listMyPublishedSchedules(token) {
  return request("/api/schedules/me/published", { token });
}

export function getMyPublishedScheduleDetails(token, scheduleId) {
  return request(`/api/schedules/me/published/${scheduleId}`, { token });
}

export function listMyManagedTeams(token) {
  return request("/api/teams/me/managed", { token });
}

export function listMyTeamMemberships(token) {
  return request("/api/teams/me/memberships", { token });
}

export function createSchedule(token, schedule) {
  return request("/api/schedules", {
    method: "POST",
    token,
    body: JSON.stringify(schedule),
  });
}

export function deleteSchedule(token, scheduleId) {
  return request(`/api/schedules/${scheduleId}`, {
    method: "DELETE",
    token,
  });
}

export function listManagedDraftSchedules(token) {
  return request("/api/schedules/me/managed/drafts", { token });
}

export function listManagedPublishedSchedules(token) {
  return request("/api/schedules/me/managed/published", { token });
}

export function getManagedPublishedScheduleDetails(token, scheduleId) {
  return request(`/api/schedules/me/managed/published/${scheduleId}`, { token });
}

export function getPublicationReadiness(token, scheduleId) {
  return request(`/api/schedules/${scheduleId}/publication-readiness`, { token });
}

export function publishSchedule(token, scheduleId, confirmUnfilled) {
  return request(`/api/schedules/${scheduleId}/publish`, {
    method: "POST",
    token,
    body: JSON.stringify({ confirmUnfilled }),
  });
}

export function reopenSchedule(token, scheduleId) {
  return request(`/api/schedules/${scheduleId}/reopen`, {
    method: "POST",
    token,
  });
}

export function listStaffingRoles(token, teamId) {
  return request(`/api/teams/${teamId}/staffing-roles`, { token });
}

export function createShift(token, scheduleId, shift) {
  return request(`/api/schedules/${scheduleId}/shifts`, {
    method: "POST",
    token,
    body: JSON.stringify(shift),
  });
}

export function listShifts(token, scheduleId) {
  return request(`/api/schedules/${scheduleId}/shifts`, { token });
}

export function listTeamEmployees(token, teamId) {
  return request(`/api/teams/${teamId}/employees`, { token });
}

export function listScheduleAssignments(token, scheduleId) {
  return request(`/api/schedules/${scheduleId}/assignments`, { token });
}

export function createAssignment(token, assignment) {
  return request("/api/assignments", {
    method: "POST",
    token,
    body: JSON.stringify(assignment),
  });
}

export function deleteAssignment(token, assignmentId) {
  return request(`/api/assignments/${assignmentId}`, {
    method: "DELETE",
    token,
  });
}

export function autoAssignSchedule(token, scheduleId) {
  return request(`/api/schedules/${scheduleId}/auto-assign`, {
    method: "POST",
    token,
  });
}

export function createShiftTemplate(token, teamId, template) {
  return request(`/api/teams/${teamId}/templates`, {
    method: "POST",
    token,
    body: JSON.stringify(template),
  });
}

export function listShiftTemplates(token, teamId) {
  return request(`/api/teams/${teamId}/templates`, { token });
}

export function deleteShiftTemplate(token, templateId) {
  return request(`/api/templates/${templateId}`, {
    method: "DELETE",
    token,
  });
}

export function createTemplateSlot(token, templateId, slot) {
  return request(`/api/templates/${templateId}/slots`, {
    method: "POST",
    token,
    body: JSON.stringify(slot),
  });
}

export function listTemplateSlots(token, templateId) {
  return request(`/api/templates/${templateId}/slots`, { token });
}

export function generateShiftsFromTemplate(token, templateId, scheduleId) {
  return request(`/api/templates/${templateId}/generate`, {
    method: "POST",
    token,
    body: JSON.stringify({ scheduleId: Number(scheduleId) }),
  });
}

export function createAvailabilityConstraint(token, constraint) {
  return request("/api/availability-constraints", {
    method: "POST",
    token,
    body: JSON.stringify(constraint),
  });
}

export function listMyAvailabilityConstraints(token) {
  return request("/api/availability-constraints/me", { token });
}

export function deleteAvailabilityConstraint(token, constraintId) {
  return request(`/api/availability-constraints/${constraintId}`, {
    method: "DELETE",
    token,
  });
}

export function listMyNotifications(token) {
  return request("/api/notifications", { token });
}

export function countMyUnreadNotifications(token) {
  return request("/api/notifications/unread-count", { token });
}

export function markNotificationRead(token, notificationId) {
  return request(`/api/notifications/${notificationId}/read`, {
    method: "POST",
    token,
  });
}

export function createTransferRequest(token, transferRequest) {
  return request("/api/requests/transfers", {
    method: "POST",
    token,
    body: JSON.stringify(transferRequest),
  });
}

export function createSwapRequest(token, swapRequest) {
  return request("/api/requests/swaps", {
    method: "POST",
    token,
    body: JSON.stringify(swapRequest),
  });
}

export function listMyOutgoingTransferRequests(token) {
  return request("/api/requests/me/outgoing", { token });
}

export function listMyIncomingTransferRequests(token) {
  return request("/api/requests/me/incoming", { token });
}

export function listPendingManagerTransferRequests(token) {
  return request("/api/requests/manager/pending", { token });
}

export function approveTransferAsTargetEmployee(token, requestId) {
  return request(`/api/requests/${requestId}/employee-approve`, {
    method: "POST",
    token,
  });
}

export function rejectTransferAsTargetEmployee(token, requestId) {
  return request(`/api/requests/${requestId}/employee-reject`, {
    method: "POST",
    token,
  });
}

export function cancelTransferAsRequester(token, requestId) {
  return request(`/api/requests/${requestId}/cancel`, {
    method: "POST",
    token,
  });
}

export function approveTransferAsManager(token, requestId) {
  return request(`/api/requests/${requestId}/manager-approve`, {
    method: "POST",
    token,
  });
}
