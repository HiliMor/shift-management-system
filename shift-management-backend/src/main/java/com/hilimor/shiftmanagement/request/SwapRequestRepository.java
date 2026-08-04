package com.hilimor.shiftmanagement.request;

import java.util.Collection;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SwapRequestRepository extends JpaRepository<SwapRequest, Long> {

    boolean existsBySourceAssignment_IdAndStatusIn(Long sourceAssignmentId, Collection<SwapRequestStatus> statuses);
}
