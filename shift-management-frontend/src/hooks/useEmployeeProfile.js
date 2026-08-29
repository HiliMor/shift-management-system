import { useEffect, useState } from "react";

import { listMyTeamMemberships } from "../api.js";

function useEmployeeProfile(session, enabled, onApiError) {
  const [teamMemberships, setTeamMemberships] = useState([]);
  const [isLoadingTeamMemberships, setIsLoadingTeamMemberships] = useState(false);
  const [teamMembershipsError, setTeamMembershipsError] = useState("");

  useEffect(() => {
    if (!session?.accessToken || !enabled) {
      resetEmployeeProfile();
      return;
    }

    setIsLoadingTeamMemberships(true);
    setTeamMembershipsError("");

    listMyTeamMemberships(session.accessToken)
      .then(setTeamMemberships)
      .catch((error) => onApiError(error, setTeamMembershipsError))
      .finally(() => setIsLoadingTeamMemberships(false));
  }, [enabled, session]);

  function resetEmployeeProfile() {
    setTeamMemberships([]);
    setIsLoadingTeamMemberships(false);
    setTeamMembershipsError("");
  }

  return {
    isLoadingTeamMemberships,
    resetEmployeeProfile,
    teamMemberships,
    teamMembershipsError,
  };
}

export default useEmployeeProfile;
