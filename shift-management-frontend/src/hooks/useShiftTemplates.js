import { useEffect, useMemo, useState } from "react";

import {
  createShiftTemplate,
  createTemplateSlot,
  deleteShiftTemplate,
  generateShiftsFromTemplate,
  listShiftTemplates,
  listStaffingRoles,
  listTemplateSlots,
} from "../api.js";

const emptyTemplateForm = {
  teamId: "",
  name: "",
  description: "",
  cycleDays: "7",
  defaultMinRestHours: "8",
};

const emptySlotForm = {
  templateId: "",
  dayOffset: "0",
  startTime: "08:00",
  durationMinutes: "480",
  description: "",
  requiredWorkers: "1",
  requiredStaffingRoleId: "",
};

const emptyGenerationForm = {
  templateId: "",
  scheduleId: "",
};

function useShiftTemplates(
  session,
  enabled,
  managedTeams,
  managedDraftSchedules,
  selectedDraftScheduleId,
  onShiftsChanged,
  onPublicationReadinessChanged,
  onApiError,
) {
  const [templateForm, setTemplateForm] = useState(emptyTemplateForm);
  const [slotForm, setSlotForm] = useState(emptySlotForm);
  const [generationForm, setGenerationForm] = useState(emptyGenerationForm);
  const [templates, setTemplates] = useState([]);
  const [templateSlots, setTemplateSlots] = useState([]);
  const [templateStaffingRoles, setTemplateStaffingRoles] = useState([]);
  const [templateGenerationReport, setTemplateGenerationReport] = useState(null);
  const [templateActionError, setTemplateActionError] = useState("");
  const [templateActionMessage, setTemplateActionMessage] = useState("");
  const [templateListError, setTemplateListError] = useState("");
  const [templateSlotError, setTemplateSlotError] = useState("");
  const [templateStaffingRolesError, setTemplateStaffingRolesError] = useState("");
  const [isLoadingTemplates, setIsLoadingTemplates] = useState(false);
  const [isLoadingTemplateSlots, setIsLoadingTemplateSlots] = useState(false);
  const [isLoadingTemplateStaffingRoles, setIsLoadingTemplateStaffingRoles] = useState(false);
  const [isCreatingTemplate, setIsCreatingTemplate] = useState(false);
  const [isCreatingTemplateSlot, setIsCreatingTemplateSlot] = useState(false);
  const [isGeneratingTemplateShifts, setIsGeneratingTemplateShifts] = useState(false);
  const [isDeletingTemplate, setIsDeletingTemplate] = useState(false);
  const [templateRefreshKey, setTemplateRefreshKey] = useState(0);
  const [templateSlotRefreshKey, setTemplateSlotRefreshKey] = useState(0);

  const selectedTemplate = useMemo(
    () => templates.find((template) => template.id.toString() === slotForm.templateId) ?? null,
    [slotForm.templateId, templates],
  );

  const selectedGenerationTemplate = useMemo(
    () => templates.find((template) => template.id.toString() === generationForm.templateId) ?? null,
    [generationForm.templateId, templates],
  );

  const generationDraftSchedules = useMemo(() => {
    if (!selectedGenerationTemplate) {
      return [];
    }

    return managedDraftSchedules.filter((schedule) => schedule.teamId === selectedGenerationTemplate.teamId);
  }, [managedDraftSchedules, selectedGenerationTemplate]);

  useEffect(() => {
    if (!enabled) {
      resetShiftTemplates();
      return;
    }

    setTemplateForm((current) => {
      const currentTeamExists = managedTeams.some((team) => team.id.toString() === current.teamId);

      return {
        ...current,
        teamId: currentTeamExists ? current.teamId : managedTeams[0]?.id?.toString() || "",
      };
    });
  }, [enabled, managedTeams]);

  useEffect(() => {
    if (!session?.accessToken || !enabled || !templateForm.teamId) {
      setTemplates([]);
      setTemplateListError("");
      setSlotForm(emptySlotForm);
      setGenerationForm(emptyGenerationForm);
      return;
    }

    setIsLoadingTemplates(true);
    setTemplateListError("");

    listShiftTemplates(session.accessToken, templateForm.teamId)
      .then((loadedTemplates) => {
        setTemplates(loadedTemplates);
        setSlotForm((current) => {
          const currentTemplateExists = loadedTemplates.some(
            (template) => template.id.toString() === current.templateId,
          );
          const nextTemplateId = currentTemplateExists ? current.templateId : loadedTemplates[0]?.id?.toString() || "";

          return {
            ...current,
            templateId: nextTemplateId,
            requiredStaffingRoleId: currentTemplateExists ? current.requiredStaffingRoleId : "",
          };
        });
        setGenerationForm((current) => {
          const currentTemplateExists = loadedTemplates.some(
            (template) => template.id.toString() === current.templateId,
          );

          return {
            templateId: currentTemplateExists ? current.templateId : loadedTemplates[0]?.id?.toString() || "",
            scheduleId: current.scheduleId,
          };
        });
      })
      .catch((error) => onApiError(error, setTemplateListError))
      .finally(() => setIsLoadingTemplates(false));
  }, [enabled, session, templateForm.teamId, templateRefreshKey]);

  useEffect(() => {
    if (!session?.accessToken || !enabled || !slotForm.templateId) {
      setTemplateSlots([]);
      setTemplateSlotError("");
      return;
    }

    setIsLoadingTemplateSlots(true);
    setTemplateSlotError("");

    listTemplateSlots(session.accessToken, slotForm.templateId)
      .then(setTemplateSlots)
      .catch((error) => onApiError(error, setTemplateSlotError))
      .finally(() => setIsLoadingTemplateSlots(false));
  }, [enabled, session, slotForm.templateId, templateSlotRefreshKey]);

  useEffect(() => {
    if (!session?.accessToken || !enabled || !selectedTemplate) {
      setTemplateStaffingRoles([]);
      setTemplateStaffingRolesError("");
      return;
    }

    setIsLoadingTemplateStaffingRoles(true);
    setTemplateStaffingRolesError("");

    listStaffingRoles(session.accessToken, selectedTemplate.teamId)
      .then((roles) => {
        setTemplateStaffingRoles(roles);
        setSlotForm((current) => {
          const currentRoleExists = roles.some((role) => role.id.toString() === current.requiredStaffingRoleId);

          return {
            ...current,
            requiredStaffingRoleId: currentRoleExists ? current.requiredStaffingRoleId : "",
          };
        });
      })
      .catch((error) => onApiError(error, setTemplateStaffingRolesError))
      .finally(() => setIsLoadingTemplateStaffingRoles(false));
  }, [enabled, selectedTemplate, session]);

  useEffect(() => {
    if (!enabled || !selectedGenerationTemplate) {
      setGenerationForm((current) => ({ ...current, scheduleId: "" }));
      return;
    }

    setGenerationForm((current) => {
      const selectedDraftExists = generationDraftSchedules.some(
        (schedule) => schedule.id.toString() === selectedDraftScheduleId,
      );

      return {
        ...current,
        scheduleId: selectedDraftExists ? selectedDraftScheduleId : "",
      };
    });
  }, [enabled, generationDraftSchedules, selectedDraftScheduleId, selectedGenerationTemplate]);

  function handleTemplateFormChange(event) {
    const { name, value } = event.target;

    setTemplateForm((current) => ({
      ...current,
      [name]: value,
    }));
    setTemplateActionError("");
    setTemplateActionMessage("");
  }

  function handleTemplateSlotFormChange(event) {
    const { name, value } = event.target;

    setSlotForm((current) => ({
      ...current,
      [name]: value,
      ...(name === "templateId" ? { requiredStaffingRoleId: "" } : {}),
    }));
    setTemplateActionError("");
    setTemplateActionMessage("");
  }

  function handleTemplateGenerationFormChange(event) {
    const { name, value } = event.target;

    setGenerationForm((current) => ({
      ...current,
      [name]: value,
      ...(name === "templateId" ? { scheduleId: "" } : {}),
    }));
    setTemplateGenerationReport(null);
    setTemplateActionError("");
    setTemplateActionMessage("");
  }

  async function submitCreateTemplate(event) {
    event.preventDefault();
    setIsCreatingTemplate(true);
    setTemplateActionError("");
    setTemplateActionMessage("");

    try {
      const template = await createShiftTemplate(session.accessToken, templateForm.teamId, {
        name: templateForm.name,
        description: templateForm.description || null,
        cycleDays: Number(templateForm.cycleDays),
        defaultMinRestHours: Number(templateForm.defaultMinRestHours),
      });

      setTemplateForm((current) => ({
        ...current,
        name: "",
        description: "",
      }));
      setSlotForm((current) => ({
        ...current,
        templateId: template.id.toString(),
        requiredStaffingRoleId: "",
      }));
      setGenerationForm((current) => ({
        ...current,
        templateId: template.id.toString(),
        scheduleId: "",
      }));
      setTemplateActionMessage({ key: "templateCreated", templateName: template.name });
      refreshTemplates();
    } catch (error) {
      onApiError(error, setTemplateActionError);
    } finally {
      setIsCreatingTemplate(false);
    }
  }

  async function submitDeleteTemplate(templateId) {
    setIsDeletingTemplate(true);
    setTemplateActionError("");
    setTemplateActionMessage("");

    try {
      await deleteShiftTemplate(session.accessToken, templateId);
      setTemplates((current) => current.filter((template) => template.id !== templateId));
      setSlotForm(emptySlotForm);
      setGenerationForm(emptyGenerationForm);
      setTemplateSlots([]);
      setTemplateGenerationReport(null);
      setTemplateActionMessage("templateDeleted");
      refreshTemplates();
    } catch (error) {
      onApiError(error, setTemplateActionError);
    } finally {
      setIsDeletingTemplate(false);
    }
  }

  async function submitCreateTemplateSlot(event) {
    event.preventDefault();
    setIsCreatingTemplateSlot(true);
    setTemplateActionError("");
    setTemplateActionMessage("");

    try {
      const slot = await createTemplateSlot(session.accessToken, slotForm.templateId, {
        dayOffset: Number(slotForm.dayOffset),
        startTime: slotForm.startTime,
        durationMinutes: Number(slotForm.durationMinutes),
        description: slotForm.description || null,
        requiredWorkers: Number(slotForm.requiredWorkers),
        requiredStaffingRoleId: slotForm.requiredStaffingRoleId ? Number(slotForm.requiredStaffingRoleId) : null,
      });

      setSlotForm((current) => ({
        ...current,
        description: "",
      }));
      setTemplateActionMessage({ key: "templateSlotCreated", slotId: slot.id });
      refreshTemplateSlots();
    } catch (error) {
      onApiError(error, setTemplateActionError);
    } finally {
      setIsCreatingTemplateSlot(false);
    }
  }

  async function submitGenerateTemplateShifts(event) {
    event.preventDefault();
    setIsGeneratingTemplateShifts(true);
    setTemplateActionError("");
    setTemplateActionMessage("");

    try {
      const report = await generateShiftsFromTemplate(
        session.accessToken,
        generationForm.templateId,
        generationForm.scheduleId,
      );

      setTemplateGenerationReport(report);
      setTemplateActionMessage({ key: "templateShiftsGenerated", shiftsCreated: report.shiftsCreated });
      onShiftsChanged(report.scheduleId);
      onPublicationReadinessChanged();
    } catch (error) {
      onApiError(error, setTemplateActionError);
    } finally {
      setIsGeneratingTemplateShifts(false);
    }
  }

  function refreshTemplates() {
    setTemplateRefreshKey((current) => current + 1);
  }

  function refreshTemplateSlots() {
    setTemplateSlotRefreshKey((current) => current + 1);
  }

  function resetShiftTemplates() {
    setTemplateForm(emptyTemplateForm);
    setSlotForm(emptySlotForm);
    setGenerationForm(emptyGenerationForm);
    setTemplates([]);
    setTemplateSlots([]);
    setTemplateStaffingRoles([]);
    setTemplateGenerationReport(null);
    setTemplateActionError("");
    setTemplateActionMessage("");
    setTemplateListError("");
    setTemplateSlotError("");
    setTemplateStaffingRolesError("");
    setIsLoadingTemplates(false);
    setIsLoadingTemplateSlots(false);
    setIsLoadingTemplateStaffingRoles(false);
    setIsCreatingTemplate(false);
    setIsCreatingTemplateSlot(false);
    setIsGeneratingTemplateShifts(false);
    setIsDeletingTemplate(false);
  }

  return {
    generationDraftSchedules,
    handleTemplateFormChange,
    handleTemplateGenerationFormChange,
    handleTemplateSlotFormChange,
    isCreatingTemplate,
    isCreatingTemplateSlot,
    isGeneratingTemplateShifts,
    isDeletingTemplate,
    isLoadingTemplateSlots,
    isLoadingTemplateStaffingRoles,
    isLoadingTemplates,
    refreshTemplateSlots,
    refreshTemplates,
    resetShiftTemplates,
    selectedGenerationTemplate,
    selectedTemplate,
    submitCreateTemplate,
    submitDeleteTemplate,
    submitCreateTemplateSlot,
    submitGenerateTemplateShifts,
    templateActionError,
    templateActionMessage,
    templateForm,
    templateGenerationForm: generationForm,
    templateGenerationReport,
    templateListError,
    templates,
    templateSlotError,
    templateSlotForm: slotForm,
    templateSlots,
    templateStaffingRoles,
    templateStaffingRolesError,
  };
}

export default useShiftTemplates;
