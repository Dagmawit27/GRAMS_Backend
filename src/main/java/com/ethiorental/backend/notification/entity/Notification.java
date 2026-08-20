package com.ethiorental.backend.notification.entity;

import com.ethiorental.backend.shared.notification.NotificationChannel;
import com.ethiorental.backend.shared.notification.NotificationType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notifications", indexes = {
        @Index(name = "idx_notification_recipient", columnList = "recipient_user_id"),
        @Index(name = "idx_notification_read",      columnList = "recipient_user_id, is_read"),
        @Index(name = "idx_notification_created",   columnList = "created_at DESC")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Notification {

    @Id
    @UuidGenerator
    private UUID id;

    /**
     * Username (email for citizens, email for employees) used as the recipient
     * identifier for lookup — <strong>not</strong> a database FK. All publishers
     * pass the username/email, and all queries (repository, controller) look up
     * by this value. Renamed from the misleading "recipientUserId" in the
     * codebase, but the DB column name is kept for migration stability.
     */
    @Column(name = "recipient_user_id", nullable = false)
    private String recipientUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    /** Source module: PROPERTY, AGREEMENT, TAX, COMPLAINT, IAM */
    @Column(nullable = false)
    private String module;

    /** The domain entity this notification relates to (property id, agreement id, etc.) */
    @Column(name = "entity_id")
    private String entityId;

    @Column(nullable = false, length = 1000)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationChannel channel;

    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private boolean read = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) this.createdAt = Instant.now();
    }
}
