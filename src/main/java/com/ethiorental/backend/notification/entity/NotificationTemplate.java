package com.ethiorental.backend.notification.entity;

import com.ethiorental.backend.shared.notification.NotificationChannel;
import com.ethiorental.backend.shared.notification.NotificationType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Entity
@Table(name = "notification_templates",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_template_type_channel",
                columnNames = {"type", "channel"}
        )
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class NotificationTemplate {

    @Id
    @UuidGenerator
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationChannel channel;

    /** For EMAIL channel only; null for IN_APP / SMS */
    @Column(name = "subject")
    private String subject;

    /**
     * Body template with optional placeholders:
     *   {entityId}   — replaced with NotificationEvent.entityId()
     *   {message}    — replaced with NotificationEvent.message()
     *   {module}     — replaced with NotificationEvent.module()
     */
    @Column(name = "body_template", nullable = false, length = 2000)
    private String bodyTemplate;
}
