import LanguageSelector from "./LanguageSelector.jsx";
import { useLanguage } from "../i18n/LanguageContext.jsx";

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
  const { t } = useLanguage();

  return (
    <main className="app-shell">
      <aside className="sidebar">
        <div>
          <p className="eyebrow">{t("appName")}</p>
          <h1>{isManager ? t("managerWorkspace") : t("employeeWorkspace")}</h1>
        </div>

        <nav aria-label={t("mainNavigation")}>
          <a className="active-link" href="#schedules">
            {t("publishedSchedules")}
          </a>
          {!isManager ? (
            <a href="#availability">
              {t("myAvailability")}
              {availabilityConstraintCount > 0 ? (
                <span className="nav-count">{availabilityConstraintCount}</span>
              ) : null}
            </a>
          ) : null}
          <a href="#transfer-requests">
            {t("transferRequests")}
            {transferRequestCount > 0 ? <span className="nav-count">{transferRequestCount}</span> : null}
          </a>
          <a href="#notifications">
            {t("notifications")}
            {unreadNotificationCount > 0 ? <span className="nav-count">{unreadNotificationCount}</span> : null}
          </a>
          {isManager ? <a href="#manager">{t("scheduleWorkflow")}</a> : null}
        </nav>
      </aside>

      <section className="content-area">
        <header className="topbar">
          <div>
            <p className="eyebrow">{t("signedIn")}</p>
            <h2>{displayName}</h2>
          </div>
          <span className="role-badge">{role === "MANAGER" ? t("manager") : t("employee")}</span>
          <LanguageSelector />
          <button className="secondary-button" onClick={onLogout} type="button">
            {t("signOut")}
          </button>
        </header>

        {children}
      </section>
    </main>
  );
}

export default AppShell;
