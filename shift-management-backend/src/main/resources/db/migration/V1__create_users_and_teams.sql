CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(200) NOT NULL,
    email VARCHAR(255),
    application_role VARCHAR(30) NOT NULL CHECK (application_role IN ('EMPLOYEE', 'MANAGER')),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE teams (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    swap_approval_policy VARCHAR(30) NOT NULL CHECK (swap_approval_policy IN ('EMPLOYEE', 'MANAGER')),
    default_min_rest_hours INTEGER NOT NULL DEFAULT 8 CHECK (default_min_rest_hours >= 0),
    time_zone VARCHAR(100) NOT NULL
);

CREATE TABLE team_members (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    team_id BIGINT NOT NULL REFERENCES teams(id),
    joined_at TIMESTAMP WITH TIME ZONE NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_team_members_user_team UNIQUE (user_id, team_id)
);

CREATE TABLE team_managers (
    id BIGSERIAL PRIMARY KEY,
    manager_id BIGINT NOT NULL REFERENCES users(id),
    team_id BIGINT NOT NULL REFERENCES teams(id),
    CONSTRAINT uk_team_managers_manager_team UNIQUE (manager_id, team_id)
);
