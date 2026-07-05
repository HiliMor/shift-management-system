CREATE TABLE shifts (
    id BIGSERIAL PRIMARY KEY,
    schedule_id BIGINT NOT NULL REFERENCES schedules(id),
    start_time TIMESTAMP WITH TIME ZONE NOT NULL,
    end_time TIMESTAMP WITH TIME ZONE NOT NULL,
    description VARCHAR(500),
    required_workers INTEGER NOT NULL CHECK (required_workers > 0),
    min_rest_hours INTEGER NOT NULL CHECK (min_rest_hours >= 0),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_shifts_time_range CHECK (end_time > start_time)
);

CREATE INDEX idx_shifts_schedule_start_time ON shifts(schedule_id, start_time);
