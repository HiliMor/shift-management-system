CREATE TABLE team_member_staffing_roles (
    id BIGSERIAL PRIMARY KEY,
    team_member_id BIGINT NOT NULL REFERENCES team_members(id),
    staffing_role_id BIGINT NOT NULL REFERENCES staffing_roles(id),
    assigned_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_team_member_staffing_roles_member_role UNIQUE (team_member_id, staffing_role_id)
);

CREATE INDEX idx_team_member_staffing_roles_member
    ON team_member_staffing_roles(team_member_id);

CREATE INDEX idx_team_member_staffing_roles_role
    ON team_member_staffing_roles(staffing_role_id);
