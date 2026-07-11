CREATE TABLE assignments (
    id BIGSERIAL PRIMARY KEY,
    shift_id BIGINT NOT NULL REFERENCES shifts(id) ON DELETE CASCADE,
    employee_id BIGINT NOT NULL REFERENCES users(id),
    assigned_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_assignments_shift_employee UNIQUE (shift_id, employee_id)
);

CREATE INDEX idx_assignments_employee ON assignments(employee_id);
