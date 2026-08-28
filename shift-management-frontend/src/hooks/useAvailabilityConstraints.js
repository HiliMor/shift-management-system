import { useEffect, useState } from "react";

import {
  createAvailabilityConstraint,
  deleteAvailabilityConstraint,
  listMyAvailabilityConstraints,
} from "../api.js";

const emptyAvailabilityForm = {
  allDay: false,
  date: "",
  startTime: "",
  endTime: "",
  reason: "",
};

function toIsoStringFromLocalDateTime(value) {
  return value ? new Date(value).toISOString() : "";
}

function toIsoStringFromLocalDate(value, dayOffset = 0) {
  if (!value) {
    return "";
  }

  const date = new Date(`${value}T00:00`);
  date.setDate(date.getDate() + dayOffset);
  return date.toISOString();
}

function useAvailabilityConstraints(session, enabled, onApiError) {
  const [availabilityConstraints, setAvailabilityConstraints] = useState([]);
  const [availabilityForm, setAvailabilityForm] = useState(emptyAvailabilityForm);
  const [availabilityError, setAvailabilityError] = useState("");
  const [availabilityActionError, setAvailabilityActionError] = useState("");
  const [availabilityActionMessage, setAvailabilityActionMessage] = useState("");
  const [isLoadingAvailability, setIsLoadingAvailability] = useState(false);
  const [isCreatingAvailability, setIsCreatingAvailability] = useState(false);
  const [deletingAvailabilityId, setDeletingAvailabilityId] = useState(null);
  const [availabilityRefreshKey, setAvailabilityRefreshKey] = useState(0);

  useEffect(() => {
    if (!session?.accessToken || !enabled) {
      resetAvailabilityConstraints();
      return;
    }

    setIsLoadingAvailability(true);
    setAvailabilityError("");

    listMyAvailabilityConstraints(session.accessToken)
      .then(setAvailabilityConstraints)
      .catch((error) => onApiError(error, setAvailabilityError))
      .finally(() => setIsLoadingAvailability(false));
  }, [availabilityRefreshKey, enabled, session]);

  function handleAvailabilityFormChange(event) {
    const { checked, name, type, value } = event.target;

    setAvailabilityForm((current) => ({
      ...current,
      [name]: type === "checkbox" ? checked : value,
    }));
  }

  async function submitAvailabilityConstraint(event) {
    event.preventDefault();
    setIsCreatingAvailability(true);
    setAvailabilityActionError("");
    setAvailabilityActionMessage("");

    try {
      const startTime = availabilityForm.allDay
        ? toIsoStringFromLocalDate(availabilityForm.date)
        : toIsoStringFromLocalDateTime(availabilityForm.startTime);
      const endTime = availabilityForm.allDay
        ? toIsoStringFromLocalDate(availabilityForm.date, 1)
        : toIsoStringFromLocalDateTime(availabilityForm.endTime);

      await createAvailabilityConstraint(session.accessToken, {
        startTime,
        endTime,
        reason: availabilityForm.reason || null,
      });
      setAvailabilityForm(emptyAvailabilityForm);
      setAvailabilityActionMessage("availabilityCreated");
      refreshAvailabilityConstraints();
    } catch (error) {
      onApiError(error, setAvailabilityActionError);
    } finally {
      setIsCreatingAvailability(false);
    }
  }

  async function removeAvailabilityConstraint(constraintId) {
    setDeletingAvailabilityId(constraintId);
    setAvailabilityActionError("");
    setAvailabilityActionMessage("");

    try {
      await deleteAvailabilityConstraint(session.accessToken, constraintId);
      setAvailabilityConstraints((current) =>
        current.filter((constraint) => constraint.id !== constraintId),
      );
      setAvailabilityActionMessage("availabilityDeleted");
    } catch (error) {
      onApiError(error, setAvailabilityActionError);
    } finally {
      setDeletingAvailabilityId(null);
    }
  }

  function refreshAvailabilityConstraints() {
    setAvailabilityRefreshKey((current) => current + 1);
  }

  function resetAvailabilityConstraints() {
    setAvailabilityConstraints([]);
    setAvailabilityForm(emptyAvailabilityForm);
    setAvailabilityError("");
    setAvailabilityActionError("");
    setAvailabilityActionMessage("");
    setIsLoadingAvailability(false);
    setIsCreatingAvailability(false);
    setDeletingAvailabilityId(null);
    setAvailabilityRefreshKey(0);
  }

  return {
    availabilityActionError,
    availabilityActionMessage,
    availabilityConstraints,
    availabilityError,
    availabilityForm,
    deletingAvailabilityId,
    handleAvailabilityFormChange,
    isCreatingAvailability,
    isLoadingAvailability,
    refreshAvailabilityConstraints,
    removeAvailabilityConstraint,
    resetAvailabilityConstraints,
    submitAvailabilityConstraint,
  };
}

export default useAvailabilityConstraints;
