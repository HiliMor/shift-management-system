package com.hilimor.shiftmanagement.request;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SwapRequestRepository extends JpaRepository<SwapRequest, Long> {

    boolean existsBySourceAssignment_IdAndStatusIn(Long sourceAssignmentId, Collection<SwapRequestStatus> statuses);

    boolean existsByTargetAssignment_IdAndStatusIn(Long targetAssignmentId, Collection<SwapRequestStatus> statuses);

    List<SwapRequest> findByRequester_UsernameOrderByCreatedAtDesc(String username);

    List<SwapRequest> findByTargetEmployee_UsernameOrderByCreatedAtDesc(String username);

    List<SwapRequest> findByStatusAndSourceAssignment_Shift_Schedule_Team_IdInOrderByCreatedAtDesc(
            SwapRequestStatus status,
            Collection<Long> teamIds
    );
}
