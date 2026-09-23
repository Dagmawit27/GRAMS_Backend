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

    @Query("SELECT COUNT(n) > 0 FROM Notification n " +
           "WHERE n.recipientUserId = :recipient " +
           "AND n.type = :type " +
           "AND n.entityId = :entityId " +
           "AND n.createdAt >= :since")
    boolean existsByRecipientAndTypeAndEntityIdSince(
            @Param("recipient") String recipient,
            @Param("type") com.ethiorental.backend.shared.notification.NotificationType type,
            @Param("entityId") String entityId,
            @Param("since") java.time.Instant since);
}

