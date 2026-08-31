export async function confirmDeletion({ loadPreview, describe, remove, confirm = (message) => window.confirm(message) }) {
  const preview = await loadPreview();
  if (!confirm(describe(preview))) {
    return false;
  }
  // Never refresh the revision and retry automatically after the user has confirmed.
  await remove(preview.revision);
  return true;
}

export function deletionErrorMessage(error, t) {
  if (error.message === "Deletion preview is out of date. Review the current data before deleting.") {
    return t("staleDeletionPreview");
  }
  if (error.message === "Schedules with transfer or swap request history cannot be deleted") {
    return t("cannotDeleteRequestHistory");
  }
  if (["Shifts", "Assignments"].some((type) =>
    error.message === `${type} with transfer or swap request history cannot be deleted`)) {
    return t("cannotDeleteAssignmentHistory");
  }
  return error.message;
}
