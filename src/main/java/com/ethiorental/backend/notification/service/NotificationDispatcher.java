package com.ethiorental.backend.notification.service;

import com.ethiorental.backend.shared.notification.NotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.concurrent.Executor;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationDispatcher {

    private final NotificationListener notificationListener;
    @Qualifier("notificationExecutor")
    private final Executor notificationExecutor;

    /**
     * Dispatch notification event with transaction-commit guard.
     * If an active transaction exists, defers actual dispatch to afterCommit().
     * If no transaction is active, dispatches immediately.
     */
    public void dispatch(NotificationEvent event) {
        if (event == null) {
            log.warn("Received null NotificationEvent, skipping dispatch.");
            return;
        }

        try {
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                log.debug("Active transaction detected for event type [{}]. Registering afterCommit synchronization.", event.type());
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        notificationExecutor.execute(() -> processEvent(event));
                    }
                });
            } else {
                notificationExecutor.execute(() -> processEvent(event));
            }
        } catch (Exception ex) {
            log.error("Failed during notification dispatch for event type [{}] recipient [{}]: {}",
                    event.type(), event.recipientUserId(), ex.getMessage(), ex);
        }
    }

    private void processEvent(NotificationEvent event) {
        try {
            notificationListener.onEvent(event);
        } catch (Exception ex) {
            log.error("Delivery failure inside NotificationListener for event type [{}] recipient [{}] entity [{}]: {}",
                    event.type(), event.recipientUserId(), event.entityId(), ex.getMessage(), ex);
        }
    }
}
