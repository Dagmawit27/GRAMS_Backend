package com.ethiorental.backend.notification.entity;

import com.ethiorental.backend.shared.notification.NotificationChannel;
import com.ethiorental.backend.shared.notification.NotificationType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "notification_preferences",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_pref_user_type",
                columnNames = {"user_id", "type"}
        ),
        indexes = @Index(name = "idx_pref_user_id", columnList = "user_id")
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class NotificationPreference {

    @Id
    @UuidGenerator
    private UUID id;

    /** Same convention as Notification.recipientUserId — username/email used for lookup, not a database FK. */
    @Column(name = "user_id", nullable = false)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    /**
     * Channels the user wants for this notification type.
     * IN_APP is always delivered regardless; this set controls optional channels.
     * If empty, only IN_APP is delivered.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "notification_preference_channels",
            joinColumns = @JoinColumn(name = "preference_id"),
            indexes = @Index(name = "idx_pref_channel_pref_id", columnList = "preference_id")
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false)
    @Builder.Default
    private Set<NotificationChannel> enabledChannels = new HashSet<>();

    /**
     * Dedicated mutator method for JPA collection update.
     * Clears and mutates the existing managed collection in-place
     * rather than replacing the collection reference.
     */
    public void updateEnabledChannels(Set<NotificationChannel> channels) {
        if (this.enabledChannels == null) {
            this.enabledChannels = new HashSet<>();
        } else {
            try {
                this.enabledChannels.clear();
            } catch (UnsupportedOperationException e) {
                this.enabledChannels = new HashSet<>();
            }
        }
        if (channels != null) {
            this.enabledChannels.addAll(channels);
        }
    }
}
