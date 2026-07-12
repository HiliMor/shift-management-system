CREATE TABLE availability_constraints (
    id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT NOT NULL REFERENCES users(id),
    start_time TIMESTAMP WITH TIME ZONE NOT NULL,
    end_time TIMESTAMP WITH TIME ZONE NOT NULL,
    reason VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_availability_constraints_time_range CHECK (end_time > start_time)
);

CREATE INDEX idx_availability_constraints_employee_time
    ON availability_constraints(employee_id, start_time, end_time);
