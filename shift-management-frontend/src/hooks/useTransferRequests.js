import { useEffect, useState } from "react";

import {
  approveTransferAsManager,
  approveTransferAsTargetEmployee,
  cancelTransferAsRequester,
  listMyIncomingTransferRequests,
  listMyOutgoingTransferRequests,
  listPendingManagerTransferRequests,
  rejectTransferAsTargetEmployee,
} from "../api.js";

function useTransferRequests(session, isManager, onApiError) {
  const [incomingTransferRequests, setIncomingTransferRequests] = useState([]);
  const [outgoingTransferRequests, setOutgoingTransferRequests] = useState([]);
  const [pendingManagerTransferRequests, setPendingManagerTransferRequests] = useState([]);
  const [transferRequestsError, setTransferRequestsError] = useState("");
  const [transferRequestActionError, setTransferRequestActionError] = useState("");
  const [transferRequestActionMessage, setTransferRequestActionMessage] = useState("");
  const [isLoadingTransferRequests, setIsLoadingTransferRequests] = useState(false);
  const [actingTransferRequest, setActingTransferRequest] = useState(null);
  const [transferRequestRefreshKey, setTransferRequestRefreshKey] = useState(0);

  const transferRequestCount = isManager
    ? pendingManagerTransferRequests.length
    : incomingTransferRequests.length + outgoingTransferRequests.length;

  useEffect(() => {
    if (!session?.accessToken) {
      setIncomingTransferRequests([]);
      setOutgoingTransferRequests([]);
      setPendingManagerTransferRequests([]);
      setTransferRequestsError("");
      return;
    }

    setIsLoadingTransferRequests(true);
    setTransferRequestsError("");

    const loadRequests = isManager
      ? listPendingManagerTransferRequests(session.accessToken).then((requests) => {
          setPendingManagerTransferRequests(requests);
          setIncomingTransferRequests([]);
          setOutgoingTransferRequests([]);
        })
      : Promise.all([
          listMyIncomingTransferRequests(session.accessToken),
          listMyOutgoingTransferRequests(session.accessToken),
        ]).then(([incomingRequests, outgoingRequests]) => {
          setIncomingTransferRequests(incomingRequests);
          setOutgoingTransferRequests(outgoingRequests);
          setPendingManagerTransferRequests([]);
        });

    loadRequests
      .catch((error) => onApiError(error, setTransferRequestsError))
      .finally(() => setIsLoadingTransferRequests(false));
  }, [isManager, session, transferRequestRefreshKey]);

  async function handleTransferRequestAction(requestId, actionName, action, successMessage) {
    setActingTransferRequest({ id: requestId, action: actionName });
    setTransferRequestActionError("");
    setTransferRequestActionMessage("");

    try {
      await action(session.accessToken, requestId);
      setTransferRequestActionMessage(successMessage);
      refreshTransferRequests();
    } catch (error) {
      onApiError(error, setTransferRequestActionError);
    } finally {
      setActingTransferRequest(null);
    }
  }

  function approveIncomingTransferRequest(requestId) {
    return handleTransferRequestAction(
      requestId,
      "employee-approve",
      approveTransferAsTargetEmployee,
      "Transfer request approved.",
    );
  }

  function approveManagerTransferRequest(requestId) {
    return handleTransferRequestAction(
      requestId,
      "manager-approve",
      approveTransferAsManager,
      "Transfer request approved by manager.",
    );
  }

  function cancelOutgoingTransferRequest(requestId) {
    return handleTransferRequestAction(
      requestId,
      "cancel",
      cancelTransferAsRequester,
      "Transfer request cancelled.",
    );
  }

  function rejectIncomingTransferRequest(requestId) {
    return handleTransferRequestAction(
      requestId,
      "employee-reject",
      rejectTransferAsTargetEmployee,
      "Transfer request rejected.",
    );
  }

  function refreshTransferRequests() {
    setTransferRequestRefreshKey((current) => current + 1);
  }

  function resetTransferRequests() {
    setIncomingTransferRequests([]);
    setOutgoingTransferRequests([]);
    setPendingManagerTransferRequests([]);
    setTransferRequestsError("");
    setTransferRequestActionError("");
    setTransferRequestActionMessage("");
    setIsLoadingTransferRequests(false);
    setActingTransferRequest(null);
    setTransferRequestRefreshKey(0);
  }

  return {
    actingTransferRequest,
    approveIncomingTransferRequest,
    approveManagerTransferRequest,
    cancelOutgoingTransferRequest,
    incomingTransferRequests,
    isLoadingTransferRequests,
    outgoingTransferRequests,
    pendingManagerTransferRequests,
    refreshTransferRequests,
    rejectIncomingTransferRequest,
    resetTransferRequests,
    transferRequestActionError,
    transferRequestActionMessage,
    transferRequestCount,
    transferRequestsError,
  };
}

export default useTransferRequests;
