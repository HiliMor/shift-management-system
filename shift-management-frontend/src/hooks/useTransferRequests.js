import { useEffect, useMemo, useState } from "react";

import {
  approveTransferAsManager,
  approveTransferAsTargetEmployee,
  cancelTransferAsRequester,
  createSwapRequest,
  createTransferRequest,
  listMyIncomingTransferRequests,
  listMyOutgoingTransferRequests,
  listManagerTransferRequests,
  rejectTransferAsTargetEmployee,
} from "../api.js";

const DEFAULT_CREATION_FORM = {
  type: "TRANSFER",
  sourceAssignmentId: "",
  targetEmployeeId: "",
  targetAssignmentId: "",
};

function buildPublishedAssignmentOptions(selectedScheduleDetails) {
  return selectedScheduleDetails?.shifts?.flatMap((shift) =>
    shift.assignments.map((assignment) => ({
      assignment,
      shift,
    })),
  ) ?? [];
}

function useTransferRequests(
  session,
  isManager,
  selectedScheduleDetails,
  onApiError,
  onScheduleContentChanged = () => {},
) {
  const [incomingTransferRequests, setIncomingTransferRequests] = useState([]);
  const [outgoingTransferRequests, setOutgoingTransferRequests] = useState([]);
  const [managerTransferRequests, setManagerTransferRequests] = useState([]);
  const [transferRequestCreationForm, setTransferRequestCreationForm] = useState(DEFAULT_CREATION_FORM);
  const [transferRequestCreationError, setTransferRequestCreationError] = useState("");
  const [transferRequestCreationMessage, setTransferRequestCreationMessage] = useState("");
  const [isCreatingTransferRequest, setIsCreatingTransferRequest] = useState(false);
  const [transferRequestsError, setTransferRequestsError] = useState("");
  const [transferRequestActionError, setTransferRequestActionError] = useState("");
  const [transferRequestActionMessage, setTransferRequestActionMessage] = useState("");
  const [isLoadingTransferRequests, setIsLoadingTransferRequests] = useState(false);
  const [actingTransferRequest, setActingTransferRequest] = useState(null);
  const [transferRequestRefreshKey, setTransferRequestRefreshKey] = useState(0);

  const currentUserId = session?.user?.id;
  const publishedAssignmentOptions = useMemo(
    () => buildPublishedAssignmentOptions(selectedScheduleDetails),
    [selectedScheduleDetails],
  );
  const sourceAssignmentOptions = useMemo(
    () => publishedAssignmentOptions.filter((option) => option.assignment.employeeId === currentUserId),
    [currentUserId, publishedAssignmentOptions],
  );
  const transferTargetEmployeeOptions = useMemo(() => {
    const employeesById = new Map();

    publishedAssignmentOptions.forEach(({ assignment }) => {
      if (assignment.employeeId === currentUserId) {
        return;
      }

      employeesById.set(assignment.employeeId, {
        id: assignment.employeeId,
        username: assignment.employeeUsername,
        fullName: assignment.employeeFullName,
      });
    });

    return Array.from(employeesById.values()).sort((left, right) =>
      (left.fullName || left.username).localeCompare(right.fullName || right.username),
    );
  }, [currentUserId, publishedAssignmentOptions]);
  const swapTargetAssignmentOptions = useMemo(
    () => publishedAssignmentOptions.filter(({ assignment }) =>
      assignment.employeeId !== currentUserId
      && assignment.id.toString() !== transferRequestCreationForm.sourceAssignmentId,
    ),
    [currentUserId, publishedAssignmentOptions, transferRequestCreationForm.sourceAssignmentId],
  );

  const transferRequestCount = isManager
    ? managerTransferRequests.length
    : incomingTransferRequests.length + outgoingTransferRequests.length;

  useEffect(() => {
    if (!session?.accessToken) {
      setIncomingTransferRequests([]);
      setOutgoingTransferRequests([]);
      setManagerTransferRequests([]);
      setTransferRequestsError("");
      setTransferRequestCreationForm(DEFAULT_CREATION_FORM);
      setTransferRequestCreationError("");
      setTransferRequestCreationMessage("");
      return;
    }

    setIsLoadingTransferRequests(true);
    setTransferRequestsError("");

    const loadRequests = isManager
      ? listManagerTransferRequests(session.accessToken).then((requests) => {
          setManagerTransferRequests(requests);
          setIncomingTransferRequests([]);
          setOutgoingTransferRequests([]);
        })
      : Promise.all([
          listMyIncomingTransferRequests(session.accessToken),
          listMyOutgoingTransferRequests(session.accessToken),
        ]).then(([incomingRequests, outgoingRequests]) => {
          setIncomingTransferRequests(incomingRequests);
          setOutgoingTransferRequests(outgoingRequests);
          setManagerTransferRequests([]);
        });

    loadRequests
      .catch((error) => onApiError(error, setTransferRequestsError))
      .finally(() => setIsLoadingTransferRequests(false));
  }, [isManager, session, transferRequestRefreshKey]);

  useEffect(() => {
    if (isManager || !session?.accessToken) {
      setTransferRequestCreationForm(DEFAULT_CREATION_FORM);
      return;
    }

    setTransferRequestCreationForm((current) => {
      const currentSourceExists = sourceAssignmentOptions.some(
        (option) => option.assignment.id.toString() === current.sourceAssignmentId,
      );
      const currentTargetEmployeeExists = transferTargetEmployeeOptions.some(
        (employee) => employee.id.toString() === current.targetEmployeeId,
      );
      const currentTargetAssignmentExists = swapTargetAssignmentOptions.some(
        (option) => option.assignment.id.toString() === current.targetAssignmentId,
      );

      const nextForm = {
        ...current,
        sourceAssignmentId: currentSourceExists
          ? current.sourceAssignmentId
          : sourceAssignmentOptions[0]?.assignment.id.toString() || "",
        targetEmployeeId: currentTargetEmployeeExists
          ? current.targetEmployeeId
          : transferTargetEmployeeOptions[0]?.id.toString() || "",
        targetAssignmentId: currentTargetAssignmentExists
          ? current.targetAssignmentId
          : swapTargetAssignmentOptions[0]?.assignment.id.toString() || "",
      };

      if (
        nextForm.sourceAssignmentId === current.sourceAssignmentId
        && nextForm.targetEmployeeId === current.targetEmployeeId
        && nextForm.targetAssignmentId === current.targetAssignmentId
      ) {
        return current;
      }

      return nextForm;
    });
  }, [
    isManager,
    session?.accessToken,
    sourceAssignmentOptions,
    swapTargetAssignmentOptions,
    transferTargetEmployeeOptions,
  ]);

  function handleTransferRequestCreationFormChange(event) {
    const { name, value } = event.target;

    setTransferRequestCreationForm((current) => ({
      ...current,
      [name]: value,
      ...(name === "type" ? { targetAssignmentId: "", targetEmployeeId: "" } : {}),
      ...(name === "sourceAssignmentId" ? { targetAssignmentId: "" } : {}),
    }));
  }

  async function submitTransferRequestCreation(event) {
    event.preventDefault();

    if (!session?.accessToken || isManager) {
      return;
    }

    setIsCreatingTransferRequest(true);
    setTransferRequestCreationError("");
    setTransferRequestCreationMessage("");
    setTransferRequestActionError("");
    setTransferRequestActionMessage("");

    try {
      if (transferRequestCreationForm.type === "SWAP") {
        await createSwapRequest(session.accessToken, {
          sourceAssignmentId: Number(transferRequestCreationForm.sourceAssignmentId),
          targetAssignmentId: Number(transferRequestCreationForm.targetAssignmentId),
        });
        setTransferRequestCreationMessage("swapRequestCreated");
      } else {
        await createTransferRequest(session.accessToken, {
          sourceAssignmentId: Number(transferRequestCreationForm.sourceAssignmentId),
          targetEmployeeId: Number(transferRequestCreationForm.targetEmployeeId),
        });
        setTransferRequestCreationMessage("transferRequestCreated");
      }

      refreshTransferRequests();
    } catch (error) {
      onApiError(error, setTransferRequestCreationError);
    } finally {
      setIsCreatingTransferRequest(false);
    }
  }

  async function handleTransferRequestAction(requestId, actionName, action, successMessage) {
    setActingTransferRequest({ id: requestId, action: actionName });
    setTransferRequestActionError("");
    setTransferRequestActionMessage("");
    setTransferRequestCreationError("");
    setTransferRequestCreationMessage("");

    try {
      await action(session.accessToken, requestId);
      setTransferRequestActionMessage(successMessage);
      refreshTransferRequests();
      if (actionName === "employee-approve" || actionName === "manager-approve") {
        onScheduleContentChanged();
      }
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
      "requestApproved",
    );
  }

  function approveManagerTransferRequest(requestId) {
    return handleTransferRequestAction(
      requestId,
      "manager-approve",
      approveTransferAsManager,
      "requestApprovedByManager",
    );
  }

  function cancelOutgoingTransferRequest(requestId) {
    return handleTransferRequestAction(
      requestId,
      "cancel",
      cancelTransferAsRequester,
      "requestCancelled",
    );
  }

  function rejectIncomingTransferRequest(requestId) {
    return handleTransferRequestAction(
      requestId,
      "employee-reject",
      rejectTransferAsTargetEmployee,
      "requestRejected",
    );
  }

  function refreshTransferRequests() {
    setTransferRequestRefreshKey((current) => current + 1);
  }

  function resetTransferRequests() {
    setIncomingTransferRequests([]);
    setOutgoingTransferRequests([]);
    setManagerTransferRequests([]);
    setTransferRequestCreationForm(DEFAULT_CREATION_FORM);
    setTransferRequestCreationError("");
    setTransferRequestCreationMessage("");
    setIsCreatingTransferRequest(false);
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
    handleTransferRequestCreationFormChange,
    incomingTransferRequests,
    isCreatingTransferRequest,
    isLoadingTransferRequests,
    outgoingTransferRequests,
    managerTransferRequests,
    refreshTransferRequests,
    rejectIncomingTransferRequest,
    resetTransferRequests,
    sourceAssignmentOptions,
    submitTransferRequestCreation,
    swapTargetAssignmentOptions,
    transferRequestCreationError,
    transferRequestCreationForm,
    transferRequestCreationMessage,
    transferRequestActionError,
    transferRequestActionMessage,
    transferRequestCount,
    transferRequestsError,
    transferTargetEmployeeOptions,
  };
}

export default useTransferRequests;
