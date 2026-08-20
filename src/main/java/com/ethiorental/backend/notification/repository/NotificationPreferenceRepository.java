package com.ethiorental.backend.notification.repository;

import com.ethiorental.backend.notification.entity.NotificationPreference;
import com.ethiorental.backend.shared.notification.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, UUID> {

    List<NotificationPreference> findByUserId(String userId);

    Optional<NotificationPreference> findByUserIdAndType(String userId, NotificationType type);
}
