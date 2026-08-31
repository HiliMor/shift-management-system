import { useEffect, useRef, useState } from "react";

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
  const [detailsRefreshKey, setDetailsRefreshKey] = useState(0);
  const listRequestId = useRef(0);
  const detailsRequestId = useRef(0);

  useEffect(() => {
    const requestId = ++listRequestId.current;
    const isCurrent = () => requestId === listRequestId.current;
    if (!session?.accessToken) {
      setPublishedSchedules([]);
      setSelectedScheduleId(null);
      setIsLoadingSchedules(false);
      return;
    }

    setIsLoadingSchedules(true);
    setScheduleError("");

    listMyPublishedSchedules(session.accessToken)
      .then((schedules) => { if (isCurrent()) setPublishedSchedules(schedules); })
      .catch((error) => { if (isCurrent()) onApiError(error, setScheduleError); })
      .finally(() => { if (isCurrent()) setIsLoadingSchedules(false); });

    return () => { if (isCurrent()) listRequestId.current += 1; };
  }, [session]);

  useEffect(() => {
    const requestId = ++detailsRequestId.current;
    const isCurrent = () => requestId === detailsRequestId.current;
    if (!session?.accessToken || !selectedScheduleId) {
      setSelectedScheduleDetails(null);
      setDetailsError("");
      setIsLoadingDetails(false);
      return;
    }

    setSelectedScheduleDetails(null);
    setIsLoadingDetails(true);
    setDetailsError("");

    getMyPublishedScheduleDetails(session.accessToken, selectedScheduleId)
      .then((details) => { if (isCurrent()) setSelectedScheduleDetails(details); })
      .catch((error) => {
        if (!isCurrent()) return;
        setSelectedScheduleDetails(null);
        onApiError(error, setDetailsError);
      })
      .finally(() => { if (isCurrent()) setIsLoadingDetails(false); });

    return () => { if (isCurrent()) detailsRequestId.current += 1; };
  }, [detailsRefreshKey, selectedScheduleId, session]);

  function selectSchedule(scheduleId) {
    if (scheduleId === selectedScheduleId) return;
    // Invalidate immediately, before React runs the next effect cleanup.
    detailsRequestId.current += 1;
    setSelectedScheduleDetails(null);
    setDetailsError("");
    setSelectedScheduleId(scheduleId);
  }

  function refreshSelectedScheduleDetails() {
    detailsRequestId.current += 1;
    setDetailsRefreshKey((current) => current + 1);
  }

  function resetPublishedSchedules() {
    listRequestId.current += 1;
    detailsRequestId.current += 1;
    setPublishedSchedules([]);
    setScheduleError("");
    setIsLoadingSchedules(false);
    setSelectedScheduleId(null);
    setSelectedScheduleDetails(null);
    setDetailsError("");
    setIsLoadingDetails(false);
    setDetailsRefreshKey(0);
  }

  return {
    detailsError,
    isLoadingDetails,
    isLoadingSchedules,
    publishedSchedules,
    resetPublishedSchedules,
    refreshSelectedScheduleDetails,
    scheduleError,
    selectedScheduleDetails,
    selectedScheduleId,
    setSelectedScheduleId: selectSchedule,
  };
}

export default usePublishedSchedules;
