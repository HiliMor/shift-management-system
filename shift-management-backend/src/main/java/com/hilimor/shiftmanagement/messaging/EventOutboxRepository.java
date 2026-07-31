package com.hilimor.shiftmanagement.messaging;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EventOutboxRepository extends JpaRepository<EventOutbox, UUID> {

    List<EventOutbox> findTop50BySentAtIsNullOrderByCreatedAtAsc();
}
