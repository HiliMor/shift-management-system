import { useEffect, useMemo, useState } from "react";
import { listMyPublishedSchedules, login } from "./api.js";

const STORAGE_KEY = "shift-management-session";

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
      return;
    }

    setIsLoadingSchedules(true);
    setScheduleError("");

    listMyPublishedSchedules(session.accessToken)
      .then(setPublishedSchedules)
      .catch((error) => setScheduleError(error.message))
      .finally(() => setIsLoadingSchedules(false));
  }, [session]);

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
              <article className="schedule-row" key={schedule.id}>
                <div>
                  <h3>{schedule.teamName}</h3>
                  <p>
                    {schedule.startDate} to {schedule.endDate}
                  </p>
                </div>
                <span>{schedule.status}</span>
              </article>
            ))}
          </div>
        </section>

        {isManager ? (
          <section className="section-block" id="manager">
            <div className="section-heading">
              <h2>Manager actions</h2>
              <span>Next</span>
            </div>
            <div className="action-grid">
              <button className="secondary-button" type="button">
                Create schedule
              </button>
              <button className="secondary-button" type="button">
                Add shift
              </button>
              <button className="secondary-button" type="button">
                Assign employee
              </button>
            </div>
          </section>
        ) : null}
      </section>
    </main>
  );
}

export default App;
