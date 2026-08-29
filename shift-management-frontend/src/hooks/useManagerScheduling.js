import { useEffect, useMemo, useState } from "react";

import {
  createAssignment,
  createSchedule,
  createShift,
  listManagedDraftSchedules,
  listMyManagedTeams,
  listScheduleAssignments,
  listStaffingRoles,
  listShifts,
  listTeamEmployees,
} from "../api.js";

const emptyScheduleForm = { teamId: "", startDate: "", endDate: "" };

const emptyShiftForm = {
  scheduleId: "",
  startTime: "",
  endTime: "",
  description: "",
  requiredWorkers: "1",
  minRestHours: "8",
  requiredStaffingRoleId: "",
};

const emptyAssignmentForm = {
  scheduleId: "",
  shiftId: "",
  employeeId: "",
};

function toIsoStringFromLocalDateTime(value) {
  return value ? new Date(value).toISOString() : "";
}

function useManagerScheduling(
  session,
  enabled,
  onApiError,
  onScheduleContentChanged = () => {},
) {
  const [managedTeams, setManagedTeams] = useState([]);
  const [isLoadingManagedTeams, setIsLoadingManagedTeams] = useState(false);
  const [managedTeamsError, setManagedTeamsError] = useState("");
  const [scheduleForm, setScheduleForm] = useState(emptyScheduleForm);
  const [createdSchedule, setCreatedSchedule] = useState(null);
  const [scheduleCreationError, setScheduleCreationError] = useState("");
  const [isCreatingSchedule, setIsCreatingSchedule] = useState(false);
  const [managedDraftSchedules, setManagedDraftSchedules] = useState([]);
  const [selectedDraftScheduleId, setSelectedDraftScheduleId] = useState("");
  const [isLoadingDraftSchedules, setIsLoadingDraftSchedules] = useState(false);
  const [draftSchedulesError, setDraftSchedulesError] = useState("");
  const [draftScheduleRefreshKey, setDraftScheduleRefreshKey] = useState(0);
  const [staffingRoles, setStaffingRoles] = useState([]);
  const [isLoadingStaffingRoles, setIsLoadingStaffingRoles] = useState(false);
  const [staffingRolesError, setStaffingRolesError] = useState("");
  const [shiftForm, setShiftForm] = useState(emptyShiftForm);
  const [createdShift, setCreatedShift] = useState(null);
  const [shiftCreationError, setShiftCreationError] = useState("");
  const [isCreatingShift, setIsCreatingShift] = useState(false);
  const [assignmentForm, setAssignmentForm] = useState(emptyAssignmentForm);
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

  const selectedDraftSchedule = useMemo(
    () => managedDraftSchedules.find((schedule) => schedule.id.toString() === selectedDraftScheduleId) ?? null,
    [managedDraftSchedules, selectedDraftScheduleId],
  );

  const selectedAssignmentSchedule = useMemo(
    () => managedDraftSchedules.find((schedule) => schedule.id.toString() === assignmentForm.scheduleId) ?? null,
    [assignmentForm.scheduleId, managedDraftSchedules],
  );

  const assignmentShiftMap = useMemo(
    () => new Map(assignmentShifts.map((shift) => [shift.id, shift])),
    [assignmentShifts],
  );

  useEffect(() => {
    const selectedScheduleExists = managedDraftSchedules.some(
      (schedule) => schedule.id.toString() === selectedDraftScheduleId,
    );
    const nextScheduleId = selectedScheduleExists
      ? selectedDraftScheduleId
      : managedDraftSchedules[0]?.id?.toString() || "";

    if (nextScheduleId !== selectedDraftScheduleId) {
      setSelectedDraftScheduleId(nextScheduleId);
      return;
    }

    if (!nextScheduleId) {
      return;
    }

    setShiftForm((current) =>
      current.scheduleId === nextScheduleId
        ? current
        : { ...current, scheduleId: nextScheduleId, requiredStaffingRoleId: "" },
    );
    setAssignmentForm((current) =>
      current.scheduleId === nextScheduleId
        ? current
        : { ...current, scheduleId: nextScheduleId, shiftId: "", employeeId: "" },
    );
  }, [managedDraftSchedules, selectedDraftScheduleId]);

  useEffect(() => {
    if (!session?.accessToken || !enabled) {
      resetManagerScheduling();
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
      .catch((error) => onApiError(error, setManagedTeamsError))
      .finally(() => setIsLoadingManagedTeams(false));
  }, [enabled, session]);

  useEffect(() => {
    if (!session?.accessToken || !enabled) {
      setManagedDraftSchedules([]);
      setDraftSchedulesError("");
      setShiftForm(emptyShiftForm);
      setAssignmentForm(emptyAssignmentForm);
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
      .catch((error) => onApiError(error, setDraftSchedulesError))
      .finally(() => setIsLoadingDraftSchedules(false));
  }, [draftScheduleRefreshKey, enabled, session]);

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
      .catch((error) => onApiError(error, setStaffingRolesError))
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
      .catch((error) => onApiError(error, setAssignmentShiftsError))
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
      .catch((error) => onApiError(error, setTeamEmployeesError))
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
      .catch((error) => onApiError(error, setScheduleAssignmentsError))
      .finally(() => setIsLoadingScheduleAssignments(false));
  }, [assignmentForm.scheduleId, assignmentRefreshKey, session]);

  function refreshDraftSchedules() {
    setDraftScheduleRefreshKey((current) => current + 1);
  }

  function refreshAssignmentData(scheduleId) {
    if (scheduleId) {
      const nextScheduleId = scheduleId.toString();
      setSelectedDraftScheduleId(nextScheduleId);
      setAssignmentForm((current) => {
        if (current.scheduleId === nextScheduleId) {
          return current;
        }

        return {
          ...current,
          scheduleId: nextScheduleId,
          shiftId: "",
          employeeId: "",
        };
      });
    }

    setAssignmentRefreshKey((current) => current + 1);
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
      setSelectedDraftScheduleId(response.id.toString());
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
      refreshDraftSchedules();
    } catch (error) {
      onApiError(error, setScheduleCreationError);
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
        requiredStaffingRoleId: shiftForm.requiredStaffingRoleId
          ? Number(shiftForm.requiredStaffingRoleId)
          : null,
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
      refreshAssignmentData();
      onScheduleContentChanged();
    } catch (error) {
      onApiError(error, setShiftCreationError);
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
      refreshAssignmentData();
      onScheduleContentChanged();
    } catch (error) {
      onApiError(error, setAssignmentCreationError);
    } finally {
      setIsCreatingAssignment(false);
    }
  }

  function resetManagerScheduling() {
    setManagedTeams([]);
    setIsLoadingManagedTeams(false);
    setManagedTeamsError("");
    setScheduleForm(emptyScheduleForm);
    setCreatedSchedule(null);
    setScheduleCreationError("");
    setIsCreatingSchedule(false);
    setManagedDraftSchedules([]);
    setSelectedDraftScheduleId("");
    setIsLoadingDraftSchedules(false);
    setDraftSchedulesError("");
    setDraftScheduleRefreshKey(0);
    setStaffingRoles([]);
    setIsLoadingStaffingRoles(false);
    setStaffingRolesError("");
    setShiftForm(emptyShiftForm);
    setCreatedShift(null);
    setShiftCreationError("");
    setIsCreatingShift(false);
    setAssignmentForm(emptyAssignmentForm);
    setAssignmentShifts([]);
    setIsLoadingAssignmentShifts(false);
    setAssignmentShiftsError("");
    setTeamEmployees([]);
    setIsLoadingTeamEmployees(false);
    setTeamEmployeesError("");
    setScheduleAssignments([]);
    setIsLoadingScheduleAssignments(false);
    setScheduleAssignmentsError("");
    setAssignmentRefreshKey(0);
    setCreatedAssignment(null);
    setAssignmentCreationError("");
    setIsCreatingAssignment(false);
  }

  return {
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
    selectDraftSchedule: setSelectedDraftScheduleId,
    selectedDraftSchedule,
    selectedDraftScheduleId,
    selectedAssignmentSchedule,
    shiftCreationError,
    shiftForm,
    staffingRoles,
    staffingRolesError,
    teamEmployees,
    teamEmployeesError,
  };
}

export default useManagerScheduling;
