CREATE TABLE schedules (
    id BIGSERIAL PRIMARY KEY,
    team_id BIGINT NOT NULL REFERENCES teams(id),
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status VARCHAR(30) NOT NULL CHECK (status IN ('DRAFT', 'PUBLISHED')),
    publication_number INTEGER NOT NULL DEFAULT 0 CHECK (publication_number >= 0),
    published_at TIMESTAMP WITH TIME ZONE,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_schedules_date_range CHECK (end_date >= start_date)
);

CREATE INDEX idx_schedules_team_dates ON schedules(team_id, start_date, end_date);
