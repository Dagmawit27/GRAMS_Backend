package com.ethiorental.backend.notification.listener;

import com.ethiorental.backend.notification.controller.NotificationSseController;
import com.ethiorental.backend.notification.entity.Notification;
import com.ethiorental.backend.notification.repository.NotificationRepository;
import com.ethiorental.backend.shared.notification.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class AgreementGeneratedListener {

    private final NotificationRepository notificationRepository;
    private final NotificationSseController sseController;

    // This listener is currently disabled since agreement generation events
    // are now handled by the agreement module. This can be re-enabled when
    // the agreement module starts publishing its own events.
    // @Async
    // @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    // public void handleAgreementGenerated(AgreementGeneratedEvent event) {
    //     try {
    //         // Create notification for tenant
    //         Notification tenantNotification = Notification.builder()
    //                 .recipientUserId(event.getTenant().getId().toString())
    //                 .type(NotificationType.AGREEMENT_GENERATED)
    //                 .module("LEASE_AGREEMENT")
    //                 .entityId(event.getRequestCode())
    //                 .message("The landlord has generated the lease agreement for " + event.getProperty().getTitle() + ". Please review and sign the agreement.")
    //                 .channel("IN_APP")
    //                 .read(false)
    //                 .createdAt(LocalDateTime.now())
    //                 .build();
    //
    //         notificationRepository.save(tenantNotification);
    //
    //         // Send SSE notification to tenant
    //         sseController.sendNotification(
    //                 event.getTenant().getId().toString(),
    //                 tenantNotification
    //         );
    //
    //         // Send unread count update
    //         long unreadCount = notificationRepository.countByRecipientUserIdAndReadFalse(event.getTenant().getId().toString());
    //         sseController.sendUnreadCountUpdate(event.getTenant().getId().toString(), unreadCount);
    //
    //         log.info("Agreement generated notification sent to tenant for request: {}", event.getRequestCode());
    //     } catch (Exception e) {
    //         log.error("Failed to send agreement generated notification", e);
    //     }
    // }
}
