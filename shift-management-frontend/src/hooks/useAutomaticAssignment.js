import { useEffect, useState } from "react";

import { autoAssignSchedule } from "../api.js";

const emptyAutomaticAssignmentForm = {
  scheduleId: "",
};

function useAutomaticAssignment(
  session,
  enabled,
  managedDraftSchedules,
  onAssignmentsChanged,
  onPublicationReadinessChanged,
  onApiError,
) {
  const [automaticAssignmentForm, setAutomaticAssignmentForm] = useState(emptyAutomaticAssignmentForm);
  const [automaticAssignmentReport, setAutomaticAssignmentReport] = useState(null);
  const [automaticAssignmentError, setAutomaticAssignmentError] = useState("");
  const [automaticAssignmentMessage, setAutomaticAssignmentMessage] = useState("");
  const [isRunningAutomaticAssignment, setIsRunningAutomaticAssignment] = useState(false);

  useEffect(() => {
    if (!enabled) {
      setAutomaticAssignmentForm(emptyAutomaticAssignmentForm);
      return;
    }

    setAutomaticAssignmentForm((current) => {
      const currentDraftExists = managedDraftSchedules.some(
        (schedule) => schedule.id.toString() === current.scheduleId,
      );

      return {
        scheduleId: currentDraftExists ? current.scheduleId : managedDraftSchedules[0]?.id?.toString() || "",
      };
    });
  }, [enabled, managedDraftSchedules]);

  function handleAutomaticAssignmentFormChange(event) {
    const { name, value } = event.target;

    setAutomaticAssignmentForm((current) => ({
      ...current,
      [name]: value,
    }));
    setAutomaticAssignmentReport(null);
    setAutomaticAssignmentError("");
    setAutomaticAssignmentMessage("");
  }

  async function submitAutomaticAssignment(event) {
    event.preventDefault();
    setIsRunningAutomaticAssignment(true);
    setAutomaticAssignmentError("");
    setAutomaticAssignmentMessage("");

    try {
      const report = await autoAssignSchedule(session.accessToken, automaticAssignmentForm.scheduleId);

      setAutomaticAssignmentReport(report);
      setAutomaticAssignmentMessage({
        key: "automaticAssignmentCreated",
        count: report.assignmentsCreated,
      });
      onAssignmentsChanged(report.scheduleId);
      onPublicationReadinessChanged();
    } catch (error) {
      onApiError(error, setAutomaticAssignmentError);
    } finally {
      setIsRunningAutomaticAssignment(false);
    }
  }

  function resetAutomaticAssignment() {
    setAutomaticAssignmentForm(emptyAutomaticAssignmentForm);
    setAutomaticAssignmentReport(null);
    setAutomaticAssignmentError("");
    setAutomaticAssignmentMessage("");
    setIsRunningAutomaticAssignment(false);
  }

  return {
    automaticAssignmentError,
    automaticAssignmentForm,
    automaticAssignmentMessage,
    automaticAssignmentReport,
    handleAutomaticAssignmentFormChange,
    isRunningAutomaticAssignment,
    resetAutomaticAssignment,
    submitAutomaticAssignment,
  };
}

export default useAutomaticAssignment;
