import { useEffect, useRef, useState } from "react";
import { deleteShift, getShiftDeletionPreview, updateShift } from "../api.js";
import { confirmDeletion, deletionErrorMessage } from "../confirmDeletion.js";
import { useLanguage } from "../i18n/LanguageContext.jsx";
import { shiftEditForm, shiftUpdatePayload } from "../utils/shiftEditing.js";

export default function useShiftEditing(session, scheduleId, onChanged, onApiError) {
  const { t, language } = useLanguage();
  const [editingShift, setEditingShift] = useState(null);
  const [form, setForm] = useState(null);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");
  const [isBusy, setIsBusy] = useState(false);
  const scope = `${session?.accessToken ?? ""}:${scheduleId ?? ""}`;
  const scopeRef = useRef(scope);
  scopeRef.current = scope;
  const operationRef = useRef(0);

  useEffect(() => {
    setEditingShift(null);
    setForm(null);
    setError("");
    setMessage("");
    setIsBusy(false);
    return () => { operationRef.current += 1; };
  }, [scope]);

  function edit(shift) {
    if (isBusy || String(shift.scheduleId) !== String(scheduleId)) return;
    setEditingShift(shift);
    setForm(shiftEditForm(shift));
    setError("");
    setMessage("");
  }

  function cancel() {
    if (isBusy) return;
    setEditingShift(null);
    setForm(null);
    setError("");
    setMessage("");
  }

  function change(event) {
    const { name, value } = event.target;
    setForm((current) => ({ ...current, [name]: value }));
    setError("");
    setMessage("");
  }

  async function run(action, successKey) {
    if (isBusy || !session?.accessToken || !scheduleId) return;
    const operation = ++operationRef.current;
    const isCurrent = () => scopeRef.current === scope && operationRef.current === operation;
    setIsBusy(true);
    setError("");
    setMessage("");
    try {
      const changed = await action(isCurrent);
      if (!isCurrent() || !changed) return;
      setEditingShift(null);
      setForm(null);
      setMessage(successKey);
      onChanged();
    } catch (failure) {
      if (isCurrent()) {
        onApiError(failure, () => setError(
          failure.body?.code === "STALE_VERSION" ? t("staleShiftEdit") : deletionErrorMessage(failure, t),
        ));
      }
    } finally {
      if (isCurrent()) setIsBusy(false);
    }
  }

  function save(event) {
    event.preventDefault();
    if (!editingShift || String(editingShift.scheduleId) !== String(scheduleId)) return;
    return run(async () => {
      await updateShift(session.accessToken, scheduleId, editingShift.id, shiftUpdatePayload(editingShift, form));
      return true;
    }, "shiftUpdated");
  }

  function remove(shift) {
    if (String(shift.scheduleId) !== String(scheduleId)) return;
    return run((isCurrent) => confirmDeletion({
      loadPreview: () => getShiftDeletionPreview(session.accessToken, scheduleId, shift.id),
      describe: ({ shift: current, assignmentCount }) => {
        const date = (value) => new Date(value).toLocaleString(language === "he" ? "he-IL" : "en-GB");
        return [t("confirmDeleteShift"), `#${current.id}: ${current.description || t("shift")}`,
          `${date(current.startTime)} - ${date(current.endTime)}`,
          `${t("assignments")}: ${assignmentCount}`].join("\n");
      },
      confirm: (text) => isCurrent() && window.confirm(text),
      remove: (revision) => deleteShift(session.accessToken, scheduleId, shift.id, revision),
    }), "shiftDeleted");
  }

  return { editingShift, form, error, message, isBusy, edit, cancel, change, save, remove };
}
