import { useEffect, useState } from "react";

import {
  getPublicationReadiness,
  listManagedPublishedSchedules,
  publishSchedule,
  reopenSchedule,
} from "../api.js";

const emptyPublicationForm = {
  scheduleId: "",
  confirmUnfilled: false,
};

function useSchedulePublication(
  session,
  enabled,
  managedDraftSchedules,
  selectedDraftScheduleId,
  onDraftSchedulesChanged,
  onApiError,
) {
  const [managedPublishedSchedules, setManagedPublishedSchedules] = useState([]);
  const [publicationForm, setPublicationForm] = useState(emptyPublicationForm);
  const [publicationReadiness, setPublicationReadiness] = useState(null);
  const [publicationError, setPublicationError] = useState("");
  const [publicationActionError, setPublicationActionError] = useState("");
  const [publicationActionMessage, setPublicationActionMessage] = useState("");
  const [isLoadingPublicationReadiness, setIsLoadingPublicationReadiness] = useState(false);
  const [isPublishingSchedule, setIsPublishingSchedule] = useState(false);
  const [isLoadingManagedPublishedSchedules, setIsLoadingManagedPublishedSchedules] = useState(false);
  const [managedPublishedSchedulesError, setManagedPublishedSchedulesError] = useState("");
  const [reopeningScheduleId, setReopeningScheduleId] = useState(null);
  const [publishedScheduleRefreshKey, setPublishedScheduleRefreshKey] = useState(0);
  const [publicationReadinessRefreshKey, setPublicationReadinessRefreshKey] = useState(0);

  useEffect(() => {
    if (!session?.accessToken || !enabled) {
      resetSchedulePublication();
      return;
    }

    setIsLoadingManagedPublishedSchedules(true);
    setManagedPublishedSchedulesError("");

    listManagedPublishedSchedules(session.accessToken)
      .then(setManagedPublishedSchedules)
      .catch((error) => onApiError(error, setManagedPublishedSchedulesError))
      .finally(() => setIsLoadingManagedPublishedSchedules(false));
  }, [enabled, publishedScheduleRefreshKey, session]);

  useEffect(() => {
    if (!enabled) {
      setPublicationForm(emptyPublicationForm);
      return;
    }

    setPublicationForm((current) => {
      const currentDraftExists = managedDraftSchedules.some(
        (schedule) => schedule.id.toString() === current.scheduleId,
      );
      const selectedDraftExists = managedDraftSchedules.some(
        (schedule) => schedule.id.toString() === selectedDraftScheduleId,
      );

      return {
        ...current,
        scheduleId: selectedDraftExists
          ? selectedDraftScheduleId
          : currentDraftExists
            ? current.scheduleId
            : managedDraftSchedules[0]?.id?.toString() || "",
        confirmUnfilled: selectedDraftExists || currentDraftExists ? current.confirmUnfilled : false,
      };
    });
  }, [enabled, managedDraftSchedules, selectedDraftScheduleId]);

  useEffect(() => {
    if (!enabled) {
      return;
    }

    setPublicationReadiness(null);
    setPublicationError("");
  }, [enabled, selectedDraftScheduleId]);

  useEffect(() => {
    if (!session?.accessToken || !enabled || !publicationForm.scheduleId) {
      setPublicationReadiness(null);
      setPublicationError("");
      return;
    }

    setIsLoadingPublicationReadiness(true);
    setPublicationError("");

    getPublicationReadiness(session.accessToken, publicationForm.scheduleId)
      .then(setPublicationReadiness)
      .catch((error) => {
        setPublicationReadiness(null);
        onApiError(error, setPublicationError);
      })
      .finally(() => setIsLoadingPublicationReadiness(false));
  }, [enabled, publicationForm.scheduleId, publicationReadinessRefreshKey, session]);

  function handlePublicationFormChange(event) {
    const { checked, name, type, value } = event.target;

    setPublicationForm((current) => ({
      ...current,
      [name]: type === "checkbox" ? checked : value,
    }));
  }

  async function submitPublishSchedule(event) {
    event.preventDefault();
    setIsPublishingSchedule(true);
    setPublicationActionError("");
    setPublicationActionMessage("");

    try {
      const publishedSchedule = await publishSchedule(
        session.accessToken,
        publicationForm.scheduleId,
        publicationForm.confirmUnfilled,
      );

      setPublicationActionMessage({ key: "schedulePublishedMessage", id: publishedSchedule.id });
      setPublicationReadiness(null);
      setPublicationForm(emptyPublicationForm);
      onDraftSchedulesChanged();
      refreshManagedPublishedSchedules();
    } catch (error) {
      onApiError(error, setPublicationActionError);
    } finally {
      setIsPublishingSchedule(false);
    }
  }

  async function submitReopenSchedule(scheduleId) {
    setReopeningScheduleId(scheduleId);
    setPublicationActionError("");
    setPublicationActionMessage("");

    try {
      const reopenedSchedule = await reopenSchedule(session.accessToken, scheduleId);
      setPublicationActionMessage({ key: "scheduleReopenedMessage", id: reopenedSchedule.id });
      onDraftSchedulesChanged();
      refreshManagedPublishedSchedules();
    } catch (error) {
      onApiError(error, setPublicationActionError);
    } finally {
      setReopeningScheduleId(null);
    }
  }

  function refreshManagedPublishedSchedules() {
    setPublishedScheduleRefreshKey((current) => current + 1);
  }

  function refreshPublicationReadiness() {
    setPublicationReadiness(null);
    setPublicationError("");
    setPublicationReadinessRefreshKey((current) => current + 1);
  }

  function resetSchedulePublication() {
    setManagedPublishedSchedules([]);
    setPublicationForm(emptyPublicationForm);
    setPublicationReadiness(null);
    setPublicationError("");
    setPublicationActionError("");
    setPublicationActionMessage("");
    setIsLoadingPublicationReadiness(false);
    setIsPublishingSchedule(false);
    setIsLoadingManagedPublishedSchedules(false);
    setManagedPublishedSchedulesError("");
    setReopeningScheduleId(null);
    setPublishedScheduleRefreshKey(0);
    setPublicationReadinessRefreshKey(0);
  }

  return {
    handlePublicationFormChange,
    isLoadingManagedPublishedSchedules,
    isLoadingPublicationReadiness,
    isPublishingSchedule,
    managedPublishedSchedules,
    managedPublishedSchedulesError,
    publicationActionError,
    publicationActionMessage,
    publicationError,
    publicationForm,
    publicationReadiness,
    refreshManagedPublishedSchedules,
    refreshPublicationReadiness,
    reopeningScheduleId,
    resetSchedulePublication,
    submitPublishSchedule,
    submitReopenSchedule,
  };
}

export default useSchedulePublication;
