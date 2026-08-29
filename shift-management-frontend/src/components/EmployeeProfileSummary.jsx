import { useLanguage } from "../i18n/LanguageContext.jsx";

function EmployeeProfileSummary({ isLoading, memberships, error }) {
  const { t, translateDomainValue } = useLanguage();

  if (isLoading) {
    return <p className="profile-status">{t("loadingProfile")}</p>;
  }

  if (error) {
    return <p className="profile-status profile-status-error">{error}</p>;
  }

  if (memberships.length === 0) {
    return <p className="profile-status">{t("noTeamMemberships")}</p>;
  }

  return (
    <div className="employee-profile-summary">
      <p className="eyebrow">{t("teamRoles")}</p>
      <div className="employee-profile-memberships">
        {memberships.map((membership) => (
          <div className="employee-profile-membership" key={membership.teamId}>
            <strong>{translateDomainValue(membership.teamName)}</strong>
            <div className="employee-profile-role-list">
              {membership.staffingRoleNames.length > 0 ? (
                membership.staffingRoleNames.map((roleName) => (
                  <span key={roleName}>{translateDomainValue(roleName)}</span>
                ))
              ) : (
                <span>{t("noStaffingRoles")}</span>
              )}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

export default EmployeeProfileSummary;
