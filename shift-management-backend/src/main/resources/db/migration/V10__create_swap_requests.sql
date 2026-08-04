CREATE TABLE swap_requests (
    id BIGSERIAL PRIMARY KEY,
    type VARCHAR(30) NOT NULL,
    requester_id BIGINT NOT NULL REFERENCES users(id),
    source_assignment_id BIGINT NOT NULL REFERENCES assignments(id),
    target_employee_id BIGINT NOT NULL REFERENCES users(id),
    target_assignment_id BIGINT REFERENCES assignments(id),
    status VARCHAR(30) NOT NULL,
    employee_approved_at TIMESTAMP WITH TIME ZONE,
    manager_approved_by BIGINT REFERENCES users(id),
    manager_approved_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_swap_requests_type CHECK (type IN ('SWAP', 'TRANSFER')),
    CONSTRAINT chk_swap_requests_status CHECK (
        status IN ('PENDING_EMPLOYEE', 'PENDING_MANAGER', 'APPROVED', 'REJECTED', 'CANCELLED', 'INVALIDATED')
    ),
    CONSTRAINT chk_swap_requests_target_assignment_for_type CHECK (
        (type = 'SWAP' AND target_assignment_id IS NOT NULL)
        OR (type = 'TRANSFER' AND target_assignment_id IS NULL)
    )
);

CREATE UNIQUE INDEX uk_swap_requests_source_assignment_active
    ON swap_requests(source_assignment_id)
    WHERE status IN ('PENDING_EMPLOYEE', 'PENDING_MANAGER');

CREATE INDEX idx_swap_requests_requester_created
    ON swap_requests(requester_id, created_at DESC);

CREATE INDEX idx_swap_requests_target_employee_status
    ON swap_requests(target_employee_id, status);
