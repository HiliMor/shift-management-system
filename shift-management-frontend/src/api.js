const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080";

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
    throw new Error(message);
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

export function createSchedule(token, schedule) {
  return request("/api/schedules", {
    method: "POST",
    token,
    body: JSON.stringify(schedule),
  });
}
