CREATE UNIQUE INDEX uk_swap_requests_target_assignment_active
    ON swap_requests(target_assignment_id)
    WHERE target_assignment_id IS NOT NULL
      AND status IN ('PENDING_EMPLOYEE', 'PENDING_MANAGER');
