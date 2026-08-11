import { useEffect, useMemo, useState } from "react";
import {
  approveTransferAsManager,
  approveTransferAsTargetEmployee,
  cancelTransferAsRequester,
  countMyUnreadNotifications,
  createAssignment,
  createSchedule,
  createShift,
  getMyPublishedScheduleDetails,
  listMyIncomingTransferRequests,
  listScheduleAssignments,
  listManagedDraftSchedules,
  listMyManagedTeams,
  listMyPublishedSchedules,
  listStaffingRoles,
  listShifts,
  listTeamEmployees,
  login,
  listMyNotifications,
  listMyOutgoingTransferRequests,
  listPendingManagerTransferRequests,
  markNotificationRead,
  rejectTransferAsTargetEmployee,
} from "./api.js";
import AppShell from "./components/AppShell.jsx";
import LoginScreen from "./components/LoginScreen.jsx";
import NotificationsSection from "./components/NotificationsSection.jsx";
import ManagerActionsSection from "./components/ManagerActionsSection.jsx";
import PublishedSchedulesSection from "./components/PublishedSchedulesSection.jsx";
import ScheduleDetailsSection from "./components/ScheduleDetailsSection.jsx";
import TransferRequestsSection from "./components/TransferRequestsSection.jsx";

const STORAGE_KEY = "shift-management-session";
const SESSION_EXPIRED_MESSAGE = "Session expired. Please sign in again.";

const dateFormatter = new Intl.DateTimeFormat("en-GB", {
  day: "2-digit",
  month: "short",
  year: "numeric",
});

const dateTimeFormatter = new Intl.DateTimeFormat("en-GB", {
  day: "2-digit",
  hour: "2-digit",
  minute: "2-digit",
  month: "short",
  year: "numeric",
});

function formatDate(value) {
  return value ? dateFormatter.format(new Date(value)) : "Not set";
}

function formatDateTime(value) {
  return value ? dateTimeFormatter.format(new Date(value)) : "Not set";
}

function toIsoStringFromLocalDateTime(value) {
  return value ? new Date(value).toISOString() : "";
}

function loadStoredSession() {
  const stored = localStorage.getItem(STORAGE_KEY);

  if (!stored) {
    return null;
  }

  try {
    return JSON.parse(stored);
  } catch {
    localStorage.removeItem(STORAGE_KEY);
    return null;
  }
}

