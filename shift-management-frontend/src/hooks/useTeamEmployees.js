import { useEffect, useRef, useState } from "react";
import { createTeamEmployee, listStaffingRoles, listTeamEmployees } from "../api.js";
import { useLanguage } from "../i18n/LanguageContext.jsx";

const emptyForm = { username: "", password: "", fullName: "", email: "", staffingRoleIds: [] };

export default function useTeamEmployees(token, teamId, onCreated, onApiError) {
  const { t } = useLanguage();
  const [form, setForm] = useState(emptyForm);
  const [employees, setEmployees] = useState([]);
  const [roles, setRoles] = useState([]);
  const [isLoading, setIsLoading] = useState(false);
  const [isSaving, setIsSaving] = useState(false);
  const [loadError, setLoadError] = useState("");
  const [error, setError] = useState("");
  const [created, setCreated] = useState(null);
  const [refreshKey, setRefreshKey] = useState(0);
  const scope = `${token}:${teamId}`;
  const scopeRef = useRef(scope);
  scopeRef.current = scope;
  const mounted = useRef(false);
  useEffect(() => { mounted.current = true; return () => { mounted.current = false; }; }, []);

  useEffect(() => {
    setForm(emptyForm);
    setError("");
    setCreated(null);
    setIsSaving(false);
  }, [scope]);

  useEffect(() => {
    let active = true;
    setEmployees([]);
    setRoles([]);
    setLoadError("");
    if (!token || !teamId) { setIsLoading(false); return; }
    setIsLoading(true);
    Promise.all([listTeamEmployees(token, teamId), listStaffingRoles(token, teamId)])
      .then(([nextEmployees, nextRoles]) => {
        if (!active) return;
        setEmployees(nextEmployees);
        setRoles(nextRoles);
      })
      .catch((failure) => { if (active) onApiError(failure, setLoadError); })
      .finally(() => { if (active) setIsLoading(false); });
    return () => { active = false; };
  }, [token, teamId, refreshKey]);

  function change(event) {
    const { name, value } = event.target;
    setForm((current) => ({ ...current, [name]: value }));
    setError("");
    setCreated(null);
  }

  function toggleRole(roleId) {
    setForm((current) => ({ ...current, staffingRoleIds: current.staffingRoleIds.includes(roleId)
      ? current.staffingRoleIds.filter((id) => id !== roleId) : [...current.staffingRoleIds, roleId] }));
    setError("");
    setCreated(null);
  }

  async function submit(event) {
    event.preventDefault();
    if (isSaving || isLoading || loadError || !teamId) return;
    if (new TextEncoder().encode(form.password).length > 72) {
      setError(t("employeePasswordTooLong"));
      return;
    }
    const isCurrent = () => mounted.current && scopeRef.current === scope;
    setIsSaving(true);
    setError("");
    setCreated(null);
    try {
      const employee = await createTeamEmployee(token, teamId, {
        ...form, username: form.username.trim(), fullName: form.fullName.trim(), email: form.email.trim() || null,
      });
      if (!isCurrent()) return;
      setForm(emptyForm);
      setCreated(employee);
      setRefreshKey((current) => current + 1);
      onCreated();
    } catch (failure) {
      if (isCurrent()) onApiError(failure, () => setError(failure.message === "Username is already in use"
        ? t("employeeUsernameTaken") : failure.message));
    } finally {
      if (isCurrent()) setIsSaving(false);
    }
  }

  return { form, employees, roles, isLoading, isSaving, loadError, error, created, change, toggleRole, submit,
    refresh: () => setRefreshKey((current) => current + 1) };
}
