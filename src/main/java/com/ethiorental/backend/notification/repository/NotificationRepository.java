package com.ethiorental.backend.notification.repository;

import com.ethiorental.backend.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    Page<Notification> findByRecipientUserIdOrderByCreatedAtDesc(String recipientUserId, Pageable pageable);

    /** Per-user unread count — used by the notification bell. */
    long countByRecipientUserIdAndReadFalse(String recipientUserId);

    /** System-wide unread count — used by admin reporting dashboard. */
    long countByReadFalse();

    @Modifying
    @Query("UPDATE Notification n SET n.read = true WHERE n.id = :id AND n.recipientUserId = :userId")
    int markAsRead(@Param("id") UUID id, @Param("userId") String userId);

    @Modifying
    @Query("UPDATE Notification n SET n.read = true WHERE n.recipientUserId = :userId AND n.read = false")
    int markAllAsRead(@Param("userId") String userId);
}

