import { useEffect, useMemo, useState } from "react";
import {
  createSchedule,
  getMyPublishedScheduleDetails,
  listMyManagedTeams,
  listMyPublishedSchedules,
  login,
} from "./api.js";

const STORAGE_KEY = "shift-management-session";

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

  const isManager = session?.user?.applicationRole === "MANAGER";

  const displayName = useMemo(() => {
    if (!session?.user) {
      return "";
    }

    return session.user.fullName || session.user.username;
  }, [session]);

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
      .catch((error) => setScheduleError(error.message))
      .finally(() => setIsLoadingSchedules(false));
  }, [session]);

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
        setDetailsError(error.message);
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
      .catch((error) => setManagedTeamsError(error.message))
      .finally(() => setIsLoadingManagedTeams(false));
  }, [isManager, session]);

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
    } catch (error) {
      setScheduleCreationError(error.message);
    } finally {
      setIsCreatingSchedule(false);
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
              <h2>Create schedule</h2>
              <span>{managedTeams.length}</span>
            </div>

            {isLoadingManagedTeams ? <p className="muted">Loading managed teams...</p> : null}
            {managedTeamsError ? <p className="error-message">{managedTeamsError}</p> : null}

            {!isLoadingManagedTeams && !managedTeamsError && managedTeams.length === 0 ? (
              <p className="muted">No managed teams are available for this user.</p>
            ) : null}

            {managedTeams.length > 0 ? (
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
            ) : null}

            {scheduleCreationError ? <p className="error-message">{scheduleCreationError}</p> : null}

            {createdSchedule ? (
              <div className="success-message">
                <strong>Draft schedule #{createdSchedule.id} created</strong>
                <span>
                  {createdSchedule.teamName}: {formatDate(createdSchedule.startDate)} to{" "}
                  {formatDate(createdSchedule.endDate)}
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
