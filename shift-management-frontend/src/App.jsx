import { useMemo, useState } from "react";
import { login } from "./api.js";
import AppShell from "./components/AppShell.jsx";
import AvailabilityConstraintsSection from "./components/AvailabilityConstraintsSection.jsx";
import LoginScreen from "./components/LoginScreen.jsx";
import NotificationsSection from "./components/NotificationsSection.jsx";
import ManagerActionsSection from "./components/ManagerActionsSection.jsx";
import PublishedSchedulesSection from "./components/PublishedSchedulesSection.jsx";
import ScheduleDetailsSection from "./components/ScheduleDetailsSection.jsx";
import TransferRequestsSection from "./components/TransferRequestsSection.jsx";
import useAvailabilityConstraints from "./hooks/useAvailabilityConstraints.js";
import useAutomaticAssignment from "./hooks/useAutomaticAssignment.js";
import useManagerScheduling from "./hooks/useManagerScheduling.js";
import useNotifications from "./hooks/useNotifications.js";
import usePublishedSchedules from "./hooks/usePublishedSchedules.js";
import useSchedulePublication from "./hooks/useSchedulePublication.js";
import useShiftTemplates from "./hooks/useShiftTemplates.js";
import useTransferRequests from "./hooks/useTransferRequests.js";
import { useLanguage } from "./i18n/LanguageContext.jsx";

