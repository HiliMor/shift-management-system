import { useEffect, useState } from "react";

import {
  getMyPublishedScheduleDetails,
  listMyPublishedSchedules,
} from "../api.js";

function usePublishedSchedules(session, onApiError) {
  const [publishedSchedules, setPublishedSchedules] = useState([]);
  const [scheduleError, setScheduleError] = useState("");
  const [isLoadingSchedules, setIsLoadingSchedules] = useState(false);
  const [selectedScheduleId, setSelectedScheduleId] = useState(null);
  const [selectedScheduleDetails, setSelectedScheduleDetails] = useState(null);
  const [detailsError, setDetailsError] = useState("");
  const [isLoadingDetails, setIsLoadingDetails] = useState(false);

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
      .catch((error) => onApiError(error, setScheduleError))
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
        onApiError(error, setDetailsError);
      })
      .finally(() => setIsLoadingDetails(false));
  }, [selectedScheduleId, session]);

  function resetPublishedSchedules() {
    setPublishedSchedules([]);
    setScheduleError("");
    setIsLoadingSchedules(false);
    setSelectedScheduleId(null);
    setSelectedScheduleDetails(null);
    setDetailsError("");
    setIsLoadingDetails(false);
  }

  return {
    detailsError,
    isLoadingDetails,
    isLoadingSchedules,
    publishedSchedules,
    resetPublishedSchedules,
    scheduleError,
    selectedScheduleDetails,
    selectedScheduleId,
    setSelectedScheduleId,
  };
}

export default usePublishedSchedules;
