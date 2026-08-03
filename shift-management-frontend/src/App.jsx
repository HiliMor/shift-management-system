import { useEffect, useMemo, useState } from "react";
import {
  countMyUnreadNotifications,
  createAssignment,
  createSchedule,
  createShift,
  getMyPublishedScheduleDetails,
  listScheduleAssignments,
  listManagedDraftSchedules,
  listMyManagedTeams,
  listMyPublishedSchedules,
  listStaffingRoles,
  listShifts,
  listTeamEmployees,
  login,
  listMyNotifications,
  markNotificationRead,
} from "./api.js";

const STORAGE_KEY = "shift-management-session";
const SESSION_EXPIRED_MESSAGE = "Session expired. Please sign in again.";

const dateFormatter = new Intl.DateTimeFormat("en-GB", {
  day: "2-digit",
  month: "short",
  year: "numeric",
});

const dateTimeFormatter = new Intl.DateTimeFormat("en-GB", {
  day: "2-digit",
  hour: "2-digit",
  minute: "2-digit",
  month: "short",
  year: "numeric",
});

function formatDate(value) {
  return value ? dateFormatter.format(new Date(value)) : "Not set";
}

function formatDateTime(value) {
  return value ? dateTimeFormatter.format(new Date(value)) : "Not set";
}

function toIsoStringFromLocalDateTime(value) {
  return value ? new Date(value).toISOString() : "";
}

function loadStoredSession() {
  const stored = localStorage.getItem(STORAGE_KEY);

  if (!stored) {
    return null;
  }

  try {
    return JSON.parse(stored);
  } catch {
    localStorage.removeItem(STORAGE_KEY);
    return null;
  }
}

