CREATE TABLE staffing_roles (
    id BIGSERIAL PRIMARY KEY,
    team_id BIGINT NOT NULL REFERENCES teams(id),
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_staffing_roles_team_name UNIQUE (team_id, name),
    CONSTRAINT chk_staffing_roles_name_not_blank CHECK (btrim(name) <> '')
);

CREATE INDEX idx_staffing_roles_team
    ON staffing_roles(team_id);
