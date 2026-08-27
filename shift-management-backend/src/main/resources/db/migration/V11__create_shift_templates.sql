CREATE TABLE shift_templates (
    id BIGSERIAL PRIMARY KEY,
    team_id BIGINT NOT NULL REFERENCES teams(id),
    name VARCHAR(150) NOT NULL,
    description VARCHAR(500),
    cycle_days INTEGER NOT NULL CHECK (cycle_days > 0),
    default_min_rest_hours INTEGER NOT NULL CHECK (default_min_rest_hours >= 0),
    active BOOLEAN NOT NULL DEFAULT FALSE,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_shift_templates_team_name UNIQUE (team_id, name),
    CONSTRAINT chk_shift_templates_name_not_blank CHECK (btrim(name) <> '')
);

CREATE UNIQUE INDEX uk_shift_templates_active_team
    ON shift_templates(team_id)
    WHERE active;

CREATE INDEX idx_shift_templates_team
    ON shift_templates(team_id);

CREATE TABLE template_slots (
    id BIGSERIAL PRIMARY KEY,
    shift_template_id BIGINT NOT NULL REFERENCES shift_templates(id) ON DELETE CASCADE,
    day_offset INTEGER NOT NULL CHECK (day_offset >= 0),
    start_time TIME NOT NULL,
    duration_minutes INTEGER NOT NULL CHECK (duration_minutes > 0),
    description VARCHAR(500),
    required_workers INTEGER NOT NULL CHECK (required_workers > 0),
    required_staffing_role_id BIGINT REFERENCES staffing_roles(id),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_template_slots_template_day_start
    ON template_slots(shift_template_id, day_offset, start_time);

CREATE INDEX idx_template_slots_required_staffing_role
    ON template_slots(required_staffing_role_id);

ALTER TABLE shifts
    ADD COLUMN template_slot_id BIGINT REFERENCES template_slots(id);

CREATE INDEX idx_shifts_template_slot
    ON shifts(template_slot_id);
