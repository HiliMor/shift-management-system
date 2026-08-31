import { useState } from "react";
import useTeamEmployees from "../hooks/useTeamEmployees.js";
import { useLanguage } from "../i18n/LanguageContext.jsx";

export default function TeamEmployeesSection({ token, teams, teamsError, isLoadingTeams, onCreated, onApiError }) {
  const { t } = useLanguage();
  const [selectedTeamId, setSelectedTeamId] = useState("");
  const teamId = teams.some((team) => String(team.id) === selectedTeamId) ? selectedTeamId : String(teams[0]?.id ?? "");
  const state = useTeamEmployees(token, teamId, onCreated, onApiError);
  return (
    <section className="section-block" id="team-employees">
      <div className="section-heading"><h2>{t("teamEmployees")}</h2>
        <button className="secondary-button compact-button" type="button" onClick={state.refresh}
          disabled={!teamId || state.isLoading || state.isSaving}>{t("refresh")}</button>
      </div>
      {isLoadingTeams ? <p className="muted">{t("loadingManagedTeams")}</p> : null}
      {teamsError ? <p className="error-message" role="alert">{teamsError}</p> : null}
      {!isLoadingTeams && !teamsError && !teamId ? <p className="muted">{t("noManagedTeams")}</p> : null}
      {teamId ? <>
        <label>{t("team")}<select value={teamId} disabled={state.isSaving} onChange={(event) => setSelectedTeamId(event.target.value)}>
          {teams.map((team) => <option key={team.id} value={team.id}>{team.name}</option>)}
        </select></label>
        {state.loadError ? <p className="error-message" role="alert">{state.loadError}</p> : null}
        {state.error ? <p className="error-message" role="alert">{state.error}</p> : null}
        {state.created ? <p className="success-message" role="status">{t("employeeCreated")}: {state.created.fullName} ({state.created.username})</p> : null}
        {state.isLoading ? <p className="muted">{t("loadingEmployees")}</p> : null}
        <form className="employee-create-form" onSubmit={state.submit}>
          <h3>{t("createEmployee")}</h3>
          <fieldset className="shift-form" disabled={state.isSaving || state.isLoading || !!state.loadError}>
            <label>{t("employeeFullName")}<input name="fullName" required maxLength="200" value={state.form.fullName} onChange={state.change} /></label>
            <label>{t("username")}<input name="username" autoComplete="off" required minLength="3" maxLength="100"
              pattern="[A-Za-z0-9][A-Za-z0-9._\-]{2,99}" value={state.form.username} onChange={state.change} /></label>
            <label>{t("password")}<input name="password" autoComplete="new-password" required minLength="8" maxLength="72"
              type="password" value={state.form.password} onChange={state.change} /></label>
            <label>{t("employeeEmail")}<input name="email" type="email" maxLength="255" value={state.form.email} onChange={state.change} /></label>
            <fieldset className="employee-role-options"><legend>{t("employeeRolesOptional")}</legend>
              {state.roles.length === 0 ? <p className="muted">{t("noStaffingRoles")}</p> : state.roles.map((role) => (
                <label key={role.id}><input type="checkbox" checked={state.form.staffingRoleIds.includes(role.id)} onChange={() => state.toggleRole(role.id)} />{role.name}</label>
              ))}
            </fieldset>
            <div className="section-actions"><button type="submit">{state.isSaving ? t("creating") : t("createEmployee")}</button></div>
          </fieldset>
        </form>
        {!state.isLoading && !state.loadError ? <div className="managed-shift-list">
          {state.employees.map((employee) => <div className="managed-shift-row" key={employee.id}>
            <div><strong>{employee.fullName}</strong><p>{employee.username}</p></div>
            <span>{employee.staffingRoleNames.join(", ") || t("noStaffingRoles")}</span>
          </div>)}
          {state.employees.length === 0 ? <p className="muted">{t("noActiveEmployees")}</p> : null}
        </div> : null}
      </> : null}
    </section>
  );
}