function App() {
  const [session, setSession] = useState(loadStoredSession);
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [loginError, setLoginError] = useState("");
  const [isLoggingIn, setIsLoggingIn] = useState(false);
  const [publishedSchedules, setPublishedSchedules] = useState([]);
  const [scheduleError, setScheduleError] = useState("");
  const [isLoadingSchedules, setIsLoadingSchedules] = useState(false);
  const [selectedScheduleId, setSelectedScheduleId] = useState(null);
  const [selectedScheduleDetails, setSelectedScheduleDetails] = useState(null);
  const [detailsError, setDetailsError] = useState("");
  const [isLoadingDetails, setIsLoadingDetails] = useState(false);
  const [managedTeams, setManagedTeams] = useState([]);
  const [isLoadingManagedTeams, setIsLoadingManagedTeams] = useState(false);
  const [managedTeamsError, setManagedTeamsError] = useState("");
  const [scheduleForm, setScheduleForm] = useState({ teamId: "", startDate: "", endDate: "" });
  const [createdSchedule, setCreatedSchedule] = useState(null);
  const [scheduleCreationError, setScheduleCreationError] = useState("");
  const [isCreatingSchedule, setIsCreatingSchedule] = useState(false);
  const [managedDraftSchedules, setManagedDraftSchedules] = useState([]);
  const [isLoadingDraftSchedules, setIsLoadingDraftSchedules] = useState(false);
  const [draftSchedulesError, setDraftSchedulesError] = useState("");
  const [draftScheduleRefreshKey, setDraftScheduleRefreshKey] = useState(0);
  const [staffingRoles, setStaffingRoles] = useState([]);
  const [isLoadingStaffingRoles, setIsLoadingStaffingRoles] = useState(false);
  const [staffingRolesError, setStaffingRolesError] = useState("");
  const [shiftForm, setShiftForm] = useState({
    scheduleId: "",
    startTime: "",
    endTime: "",
    description: "",
    requiredWorkers: "1",
    minRestHours: "8",
    requiredStaffingRoleId: "",
  });
  const [createdShift, setCreatedShift] = useState(null);
  const [shiftCreationError, setShiftCreationError] = useState("");
  const [isCreatingShift, setIsCreatingShift] = useState(false);
  const [assignmentForm, setAssignmentForm] = useState({
    scheduleId: "",
    shiftId: "",
    employeeId: "",
  });
  const [assignmentShifts, setAssignmentShifts] = useState([]);
  const [isLoadingAssignmentShifts, setIsLoadingAssignmentShifts] = useState(false);
  const [assignmentShiftsError, setAssignmentShiftsError] = useState("");
  const [teamEmployees, setTeamEmployees] = useState([]);
  const [isLoadingTeamEmployees, setIsLoadingTeamEmployees] = useState(false);
  const [teamEmployeesError, setTeamEmployeesError] = useState("");
  const [scheduleAssignments, setScheduleAssignments] = useState([]);
  const [isLoadingScheduleAssignments, setIsLoadingScheduleAssignments] = useState(false);
  const [scheduleAssignmentsError, setScheduleAssignmentsError] = useState("");
  const [assignmentRefreshKey, setAssignmentRefreshKey] = useState(0);
  const [createdAssignment, setCreatedAssignment] = useState(null);
  const [assignmentCreationError, setAssignmentCreationError] = useState("");
  const [isCreatingAssignment, setIsCreatingAssignment] = useState(false);
  const [notifications, setNotifications] = useState([]);
  const [unreadNotificationCount, setUnreadNotificationCount] = useState(0);
  const [notificationsError, setNotificationsError] = useState("");
  const [isLoadingNotifications, setIsLoadingNotifications] = useState(false);
  const [markingNotificationId, setMarkingNotificationId] = useState(null);
  const [notificationRefreshKey, setNotificationRefreshKey] = useState(0);
  const [incomingTransferRequests, setIncomingTransferRequests] = useState([]);
  const [outgoingTransferRequests, setOutgoingTransferRequests] = useState([]);
  const [pendingManagerTransferRequests, setPendingManagerTransferRequests] = useState([]);
  const [transferRequestsError, setTransferRequestsError] = useState("");
  const [transferRequestActionError, setTransferRequestActionError] = useState("");
  const [transferRequestActionMessage, setTransferRequestActionMessage] = useState("");
  const [isLoadingTransferRequests, setIsLoadingTransferRequests] = useState(false);
  const [actingTransferRequest, setActingTransferRequest] = useState(null);
  const [transferRequestRefreshKey, setTransferRequestRefreshKey] = useState(0);

  const isManager = session?.user?.applicationRole === "MANAGER";

  const transferRequestCount = isManager
    ? pendingManagerTransferRequests.length
    : incomingTransferRequests.length + outgoingTransferRequests.length;

  const displayName = useMemo(() => {
    if (!session?.user) {
      return "";
    }

    return session.user.fullName || session.user.username;
  }, [session]);

  const selectedDraftSchedule = useMemo(
    () => managedDraftSchedules.find((schedule) => schedule.id.toString() === shiftForm.scheduleId) ?? null,
    [managedDraftSchedules, shiftForm.scheduleId],
  );

  const selectedAssignmentSchedule = useMemo(
    () => managedDraftSchedules.find((schedule) => schedule.id.toString() === assignmentForm.scheduleId) ?? null,
    [assignmentForm.scheduleId, managedDraftSchedules],
  );

  const assignmentShiftMap = useMemo(
    () => new Map(assignmentShifts.map((shift) => [shift.id, shift])),
    [assignmentShifts],
  );

  function clearAuthenticatedState() {
    setPublishedSchedules([]);
    setScheduleError("");
    setSelectedScheduleId(null);
    setSelectedScheduleDetails(null);
    setDetailsError("");
    setManagedTeams([]);
    setManagedTeamsError("");
    setScheduleForm({ teamId: "", startDate: "", endDate: "" });
    setCreatedSchedule(null);
    setScheduleCreationError("");
    setManagedDraftSchedules([]);
    setDraftSchedulesError("");
    setStaffingRoles([]);
    setStaffingRolesError("");
    setShiftForm({
      scheduleId: "",
      startTime: "",
      endTime: "",
      description: "",
      requiredWorkers: "1",
      minRestHours: "8",
      requiredStaffingRoleId: "",
    });
    setCreatedShift(null);
    setShiftCreationError("");
    setAssignmentForm({ scheduleId: "", shiftId: "", employeeId: "" });
    setAssignmentShifts([]);
    setAssignmentShiftsError("");
    setTeamEmployees([]);
    setTeamEmployeesError("");
    setScheduleAssignments([]);
    setScheduleAssignmentsError("");
    setCreatedAssignment(null);
    setAssignmentCreationError("");
    setNotifications([]);
    setUnreadNotificationCount(0);
    setNotificationsError("");
    setMarkingNotificationId(null);
    setNotificationRefreshKey(0);
    setIncomingTransferRequests([]);
    setOutgoingTransferRequests([]);
    setPendingManagerTransferRequests([]);
    setTransferRequestsError("");
    setTransferRequestActionError("");
    setTransferRequestActionMessage("");
    setActingTransferRequest(null);
    setTransferRequestRefreshKey(0);
  }

  function expireSession() {
    localStorage.removeItem(STORAGE_KEY);
    setSession(null);
    clearAuthenticatedState();
    setLoginError(SESSION_EXPIRED_MESSAGE);
  }

  function handleApiError(error, setError) {
    if (error.status === 401) {
      expireSession();
      return;
    }

    setError(error.message);
  }

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
      .catch((error) => handleApiError(error, setScheduleError))
      .finally(() => setIsLoadingSchedules(false));
  }, [session]);

  useEffect(() => {
    if (!session?.accessToken) {
      setNotifications([]);
      setUnreadNotificationCount(0);
      setNotificationsError("");
      return;
    }

    setIsLoadingNotifications(true);
    setNotificationsError("");

    Promise.all([
      listMyNotifications(session.accessToken),
      countMyUnreadNotifications(session.accessToken),
    ])
      .then(([notificationList, unreadCount]) => {
        setNotifications(notificationList);
        setUnreadNotificationCount(unreadCount.unreadCount);
      })
      .catch((error) => handleApiError(error, setNotificationsError))
      .finally(() => setIsLoadingNotifications(false));
  }, [notificationRefreshKey, session]);

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
      .catch((error) => handleApiError(error, setTransferRequestsError))
      .finally(() => setIsLoadingTransferRequests(false));
  }, [isManager, session, transferRequestRefreshKey]);

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
        handleApiError(error, setDetailsError);
      })
      .finally(() => setIsLoadingDetails(false));
  }, [selectedScheduleId, session]);

  useEffect(() => {
    if (!session?.accessToken || !isManager) {
      setManagedTeams([]);
      setManagedTeamsError("");
      setScheduleForm({ teamId: "", startDate: "", endDate: "" });
      return;
    }

    setIsLoadingManagedTeams(true);
    setManagedTeamsError("");

    listMyManagedTeams(session.accessToken)
      .then((teams) => {
        setManagedTeams(teams);
        setScheduleForm((current) => ({
          ...current,
          teamId: current.teamId || teams[0]?.id?.toString() || "",
        }));
      })
      .catch((error) => handleApiError(error, setManagedTeamsError))
      .finally(() => setIsLoadingManagedTeams(false));
  }, [isManager, session]);

  useEffect(() => {
    if (!session?.accessToken || !isManager) {
      setManagedDraftSchedules([]);
      setDraftSchedulesError("");
      setShiftForm({
        scheduleId: "",
        startTime: "",
        endTime: "",
        description: "",
        requiredWorkers: "1",
        minRestHours: "8",
        requiredStaffingRoleId: "",
      });
      setAssignmentForm({ scheduleId: "", shiftId: "", employeeId: "" });
      setAssignmentShifts([]);
      setAssignmentShiftsError("");
      setTeamEmployees([]);
      setTeamEmployeesError("");
      setScheduleAssignments([]);
      setScheduleAssignmentsError("");
      return;
    }

    setIsLoadingDraftSchedules(true);
    setDraftSchedulesError("");

    listManagedDraftSchedules(session.accessToken)
      .then((schedules) => {
        setManagedDraftSchedules(schedules);
        setShiftForm((current) => ({
          ...current,
          scheduleId: current.scheduleId || schedules[0]?.id?.toString() || "",
        }));
        setAssignmentForm((current) => ({
          ...current,
          scheduleId: current.scheduleId || schedules[0]?.id?.toString() || "",
        }));
      })
      .catch((error) => handleApiError(error, setDraftSchedulesError))
      .finally(() => setIsLoadingDraftSchedules(false));
  }, [draftScheduleRefreshKey, isManager, session]);

  useEffect(() => {
    if (!session?.accessToken || !selectedDraftSchedule) {
      setStaffingRoles([]);
      setStaffingRolesError("");
      return;
    }

    setIsLoadingStaffingRoles(true);
    setStaffingRolesError("");

    listStaffingRoles(session.accessToken, selectedDraftSchedule.teamId)
      .then(setStaffingRoles)
      .catch((error) => handleApiError(error, setStaffingRolesError))
      .finally(() => setIsLoadingStaffingRoles(false));
  }, [selectedDraftSchedule, session]);

  useEffect(() => {
    if (!session?.accessToken || !assignmentForm.scheduleId) {
      setAssignmentShifts([]);
      setAssignmentShiftsError("");
      setAssignmentForm((current) => ({ ...current, shiftId: "" }));
      return;
    }

    setIsLoadingAssignmentShifts(true);
    setAssignmentShiftsError("");

    listShifts(session.accessToken, assignmentForm.scheduleId)
      .then((shifts) => {
        setAssignmentShifts(shifts);
        setAssignmentForm((current) => {
          const currentShiftExists = shifts.some((shift) => shift.id.toString() === current.shiftId);

          return {
            ...current,
            shiftId: currentShiftExists ? current.shiftId : shifts[0]?.id?.toString() || "",
          };
        });
      })
      .catch((error) => handleApiError(error, setAssignmentShiftsError))
      .finally(() => setIsLoadingAssignmentShifts(false));
  }, [assignmentForm.scheduleId, assignmentRefreshKey, session]);

  useEffect(() => {
    if (!session?.accessToken || !selectedAssignmentSchedule) {
      setTeamEmployees([]);
      setTeamEmployeesError("");
      setAssignmentForm((current) => ({ ...current, employeeId: "" }));
      return;
    }

    setIsLoadingTeamEmployees(true);
    setTeamEmployeesError("");

    listTeamEmployees(session.accessToken, selectedAssignmentSchedule.teamId)
      .then((employees) => {
        setTeamEmployees(employees);
        setAssignmentForm((current) => {
          const currentEmployeeExists = employees.some((employee) => employee.id.toString() === current.employeeId);

          return {
            ...current,
            employeeId: currentEmployeeExists ? current.employeeId : employees[0]?.id?.toString() || "",
          };
        });
      })
      .catch((error) => handleApiError(error, setTeamEmployeesError))
      .finally(() => setIsLoadingTeamEmployees(false));
  }, [selectedAssignmentSchedule, session]);

  useEffect(() => {
    if (!session?.accessToken || !assignmentForm.scheduleId) {
      setScheduleAssignments([]);
      setScheduleAssignmentsError("");
      return;
    }

    setIsLoadingScheduleAssignments(true);
    setScheduleAssignmentsError("");

    listScheduleAssignments(session.accessToken, assignmentForm.scheduleId)
      .then(setScheduleAssignments)
      .catch((error) => handleApiError(error, setScheduleAssignmentsError))
      .finally(() => setIsLoadingScheduleAssignments(false));
  }, [assignmentForm.scheduleId, assignmentRefreshKey, session]);

  async function handleLogin(event) {
    event.preventDefault();
    setIsLoggingIn(true);
    setLoginError("");

    try {
      const response = await login(username, password);
      localStorage.setItem(STORAGE_KEY, JSON.stringify(response));
      setSession(response);
      setPassword("");
    } catch (error) {
      setLoginError(error.message);
    } finally {
      setIsLoggingIn(false);
    }
  }

  function handleLogout() {
    localStorage.removeItem(STORAGE_KEY);
    setSession(null);
    clearAuthenticatedState();
    setLoginError("");
  }

  function handleScheduleFormChange(event) {
    const { name, value } = event.target;

    setScheduleForm((current) => ({
      ...current,
      [name]: value,
    }));
  }

  async function handleCreateSchedule(event) {
    event.preventDefault();
    setIsCreatingSchedule(true);
    setScheduleCreationError("");
    setCreatedSchedule(null);

    try {
      const response = await createSchedule(session.accessToken, {
        teamId: Number(scheduleForm.teamId),
        startDate: scheduleForm.startDate,
        endDate: scheduleForm.endDate,
      });
      setCreatedSchedule(response);
      setShiftForm((current) => ({
        ...current,
        scheduleId: response.id.toString(),
        minRestHours: managedTeams
          .find((team) => team.id === response.teamId)
          ?.defaultMinRestHours?.toString() || current.minRestHours,
      }));
      setAssignmentForm((current) => ({
        ...current,
        scheduleId: response.id.toString(),
        shiftId: "",
      }));
      setDraftScheduleRefreshKey((current) => current + 1);
    } catch (error) {
      handleApiError(error, setScheduleCreationError);
    } finally {
      setIsCreatingSchedule(false);
    }
  }

  function handleShiftFormChange(event) {
    const { name, value } = event.target;

    setShiftForm((current) => ({
      ...current,
      [name]: value,
      ...(name === "scheduleId" ? { requiredStaffingRoleId: "" } : {}),
    }));
  }

  async function handleCreateShift(event) {
    event.preventDefault();
    setIsCreatingShift(true);
    setShiftCreationError("");
    setCreatedShift(null);

    try {
      const response = await createShift(session.accessToken, shiftForm.scheduleId, {
        startTime: toIsoStringFromLocalDateTime(shiftForm.startTime),
        endTime: toIsoStringFromLocalDateTime(shiftForm.endTime),
        description: shiftForm.description || null,
        requiredWorkers: Number(shiftForm.requiredWorkers),
        minRestHours: Number(shiftForm.minRestHours),
        requiredStaffingRoleId: shiftForm.requiredStaffingRoleId ? Number(shiftForm.requiredStaffingRoleId) : null,
      });
      setCreatedShift(response);
      setShiftForm((current) => ({
        ...current,
        startTime: "",
        endTime: "",
        description: "",
      }));
      setAssignmentForm((current) => ({
        ...current,
        scheduleId: response.scheduleId.toString(),
        shiftId: response.id.toString(),
      }));
      setAssignmentRefreshKey((current) => current + 1);
    } catch (error) {
      handleApiError(error, setShiftCreationError);
    } finally {
      setIsCreatingShift(false);
    }
  }

  function handleAssignmentFormChange(event) {
    const { name, value } = event.target;

    setAssignmentForm((current) => ({
      ...current,
      [name]: value,
      ...(name === "scheduleId" ? { shiftId: "", employeeId: "" } : {}),
    }));
  }

  async function handleCreateAssignment(event) {
    event.preventDefault();
    setIsCreatingAssignment(true);
    setAssignmentCreationError("");
    setCreatedAssignment(null);

    try {
      const response = await createAssignment(session.accessToken, {
        shiftId: Number(assignmentForm.shiftId),
        employeeId: Number(assignmentForm.employeeId),
      });
      setCreatedAssignment(response);
      setAssignmentRefreshKey((current) => current + 1);
    } catch (error) {
      handleApiError(error, setAssignmentCreationError);
    } finally {
      setIsCreatingAssignment(false);
    }
  }

  async function handleMarkNotificationRead(notificationId) {
    setMarkingNotificationId(notificationId);
    setNotificationsError("");

    try {
      const updatedNotification = await markNotificationRead(session.accessToken, notificationId);
      const wasUnread = notifications.some(
        (notification) => notification.id === updatedNotification.id && !notification.read,
      );

      setNotifications((current) =>
        current.map((notification) =>
          notification.id === updatedNotification.id ? updatedNotification : notification,
        ),
      );
      if (wasUnread) {
        setUnreadNotificationCount((current) => Math.max(0, current - 1));
      }
    } catch (error) {
      handleApiError(error, setNotificationsError);
    } finally {
      setMarkingNotificationId(null);
    }
  }

  async function handleTransferRequestAction(requestId, actionName, action, successMessage) {
    setActingTransferRequest({ id: requestId, action: actionName });
    setTransferRequestActionError("");
    setTransferRequestActionMessage("");

    try {
      await action(session.accessToken, requestId);
      setTransferRequestActionMessage(successMessage);
      setTransferRequestRefreshKey((current) => current + 1);
    } catch (error) {
      handleApiError(error, setTransferRequestActionError);
    } finally {
      setActingTransferRequest(null);
    }
  }

  if (!session) {
    return (
      <LoginScreen
        isLoggingIn={isLoggingIn}
        loginError={loginError}
        onLogin={handleLogin}
        onPasswordChange={setPassword}
        onUsernameChange={setUsername}
        password={password}
        username={username}
      />
    );
  }

  return (
    <AppShell
      displayName={displayName}
      isManager={isManager}
      onLogout={handleLogout}
      role={session.user.applicationRole}
      transferRequestCount={transferRequestCount}
      unreadNotificationCount={unreadNotificationCount}
    >
      <NotificationsSection
        formatDateTime={formatDateTime}
        isLoadingNotifications={isLoadingNotifications}
        markingNotificationId={markingNotificationId}
        notifications={notifications}
        notificationsError={notificationsError}
        onMarkNotificationRead={handleMarkNotificationRead}
        onRefreshNotifications={() => setNotificationRefreshKey((current) => current + 1)}
        unreadNotificationCount={unreadNotificationCount}
      />

      <TransferRequestsSection
        actingTransferRequest={actingTransferRequest}
        formatDateTime={formatDateTime}
        incomingTransferRequests={incomingTransferRequests}
        isLoadingTransferRequests={isLoadingTransferRequests}
        isManager={isManager}
        onApproveIncomingTransferRequest={(requestId) =>
          handleTransferRequestAction(
            requestId,
            "employee-approve",
            approveTransferAsTargetEmployee,
            "Transfer request approved.",
          )
        }
        onApproveManagerTransferRequest={(requestId) =>
          handleTransferRequestAction(
            requestId,
            "manager-approve",
            approveTransferAsManager,
            "Transfer request approved by manager.",
          )
        }
        onCancelOutgoingTransferRequest={(requestId) =>
          handleTransferRequestAction(
            requestId,
            "cancel",
            cancelTransferAsRequester,
            "Transfer request cancelled.",
          )
        }
        onRefreshTransferRequests={() => setTransferRequestRefreshKey((current) => current + 1)}
        onRejectIncomingTransferRequest={(requestId) =>
          handleTransferRequestAction(
            requestId,
            "employee-reject",
            rejectTransferAsTargetEmployee,
            "Transfer request rejected.",
          )
        }
        outgoingTransferRequests={outgoingTransferRequests}
        pendingManagerTransferRequests={pendingManagerTransferRequests}
        transferRequestActionError={transferRequestActionError}
        transferRequestActionMessage={transferRequestActionMessage}
        transferRequestCount={transferRequestCount}
        transferRequestsError={transferRequestsError}
      />

      <PublishedSchedulesSection
        formatDate={formatDate}
        isLoadingSchedules={isLoadingSchedules}
        onSelectSchedule={setSelectedScheduleId}
        publishedSchedules={publishedSchedules}
        scheduleError={scheduleError}
        selectedScheduleId={selectedScheduleId}
      />

      <ScheduleDetailsSection
        detailsError={detailsError}
        formatDate={formatDate}
        formatDateTime={formatDateTime}
        isLoadingDetails={isLoadingDetails}
        selectedScheduleDetails={selectedScheduleDetails}
        selectedScheduleId={selectedScheduleId}
      />

      {isManager ? (
        <ManagerActionsSection
          assignmentCreationError={assignmentCreationError}
          assignmentForm={assignmentForm}
          assignmentShiftMap={assignmentShiftMap}
          assignmentShifts={assignmentShifts}
          assignmentShiftsError={assignmentShiftsError}
          createdAssignment={createdAssignment}
          createdSchedule={createdSchedule}
          createdShift={createdShift}
          draftSchedulesError={draftSchedulesError}
          formatDate={formatDate}
          formatDateTime={formatDateTime}
          isCreatingAssignment={isCreatingAssignment}
          isCreatingSchedule={isCreatingSchedule}
          isCreatingShift={isCreatingShift}
          isLoadingAssignmentShifts={isLoadingAssignmentShifts}
          isLoadingDraftSchedules={isLoadingDraftSchedules}
          isLoadingManagedTeams={isLoadingManagedTeams}
          isLoadingScheduleAssignments={isLoadingScheduleAssignments}
          isLoadingStaffingRoles={isLoadingStaffingRoles}
          isLoadingTeamEmployees={isLoadingTeamEmployees}
          managedDraftSchedules={managedDraftSchedules}
          managedTeams={managedTeams}
          managedTeamsError={managedTeamsError}
          onAssignmentFormChange={handleAssignmentFormChange}
          onCreateAssignment={handleCreateAssignment}
          onCreateSchedule={handleCreateSchedule}
          onCreateShift={handleCreateShift}
          onScheduleFormChange={handleScheduleFormChange}
          onShiftFormChange={handleShiftFormChange}
          scheduleAssignments={scheduleAssignments}
          scheduleAssignmentsError={scheduleAssignmentsError}
          scheduleCreationError={scheduleCreationError}
          scheduleForm={scheduleForm}
          selectedAssignmentSchedule={selectedAssignmentSchedule}
          shiftCreationError={shiftCreationError}
          shiftForm={shiftForm}
          staffingRoles={staffingRoles}
          staffingRolesError={staffingRolesError}
          teamEmployees={teamEmployees}
          teamEmployeesError={teamEmployeesError}
        />
      ) : null}
    </AppShell>
  );
}

export default App;