function App() {
  const [session, setSession] = useState(loadStoredSession);
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [loginError, setLoginError] = useState("");
  const [isLoggingIn, setIsLoggingIn] = useState(false);
  const [publishedSchedules, setPublishedSchedules] = useState([]);
  const [scheduleError, setScheduleError] = useState("");
  const [isLoadingSchedules, setIsLoadingSchedules] = useState(false);
  const [selectedScheduleId, setSelectedScheduleId] = useState(null);
  const [selectedScheduleDetails, setSelectedScheduleDetails] = useState(null);
  const [detailsError, setDetailsError] = useState("");
  const [isLoadingDetails, setIsLoadingDetails] = useState(false);
  const [managedTeams, setManagedTeams] = useState([]);
  const [isLoadingManagedTeams, setIsLoadingManagedTeams] = useState(false);
  const [managedTeamsError, setManagedTeamsError] = useState("");
  const [scheduleForm, setScheduleForm] = useState({ teamId: "", startDate: "", endDate: "" });
  const [createdSchedule, setCreatedSchedule] = useState(null);
  const [scheduleCreationError, setScheduleCreationError] = useState("");
  const [isCreatingSchedule, setIsCreatingSchedule] = useState(false);
  const [managedDraftSchedules, setManagedDraftSchedules] = useState([]);
  const [isLoadingDraftSchedules, setIsLoadingDraftSchedules] = useState(false);
  const [draftSchedulesError, setDraftSchedulesError] = useState("");
  const [draftScheduleRefreshKey, setDraftScheduleRefreshKey] = useState(0);
  const [staffingRoles, setStaffingRoles] = useState([]);
  const [isLoadingStaffingRoles, setIsLoadingStaffingRoles] = useState(false);
  const [staffingRolesError, setStaffingRolesError] = useState("");
  const [shiftForm, setShiftForm] = useState({
    scheduleId: "",
    startTime: "",
    endTime: "",
    description: "",
    requiredWorkers: "1",
    minRestHours: "8",
    requiredStaffingRoleId: "",
  });
  const [createdShift, setCreatedShift] = useState(null);
  const [shiftCreationError, setShiftCreationError] = useState("");
  const [isCreatingShift, setIsCreatingShift] = useState(false);
  const [assignmentForm, setAssignmentForm] = useState({
    scheduleId: "",
    shiftId: "",
    employeeId: "",
  });
  const [assignmentShifts, setAssignmentShifts] = useState([]);
  const [isLoadingAssignmentShifts, setIsLoadingAssignmentShifts] = useState(false);
  const [assignmentShiftsError, setAssignmentShiftsError] = useState("");
  const [teamEmployees, setTeamEmployees] = useState([]);
  const [isLoadingTeamEmployees, setIsLoadingTeamEmployees] = useState(false);
  const [teamEmployeesError, setTeamEmployeesError] = useState("");
  const [scheduleAssignments, setScheduleAssignments] = useState([]);
  const [isLoadingScheduleAssignments, setIsLoadingScheduleAssignments] = useState(false);
  const [scheduleAssignmentsError, setScheduleAssignmentsError] = useState("");
  const [assignmentRefreshKey, setAssignmentRefreshKey] = useState(0);
  const [createdAssignment, setCreatedAssignment] = useState(null);
  const [assignmentCreationError, setAssignmentCreationError] = useState("");
  const [isCreatingAssignment, setIsCreatingAssignment] = useState(false);
  const [notifications, setNotifications] = useState([]);
  const [unreadNotificationCount, setUnreadNotificationCount] = useState(0);
  const [notificationsError, setNotificationsError] = useState("");
  const [isLoadingNotifications, setIsLoadingNotifications] = useState(false);
  const [markingNotificationId, setMarkingNotificationId] = useState(null);
  const [notificationRefreshKey, setNotificationRefreshKey] = useState(0);

  const isManager = session?.user?.applicationRole === "MANAGER";

  const displayName = useMemo(() => {
    if (!session?.user) {
      return "";
    }

    return session.user.fullName || session.user.username;
  }, [session]);

  const selectedDraftSchedule = useMemo(
    () => managedDraftSchedules.find((schedule) => schedule.id.toString() === shiftForm.scheduleId) ?? null,
    [managedDraftSchedules, shiftForm.scheduleId],
  );

  const selectedAssignmentSchedule = useMemo(
    () => managedDraftSchedules.find((schedule) => schedule.id.toString() === assignmentForm.scheduleId) ?? null,
    [assignmentForm.scheduleId, managedDraftSchedules],
  );

  const assignmentShiftMap = useMemo(
    () => new Map(assignmentShifts.map((shift) => [shift.id, shift])),
    [assignmentShifts],
  );

  function clearAuthenticatedState() {
    setPublishedSchedules([]);
    setScheduleError("");
    setSelectedScheduleId(null);
    setSelectedScheduleDetails(null);
    setDetailsError("");
    setManagedTeams([]);
    setManagedTeamsError("");
    setScheduleForm({ teamId: "", startDate: "", endDate: "" });
    setCreatedSchedule(null);
    setScheduleCreationError("");
    setManagedDraftSchedules([]);
    setDraftSchedulesError("");
    setStaffingRoles([]);
    setStaffingRolesError("");
    setShiftForm({
      scheduleId: "",
      startTime: "",
      endTime: "",
      description: "",
      requiredWorkers: "1",
      minRestHours: "8",
      requiredStaffingRoleId: "",
    });
    setCreatedShift(null);
    setShiftCreationError("");
    setAssignmentForm({ scheduleId: "", shiftId: "", employeeId: "" });
    setAssignmentShifts([]);
    setAssignmentShiftsError("");
    setTeamEmployees([]);
    setTeamEmployeesError("");
    setScheduleAssignments([]);
    setScheduleAssignmentsError("");
    setCreatedAssignment(null);
    setAssignmentCreationError("");
    setNotifications([]);
    setUnreadNotificationCount(0);
    setNotificationsError("");
    setMarkingNotificationId(null);
    setNotificationRefreshKey(0);
  }

  function expireSession() {
    localStorage.removeItem(STORAGE_KEY);
    setSession(null);
    clearAuthenticatedState();
    setLoginError(SESSION_EXPIRED_MESSAGE);
  }

  function handleApiError(error, setError) {
    if (error.status === 401) {
      expireSession();
      return;
    }

    setError(error.message);
  }

  useEffect(() => {
    if (!session?.accessToken) {
      setPublishedSchedules([]);
      setSelectedScheduleId(null);
      return;
    }

    setIsLoadingSchedules(true);
    setScheduleError("");

    listMyPublishedSchedules(session.accessToken)
      .then(setPublishedSchedules)
      .catch((error) => handleApiError(error, setScheduleError))
      .finally(() => setIsLoadingSchedules(false));
  }, [session]);

  useEffect(() => {
    if (!session?.accessToken) {
      setNotifications([]);
      setUnreadNotificationCount(0);
      setNotificationsError("");
      return;
    }

    setIsLoadingNotifications(true);
    setNotificationsError("");

    Promise.all([
      listMyNotifications(session.accessToken),
      countMyUnreadNotifications(session.accessToken),
    ])
      .then(([notificationList, unreadCount]) => {
        setNotifications(notificationList);
        setUnreadNotificationCount(unreadCount.unreadCount);
      })
      .catch((error) => handleApiError(error, setNotificationsError))
      .finally(() => setIsLoadingNotifications(false));
  }, [notificationRefreshKey, session]);

  useEffect(() => {
    if (!session?.accessToken || !selectedScheduleId) {
      setSelectedScheduleDetails(null);
      setDetailsError("");
      return;
    }

    setIsLoadingDetails(true);
    setDetailsError("");

    getMyPublishedScheduleDetails(session.accessToken, selectedScheduleId)
      .then(setSelectedScheduleDetails)
      .catch((error) => {
        setSelectedScheduleDetails(null);
        handleApiError(error, setDetailsError);
      })
      .finally(() => setIsLoadingDetails(false));
  }, [selectedScheduleId, session]);

  useEffect(() => {
    if (!session?.accessToken || !isManager) {
      setManagedTeams([]);
      setManagedTeamsError("");
      setScheduleForm({ teamId: "", startDate: "", endDate: "" });
      return;
    }

    setIsLoadingManagedTeams(true);
    setManagedTeamsError("");

    listMyManagedTeams(session.accessToken)
      .then((teams) => {
        setManagedTeams(teams);
        setScheduleForm((current) => ({
          ...current,
          teamId: current.teamId || teams[0]?.id?.toString() || "",
        }));
      })
      .catch((error) => handleApiError(error, setManagedTeamsError))
      .finally(() => setIsLoadingManagedTeams(false));
  }, [isManager, session]);

  useEffect(() => {
    if (!session?.accessToken || !isManager) {
      setManagedDraftSchedules([]);
      setDraftSchedulesError("");
      setShiftForm({
        scheduleId: "",
        startTime: "",
        endTime: "",
        description: "",
        requiredWorkers: "1",
        minRestHours: "8",
        requiredStaffingRoleId: "",
      });
      setAssignmentForm({ scheduleId: "", shiftId: "", employeeId: "" });
      setAssignmentShifts([]);
      setAssignmentShiftsError("");
      setTeamEmployees([]);
      setTeamEmployeesError("");
      setScheduleAssignments([]);
      setScheduleAssignmentsError("");
      return;
    }

    setIsLoadingDraftSchedules(true);
    setDraftSchedulesError("");

    listManagedDraftSchedules(session.accessToken)
      .then((schedules) => {
        setManagedDraftSchedules(schedules);
        setShiftForm((current) => ({
          ...current,
          scheduleId: current.scheduleId || schedules[0]?.id?.toString() || "",
        }));
        setAssignmentForm((current) => ({
          ...current,
          scheduleId: current.scheduleId || schedules[0]?.id?.toString() || "",
        }));
      })
      .catch((error) => handleApiError(error, setDraftSchedulesError))
      .finally(() => setIsLoadingDraftSchedules(false));
  }, [draftScheduleRefreshKey, isManager, session]);

  useEffect(() => {
    if (!session?.accessToken || !selectedDraftSchedule) {
      setStaffingRoles([]);
      setStaffingRolesError("");
      return;
    }

    setIsLoadingStaffingRoles(true);
    setStaffingRolesError("");

    listStaffingRoles(session.accessToken, selectedDraftSchedule.teamId)
      .then(setStaffingRoles)
      .catch((error) => handleApiError(error, setStaffingRolesError))
      .finally(() => setIsLoadingStaffingRoles(false));
  }, [selectedDraftSchedule, session]);

  useEffect(() => {
    if (!session?.accessToken || !assignmentForm.scheduleId) {
      setAssignmentShifts([]);
      setAssignmentShiftsError("");
      setAssignmentForm((current) => ({ ...current, shiftId: "" }));
      return;
    }

    setIsLoadingAssignmentShifts(true);
    setAssignmentShiftsError("");

    listShifts(session.accessToken, assignmentForm.scheduleId)
      .then((shifts) => {
        setAssignmentShifts(shifts);
        setAssignmentForm((current) => {
          const currentShiftExists = shifts.some((shift) => shift.id.toString() === current.shiftId);

          return {
            ...current,
            shiftId: currentShiftExists ? current.shiftId : shifts[0]?.id?.toString() || "",
          };
        });
      })
      .catch((error) => handleApiError(error, setAssignmentShiftsError))
      .finally(() => setIsLoadingAssignmentShifts(false));
  }, [assignmentForm.scheduleId, assignmentRefreshKey, session]);

  useEffect(() => {
    if (!session?.accessToken || !selectedAssignmentSchedule) {
      setTeamEmployees([]);
      setTeamEmployeesError("");
      setAssignmentForm((current) => ({ ...current, employeeId: "" }));
      return;
    }

    setIsLoadingTeamEmployees(true);
    setTeamEmployeesError("");

    listTeamEmployees(session.accessToken, selectedAssignmentSchedule.teamId)
      .then((employees) => {
        setTeamEmployees(employees);
        setAssignmentForm((current) => {
          const currentEmployeeExists = employees.some((employee) => employee.id.toString() === current.employeeId);

          return {
            ...current,
            employeeId: currentEmployeeExists ? current.employeeId : employees[0]?.id?.toString() || "",
          };
        });
      })
      .catch((error) => handleApiError(error, setTeamEmployeesError))
      .finally(() => setIsLoadingTeamEmployees(false));
  }, [selectedAssignmentSchedule, session]);

  useEffect(() => {
    if (!session?.accessToken || !assignmentForm.scheduleId) {
      setScheduleAssignments([]);
      setScheduleAssignmentsError("");
      return;
    }

    setIsLoadingScheduleAssignments(true);
    setScheduleAssignmentsError("");

    listScheduleAssignments(session.accessToken, assignmentForm.scheduleId)
      .then(setScheduleAssignments)
      .catch((error) => handleApiError(error, setScheduleAssignmentsError))
      .finally(() => setIsLoadingScheduleAssignments(false));
  }, [assignmentForm.scheduleId, assignmentRefreshKey, session]);

  async function handleLogin(event) {
    event.preventDefault();
    setIsLoggingIn(true);
    setLoginError("");

    try {
      const response = await login(username, password);
      localStorage.setItem(STORAGE_KEY, JSON.stringify(response));
      setSession(response);
      setPassword("");
    } catch (error) {
      setLoginError(error.message);
    } finally {
      setIsLoggingIn(false);
    }
  }

  function handleLogout() {
    localStorage.removeItem(STORAGE_KEY);
    setSession(null);
    clearAuthenticatedState();
    setLoginError("");
  }

  function handleScheduleFormChange(event) {
    const { name, value } = event.target;

    setScheduleForm((current) => ({
      ...current,
      [name]: value,
    }));
  }

  async function handleCreateSchedule(event) {
    event.preventDefault();
    setIsCreatingSchedule(true);
    setScheduleCreationError("");
    setCreatedSchedule(null);

    try {
      const response = await createSchedule(session.accessToken, {
        teamId: Number(scheduleForm.teamId),
        startDate: scheduleForm.startDate,
        endDate: scheduleForm.endDate,
      });
      setCreatedSchedule(response);
      setShiftForm((current) => ({
        ...current,
        scheduleId: response.id.toString(),
        minRestHours: managedTeams
          .find((team) => team.id === response.teamId)
          ?.defaultMinRestHours?.toString() || current.minRestHours,
      }));
      setAssignmentForm((current) => ({
        ...current,
        scheduleId: response.id.toString(),
        shiftId: "",
      }));
      setDraftScheduleRefreshKey((current) => current + 1);
    } catch (error) {
      handleApiError(error, setScheduleCreationError);
    } finally {
      setIsCreatingSchedule(false);
    }
  }

  function handleShiftFormChange(event) {
    const { name, value } = event.target;

    setShiftForm((current) => ({
      ...current,
      [name]: value,
      ...(name === "scheduleId" ? { requiredStaffingRoleId: "" } : {}),
    }));
  }

  async function handleCreateShift(event) {
    event.preventDefault();
    setIsCreatingShift(true);
    setShiftCreationError("");
    setCreatedShift(null);

    try {
      const response = await createShift(session.accessToken, shiftForm.scheduleId, {
        startTime: toIsoStringFromLocalDateTime(shiftForm.startTime),
        endTime: toIsoStringFromLocalDateTime(shiftForm.endTime),
        description: shiftForm.description || null,
        requiredWorkers: Number(shiftForm.requiredWorkers),
        minRestHours: Number(shiftForm.minRestHours),
        requiredStaffingRoleId: shiftForm.requiredStaffingRoleId ? Number(shiftForm.requiredStaffingRoleId) : null,
      });
      setCreatedShift(response);
      setShiftForm((current) => ({
        ...current,
        startTime: "",
        endTime: "",
        description: "",
      }));
      setAssignmentForm((current) => ({
        ...current,
        scheduleId: response.scheduleId.toString(),
        shiftId: response.id.toString(),
      }));
      setAssignmentRefreshKey((current) => current + 1);
    } catch (error) {
      handleApiError(error, setShiftCreationError);
    } finally {
      setIsCreatingShift(false);
    }
  }

  function handleAssignmentFormChange(event) {
    const { name, value } = event.target;

    setAssignmentForm((current) => ({
      ...current,
      [name]: value,
      ...(name === "scheduleId" ? { shiftId: "", employeeId: "" } : {}),
    }));
  }

  async function handleCreateAssignment(event) {
    event.preventDefault();
    setIsCreatingAssignment(true);
    setAssignmentCreationError("");
    setCreatedAssignment(null);

    try {
      const response = await createAssignment(session.accessToken, {
        shiftId: Number(assignmentForm.shiftId),
        employeeId: Number(assignmentForm.employeeId),
      });
      setCreatedAssignment(response);
      setAssignmentRefreshKey((current) => current + 1);
    } catch (error) {
      handleApiError(error, setAssignmentCreationError);
    } finally {
      setIsCreatingAssignment(false);
    }
  }

  async function handleMarkNotificationRead(notificationId) {
    setMarkingNotificationId(notificationId);
    setNotificationsError("");

    try {
      const updatedNotification = await markNotificationRead(session.accessToken, notificationId);
      const wasUnread = notifications.some(
        (notification) => notification.id === updatedNotification.id && !notification.read,
      );

      setNotifications((current) =>
        current.map((notification) =>
          notification.id === updatedNotification.id ? updatedNotification : notification,
        ),
      );
      if (wasUnread) {
        setUnreadNotificationCount((current) => Math.max(0, current - 1));
      }
    } catch (error) {
      handleApiError(error, setNotificationsError);
    } finally {
      setMarkingNotificationId(null);
    }
  }

  if (!session) {
    return (
      <main className="auth-layout">
        <section className="auth-panel" aria-labelledby="login-title">
          <p className="eyebrow">Shift Management</p>
          <h1 id="login-title">Sign in</h1>

          <form className="login-form" onSubmit={handleLogin}>
            <label>
              Username
              <input
                autoComplete="username"
                name="username"
                onChange={(event) => setUsername(event.target.value)}
                required
                type="text"
                value={username}
              />
            </label>

            <label>
              Password
              <input
                autoComplete="current-password"
                name="password"
                onChange={(event) => setPassword(event.target.value)}
                required
                type="password"
                value={password}
              />
            </label>

            {loginError ? <p className="error-message">{loginError}</p> : null}

            <button disabled={isLoggingIn} type="submit">
              {isLoggingIn ? "Signing in..." : "Sign in"}
            </button>
          </form>
        </section>
      </main>
    );
  }

  return (
    <main className="app-shell">
      <aside className="sidebar">
        <div>
          <p className="eyebrow">Shift Management</p>
          <h1>{isManager ? "Manager workspace" : "Employee workspace"}</h1>
        </div>

        <nav aria-label="Main navigation">
          <a className="active-link" href="#schedules">
            Published schedules
          </a>
          <a href="#notifications">
            Notifications
            {unreadNotificationCount > 0 ? <span className="nav-count">{unreadNotificationCount}</span> : null}
          </a>
          {isManager ? <a href="#manager">Manager actions</a> : null}
        </nav>
      </aside>

      <section className="content-area">
        <header className="topbar">
          <div>
            <p className="eyebrow">Signed in</p>
            <h2>{displayName}</h2>
          </div>
          <span className="role-badge">{session.user.applicationRole}</span>
          <button className="secondary-button" onClick={handleLogout} type="button">
            Sign out
          </button>
        </header>

        <section className="section-block" id="notifications">
          <div className="section-heading">
            <h2>Notifications</h2>
            <div className="section-actions">
              <span>{unreadNotificationCount} unread</span>
              <button
                className="secondary-button compact-button"
                disabled={isLoadingNotifications}
                onClick={() => setNotificationRefreshKey((current) => current + 1)}
                type="button"
              >
                Refresh
              </button>
            </div>
          </div>

          {isLoadingNotifications ? <p className="muted">Loading notifications...</p> : null}
          {notificationsError ? <p className="error-message">{notificationsError}</p> : null}

          {!isLoadingNotifications && !notificationsError && notifications.length === 0 ? (
            <p className="muted">No notifications are available for this user.</p>
          ) : null}

          {notifications.length > 0 ? (
            <div className="notification-list">
              {notifications.map((notification) => (
                <article
                  className={notification.read ? "notification-row" : "notification-row unread-notification"}
                  key={notification.id}
                >
                  <div>
                    <div className="notification-title-row">
                      <h3>{notification.title}</h3>
                      {!notification.read ? <span>Unread</span> : null}
                    </div>
                    <p>{notification.message}</p>
                    <p className="notification-meta">
                      {notification.type} - {formatDateTime(notification.createdAt)}
                    </p>
                  </div>

                  {!notification.read ? (
                    <button
                      className="secondary-button compact-button"
                      disabled={markingNotificationId === notification.id}
                      onClick={() => handleMarkNotificationRead(notification.id)}
                      type="button"
                    >
                      {markingNotificationId === notification.id ? "Updating..." : "Mark as read"}
                    </button>
                  ) : (
                    <span className="read-state">Read</span>
                  )}
                </article>
              ))}
            </div>
          ) : null}
        </section>

        <section className="section-block" id="schedules">
          <div className="section-heading">
            <h2>Published schedules</h2>
            <span>{publishedSchedules.length}</span>
          </div>

          {isLoadingSchedules ? <p className="muted">Loading schedules...</p> : null}
          {scheduleError ? <p className="error-message">{scheduleError}</p> : null}

          {!isLoadingSchedules && !scheduleError && publishedSchedules.length === 0 ? (
            <p className="muted">No published schedules are available for this user.</p>
          ) : null}

          <div className="schedule-list">
            {publishedSchedules.map((schedule) => (
              <article
                className={schedule.id === selectedScheduleId ? "schedule-row selected-row" : "schedule-row"}
                key={schedule.id}
              >
                <div>
                  <h3>{schedule.teamName}</h3>
                  <p>
                    {formatDate(schedule.startDate)} to {formatDate(schedule.endDate)}
                  </p>
                </div>
                <span>{schedule.status}</span>
                <button
                  className="secondary-button compact-button"
                  onClick={() => setSelectedScheduleId(schedule.id)}
                  type="button"
                >
                  View details
                </button>
              </article>
            ))}
          </div>
        </section>

        <section className="section-block" id="schedule-details">
          <div className="section-heading">
            <h2>Schedule details</h2>
            <span>{selectedScheduleDetails?.shifts?.length ?? 0}</span>
          </div>

          {!selectedScheduleId ? <p className="muted">Select a published schedule to view its shifts.</p> : null}
          {isLoadingDetails ? <p className="muted">Loading schedule details...</p> : null}
          {detailsError ? <p className="error-message">{detailsError}</p> : null}

          {selectedScheduleDetails && !isLoadingDetails ? (
            <div className="details-stack">
              <div className="details-summary">
                <div>
                  <p className="eyebrow">Team</p>
                  <strong>{selectedScheduleDetails.schedule.teamName}</strong>
                </div>
                <div>
                  <p className="eyebrow">Dates</p>
                  <strong>
                    {formatDate(selectedScheduleDetails.schedule.startDate)} to{" "}
                    {formatDate(selectedScheduleDetails.schedule.endDate)}
                  </strong>
                </div>
                <div>
                  <p className="eyebrow">Publication</p>
                  <strong>#{selectedScheduleDetails.schedule.publicationNumber}</strong>
                </div>
              </div>

              {selectedScheduleDetails.shifts.length === 0 ? (
                <p className="muted">This published schedule has no shifts.</p>
              ) : null}

              <div className="shift-list">
                {selectedScheduleDetails.shifts.map((shift) => (
                  <article className="shift-row" key={shift.id}>
                    <div className="shift-main">
                      <div>
                        <h3>{shift.description || "Shift"}</h3>
                        <p>
                          {formatDateTime(shift.startTime)} to {formatDateTime(shift.endTime)}
                        </p>
                      </div>
                      <div className="shift-meta">
                        <span>{shift.requiredWorkers} required</span>
                        {shift.requiredStaffingRoleName ? <span>{shift.requiredStaffingRoleName}</span> : null}
                      </div>
                    </div>

                    <div className="assignment-list">
                      {shift.assignments.length === 0 ? (
                        <p className="muted">No employees assigned yet.</p>
                      ) : (
                        shift.assignments.map((assignment) => (
                          <div className="assignment-row" key={assignment.id}>
                            <strong>{assignment.employeeFullName || assignment.employeeUsername}</strong>
                            <span>{assignment.employeeUsername}</span>
                          </div>
                        ))
                      )}
                    </div>
                  </article>
                ))}
              </div>
            </div>
          ) : null}
        </section>

        {isManager ? (
          <section className="section-block" id="manager">
            <div className="section-heading">
              <h2>Manager actions</h2>
              <span>{managedTeams.length}</span>
            </div>

            {isLoadingManagedTeams ? <p className="muted">Loading managed teams...</p> : null}
            {managedTeamsError ? <p className="error-message">{managedTeamsError}</p> : null}

            {!isLoadingManagedTeams && !managedTeamsError && managedTeams.length === 0 ? (
              <p className="muted">No managed teams are available for this user.</p>
            ) : null}

            {managedTeams.length > 0 ? (
              <div className="manager-stack">
                <section className="manager-panel">
                  <h3>Create draft schedule</h3>
                  <form className="manager-form" onSubmit={handleCreateSchedule}>
                    <label>
                      Team
                      <select
                        name="teamId"
                        onChange={handleScheduleFormChange}
                        required
                        value={scheduleForm.teamId}
                      >
                        {managedTeams.map((team) => (
                          <option key={team.id} value={team.id}>
                            {team.name}
                          </option>
                        ))}
                      </select>
                    </label>

                    <label>
                      Start date
                      <input
                        name="startDate"
                        onChange={handleScheduleFormChange}
                        required
                        type="date"
                        value={scheduleForm.startDate}
                      />
                    </label>

                    <label>
                      End date
                      <input
                        name="endDate"
                        onChange={handleScheduleFormChange}
                        required
                        type="date"
                        value={scheduleForm.endDate}
                      />
                    </label>

                    <button disabled={isCreatingSchedule} type="submit">
                      {isCreatingSchedule ? "Creating..." : "Create draft schedule"}
                    </button>
                  </form>
                </section>

                <section className="manager-panel">
                  <h3>Create shift</h3>

                  {isLoadingDraftSchedules ? <p className="muted">Loading draft schedules...</p> : null}
                  {draftSchedulesError ? <p className="error-message">{draftSchedulesError}</p> : null}
                  {staffingRolesError ? <p className="error-message">{staffingRolesError}</p> : null}

                  {!isLoadingDraftSchedules && !draftSchedulesError && managedDraftSchedules.length === 0 ? (
                    <p className="muted">Create a draft schedule before adding shifts.</p>
                  ) : null}

                  {managedDraftSchedules.length > 0 ? (
                    <form className="shift-form" onSubmit={handleCreateShift}>
                      <label>
                        Draft schedule
                        <select
                          name="scheduleId"
                          onChange={handleShiftFormChange}
                          required
                          value={shiftForm.scheduleId}
                        >
                          {managedDraftSchedules.map((schedule) => (
                            <option key={schedule.id} value={schedule.id}>
                              #{schedule.id} - {schedule.teamName}, {formatDate(schedule.startDate)} to{" "}
                              {formatDate(schedule.endDate)}
                            </option>
                          ))}
                        </select>
                      </label>

                      <label>
                        Start time
                        <input
                          name="startTime"
                          onChange={handleShiftFormChange}
                          required
                          type="datetime-local"
                          value={shiftForm.startTime}
                        />
                      </label>

                      <label>
                        End time
                        <input
                          name="endTime"
                          onChange={handleShiftFormChange}
                          required
                          type="datetime-local"
                          value={shiftForm.endTime}
                        />
                      </label>

                      <label>
                        Description
                        <input
                          maxLength="500"
                          name="description"
                          onChange={handleShiftFormChange}
                          type="text"
                          value={shiftForm.description}
                        />
                      </label>

                      <label>
                        Required workers
                        <input
                          min="1"
                          name="requiredWorkers"
                          onChange={handleShiftFormChange}
                          required
                          type="number"
                          value={shiftForm.requiredWorkers}
                        />
                      </label>

                      <label>
                        Minimum rest hours
                        <input
                          min="0"
                          name="minRestHours"
                          onChange={handleShiftFormChange}
                          required
                          type="number"
                          value={shiftForm.minRestHours}
                        />
                      </label>

                      <label>
                        Required role
                        <select
                          disabled={isLoadingStaffingRoles}
                          name="requiredStaffingRoleId"
                          onChange={handleShiftFormChange}
                          value={shiftForm.requiredStaffingRoleId}
                        >
                          <option value="">No specific role</option>
                          {staffingRoles.map((role) => (
                            <option key={role.id} value={role.id}>
                              {role.name}
                            </option>
                          ))}
                        </select>
                      </label>

                      <button disabled={isCreatingShift} type="submit">
                        {isCreatingShift ? "Creating..." : "Create shift"}
                      </button>
                    </form>
                  ) : null}
                </section>

                <section className="manager-panel">
                  <h3>Assign employee</h3>

                  {assignmentShiftsError ? <p className="error-message">{assignmentShiftsError}</p> : null}
                  {teamEmployeesError ? <p className="error-message">{teamEmployeesError}</p> : null}
                  {scheduleAssignmentsError ? <p className="error-message">{scheduleAssignmentsError}</p> : null}

                  {!isLoadingDraftSchedules && !draftSchedulesError && managedDraftSchedules.length === 0 ? (
                    <p className="muted">Create a draft schedule before assigning employees.</p>
                  ) : null}

                  {managedDraftSchedules.length > 0 ? (
                    <form className="assignment-form" onSubmit={handleCreateAssignment}>
                      <label>
                        Draft schedule
                        <select
                          name="scheduleId"
                          onChange={handleAssignmentFormChange}
                          required
                          value={assignmentForm.scheduleId}
                        >
                          {managedDraftSchedules.map((schedule) => (
                            <option key={schedule.id} value={schedule.id}>
                              #{schedule.id} - {schedule.teamName}, {formatDate(schedule.startDate)} to{" "}
                              {formatDate(schedule.endDate)}
                            </option>
                          ))}
                        </select>
                      </label>

                      <label>
                        Shift
                        <select
                          disabled={isLoadingAssignmentShifts || assignmentShifts.length === 0}
                          name="shiftId"
                          onChange={handleAssignmentFormChange}
                          required
                          value={assignmentForm.shiftId}
                        >
                          {assignmentShifts.map((shift) => (
                            <option key={shift.id} value={shift.id}>
                              #{shift.id} - {shift.description || "Shift"}, {formatDateTime(shift.startTime)}
                            </option>
                          ))}
                        </select>
                      </label>

                      <label>
                        Employee
                        <select
                          disabled={isLoadingTeamEmployees || teamEmployees.length === 0}
                          name="employeeId"
                          onChange={handleAssignmentFormChange}
                          required
                          value={assignmentForm.employeeId}
                        >
                          {teamEmployees.map((employee) => (
                            <option key={employee.id} value={employee.id}>
                              {employee.fullName || employee.username}
                            </option>
                          ))}
                        </select>
                      </label>

                      <button
                        disabled={
                          isCreatingAssignment ||
                          isLoadingAssignmentShifts ||
                          isLoadingTeamEmployees ||
                          assignmentShifts.length === 0 ||
                          teamEmployees.length === 0
                        }
                        type="submit"
                      >
                        {isCreatingAssignment ? "Assigning..." : "Assign employee"}
                      </button>
                    </form>
                  ) : null}

                  {isLoadingAssignmentShifts ? <p className="muted">Loading shifts...</p> : null}
                  {isLoadingTeamEmployees ? <p className="muted">Loading employees...</p> : null}

                  {!isLoadingAssignmentShifts &&
                  !assignmentShiftsError &&
                  managedDraftSchedules.length > 0 &&
                  assignmentShifts.length === 0 ? (
                    <p className="muted">Add a shift before assigning employees.</p>
                  ) : null}

                  {!isLoadingTeamEmployees &&
                  !teamEmployeesError &&
                  selectedAssignmentSchedule &&
                  teamEmployees.length === 0 ? (
                    <p className="muted">No active employees are available for this team.</p>
                  ) : null}

                  <div className="assignment-panel-list">
                    <h4>Current assignments</h4>
                    {isLoadingScheduleAssignments ? <p className="muted">Loading assignments...</p> : null}

                    {!isLoadingScheduleAssignments &&
                    !scheduleAssignmentsError &&
                    assignmentForm.scheduleId &&
                    scheduleAssignments.length === 0 ? (
                      <p className="muted">No employees assigned in this draft schedule yet.</p>
                    ) : null}

                    {scheduleAssignments.map((assignment) => {
                      const assignedShift = assignmentShiftMap.get(assignment.shiftId);

                      return (
                        <div className="assignment-row" key={assignment.id}>
                          <strong>{assignment.employeeFullName || assignment.employeeUsername}</strong>
                          <span>
                            {assignedShift
                              ? `#${assignedShift.id} - ${assignedShift.description || "Shift"}, ${formatDateTime(
                                  assignedShift.startTime,
                                )}`
                              : `Shift #${assignment.shiftId}`}
                          </span>
                        </div>
                      );
                    })}
                  </div>
                </section>
              </div>
            ) : null}

            {scheduleCreationError ? <p className="error-message">{scheduleCreationError}</p> : null}
            {shiftCreationError ? <p className="error-message">{shiftCreationError}</p> : null}
            {assignmentCreationError ? <p className="error-message">{assignmentCreationError}</p> : null}

            {createdSchedule ? (
              <div className="success-message">
                <strong>Draft schedule #{createdSchedule.id} created</strong>
                <span>
                  {createdSchedule.teamName}: {formatDate(createdSchedule.startDate)} to{" "}
                  {formatDate(createdSchedule.endDate)}
                </span>
              </div>
            ) : null}

            {createdShift ? (
              <div className="success-message">
                <strong>Shift #{createdShift.id} created</strong>
                <span>
                  {formatDateTime(createdShift.startTime)} to {formatDateTime(createdShift.endTime)}
                </span>
              </div>
            ) : null}

            {createdAssignment ? (
              <div className="success-message">
                <strong>Assignment #{createdAssignment.id} created</strong>
                <span>
                  {createdAssignment.employeeFullName || createdAssignment.employeeUsername} assigned to shift #
                  {createdAssignment.shiftId}
                </span>
              </div>
            ) : null}
          </section>
        ) : null}
      </section>
    </main>
  );
}

export default App;
