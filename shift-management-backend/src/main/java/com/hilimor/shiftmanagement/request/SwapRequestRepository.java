package com.hilimor.shiftmanagement.request;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SwapRequestRepository extends JpaRepository<SwapRequest, Long> {

    @Query("""
            select request from SwapRequest request
            where request.id <> :requestId
              and request.status in :statuses
              and (request.sourceAssignment.id in :assignmentIds
                   or request.targetAssignment.id in :assignmentIds)
            order by request.id
            """)
    List<SwapRequest> findActiveReferencingAssignments(
            @Param("requestId") Long requestId,
            @Param("assignmentIds") Collection<Long> assignmentIds,
            @Param("statuses") Collection<SwapRequestStatus> statuses
    );

    boolean existsBySourceAssignment_IdAndStatusIn(Long sourceAssignmentId, Collection<SwapRequestStatus> statuses);

    boolean existsByTargetAssignment_IdAndStatusIn(Long targetAssignmentId, Collection<SwapRequestStatus> statuses);

    List<SwapRequest> findByRequester_UsernameOrderByCreatedAtDesc(String username);

    List<SwapRequest> findByTargetEmployee_UsernameOrderByCreatedAtDesc(String username);

    List<SwapRequest> findByStatusAndSourceAssignment_Shift_Schedule_Team_IdInOrderByCreatedAtDesc(
            SwapRequestStatus status,
            Collection<Long> teamIds
    );

    List<SwapRequest> findByStatusInAndSourceAssignment_Shift_Schedule_Team_IdInOrderByCreatedAtDesc(
            Collection<SwapRequestStatus> statuses,
            Collection<Long> teamIds
    );
}
