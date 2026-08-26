function AppShell({
  availabilityConstraintCount,
  children,
  displayName,
  isManager,
  onLogout,
  role,
  transferRequestCount,
  unreadNotificationCount,
}) {
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
          {!isManager ? (
            <a href="#availability">
              My availability
              {availabilityConstraintCount > 0 ? (
                <span className="nav-count">{availabilityConstraintCount}</span>
              ) : null}
            </a>
          ) : null}
          <a href="#transfer-requests">
            Transfer requests
            {transferRequestCount > 0 ? <span className="nav-count">{transferRequestCount}</span> : null}
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
          <span className="role-badge">{role}</span>
          <button className="secondary-button" onClick={onLogout} type="button">
            Sign out
          </button>
        </header>

        {children}
      </section>
    </main>
  );
}

export default AppShell;
