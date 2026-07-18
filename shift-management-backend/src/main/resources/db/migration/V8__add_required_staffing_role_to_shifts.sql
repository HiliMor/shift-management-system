ALTER TABLE shifts
    ADD COLUMN required_staffing_role_id BIGINT REFERENCES staffing_roles(id);

CREATE INDEX idx_shifts_required_staffing_role
    ON shifts(required_staffing_role_id);