const STORAGE_KEY = "shift-management-session";

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
  const { language, t } = useLanguage();
  const isManager = session?.user?.applicationRole === "MANAGER";
  const isEmployee = session?.user?.applicationRole === "EMPLOYEE";

  const dateFormatter = useMemo(
    () => new Intl.DateTimeFormat(language === "he" ? "he-IL" : "en-GB", {
      day: "2-digit",
      month: "short",
      year: "numeric",
    }),
    [language],
  );

  const dateTimeFormatter = useMemo(
    () => new Intl.DateTimeFormat(language === "he" ? "he-IL" : "en-GB", {
      day: "2-digit",
      hour: "2-digit",
      minute: "2-digit",
      month: "short",
      year: "numeric",
    }),
    [language],
  );

  function formatDate(value) {
    return value ? dateFormatter.format(new Date(value)) : t("notSet");
  }

  function formatDateTime(value) {
    return value ? dateTimeFormatter.format(new Date(value)) : t("notSet");
  }

  const displayName = useMemo(() => {
    if (!session?.user) {
      return "";
    }

    return session.user.fullName || session.user.username;
  }, [session]);

  function clearAuthenticatedState() {
    resetPublishedSchedules();
    resetManagerScheduling();
    resetAvailabilityConstraints();
    resetNotifications();
    resetTransferRequests();
    resetSchedulePublication();
    resetAutomaticAssignment();
    resetShiftTemplates();
  }

  function expireSession() {
    localStorage.removeItem(STORAGE_KEY);
    setSession(null);
    clearAuthenticatedState();
    setLoginError(t("sessionExpired"));
  }

  function handleApiError(error, setError) {
    if (error.status === 401) {
      expireSession();
      return;
    }

    setError(error.message);
  }

  const {
    detailsError,
    isLoadingDetails,
    isLoadingSchedules,
    publishedSchedules,
    resetPublishedSchedules,
    scheduleError,
    selectedScheduleDetails,
    selectedScheduleId,
    setSelectedScheduleId,
  } = usePublishedSchedules(session, handleApiError);

  function handleManagerScheduleContentChanged() {
    refreshPublicationReadiness();
  }

  const {
    assignmentCreationError,
    assignmentForm,
    assignmentShiftMap,
    assignmentShifts,
    assignmentShiftsError,
    createdAssignment,
    createdSchedule,
    createdShift,
    draftSchedulesError,
    handleAssignmentFormChange,
    handleCreateAssignment,
    handleCreateSchedule,
    handleCreateShift,
    handleScheduleFormChange,
    handleShiftFormChange,
    isCreatingAssignment,
    isCreatingSchedule,
    isCreatingShift,
    isLoadingAssignmentShifts,
    isLoadingDraftSchedules,
    isLoadingManagedTeams,
    isLoadingScheduleAssignments,
    isLoadingStaffingRoles,
    isLoadingTeamEmployees,
    managedDraftSchedules,
    managedTeams,
    managedTeamsError,
    refreshAssignmentData,
    refreshDraftSchedules,
    resetManagerScheduling,
    scheduleAssignments,
    scheduleAssignmentsError,
    scheduleCreationError,
    scheduleForm,
    selectDraftSchedule,
    selectedDraftSchedule,
    selectedDraftScheduleId,
    selectedAssignmentSchedule,
    shiftCreationError,
    shiftForm,
    staffingRoles,
    staffingRolesError,
    teamEmployees,
    teamEmployeesError,
  } = useManagerScheduling(
    session,
    isManager,
    handleApiError,
    handleManagerScheduleContentChanged,
  );

  const {
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
  } = useAvailabilityConstraints(session, isEmployee, handleApiError);

  const {
    isLoadingNotifications,
    markingNotificationId,
    markNotificationAsRead,
    notifications,
    notificationsError,
    refreshNotifications,
    resetNotifications,
    unreadNotificationCount,
  } = useNotifications(session, handleApiError);

  const {
    actingTransferRequest,
    approveIncomingTransferRequest,
    approveManagerTransferRequest,
    cancelOutgoingTransferRequest,
    handleTransferRequestCreationFormChange,
    incomingTransferRequests,
    isCreatingTransferRequest,
    isLoadingTransferRequests,
    outgoingTransferRequests,
    pendingManagerTransferRequests,
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
  } = useTransferRequests(session, isManager, selectedScheduleDetails, handleApiError);

  const {
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
  } = useSchedulePublication(
    session,
    isManager,
    managedDraftSchedules,
    selectedDraftScheduleId,
    refreshDraftSchedules,
    handleApiError,
  );

  const {
    automaticAssignmentError,
    automaticAssignmentForm,
    automaticAssignmentMessage,
    automaticAssignmentReport,
    handleAutomaticAssignmentFormChange,
    isRunningAutomaticAssignment,
    resetAutomaticAssignment,
    submitAutomaticAssignment,
  } = useAutomaticAssignment(
    session,
    isManager,
    managedDraftSchedules,
    selectedDraftScheduleId,
    refreshAssignmentData,
    refreshPublicationReadiness,
    handleApiError,
  );

  const {
    generationDraftSchedules,
    handleTemplateFormChange,
    handleTemplateGenerationFormChange,
    handleTemplateSlotFormChange,
    isCreatingTemplate,
    isCreatingTemplateSlot,
    isGeneratingTemplateShifts,
    isLoadingTemplateSlots,
    isLoadingTemplateStaffingRoles,
    isLoadingTemplates,
    refreshTemplateSlots,
    refreshTemplates,
    resetShiftTemplates,
    selectedGenerationTemplate,
    selectedTemplate,
    submitCreateTemplate,
    submitCreateTemplateSlot,
    submitGenerateTemplateShifts,
    templateActionError,
    templateActionMessage,
    templateForm,
    templateGenerationForm,
    templateGenerationReport,
    templateListError,
    templates,
    templateSlotError,
    templateSlotForm,
    templateSlots,
    templateStaffingRoles,
    templateStaffingRolesError,
  } = useShiftTemplates(
    session,
    isManager,
    managedTeams,
    managedDraftSchedules,
    selectedDraftScheduleId,
    refreshAssignmentData,
    refreshPublicationReadiness,
    handleApiError,
  );

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
      availabilityConstraintCount={isEmployee ? availabilityConstraints.length : 0}
      displayName={displayName}
      isManager={isManager}
      onLogout={handleLogout}
      role={session.user.applicationRole}
      transferRequestCount={transferRequestCount}
      unreadNotificationCount={unreadNotificationCount}
    >
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

      {isEmployee ? (
        <AvailabilityConstraintsSection
          availabilityActionError={availabilityActionError}
          availabilityActionMessage={availabilityActionMessage}
          availabilityConstraints={availabilityConstraints}
          availabilityError={availabilityError}
          availabilityForm={availabilityForm}
          deletingAvailabilityId={deletingAvailabilityId}
          formatDateTime={formatDateTime}
          isCreatingAvailability={isCreatingAvailability}
          isLoadingAvailability={isLoadingAvailability}
          onAvailabilityFormChange={handleAvailabilityFormChange}
          onCreateAvailabilityConstraint={submitAvailabilityConstraint}
          onDeleteAvailabilityConstraint={removeAvailabilityConstraint}
          onRefreshAvailabilityConstraints={refreshAvailabilityConstraints}
        />
      ) : null}

      <TransferRequestsSection
        actingTransferRequest={actingTransferRequest}
        formatDateTime={formatDateTime}
        incomingTransferRequests={incomingTransferRequests}
        isCreatingTransferRequest={isCreatingTransferRequest}
        isLoadingTransferRequests={isLoadingTransferRequests}
        isManager={isManager}
        onApproveIncomingTransferRequest={approveIncomingTransferRequest}
        onApproveManagerTransferRequest={approveManagerTransferRequest}
        onCancelOutgoingTransferRequest={cancelOutgoingTransferRequest}
        onCreateTransferRequest={submitTransferRequestCreation}
        onRefreshTransferRequests={refreshTransferRequests}
        onRejectIncomingTransferRequest={rejectIncomingTransferRequest}
        onTransferRequestCreationFormChange={handleTransferRequestCreationFormChange}
        outgoingTransferRequests={outgoingTransferRequests}
        pendingManagerTransferRequests={pendingManagerTransferRequests}
        selectedScheduleDetails={selectedScheduleDetails}
        sourceAssignmentOptions={sourceAssignmentOptions}
        swapTargetAssignmentOptions={swapTargetAssignmentOptions}
        transferRequestCreationError={transferRequestCreationError}
        transferRequestCreationForm={transferRequestCreationForm}
        transferRequestCreationMessage={transferRequestCreationMessage}
        transferRequestActionError={transferRequestActionError}
        transferRequestActionMessage={transferRequestActionMessage}
        transferRequestCount={transferRequestCount}
        transferRequestsError={transferRequestsError}
        transferTargetEmployeeOptions={transferTargetEmployeeOptions}
      />

      <NotificationsSection
        formatDateTime={formatDateTime}
        isLoadingNotifications={isLoadingNotifications}
        markingNotificationId={markingNotificationId}
        notifications={notifications}
        notificationsError={notificationsError}
        onMarkNotificationRead={markNotificationAsRead}
        onRefreshNotifications={refreshNotifications}
        unreadNotificationCount={unreadNotificationCount}
      />

      {isManager ? (
        <ManagerActionsSection
          automaticAssignmentError={automaticAssignmentError}
          automaticAssignmentForm={automaticAssignmentForm}
          automaticAssignmentMessage={automaticAssignmentMessage}
          automaticAssignmentReport={automaticAssignmentReport}
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
          isCreatingTemplate={isCreatingTemplate}
          isCreatingTemplateSlot={isCreatingTemplateSlot}
          isGeneratingTemplateShifts={isGeneratingTemplateShifts}
          isLoadingAssignmentShifts={isLoadingAssignmentShifts}
          isLoadingDraftSchedules={isLoadingDraftSchedules}
          isLoadingManagedTeams={isLoadingManagedTeams}
          isLoadingManagedPublishedSchedules={isLoadingManagedPublishedSchedules}
          isLoadingPublicationReadiness={isLoadingPublicationReadiness}
          isLoadingScheduleAssignments={isLoadingScheduleAssignments}
          isLoadingStaffingRoles={isLoadingStaffingRoles}
          isLoadingTemplateSlots={isLoadingTemplateSlots}
          isLoadingTemplateStaffingRoles={isLoadingTemplateStaffingRoles}
          isLoadingTemplates={isLoadingTemplates}
          isLoadingTeamEmployees={isLoadingTeamEmployees}
          isRunningAutomaticAssignment={isRunningAutomaticAssignment}
          isPublishingSchedule={isPublishingSchedule}
          generationDraftSchedules={generationDraftSchedules}
          managedDraftSchedules={managedDraftSchedules}
          managedPublishedSchedules={managedPublishedSchedules}
          managedPublishedSchedulesError={managedPublishedSchedulesError}
          managedTeams={managedTeams}
          managedTeamsError={managedTeamsError}
          onAutomaticAssignmentFormChange={handleAutomaticAssignmentFormChange}
          onAssignmentFormChange={handleAssignmentFormChange}
          onCreateAssignment={handleCreateAssignment}
          onCreateSchedule={handleCreateSchedule}
          onCreateShift={handleCreateShift}
          onCreateTemplate={submitCreateTemplate}
          onCreateTemplateSlot={submitCreateTemplateSlot}
          onGenerateTemplateShifts={submitGenerateTemplateShifts}
          onPublicationFormChange={handlePublicationFormChange}
          onPublishSchedule={submitPublishSchedule}
          onRefreshPublishedSchedules={refreshManagedPublishedSchedules}
          onRefreshPublicationReadiness={refreshPublicationReadiness}
          onRefreshTemplateSlots={refreshTemplateSlots}
          onRefreshTemplates={refreshTemplates}
          onReopenSchedule={submitReopenSchedule}
          onRunAutomaticAssignment={submitAutomaticAssignment}
          onSelectDraftSchedule={selectDraftSchedule}
          onScheduleFormChange={handleScheduleFormChange}
          onShiftFormChange={handleShiftFormChange}
          onTemplateFormChange={handleTemplateFormChange}
          onTemplateGenerationFormChange={handleTemplateGenerationFormChange}
          onTemplateSlotFormChange={handleTemplateSlotFormChange}
          publicationActionError={publicationActionError}
          publicationActionMessage={publicationActionMessage}
          publicationError={publicationError}
          publicationForm={publicationForm}
          publicationReadiness={publicationReadiness}
          reopeningScheduleId={reopeningScheduleId}
          scheduleAssignments={scheduleAssignments}
          scheduleAssignmentsError={scheduleAssignmentsError}
          scheduleCreationError={scheduleCreationError}
          selectedDraftSchedule={selectedDraftSchedule}
          selectedDraftScheduleId={selectedDraftScheduleId}
          selectedGenerationTemplate={selectedGenerationTemplate}
          scheduleForm={scheduleForm}
          selectedAssignmentSchedule={selectedAssignmentSchedule}
          selectedTemplate={selectedTemplate}
          shiftCreationError={shiftCreationError}
          shiftForm={shiftForm}
          staffingRoles={staffingRoles}
          staffingRolesError={staffingRolesError}
          templateActionError={templateActionError}
          templateActionMessage={templateActionMessage}
          templateForm={templateForm}
          templateGenerationForm={templateGenerationForm}
          templateGenerationReport={templateGenerationReport}
          templateListError={templateListError}
          templates={templates}
          templateSlotError={templateSlotError}
          templateSlotForm={templateSlotForm}
          templateSlots={templateSlots}
          templateStaffingRoles={templateStaffingRoles}
          templateStaffingRolesError={templateStaffingRolesError}
          teamEmployees={teamEmployees}
          teamEmployeesError={teamEmployeesError}
        />
      ) : null}
    </AppShell>
  );
}

export default App;
