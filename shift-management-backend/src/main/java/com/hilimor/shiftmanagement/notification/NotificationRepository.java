package com.hilimor.shiftmanagement.notification;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByRecipient_UsernameOrderByCreatedAtDesc(String username);

    long countByRecipient_UsernameAndReadAtIsNull(String username);

    Optional<Notification> findByIdAndRecipient_Username(Long id, String username);

    Optional<Notification> findByEventIdAndRecipient_Id(UUID eventId, Long recipientId);
}
